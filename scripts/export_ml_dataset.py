"""
Exporta do Firestore, para pesquisa e treino de modelos, os dados de quem deu o
consentimento de pesquisa no app (users/{uid}.researchConsent == true).

Regras aplicadas (não desligue sem rever a base legal — LGPD art. 11, I):
  - Só entram usuários com consentimento de pesquisa ativo, numa versão de texto
    aceita (ACCEPTED_CONSENT_VERSIONS). Quem revogou não entra nas próximas
    exportações. Exportações já feitas e modelos já treinados não mudam sozinhos.
  - Os dados de quem não consentiu nem chegam a ser lidos.
  - O userId vira um código (HMAC-SHA256 com salt secreto). Isso é pseudonimização,
    não anonimização: o CSV continua sendo dado pessoal de saúde. Guarde-o com o
    mesmo cuidado do banco e não o publique.
  - Não saem nome, e-mail, telefone, contato de emergência, mensagens de chat,
    textos livres do perfil (alergias, medicações, observações), dados do Helper,
    nem a localização exata: dos pedidos de ajuda sai só a região (grade de
    0,01°, cerca de 1 km).
  - Só dados a partir de --since (padrão 13/11/2026, 00:00 de Brasília). Check-ins
    com "formVersion" abaixo de MIN_CHECKIN_FORM_VERSION vêm de versões antigas do
    app (respostas pré-marcadas, botões rápidos invertidos no iOS, lembrete matinal
    filtrado por risco no Android) e são descartados, mesmo depois dessa data, para
    quem ainda não atualizou o app.

Pré-requisitos:
    pip install firebase-admin

Como usar:
    1. Coloque a credencial do Firebase Admin SDK em scripts/firebase-adminsdk.json
       (fora do Git; ver firebase-adminsdk.json.example).
    2. Gere um salt uma vez e guarde-o num cofre de senhas (o mesmo salt mantém os
       mesmos códigos entre exportações):
           python -c "import secrets; print(secrets.token_hex(32))"
    3. Execute:
           AFILAXY_PSEUDO_SALT=<salt> python scripts/export_ml_dataset.py
       Opções: --since AAAA-MM-DD   --out <pasta>

Saída (pasta --out, padrão scripts/ml_export_<data>/):
    participants.csv     uma linha por participante (ano de nascimento, perfil de saúde estruturado)
    checkins.csv         check-ins diários (manhã e noite)
    act.csv              Teste de Controle da Asma
    risk_snapshots.csv   retrato diário do índice de risco (inclui o rótulo candidato)
    emergencies.csv      pedidos de ajuda (gravidade, SAMU, pronto-socorro, ambiente, região)
    LEIA-ME.txt          parâmetros e contagens desta exportação
"""

import argparse
import csv
import hashlib
import hmac
import os
import sys
from datetime import datetime, timezone
from zoneinfo import ZoneInfo

try:
    import firebase_admin
    from firebase_admin import credentials, firestore
except ImportError:
    print("Erro: instale firebase-admin com:  pip install firebase-admin")
    sys.exit(1)

CREDENTIALS_PATH = "scripts/firebase-adminsdk.json"
TZ = ZoneInfo("America/Sao_Paulo")

# Data da correção do check-in (perguntas sem resposta pré-marcada e botões rápidos
# do iOS corrigidos). Dados anteriores estão enviesados para "bem".
DEFAULT_SINCE = "2026-11-13"

# Versões do texto de consentimento que cobrem os dados exportados abaixo. Se o texto
# do app mudar de sentido (nova versão em ResearchConsent.kt / ResearchConsent.swift),
# só acrescente a nova versão aqui depois de conferir que ela cobre estes dados.
ACCEPTED_CONSENT_VERSIONS = {"2026-11"}

# Versão mínima do formulário de check-in (CheckInRepositoryImpl.CHECKIN_FORM_VERSION).
# 2 ainda tinha, no Android, o lembrete matinal só em dias de risco >= 45 (resposta da
# manhã dependente do risco); 3 lembra todo dia nas duas plataformas.
MIN_CHECKIN_FORM_VERSION = 3

# Grade da região dos pedidos de ajuda: 0,01° ≈ 1,1 km (norte–sul) × ~1,0 km (leste–oeste
# nas latitudes do Brasil).
REGION_DECIMALS = 2

PARTICIPANT_FIELDS = [
    "participant", "birthYear", "consentVersion",
    # Perfil de saúde estruturado (autodeclarado, fatores de risco da GINA). Os campos de
    # texto livre (alergias, medicações, condições, observações) e os de acessibilidade
    # (cadeira de rodas, baixa visão) ficam de fora.
    "hasGerd", "hasSleepApnea", "hasRhinitis", "hasObesity",
    "hasFoodAllergy", "hasNsaidAllergy", "hasInhalantAllergy",
    "severeExacerbations12m", "severeExacerbationsInformedOn", "everIcuOrIntubated",
]

CHECKIN_FIELDS = [
    "participant", "date", "type", "hourOfDay", "dayOfWeek", "monthOfYear",
    "morningMoodGood", "morningEnergyGood", "selfCareGood", "rescueInhalerUses",
    "nighttimeAwakening", "daytimeBreathingEase", "activityAsPlanned",
    "riskScore", "aqi", "temperature", "humidity", "formVersion", "platform",
]

ACT_FIELDS = [
    "participant", "date",
    "activityImpairment", "shortnessOfBreath", "nighttimeSymptoms",
    "rescueMedicationUse", "selfRatedControl", "score",
    "oralSteroidCourse4w", "emergencyVisit4w",
]

RISK_FIELDS = [
    "participant", "date", "score", "level", "crises7d", "crises30d",
    "severeExacerbation12m",  # rótulo candidato do modelo supervisionado
    "aqi", "temperature", "humidity",
]

EMERGENCY_FIELDS = [
    "participant", "date", "hour", "status", "severity", "matched", "durationMin",
    "samuCalled", "emergencyVisitAfter",
    "envTemperature", "envHumidity", "envAqi", "envPm25",
    "regionLat", "regionLon",
]


# ── Utilidades ──────────────────────────────────────────────────────────────────

def pseudonymize(uid: str, salt: str) -> str:
    return hmac.new(salt.encode(), uid.encode(), hashlib.sha256).hexdigest()[:20]


def to_ms(value):
    """Aceita epoch em ms (int/float) ou Timestamp do Firestore (datetime)."""
    if value is None:
        return None
    if isinstance(value, (int, float)):
        return int(value)
    if isinstance(value, datetime):
        return int(value.timestamp() * 1000)
    return None


def local_dt(ms):
    return datetime.fromtimestamp(ms / 1000, tz=timezone.utc).astimezone(TZ)


def local_date(ms):
    return local_dt(ms).date().isoformat() if ms else None


def region(value):
    return round(value, REGION_DECIMALS) if isinstance(value, (int, float)) else None


def pick(data: dict, fields, **extra):
    row = {f: data.get(f) for f in fields}
    row.update(extra)
    return row


# ── Leitura por participante ────────────────────────────────────────────────────

def consenting_users(db):
    """Usuários com consentimento ativo numa versão aceita. Só eles são lidos depois."""
    try:
        from google.cloud.firestore_v1.base_query import FieldFilter
        query = db.collection("users").where(filter=FieldFilter("researchConsent", "==", True))
    except ImportError:
        query = db.collection("users").where("researchConsent", "==", True)

    accepted, other_version = [], 0
    for doc in query.stream():
        data = doc.to_dict() or {}
        if data.get("researchConsentVersion") in ACCEPTED_CONSENT_VERSIONS:
            accepted.append((doc.id, data))
        else:
            other_version += 1
    return accepted, other_version


def participant_row(pid, user):
    health = user.get("healthData") or {}
    informed_ms = to_ms(health.get("severeExacerbationsUpdatedAt"))
    birth_year = health.get("birthYear") or None  # 0 = não informado
    flags = ["hasGerd", "hasSleepApnea", "hasRhinitis", "hasObesity",
             "hasFoodAllergy", "hasNsaidAllergy", "hasInhalantAllergy",
             "severeExacerbations12m", "everIcuOrIntubated"]
    return {
        "participant": pid,
        "birthYear": birth_year,
        "consentVersion": user.get("researchConsentVersion"),
        **{f: health.get(f) for f in flags},
        "severeExacerbationsInformedOn": local_date(informed_ms) if informed_ms else None,
    }


def export_user(db, uid, pid, since_ms, counts):
    checkins, acts, risks, emergencies = [], [], [], []

    for doc in db.collection("checkins").document(uid).collection("responses").stream():
        d = doc.to_dict() or {}
        ts = to_ms(d.get("timestamp"))
        if not ts or ts < since_ms:
            continue
        if (d.get("formVersion") or 0) < MIN_CHECKIN_FORM_VERSION:
            counts["checkins_legacy_skipped"] += 1
            continue
        checkins.append(pick(d, CHECKIN_FIELDS, participant=pid, date=local_date(ts)))

    for doc in db.collection("act").document(uid).collection("results").stream():
        d = doc.to_dict() or {}
        ts = to_ms(d.get("timestamp"))
        if ts and ts >= since_ms:
            acts.append(pick(d, ACT_FIELDS, participant=pid, date=local_date(ts)))

    for doc in db.collection("risk_scores").document(uid).collection("snapshots").stream():
        d = doc.to_dict() or {}
        ts = to_ms(d.get("timestamp"))
        if ts and ts >= since_ms:
            risks.append(pick(d, RISK_FIELDS, participant=pid, date=d.get("date") or local_date(ts)))

    # Só os pedidos em que o participante pediu ajuda. Os que ele atendeu como Helper
    # são dados de saúde de outra pessoa e não entram.
    for doc in db.collection("emergency_requests").where("requesterId", "==", uid).stream():
        d = doc.to_dict() or {}
        ts = to_ms(d.get("timestamp"))
        if not ts or ts < since_ms:
            continue
        resolved = to_ms(d.get("resolvedAt"))
        emergencies.append({
            "participant": pid,
            "date": local_date(ts),
            "hour": local_dt(ts).hour,
            "status": d.get("status"),
            "severity": d.get("severity"),
            "matched": bool(d.get("helperId")) or d.get("status") in ("matched", "resolved"),
            "durationMin": round((resolved - ts) / 60000) if resolved and resolved >= ts else None,
            "samuCalled": d.get("samuCalled"),
            "emergencyVisitAfter": d.get("emergencyVisitAfter"),
            "envTemperature": d.get("envTemperature"),
            "envHumidity": d.get("envHumidity"),
            "envAqi": d.get("envAqi"),
            "envPm25": d.get("envPm25"),
            # Região, nunca o ponto exato. Usa o campo de região se um dia existir no banco.
            "regionLat": region(d.get("regionLat", d.get("latitude"))),
            "regionLon": region(d.get("regionLon", d.get("longitude"))),
        })

    return checkins, acts, risks, emergencies


def write_csv(path, fields, rows):
    with open(path, "w", newline="", encoding="utf-8") as f:
        writer = csv.DictWriter(f, fieldnames=fields, extrasaction="ignore")
        writer.writeheader()
        writer.writerows(rows)


# ── Principal ───────────────────────────────────────────────────────────────────

def main():
    parser = argparse.ArgumentParser(description=__doc__.split("\n\n")[0])
    parser.add_argument("--since", default=DEFAULT_SINCE,
                        help=f"primeiro dia incluído, AAAA-MM-DD (padrão {DEFAULT_SINCE})")
    parser.add_argument("--out", default=None, help="pasta de saída")
    args = parser.parse_args()

    salt = os.environ.get("AFILAXY_PSEUDO_SALT", "")
    if len(salt) < 32:
        print("Defina AFILAXY_PSEUDO_SALT (mínimo 32 caracteres). Gere com:\n"
              "  python -c \"import secrets; print(secrets.token_hex(32))\"")
        sys.exit(1)

    since_dt = datetime.strptime(args.since, "%Y-%m-%d").replace(tzinfo=TZ)
    since_ms = int(since_dt.timestamp() * 1000)
    out_dir = args.out or f"scripts/ml_export_{datetime.now(TZ):%Y%m%d}"
    os.makedirs(out_dir, exist_ok=True)

    firebase_admin.initialize_app(credentials.Certificate(CREDENTIALS_PATH))
    db = firestore.client()

    users, other_version = consenting_users(db)
    print(f"Participantes com consentimento de pesquisa: {len(users)}"
          + (f" (+{other_version} numa versão de texto não aceita, ignorados)" if other_version else ""))

    counts = {"checkins_legacy_skipped": 0}
    participants, checkins, acts, risks, emergencies = [], [], [], [], []
    for uid, user in users:
        pid = pseudonymize(uid, salt)
        participants.append(participant_row(pid, user))
        c, a, r, e = export_user(db, uid, pid, since_ms, counts)
        checkins += c
        acts += a
        risks += r
        emergencies += e

    write_csv(os.path.join(out_dir, "participants.csv"), PARTICIPANT_FIELDS, participants)
    write_csv(os.path.join(out_dir, "checkins.csv"), CHECKIN_FIELDS, checkins)
    write_csv(os.path.join(out_dir, "act.csv"), ACT_FIELDS, acts)
    write_csv(os.path.join(out_dir, "risk_snapshots.csv"), RISK_FIELDS, risks)
    write_csv(os.path.join(out_dir, "emergencies.csv"), EMERGENCY_FIELDS, emergencies)

    summary = [
        f"Exportação Afilaxy para pesquisa — {datetime.now(TZ):%d/%m/%Y %H:%M} (Brasília)",
        f"Dados a partir de: {since_dt:%d/%m/%Y}",
        f"Versões de consentimento aceitas: {', '.join(sorted(ACCEPTED_CONSENT_VERSIONS))}",
        f"Participantes: {len(participants)}",
        f"Check-ins: {len(checkins)} (descartados por versão antiga do formulário: "
        f"{counts['checkins_legacy_skipped']})",
        f"ACT: {len(acts)}",
        f"Retratos diários de risco: {len(risks)}",
        f"Pedidos de ajuda: {len(emergencies)} (inclui testes, se houver; não há como separá-los)",
        "",
        "Dado pessoal de saúde PSEUDONIMIZADO (não anonimizado). Não publicar nem compartilhar",
        "fora da equipe de pesquisa. Região dos pedidos de ajuda em grade de ~1 km: para",
        "mapas ou relatórios externos, publique só contagens agregadas por região.",
    ]
    with open(os.path.join(out_dir, "LEIA-ME.txt"), "w", encoding="utf-8") as f:
        f.write("\n".join(summary) + "\n")

    print("\n".join(summary[:8]))
    print(f"\nArquivos em: {out_dir}/")


if __name__ == "__main__":
    main()
