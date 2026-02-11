# Firebase Functions - Afilaxy

## 📋 Pré-requisitos

```bash
npm install -g firebase-tools
firebase login
```

## 🚀 Deploy

```bash
cd functions
npm install
firebase deploy --only functions
```

## 🔧 Como Funciona

### sendEmergencyNotification

Trigger: Quando um documento é criado em `push_notifications/`

**Fluxo:**
1. App cria emergência → `EmergencyRepositoryImpl.createEmergency()`
2. Para cada helper ativo, cria documento em `push_notifications/`
3. Cloud Function detecta novo documento
4. Busca FCM token do helper no Firestore (`users/{helperId}`)
5. Envia notificação FCM para o helper
6. Marca documento como `processed: true`

**Estrutura do documento:**
```json
{
  "to": "helperId",
  "data": {
    "type": "emergency_request",
    "emergencyId": "abc123",
    "requesterName": "João Silva",
    "title": "🆘 Emergência de Asma",
    "body": "João Silva precisa de ajuda próximo a você"
  },
  "timestamp": 1234567890,
  "processed": false
}
```

## 📱 Tipos de Notificação

- `emergency_request`: Nova emergência próxima
- `emergency_cancelled`: Emergência cancelada
- `helper_response`: Helper aceitou emergência

## 🧪 Testar Localmente

```bash
firebase emulators:start --only functions
```

## 📊 Ver Logs

```bash
firebase functions:log
```
