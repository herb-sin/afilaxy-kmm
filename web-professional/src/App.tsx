import { useState } from 'react'
import { loadStripe } from '@stripe/stripe-js'
import './App.css'

const stripePromise = loadStripe('pk_test_SEU_PUBLISHABLE_KEY')

type Period = 'quarterly' | 'semiannual' | 'annual'

interface PlanTier {
  name: string
  monthlyEquiv: { quarterly: number; semiannual: number; annual: number }
  priceIds: { quarterly: string; semiannual: string; annual: string }
  features: string[]
  popular?: boolean
  highlight: string
}

const tiers: PlanTier[] = [
  {
    name: 'Básico',
    highlight: 'Visibilidade garantida',
    monthlyEquiv: { quarterly: 99, semiannual: 89, annual: 79 },
    priceIds: {
      quarterly: 'price_basic_quarterly',
      semiannual: 'price_basic_semiannual',
      annual: 'price_basic_annual',
    },
    features: [
      'Perfil completo no app',
      'Badge de perfil verificado',
      'Contato direto via WhatsApp',
      'Indexado no Google',
      'Apoie a saúde pública',
    ],
  },
  {
    name: 'Pro',
    highlight: 'Mais popular entre especialistas',
    monthlyEquiv: { quarterly: 199, semiannual: 179, annual: 159 },
    priceIds: {
      quarterly: 'price_pro_quarterly',
      semiannual: 'price_pro_semiannual',
      annual: 'price_pro_annual',
    },
    features: [
      'Tudo do Básico',
      'Analytics de visualizações do perfil',
      'Publicação de conteúdo educativo patrocinado',
      'Prioridade no suporte',
    ],
    popular: true,
  },
  {
    name: 'Premium',
    highlight: 'Máxima exposição',
    monthlyEquiv: { quarterly: 399, semiannual: 359, annual: 319 },
    priceIds: {
      quarterly: 'price_premium_quarterly',
      semiannual: 'price_premium_semiannual',
      annual: 'price_premium_annual',
    },
    features: [
      'Tudo do Pro',
      'Analytics avançado',
      'Suporte prioritário',
      'Webinars exclusivos',
    ],
  },
]

const periodLabels: Record<Period, string> = {
  quarterly: 'Trimestral',
  semiannual: 'Semestral',
  annual: 'Anual',
}

const periodMonths: Record<Period, number> = {
  quarterly: 3,
  semiannual: 6,
  annual: 12,
}

const discountLabel: Record<Period, string | null> = {
  quarterly: null,
  semiannual: '10% off',
  annual: '20% off',
}

function App() {
  const [period, setPeriod] = useState<Period>('quarterly')
  const [loading, setLoading] = useState<string | null>(null)
  const [email, setEmail] = useState('')
  const [name, setName] = useState('')
  const [crm, setCrm] = useState('')

  const handleSubscribe = async (tier: PlanTier) => {
    if (!email || !name || !crm) {
      alert('Por favor, preencha todos os campos antes de assinar.')
      return
    }
    setLoading(tier.name)
    try {
      const priceId = tier.priceIds[period]
      const planType = `${tier.name.toUpperCase()}_${period.toUpperCase()}`
      const response = await fetch(
        'https://us-central1-SEU_PROJETO.cloudfunctions.net/createCheckoutSession',
        {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ priceId, email, metadata: { name, crm, planType } }),
        }
      )
      const { sessionId } = await response.json()
      const stripe = await stripePromise
      if (stripe) {
        const { error } = await stripe.redirectToCheckout({ sessionId })
        if (error) alert('Erro ao processar pagamento. Tente novamente.')
      }
    } catch {
      alert('Erro ao processar pagamento. Tente novamente.')
    } finally {
      setLoading(null)
    }
  }

  return (
    <div className="app">
      <header className="header">
        <div className="container">
          <h1>🫁 Afilaxy</h1>
          <p>Portal Profissional</p>
        </div>
      </header>

      <section className="hero">
        <div className="container">
          <h2>Ganhe Visibilidade e Apoie a Saúde Pública</h2>
          <p className="subtitle">
            Conecte-se com pacientes que precisam de você no momento certo.
            Apoie uma iniciativa que democratiza o acesso à saúde respiratória no Brasil.
          </p>
        </div>
      </section>

      <section className="form-section">
        <div className="container">
          <div className="form-card">
            <h3>Seus Dados</h3>
            <input type="text" placeholder="Nome completo" value={name}
              onChange={(e) => setName(e.target.value)} required />
            <input type="email" placeholder="Email profissional" value={email}
              onChange={(e) => setEmail(e.target.value)} required />
            <input type="text" placeholder="CRM / CREFITO" value={crm}
              onChange={(e) => setCrm(e.target.value)} required />
          </div>
        </div>
      </section>

      <section className="plans">
        <div className="container">
          <h2>Escolha seu Plano</h2>

          {/* Period toggle */}
          <div className="period-toggle">
            {(['quarterly', 'semiannual', 'annual'] as Period[]).map((p) => (
              <button
                key={p}
                className={`period-btn ${period === p ? 'active' : ''}`}
                onClick={() => setPeriod(p)}
              >
                {periodLabels[p]}
                {discountLabel[p] && <span className="discount-tag">{discountLabel[p]}</span>}
              </button>
            ))}
          </div>

          <div className="plans-grid">
            {tiers.map((tier) => {
              const monthly = tier.monthlyEquiv[period]
              const total = monthly * periodMonths[period]
              return (
                <div key={tier.name} className={`plan-card ${tier.popular ? 'popular' : ''}`}>
                  {tier.popular && <div className="badge">Mais Popular</div>}
                  <h3>{tier.name}</h3>
                  <p className="plan-highlight">{tier.highlight}</p>
                  <div className="price">
                    <span className="currency">R$</span>
                    <span className="amount">{monthly}</span>
                    <span className="period">/mês</span>
                  </div>
                  <p className="total-label">
                    Total: R$ {total} por {periodMonths[period]} meses
                  </p>
                  <ul className="features">
                    {tier.features.map((f, i) => <li key={i}>✓ {f}</li>)}
                  </ul>
                  <button
                    onClick={() => handleSubscribe(tier)}
                    disabled={loading !== null}
                    className={tier.popular ? 'primary' : 'secondary'}
                  >
                    {loading === tier.name ? 'Processando...' : 'Assinar Agora'}
                  </button>
                </div>
              )
            })}
          </div>
        </div>
      </section>

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
