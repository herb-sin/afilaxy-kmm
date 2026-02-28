# Scripts de Seed - Afilaxy

## Popular UBS no Firestore

### Pré-requisitos

1. Node.js instalado
2. Service Account Key do Firebase

### Como obter o Service Account Key

1. Acesse [Firebase Console](https://console.firebase.google.com)
2. Selecione seu projeto `afilaxy-app`
3. Vá em **Project Settings** (⚙️) > **Service Accounts**
4. Clique em **Generate New Private Key**
5. Salve o arquivo JSON (ex: `serviceAccountKey.json`)

### Executar o seed

```bash
cd scripts

# Instalar dependências
npm install firebase-admin

# Executar seed
node seed_ubs.js /path/to/serviceAccountKey.json
```

### Verificar no Firebase Console

Após executar, acesse:
https://console.firebase.google.com/project/afilaxy-app/firestore/data/~2Fubs

Você verá 8 UBS cadastradas em São Paulo.

### Estrutura dos dados

```json
{
  "name": "UBS Vila Mariana",
  "address": "Rua Domingos de Morais, 2187 - Vila Mariana, São Paulo - SP",
  "location": GeoPoint(-23.5880, -46.6395),
  "phone": "(11) 5549-8888",
  "openingHours": "Segunda a Sexta: 7h às 19h",
  "hasAsthmaProgram": true,
  "hasFreeMedication": true,
  "createdAt": Timestamp
}
```

### Adicionar mais UBS

Edite `ubs_seed_data.json` e execute o script novamente.
