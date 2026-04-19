# 🌐 Portal Web - Deploy Guide

## 📋 Estrutura

```
web-professional/
├── src/
│   ├── App.tsx          # Componente principal
│   ├── App.css          # Estilos
│   ├── main.tsx         # Entry point
│   └── index.css        # CSS global
├── public/              # Assets estáticos
├── index.html           # HTML principal
├── package.json         # Dependências
└── vite.config.ts       # Configuração Vite
```

---

## 🚀 Setup Local

### 1. Instalar Dependências

```bash
cd web-professional
npm install
```

### 2. Configurar Variáveis

Editar `src/App.tsx`:

```typescript
// Linha 5: Substituir pela sua chave pública do Stripe
const stripePromise = loadStripe('pk_test_SUA_CHAVE_AQUI')

// Linha 15-17: Substituir pelos Price IDs reais do Stripe
priceId: 'price_1234...' // Básico
priceId: 'price_5678...' // Pro
priceId: 'price_9012...' // Premium

// Linha 48: Substituir pelo ID do seu projeto Firebase
fetch('https://us-central1-SEU_PROJETO.cloudfunctions.net/createCheckoutSession', {
```

### 3. Rodar Localmente

```bash
npm run dev
```

Acesse: http://localhost:5173

---

## 🔧 Configurar Firebase Hosting

### 1. Atualizar firebase.json

Adicionar configuração de hosting:

```json
{
  "hosting": {
    "public": "public/professional",
    "ignore": [
      "firebase.json",
      "**/.*",
      "**/node_modules/**"
    ],
    "rewrites": [
      {
        "source": "**",
        "destination": "/index.html"
      }
    ]
  }
}
```

### 2. Build para Produção

```bash
cd web-professional
npm run build
```

Isso cria os arquivos em `public/professional/`

### 3. Deploy

```bash
# Na raiz do projeto
firebase deploy --only hosting
```

URL: `https://SEU_PROJETO.web.app/professional`

---

## 🎨 Personalização

### Cores

Editar `src/App.css`:

```css
/* Gradiente principal */
background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);

/* Cor primária */
color: #667eea;
```

### Planos

Editar `src/App.tsx`:

```typescript
const plans: Plan[] = [
  {
    name: 'Básico',
    price: 99,
    priceId: 'price_...',
    features: [
      'Feature 1',
      'Feature 2'
    ]
  }
]
```

### Textos

```typescript
// Hero
<h2>Seu título aqui</h2>
<p>Sua descrição aqui</p>

// Benefits
<h3>Benefício</h3>
<p>Descrição do benefício</p>
```

---

## 🧪 Testar Fluxo Completo

### 1. Preencher Formulário
- Nome: Dr. João Silva
- Email: joao@example.com
- CRM: 12345-SP

### 2. Escolher Plano
- Clicar em "Assinar Agora"

### 3. Stripe Checkout
- Usar cartão de teste: `4242 4242 4242 4242`
- Data: Qualquer data futura
- CVC: Qualquer 3 dígitos
- CEP: Qualquer

### 4. Verificar Firestore
- Coleção: `health_professionals`
- Documento criado com email
- `subscriptionPlan` atualizado após webhook

### 5. Verificar App Android
- Abrir tela de profissionais
- Ver profissional listado
- Ordenado por plano

---

## 📊 Monitoramento

### Logs do Checkout

```bash
firebase functions:log --only createCheckoutSession
```

### Logs do Webhook

```bash
firebase functions:log --only stripeWebhook
```

### Analytics

- Firebase Console → Analytics
- Stripe Dashboard → Payments

---

## 🔒 Segurança

### CORS

Firebase Hosting já configura CORS automaticamente.

### Validação

A Cloud Function `createCheckoutSession` valida:
- ✅ Email válido
- ✅ Campos obrigatórios
- ✅ Price ID existe no Stripe

### Dados Sensíveis

- ❌ Nunca expor Secret Key do Stripe no frontend
- ✅ Usar apenas Publishable Key
- ✅ Processar pagamentos no backend (Cloud Functions)

---

## 🎯 URLs Importantes

### Desenvolvimento
- Local: http://localhost:5173
- Emulador Functions: http://localhost:5001

### Produção
- Portal: https://SEU_PROJETO.web.app/professional
- Functions: https://us-central1-SEU_PROJETO.cloudfunctions.net

### Stripe
- Dashboard: https://dashboard.stripe.com
- Webhooks: https://dashboard.stripe.com/webhooks
- Produtos: https://dashboard.stripe.com/products

---

## ✅ Checklist de Deploy

- [ ] Dependências instaladas (`npm install`)
- [ ] Chave pública Stripe configurada
- [ ] Price IDs configurados
- [ ] URL da Cloud Function atualizada
- [ ] Build executado (`npm run build`)
- [ ] Firebase Hosting configurado
- [ ] Deploy realizado (`firebase deploy --only hosting`)
- [ ] Testado em produção (cartão de teste)
- [ ] Webhook Stripe funcionando
- [ ] Profissional aparece no app Android

---

## 🐛 Troubleshooting

### Erro: "Stripe is not defined"
- Verificar se `@stripe/stripe-js` está instalado
- Verificar chave pública no código

### Erro: "Function not found"
- Verificar se Cloud Function foi deployada
- Verificar URL da function no código

### Erro: "Invalid price ID"
- Verificar se Price ID existe no Stripe
- Verificar se está usando o correto (test vs live)

### Checkout não redireciona
- Verificar success_url e cancel_url
- Verificar se domínio está autorizado no Stripe

---

**Status:** Portal Web completo ✅  
**Próximo:** Testar fluxo end-to-end
