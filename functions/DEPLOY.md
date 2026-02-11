# 🚀 Guia de Deploy - Cloud Functions

## Pré-requisitos

- Node.js instalado ✅ (você tem v22)
- npm dependencies instaladas ✅ (já feito)

## Passos para Deploy

### 1. Instalar Firebase CLI

```bash
sudo npm install -g firebase-tools
```

### 2. Login no Firebase

```bash
firebase login
```

Isso abrirá seu navegador para autenticação.

### 3. Associar ao Projeto Firebase

```bash
firebase use --add
```

- Selecione seu projeto **Afilaxy** da lista
- Dê um alias (ex: `default`)

### 4. Build das Functions

```bash
npm run build
```

Isso compila o TypeScript para JavaScript.

### 5. Deploy!

```bash
npm run deploy
```

Ou, se preferir deploy individual:

```bash
# Deploy apenas função específica
firebase deploy --only functions:onEmergencyCreated
firebase deploy --only functions:onEmergencyAccepted
firebase deploy --only functions:onChatMessage
```

## Verificação

Após deploy bem-sucedido:

1. **Firebase Console:** https://console.firebase.google.com
   - Vá em "Functions"
   - Verifique que as 3 funções estão listadas

2. **Ver logs:**
   ```bash
   npm run logs
   ```

3. **Testar:**
   - Crie uma emergência no app
   - Verifique logs para ver se função foi triggered
   - Confirme que helpers receberam notificação

## Solução de Problemas

### Erro: "Firebase CLI not found"
```bash
sudo npm install -g firebase-tools
```

### Erro: "Project not found"
```bash
firebase use --add
```

### Erro: "Insufficient permissions"
- Verifique que seu projeto está no **Blaze Plan**
- Firebase Console → Settings → Usage and billing

### Ver logs detalhados:
```bash
firebase functions:log --only onEmergencyCreated
```

## Deploy Futuro (Rápido)

Depois da primeira configuração:

```bash
npm run build && npm run deploy
```

🎉 Pronto!
