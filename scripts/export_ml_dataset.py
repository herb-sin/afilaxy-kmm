"""
Exporta os dados do Firestore (coleção 'checkins') para CSV
pronto para uso em análise de ML no Google Colab.

Pré-requisitos:
    pip install firebase-admin pandas

Como usar:
    1. Coloque o arquivo de credenciais do Firebase Admin SDK em:
       scripts/firebase-adminsdk.json
       (copie de firebase-adminsdk.json.example e preencha com os dados reais)

    2. Execute:
       python scripts/export_ml_dataset.py

    3. Faça upload do CSV gerado para o Google Colab ou Google Drive.
"""

import csv
import hashlib
import os
import sys
from datetime import datetime, timezone

try:
    import firebase_admin
    from firebase_admin import credentials, firestore
except ImportError:
    print("Erro: instale firebase-admin com:  pip install firebase-admin")
    sys.exit(1)

try:
    import pandas as pd
    HAS_PANDAS = True
except ImportError:
    HAS_PANDAS = False

CREDENTIALS_PATH = "scripts/firebase-adminsdk.json"
OUTPUT_CSV        = "scripts/afilaxy_checkins_ml.csv"

# Salt para pseudonimização — obrigatório via variável de ambiente.
# Sem o salt o hash não pode ser revertido, mas datasets exportados
# com o mesmo salt permitem joins futuros (sem expor o userId real).
# Gere um salt forte com: python -c "import secrets; print(secrets.token_hex(32))"
PSEUDO_SALT = os.environ.get("AFILAXY_PSEUDO_SALT", "")


def pseudonymize(user_id: str) -> str:
    if not PSEUDO_SALT:
        raise RuntimeError(
            "Variável de ambiente AFILAXY_PSEUDO_SALT não definida.\n"
            "Gere um salt com:  python -c \"import secrets; print(secrets.token_hex(32))\"\n"
            "Depois execute:    AFILAXY_PSEUDO_SALT=<salt> python scripts/export_ml_dataset.py"
        )
    return hashlib.sha256(f"{PSEUDO_SALT}{user_id}".encode()).hexdigest()

# Campos a exportar (correspondentes ao modelo CheckInResponse)
FIELDS = [
    "id",
    "userId",
    "type",                 # MORNING / EVENING
    "timestamp",
    "hasRescueInhaler",
    "rescueInhalerName",
    "hadCrisisToday",       # ← variável alvo (target) do modelo ML
    "crisisSeverity",       # leve / moderada / grave
    "usedRescueInhaler",
    "riskScore",            # 0-100 (score heurístico de risco)
    "aqi",                  # Air Quality Index
    "temperature",
    "humidity",
    "hourOfDay",
    "dayOfWeek",
    "monthOfYear",
    "asmaType",
    "asmaTypeSeverity",
]


def ts_to_iso(ts: int | None) -> str | None:
    if ts is None:
        return None
    try:
        return datetime.fromtimestamp(ts / 1000, tz=timezone.utc).isoformat()
    except Exception:
        return ts


def main():
    print(f"Conectando ao Firebase usando '{CREDENTIALS_PATH}'...")
    cred = credentials.Certificate(CREDENTIALS_PATH)
    firebase_admin.initialize_app(cred)

    db = firestore.client()

    # Estrutura real: checkins/{userId}/responses/{docId}
    print("Buscando documentos via collection_group 'responses'...")
    docs = db.collection_group("responses").stream()

    rows = []
    for doc in docs:
        data = doc.to_dict()
        row = {field: data.get(field) for field in FIELDS}
        row["id"] = doc.id
        # Substitui userId pelo hash pseudônimo — o userId real nunca entra no CSV.
        row["userId"] = pseudonymize(row["userId"]) if row.get("userId") else None
        row["timestamp_iso"] = ts_to_iso(row.get("timestamp"))
        rows.append(row)

    print(f"  → {len(rows)} registros encontrados.")

    if not rows:
        print("Nenhum dado encontrado. Verifique as regras do Firestore e as credenciais.")
        return

    all_fields = FIELDS + ["timestamp_iso"]

    with open(OUTPUT_CSV, "w", newline="", encoding="utf-8") as f:
        writer = csv.DictWriter(f, fieldnames=all_fields)
        writer.writeheader()
        writer.writerows(rows)

    print(f"CSV exportado (pseudonimizado): {OUTPUT_CSV}")

    if HAS_PANDAS:
        df = pd.read_csv(OUTPUT_CSV)
        print("\n── Resumo do dataset ──────────────────────────────")
        print(f"  Shape      : {df.shape[0]} linhas × {df.shape[1]} colunas")
        print(f"  Target     : hadCrisisToday")
        print(f"  Distribuição do target:")
        print(df["hadCrisisToday"].value_counts(dropna=False).to_string())
        print(f"\n  Valores nulos por coluna:")
        print(df.isnull().sum().to_string())
        print("───────────────────────────────────────────────────")


if __name__ == "__main__":
    main()
