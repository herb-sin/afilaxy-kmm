# 🔧 Configuração Stripe + Cloud Functions

## 📋 Pré-requisitos

1. Conta Stripe (https://stripe.com)
2. Firebase CLI instalado (`npm install -g firebase-tools`)
3. Projeto Firebase configurado

---

## 🚀 Setup Stripe

### 1. Criar Conta Stripe

1. Acesse https://dashboard.stripe.com/register
2. Complete o cadastro
3. Ative o modo de teste

### 2. Criar Produtos

No Dashboard Stripe → Products → Add Product:

#### Produto 1: Afilaxy Básico
- Nome: `Afilaxy Básico`
- Preço: `R$ 99,00/mês`
- Tipo: `Recurring`
- Billing period: `Monthly`
- Copiar `Price ID` (ex: `price_1234...`)

#### Produto 2: Afilaxy Pro
- Nome: `Afilaxy Pro`
- Preço: `R$ 199,00/mês`
- Tipo: `Recurring`
- Billing period: `Monthly`
- Copiar `Price ID`

#### Produto 3: Afilaxy Premium
- Nome: `Afilaxy Premium`
- Preço: `R$ 399,00/mês`
- Tipo: `Recurring`
- Billing period: `Monthly`
- Copiar `Price ID`

### 3. Obter Chaves da API

Dashboard → Developers → API Keys:

- **Publishable key** (começa com `pk_test_...`)
- **Secret key** (começa com `sk_test_...`)

### 4. Configurar Webhook

Dashboard → Developers → Webhooks → Add endpoint:

- **URL:** `https://us-central1-SEU_PROJETO.cloudfunctions.net/stripeWebhook`
- **Events to send:**
  - `checkout.session.completed`
  - `customer.subscription.deleted`
- Copiar **Signing secret** (começa com `whsec_...`)

---

## ⚙️ Configurar Firebase Functions

### 1. Instalar Dependências

```bash
cd functions
npm install
```

### 2. Configurar Variáveis de Ambiente

```bash
firebase functions:config:set \
  stripe.secret_key="sk_test_SEU_SECRET_KEY" \
  stripe.webhook_secret="whsec_SEU_WEBHOOK_SECRET"
```

Verificar configuração:
```bash
firebase functions:config:get
```

### 3. Build TypeScript

```bash
cd functions
npm run build
```

### 4. Deploy Functions

```bash
firebase deploy --only functions
```

Ou deploy individual:
```bash
firebase deploy --only functions:stripeWebhook
firebase deploy --only functions:checkExpiredSubscriptions
```

---

## 🧪 Testar Webhook Localmente

### 1. Instalar Stripe CLI

```bash
# macOS
brew install stripe/stripe-cli/stripe

# Linux
wget https://github.com/stripe/stripe-cli/releases/download/v1.19.0/stripe_1.19.0_linux_x86_64.tar.gz
tar -xvf stripe_1.19.0_linux_x86_64.tar.gz
sudo mv stripe /usr/local/bin/
```

### 2. Login no Stripe

```bash
stripe login
```

### 3. Iniciar Emulador Firebase

```bash
firebase emulators:start --only functions
```

### 4. Forward Webhook (em outro terminal)

```bash
stripe listen --forward-to http://localhost:5001/SEU_PROJETO/us-central1/stripeWebhook
```

### 5. Testar Evento

```bash
stripe trigger checkout.session.completed
```

---

## 📊 Estrutura Firestore

### Coleção: `health_professionals`

```javascript
{
  "id": "prof123",
  "name": "Dr. João Silva",
  "specialty": "PNEUMOLOGIST", // PNEUMOLOGIST | ALLERGIST | PHYSIOTHERAPIST
  "crm": "12345-SP",
  "subscriptionPlan": "PREMIUM", // NONE | BASIC | PRO | PREMIUM
  "subscriptionExpiry": 1735689600000, // timestamp
  "stripeCustomerId": "cus_...",
  "stripeSubscriptionId": "sub_...",
  "profilePhoto": "https://...",
  "bio": "Pneumologista com 15 anos de experiência",
  "whatsapp": "11999999999",
  "phone": "11999999999",
  "clinicAddress": "Rua X, 123 - São Paulo",
  "location": {
    "latitude": -23.5505,
    "longitude": -46.6333
  },
  "rating": 4.8,
  "totalReviews": 42,
  "isVerified": true,
  "createdAt": "2025-01-01T00:00:00Z",
  "updatedAt": "2025-01-15T10:30:00Z"
}
```

---

## 🔐 Segurança

### Regras Firestore

```javascript
// firestore.rules
match /health_professionals/{professionalId} {
  // Qualquer um pode ler profissionais ativos
  allow read: if resource.data.subscriptionExpiry > request.time.toMillis();
  
  // Apenas o próprio profissional pode atualizar
  allow update: if request.auth.uid == professionalId;
  
  // Apenas admins podem criar/deletar
  allow create, delete: if request.auth.token.admin == true;
}
```

---

## 📝 Adicionar Profissional de Teste

### Via Firebase Console

1. Firestore → `health_professionals` → Add document
2. Document ID: `prof1`
3. Campos:

```json
{
  "name": "Dr. João Silva",
  "specialty": "PNEUMOLOGIST",
  "crm": "12345-SP",
  "subscriptionPlan": "PREMIUM",
  "subscriptionExpiry": 1767225600000,
  "profilePhoto": "https://i.pravatar.cc/150?img=12",
  "bio": "Pneumologista com 15 anos de experiência em asma e DPOC.",
  "whatsapp": "11999999999",
  "rating": 4.8,
  "totalReviews": 42,
  "isVerified": true
}
```

### Via Script (Node.js)

```javascript
// add-professional.js
const admin = require('firebase-admin');
admin.initializeApp();

const db = admin.firestore();

const professional = {
  name: "Dr. João Silva",
  specialty: "PNEUMOLOGIST",
  crm: "12345-SP",
  subscriptionPlan: "PREMIUM",
  subscriptionExpiry: Date.now() + (365 * 24 * 60 * 60 * 1000), // 1 ano
  profilePhoto: "https://i.pravatar.cc/150?img=12",
  bio: "Pneumologista com 15 anos de experiência.",
  whatsapp: "11999999999",
  rating: 4.8,
  totalReviews: 42,
  isVerified: true
};

db.collection('health_professionals').add(professional)
  .then(() => console.log('✅ Profissional adicionado'))
  .catch(err => console.error('❌ Erro:', err));
```

Executar:
```bash
node add-professional.js
```

---

## 🎯 Fluxo Completo

### 1. Profissional Assina
1. Acessa portal web
2. Escolhe plano (Básico/Pro/Premium)
3. Redireciona para Stripe Checkout
4. Paga com cartão

### 2. Stripe Processa
1. Pagamento aprovado
2. Envia webhook `checkout.session.completed`
3. Cloud Function `stripeWebhook` recebe

### 3. Firestore Atualiza
1. Function atualiza `subscriptionPlan` e `subscriptionExpiry`
2. Profissional aparece no app ordenado por plano

### 4. Cron Job Diário
1. Todo dia às 00:00 (horário de Brasília)
2. Function `checkExpiredSubscriptions` executa
3. Profissionais expirados voltam para `NONE`

---

## 📊 Monitoramento

### Logs das Functions

```bash
firebase functions:log
```

### Logs específicos

```bash
firebase functions:log --only stripeWebhook
firebase functions:log --only checkExpiredSubscriptions
```

### Dashboard Stripe

- Payments → Ver pagamentos
- Subscriptions → Ver assinaturas ativas
- Webhooks → Ver eventos enviados

---

## ✅ Checklist de Deploy

- [ ] Stripe configurado (produtos + webhook)
- [ ] Variáveis de ambiente configuradas
- [ ] Dependências instaladas (`npm install`)
- [ ] TypeScript compilado (`npm run build`)
- [ ] Functions deployadas (`firebase deploy --only functions`)
- [ ] Webhook testado (Stripe CLI ou evento real)
- [ ] Profissional de teste adicionado no Firestore
- [ ] App Android testado (lista de profissionais)

---

**Status:** Cloud Functions prontas ✅  
**Próximo:** Portal Web para profissionais assinarem
