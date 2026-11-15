"""
Lista dados do Firestore que pertencem a contas que não existem mais no Firebase
Authentication. Só lê: não apaga nada.

Por que existem: excluir a conta pelo app chama a função deleteAccount
(functions/src/index.ts), que apaga os dados e por último o login. Excluir pelo
console do Firebase apaga só o login, e os dados ficam no banco sem dono.

As coleções abaixo espelham OWNED_BY_ID e OWNED_BY_FIELD do deleteAccount; se a
lista de lá mudar, atualize aqui. O Storage (fotos de perfil) não é verificado.

Pré-requisitos:
    pip install firebase-admin
    Credencial do Admin SDK em scripts/firebase-adminsdk.json (fora do Git).

Como usar:
    python scripts/find_orphan_data.py

Saída: um resumo por coleção no terminal e, em scripts/orphan_uids_<data>.txt
(fora do Git), os UIDs sem conta, cada um com as coleções em que aparece.
"""

import os
import sys
from collections import defaultdict
from datetime import datetime

try:
    import firebase_admin
    from firebase_admin import auth, credentials, firestore
except ImportError:
    print("Erro: instale firebase-admin com:  pip install firebase-admin")
    sys.exit(1)

CREDENTIALS_PATH = "scripts/firebase-adminsdk.json"

# Documentos com id = uid (a leitura inclui documentos "fantasma", que só existem
# porque têm subcoleções, como checkins/{uid}/responses).
OWNED_BY_ID = [
    "users", "helpers", "medical_profiles", "checkins", "act", "risk_scores",
    "user_stats", "health_professionals", "health_professionals_billing",
]

# Documentos ligados ao usuário por um campo.
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
    ("emergency_requests", "requesterId"),
    ("emergency_requests", "helperId"),
]


def main():
    firebase_admin.initialize_app(credentials.Certificate(CREDENTIALS_PATH))
    db = firestore.client()

    live = {u.uid for u in auth.list_users().iterate_all()}
    print(f"Contas de login existentes: {len(live)}\n")

    found = defaultdict(set)   # uid órfão -> coleções em que aparece
    summary = []

    for name in OWNED_BY_ID:
        orphans = [ref.id for ref in db.collection(name).list_documents() if ref.id not in live]
        for uid in orphans:
            found[uid].add(name)
        summary.append((f"{name}/{{uid}}", len(orphans)))

    for name, field in OWNED_BY_FIELD:
        count = 0
        for doc in db.collection(name).select([field]).stream():
            uid = (doc.to_dict() or {}).get(field)
            if uid and uid not in live:
                found[uid].add(f"{name}.{field}")
                count += 1
        summary.append((f"{name} ({field})", count))

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

    print(f"\nUIDs sem conta: {len(found)} (com consentimento de pesquisa ativo: {consent})")
    if not found:
        return

    out = f"scripts/orphan_uids_{datetime.now():%Y%m%d}.txt"
    with open(out, "w", encoding="utf-8") as f:
        for uid in sorted(found):
            f.write(f"{uid}\t{', '.join(sorted(found[uid]))}\n")
    print(f"Lista em: {out}  (não versionar)")


if __name__ == "__main__":
    main()
