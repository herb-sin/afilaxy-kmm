"""
Limpa os emergency_pings de emergências já encerradas, deixando só active, status e
expiresAt (requesterId, helperId, latitude, longitude e timestamp viram null).

Por quê: qualquer usuário autenticado lê emergency_pings. Desde a mudança em
onEmergencyRequestWrite (functions/src/index.ts), o ping é reduzido assim que a
emergência encerra; este script aplica a mesma redução aos pings antigos, que ainda
guardam quem pediu ajuda, onde (~111 m) e quando.

Pré-requisitos:
    pip install firebase-admin
    Credencial: scripts/firebase-adminsdk.json (fora do Git) ou, sem ela, o login do
    Google do terminal — no Cloud Shell funciona direto, sem criar chave.

Como usar (na raiz do repositório):
    python3 scripts/strip_inactive_pings.py           # só conta
    python3 scripts/strip_inactive_pings.py --apply   # pede confirmação e limpa
"""

import argparse
import os
import sys

try:
    import firebase_admin
    from firebase_admin import credentials, firestore
except ImportError:
    print("Erro: instale firebase-admin com:  pip install firebase-admin")
    sys.exit(1)

CREDENTIALS_PATH = "scripts/firebase-adminsdk.json"
PROJECT_ID = "afilaxy-app"
CLEARED_FIELDS = ("requesterId", "helperId", "latitude", "longitude", "timestamp")
BATCH_LIMIT = 400


def main():
    parser = argparse.ArgumentParser(description=__doc__.split("\n\n")[0])
    parser.add_argument("--apply", action="store_true", help="limpa (pede confirmação antes)")
    args = parser.parse_args()

    # Sem o arquivo de chave, usa o login do Google do terminal (Application Default
    # Credentials) — no Cloud Shell já vem pronto e dispensa criar chave de conta de serviço.
    if os.path.exists(CREDENTIALS_PATH):
        cred = credentials.Certificate(CREDENTIALS_PATH)
    else:
        cred = credentials.ApplicationDefault()
    firebase_admin.initialize_app(cred, {"projectId": PROJECT_ID})
    db = firestore.client()

    total = active = 0
    pending = []
    for doc in db.collection("emergency_pings").stream():
        total += 1
        data = doc.to_dict() or {}
        if data.get("active") is True:
            active += 1
            continue
        if any(data.get(field) is not None for field in CLEARED_FIELDS):
            pending.append(doc.reference)

    print(f"Pings: {total} · ativos (não mexe): {active} · encerrados com dados a limpar: {len(pending)}")
    if not pending or not args.apply:
        return

    word = f"LIMPAR {len(pending)}"
    if input(f'Para confirmar, digite exatamente "{word}": ').strip() != word:
        print("Cancelado. Nada foi alterado.")
        return

    update = {field: None for field in CLEARED_FIELDS}
    update["active"] = False
    for i in range(0, len(pending), BATCH_LIMIT):
        batch = db.batch()
        for ref in pending[i:i + BATCH_LIMIT]:
            batch.update(ref, update)
        batch.commit()
    print(f"Limpos: {len(pending)}")


if __name__ == "__main__":
    main()
