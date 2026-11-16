"""
Lista, e com --delete apaga, dados do Firestore e do Storage que pertencem a contas
que não existem mais no Firebase Authentication.

Por que existem: excluir a conta pelo app chama a função deleteAccount
(functions/src/index.ts), que apaga os dados e por último o login. Excluir pelo
console do Firebase apaga só o login, e os dados ficam no banco sem dono.

As coleções e as regras abaixo espelham o deleteAccount (OWNED_BY_ID, OWNED_BY_FIELD,
emergências e Storage); se ele mudar, atualize aqui. Com --delete, para cada conta:
  - pedidos de ajuda que ela fez: apaga o pedido e o chat inteiro;
  - atendimentos que ela fez a outra pessoa: o registro fica (é histórico de quem pediu),
    sem helperId e com helperName "Conta excluída"; se ainda estava ativo, é cancelado;
    as mensagens dela no chat são apagadas;
  - demais documentos dela (por campo e com id = uid, com subcoleções) e fotos no Storage.
Só entram ids com formato de UID do Authentication (28 caracteres); os outros (ex.: id
automático de 20 caracteres do Firestore) são listados à parte e nunca apagados.
Uma conta com assinatura no Stripe é pulada: cancele a assinatura antes.

Pré-requisitos:
    pip install firebase-admin
    Credencial do Admin SDK em scripts/firebase-adminsdk.json (fora do Git).

Como usar (na raiz do repositório):
    python3 scripts/find_orphan_data.py            # só lista
    python3 scripts/find_orphan_data.py --delete   # lista, pede confirmação e apaga

Saída: um resumo por coleção no terminal e, em scripts/orphan_uids_<data>.txt
(fora do Git), os ids sem conta, cada um com as coleções em que aparece.
"""

import argparse
import sys
from collections import defaultdict
from datetime import datetime

try:
    import firebase_admin
    from firebase_admin import auth, credentials, firestore, storage
    from google.cloud.firestore_v1.base_query import FieldFilter
except ImportError:
    print("Erro: instale firebase-admin com:  pip install firebase-admin")
    sys.exit(1)

CREDENTIALS_PATH = "scripts/firebase-adminsdk.json"
STORAGE_BUCKET = "afilaxy-app.firebasestorage.app"
UID_LENGTH = 28
BATCH_LIMIT = 400

# Documentos com id = uid (a leitura inclui documentos "fantasma", que só existem
# porque têm subcoleções, como checkins/{uid}/responses).
OWNED_BY_ID = [
    "users", "helpers", "medical_profiles", "checkins", "act", "risk_scores",
    "user_stats", "health_professionals", "health_professionals_billing",
]

# Documentos que pertencem ao usuário por um campo (apagados).
OWNED_BY_FIELD = [
    ("medications", "userId"),
    ("emergency_contacts", "userId"),
    ("medical_exams", "userId"),
    ("notifications", "userId"),
    ("nps_responses", "userId"),
    ("consultation_interest", "userId"),
    ("ratings", "userId"),
    ("ratings", "reviewedId"),
    ("reviews", "reviewerId"),
    ("reviews", "reviewedId"),
]

# Emergências: verificadas, mas com regra própria na exclusão (ver delete_account_data).
EMERGENCY_FIELDS = [
    ("emergency_requests", "requesterId"),
    ("emergency_requests", "helperId"),
]

STORAGE_PREFIXES = ("users/{uid}/", "professionals/{uid}/")


# ── Busca ──────────────────────────────────────────────────────────────────────

def find_orphans(db, live):
    """Retorna ({id órfão: coleções em que aparece}, [(rótulo, contagem)])."""
    found = defaultdict(set)
    summary = []

    for name in OWNED_BY_ID:
        orphans = [ref.id for ref in db.collection(name).list_documents() if ref.id not in live]
        for uid in orphans:
            found[uid].add(name)
        summary.append((f"{name}/{{uid}}", len(orphans)))

    for name, field in OWNED_BY_FIELD + EMERGENCY_FIELDS:
        count = 0
        for doc in db.collection(name).select([field]).stream():
            uid = (doc.to_dict() or {}).get(field)
            if uid and uid not in live:
                found[uid].add(f"{name}.{field}")
                count += 1
        summary.append((f"{name} ({field})", count))

    return found, summary


# ── Exclusão (espelho do deleteAccount) ─────────────────────────────────────────

def commit_in_batches(db, ops):
    for i in range(0, len(ops), BATCH_LIMIT):
        batch = db.batch()
        for op in ops[i:i + BATCH_LIMIT]:
            op(batch)
        batch.commit()


def delete_account_data(db, bucket, uid):
    """Apaga os dados de uma conta que já não existe. Idempotente: rodar de novo só
    não encontra o que já saiu."""
    deletes = {}   # caminho -> ref (o mesmo documento pode aparecer em duas consultas)
    updates = []
    emergencies = db.collection("emergency_requests")

    # 1. Pedidos de ajuda feitos pela conta: pedido + chat inteiro.
    as_requester = list(emergencies.where(filter=FieldFilter("requesterId", "==", uid)).stream())
    for doc in as_requester:
        db.recursive_delete(db.collection("emergency_chats").document(doc.id))
        deletes[doc.reference.path] = doc.reference

    # 2. Atendimentos a outras pessoas: anonimiza e apaga só as mensagens da conta.
    anonymized = 0
    for doc in emergencies.where(filter=FieldFilter("helperId", "==", uid)).stream():
        data = doc.to_dict() or {}
        if data.get("requesterId") == uid:
            continue  # já sai inteiro no passo 1
        update = {"helperId": firestore.DELETE_FIELD, "helperName": "Conta excluída"}
        if data.get("active") is True:
            update.update({"active": False, "status": "cancelled"})
        updates.append((doc.reference, update))
        anonymized += 1
        messages = (db.collection("emergency_chats").document(doc.id).collection("messages")
                    .where(filter=FieldFilter("senderId", "==", uid)).stream())
        for msg in messages:
            deletes[msg.reference.path] = msg.reference

    # 3. Documentos ligados por campo.
    for name, field in OWNED_BY_FIELD:
        for doc in db.collection(name).where(filter=FieldFilter(field, "==", uid)).stream():
            deletes[doc.reference.path] = doc.reference
    rate_limit = db.collection("rate_limits").document(f"getNearbyHelpers_{uid}")
    deletes[rate_limit.path] = rate_limit

    ops = [lambda b, r=ref, d=data: b.update(r, d) for ref, data in updates]
    ops += [lambda b, r=ref: b.delete(r) for ref in deletes.values()]
    commit_in_batches(db, ops)

    # 4. Documentos com id = uid, com subcoleções.
    for name in OWNED_BY_ID:
        db.recursive_delete(db.collection(name).document(uid))

    # 5. Arquivos (fotos de perfil). Sem bucket (Storage não ativado) não há o que apagar.
    files = 0
    for prefix in (STORAGE_PREFIXES if bucket is not None else ()):
        for blob in bucket.list_blobs(prefix=prefix.format(uid=uid)):
            blob.delete()
            files += 1

    return len(as_requester), anonymized, files


def has_stripe_subscription(db, uid):
    billing = db.collection("health_professionals_billing").document(uid).get()
    return bool((billing.to_dict() or {}).get("stripeSubscriptionId"))


# ── Execução ───────────────────────────────────────────────────────────────────

def main():
    parser = argparse.ArgumentParser(description=__doc__.split("\n\n")[0])
    parser.add_argument("--delete", action="store_true",
                        help="apaga os dados encontrados (pede confirmação antes)")
    args = parser.parse_args()

    firebase_admin.initialize_app(credentials.Certificate(CREDENTIALS_PATH),
                                  {"storageBucket": STORAGE_BUCKET})
    db = firestore.client()

    live = {u.uid for u in auth.list_users().iterate_all()}
    print(f"Contas de login existentes: {len(live)}\n")

    found, summary = find_orphans(db, live)

    width = max(len(label) for label, _ in summary)
    print("Documentos sem conta de login, por coleção:")
    for label, count in summary:
        print(f"  {label.ljust(width)}  {count}")

    # Quem consentiu com a pesquisa entraria no export se não fosse a checagem de
    # conta em export_ml_dataset.py; vale saber quantos são.
    consent = sum(
        1 for uid in found if "users" in found[uid]
        and (db.collection("users").document(uid).get().to_dict() or {}).get("researchConsent") is True
    )
    print(f"\nIds sem conta: {len(found)} (com consentimento de pesquisa ativo: {consent})")
    if not found:
        return

    out = f"scripts/orphan_uids_{datetime.now():%Y%m%d}.txt"
    with open(out, "w", encoding="utf-8") as f:
        for uid in sorted(found):
            f.write(f"{uid}\t{', '.join(sorted(found[uid]))}\n")
    print(f"Lista em: {out}  (não versionar)")

    if not args.delete:
        return

    targets = sorted(uid for uid in found if len(uid) == UID_LENGTH)
    not_uid = sorted(uid for uid in found if len(uid) != UID_LENGTH)
    if not_uid:
        print("\nFora da exclusão (id não tem formato de UID, confira no console):")
        for uid in not_uid:
            print(f"  {uid}  ({', '.join(sorted(found[uid]))})")
    stripe = [uid for uid in targets if has_stripe_subscription(db, uid)]
    if stripe:
        print(f"\nPuladas por terem assinatura no Stripe (cancele antes): {len(stripe)}")
        targets = [uid for uid in targets if uid not in stripe]
    if not targets:
        print("\nNada a apagar.")
        return

    word = f"APAGAR {len(targets)}"
    print(f"\nVão ser apagados os dados de {len(targets)} contas sem login (lista no arquivo acima).")
    print("Isso não pode ser desfeito.")
    if input(f'Para confirmar, digite exatamente "{word}": ').strip() != word:
        print("Cancelado. Nada foi apagado.")
        return

    bucket = storage.bucket()
    if not bucket.exists():
        print(f"\nO bucket {STORAGE_BUCKET} não existe (Storage não ativado): não há arquivos a apagar.")
        bucket = None
    done, failed = 0, []
    total_requests = total_anonymized = total_files = 0
    for uid in targets:
        try:
            requests, anonymized, files = delete_account_data(db, bucket, uid)
        except Exception as error:  # segue com as outras; rodar de novo retoma
            failed.append(uid)
            print(f"  falha em {uid}: {error}")
            continue
        done += 1
        total_requests += requests
        total_anonymized += anonymized
        total_files += files

    print(f"\nContas limpas: {done} de {len(targets)}"
          f" · pedidos de ajuda apagados: {total_requests}"
          f" · atendimentos anonimizados: {total_anonymized}"
          f" · arquivos apagados: {total_files}")
    if failed:
        print(f"Falharam {len(failed)}; rode de novo para tentar só o que sobrou.")


if __name__ == "__main__":
    main()
