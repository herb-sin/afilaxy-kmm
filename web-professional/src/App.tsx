import { useState } from 'react'
import { loadStripe } from '@stripe/stripe-js'
import './App.css'

// Substituir pela sua chave pública do Stripe
const stripePromise = loadStripe('pk_test_SEU_PUBLISHABLE_KEY')

interface Plan {
  name: string
  price: number
  priceId: string
  features: string[]
  popular?: boolean
}

const plans: Plan[] = [
  {
    name: 'Básico',
    price: 99,
    priceId: 'price_basic', // Substituir pelo Price ID real do Stripe
    features: [
      'Perfil listado no app',
      'Badge verificado',
      'Contato direto via WhatsApp',
      'Apoie a saúde pública'
    ]
  },
  {
    name: 'Pro',
    price: 199,
    priceId: 'price_pro',
    features: [
      'Tudo do Básico',
      'Destaque na lista',
      'Badge Pro',
      'Analytics básico',
      'Prioridade no suporte'
    ],
    popular: true
  },
  {
    name: 'Premium',
    price: 399,
    priceId: 'price_premium',
    features: [
      'Tudo do Pro',
      'Topo da lista',
      'Badge Premium ⭐',
      'Analytics avançado',
      'Suporte prioritário',
      'Webinars exclusivos'
    ]
  }
]

function App() {
  const [loading, setLoading] = useState<string | null>(null)
  const [email, setEmail] = useState('')
  const [name, setName] = useState('')
  const [crm, setCrm] = useState('')

  const handleSubscribe = async (plan: Plan) => {
    if (!email || !name || !crm) {
      alert('Por favor, preencha todos os campos antes de assinar.')
      return
    }

    setLoading(plan.name)

    try {
      // Chamar Cloud Function para criar sessão de checkout
      const response = await fetch('https://us-central1-SEU_PROJETO.cloudfunctions.net/createCheckoutSession', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          priceId: plan.priceId,
          email,
          metadata: {
            name,
            crm,
            planType: plan.name.toUpperCase()
          }
        })
      })

      const { sessionId } = await response.json()
      const stripe = await stripePromise

      if (stripe) {
        const { error } = await stripe.redirectToCheckout({ sessionId })
        if (error) {
          console.error('Erro no checkout:', error)
          alert('Erro ao processar pagamento. Tente novamente.')
        }
      }
    } catch (error) {
      console.error('Erro:', error)
      alert('Erro ao processar pagamento. Tente novamente.')
    } finally {
      setLoading(null)
    }
  }

  return (
    <div className="app">
      {/* Header */}
      <header className="header">
        <div className="container">
          <h1>🫁 Afilaxy</h1>
          <p>Portal Profissional</p>
        </div>
      </header>

      {/* Hero */}
      <section className="hero">
        <div className="container">
          <h2>Ganhe Visibilidade e Apoie a Saúde Pública</h2>
          <p className="subtitle">
            Conecte-se com pacientes que precisam de você. Apoie uma iniciativa que democratiza o acesso à saúde respiratória no Brasil.
          </p>
        </div>
      </section>

      {/* Form */}
      <section className="form-section">
        <div className="container">
          <div className="form-card">
            <h3>Seus Dados</h3>
            <input
              type="text"
              placeholder="Nome completo"
              value={name}
              onChange={(e) => setName(e.target.value)}
              required
            />
            <input
              type="email"
              placeholder="Email profissional"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              required
            />
            <input
              type="text"
              placeholder="CRM/CREFITO"
              value={crm}
              onChange={(e) => setCrm(e.target.value)}
              required
            />
          </div>
        </div>
      </section>

      {/* Plans */}
      <section className="plans">
        <div className="container">
          <h2>Escolha seu Plano</h2>
          <div className="plans-grid">
            {plans.map((plan) => (
              <div key={plan.name} className={`plan-card ${plan.popular ? 'popular' : ''}`}>
                {plan.popular && <div className="badge">Mais Popular</div>}
                <h3>{plan.name}</h3>
                <div className="price">
                  <span className="currency">R$</span>
                  <span className="amount">{plan.price}</span>
                  <span className="period">/mês</span>
                </div>
                <ul className="features">
                  {plan.features.map((feature, i) => (
                    <li key={i}>✓ {feature}</li>
                  ))}
                </ul>
                <button
                  onClick={() => handleSubscribe(plan)}
                  disabled={loading !== null}
                  className={plan.popular ? 'primary' : 'secondary'}
                >
                  {loading === plan.name ? 'Processando...' : 'Assinar Agora'}
                </button>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* Benefits */}
      <section className="benefits">
        <div className="container">
          <h2>Por que Assinar?</h2>
          <div className="benefits-grid">
            <div className="benefit">
              <div className="icon">👥</div>
              <h3>Visibilidade</h3>
              <p>Apareça para pacientes que precisam de ajuda no momento certo</p>
            </div>
            <div className="benefit">
              <div className="icon">🎯</div>
              <h3>Leads Qualificados</h3>
              <p>Conecte-se com pacientes realmente interessados em tratamento</p>
            </div>
            <div className="benefit">
              <div className="icon">❤️</div>
              <h3>Impacto Social</h3>
              <p>Apoie uma iniciativa que democratiza o acesso à saúde no Brasil</p>
            </div>
            <div className="benefit">
              <div className="icon">📊</div>
              <h3>Analytics</h3>
              <p>Acompanhe visualizações e contatos gerados pelo seu perfil</p>
            </div>
          </div>
        </div>
      </section>

      {/* Footer */}
      <footer className="footer">
        <div className="container">
          <p>© 2025 Afilaxy - Health Equity para Asma</p>
          <p>Desenvolvido com ❤️ para democratizar o acesso à saúde respiratória no Brasil 🇧🇷</p>
        </div>
      </footer>
    </div>
  )
}

export default App
