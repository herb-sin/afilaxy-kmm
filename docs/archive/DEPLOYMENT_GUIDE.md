# 🚀 Guia de Deployment Seguro - Afilaxy

## Configuração de Variáveis de Ambiente para Produção

### 1. Plataformas de Hosting

#### Vercel
```bash
# Via CLI
vercel env add VITE_FIREBASE_API_KEY
vercel env add VITE_FIREBASE_AUTH_DOMAIN
vercel env add VITE_FIREBASE_PROJECT_ID
vercel env add VITE_FIREBASE_STORAGE_BUCKET
vercel env add VITE_FIREBASE_MESSAGING_SENDER_ID
vercel env add VITE_FIREBASE_APP_ID

# Via Dashboard: Settings > Environment Variables
```

#### Netlify
```bash
# Via CLI
netlify env:set VITE_FIREBASE_API_KEY "sua_chave_aqui"
netlify env:set VITE_FIREBASE_AUTH_DOMAIN "afilaxy-app.firebaseapp.com"
netlify env:set VITE_FIREBASE_PROJECT_ID "afilaxy-app"
netlify env:set VITE_FIREBASE_STORAGE_BUCKET "afilaxy-app.firebasestorage.app"
netlify env:set VITE_FIREBASE_MESSAGING_SENDER_ID "19540410113"
netlify env:set VITE_FIREBASE_APP_ID "1:19540410113:web:eb0cc4543701d9f3b9e500"

# Via Dashboard: Site Settings > Environment Variables
```

#### Firebase Hosting
```bash
# firebase.json
{
  "hosting": {
    "headers": [
      {
        "source": "**",
        "headers": [
          {
            "key": "X-Content-Type-Options",
            "value": "nosniff"
          }
        ]
      }
    ]
  }
}

# Use Firebase Functions para variáveis sensíveis
firebase functions:config:set firebase.api_key="sua_chave_aqui"
```

### 2. Configuração de Segurança do Firebase

#### Restrições de API Key
1. Acesse o [Google Cloud Console](https://console.cloud.google.com/)
2. Navegue para "APIs & Services" > "Credentials"
3. Clique na sua API Key
4. Configure as restrições:

**Restrições de Aplicação:**
- Selecione "HTTP referrers (web sites)"
- Adicione seus domínios:
  ```
  https://afilaxy.com/*
  https://*.afilaxy.com/*
  https://afilaxy-app.web.app/*
  https://afilaxy-app.firebaseapp.com/*
  ```

**Restrições de API:**
- Selecione "Restrict key"
- Habilite apenas as APIs necessárias:
  - Identity and Access Management (IAM) API
  - Firebase Authentication API
  - Cloud Firestore API
  - Firebase Storage API

### 3. Monitoramento e Alertas

#### Google Cloud Monitoring
```bash
# Configurar alertas para uso anômalo da API
gcloud alpha monitoring policies create --policy-from-file=monitoring-policy.yaml
```

#### Firebase Analytics
- Configure eventos personalizados para monitorar autenticação
- Implemente logging de segurança para tentativas de acesso

### 4. Rotação de Chaves

#### Script de Rotação Mensal
```bash
#!/bin/bash
# rotate-api-keys.sh

echo "🔄 Iniciando rotação de chaves API..."

# 1. Gerar nova chave no Firebase Console
# 2. Atualizar variáveis de ambiente
# 3. Fazer deploy
# 4. Testar funcionalidade
# 5. Revogar chave antiga

echo "✅ Rotação concluída"
```

### 5. Checklist de Segurança

- [ ] ✅ Variáveis de ambiente configuradas
- [ ] ✅ API Key com restrições aplicadas
- [ ] ✅ Domínios autorizados configurados
- [ ] ✅ Monitoramento ativo
- [ ] ✅ Backup das configurações
- [ ] ✅ Documentação atualizada
- [ ] ✅ Equipe treinada

### 6. Comandos de Build Seguro

```bash
# Validar ambiente antes do build
bash scripts/validate-env.sh

# Build para produção
npm run build

# Deploy com verificação
npm run deploy:prod
```

### 7. Troubleshooting

#### Erro: "Firebase configuration missing"
```bash
# Verificar se todas as variáveis estão definidas
env | grep VITE_FIREBASE

# Recriar .env.local se necessário
cp .env.example .env.local
```

#### Erro: "API key restrictions"
- Verificar se o domínio está na lista de referrers autorizados
- Confirmar se as APIs necessárias estão habilitadas

### 8. Contatos de Emergência

**Responsável pela Segurança:** Herbert Jung Sin  
**Email:** afilaxy@gmail.com  
**Procedimento de Incidente:** Seguir SECURITY_FIX.md