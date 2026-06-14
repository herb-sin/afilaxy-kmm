# 🫁 Afilaxy — Plataforma de Gestão de Asma

[![Android CI](https://github.com/herb-sin/afilaxy-kmm/actions/workflows/android-build.yml/badge.svg)](https://github.com/herb-sin/afilaxy-kmm/actions)
[![Shared Tests](https://github.com/herb-sin/afilaxy-kmm/actions/workflows/shared-tests.yml/badge.svg)](https://github.com/herb-sin/afilaxy-kmm/actions)
[![iOS Build](https://github.com/herb-sin/afilaxy-kmm/actions/workflows/ios-build.yml/badge.svg)](https://github.com/herb-sin/afilaxy-kmm/actions)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-blue.svg)](https://kotlinlang.org)
[![KMM](https://img.shields.io/badge/KMM-Ready-brightgreen.svg)](https://kotlinlang.org/docs/multiplatform.html)
[![Version](https://img.shields.io/badge/version-2.3.0-orange.svg)](https://github.com/herb-sin/afilaxy-kmm/releases)

A única plataforma digital de gestão de asma em português no Brasil. O Afilaxy preenche o vazio entre as consultas médicas — onde a maioria das crises acontece — conectando pacientes a dados, especialistas e boas práticas médicas antes que a emergência aconteça.

---

## 🎯 Visão

### O Problema

O sistema de saúde "perde" o asmático entre as consultas, gerando uma gestão **reativa** da doença. O resultado visível é a **"automedicação social"**: 18% dos pacientes já pediram ou emprestaram uma bombinha em uma emergência — um sinal de que milhões de brasileiros gerenciam sua condição sem dados ou vínculo com especialistas, onerando o sistema apenas em crises graves.

- A asma afeta **20 milhões de brasileiros** e causa **2.000+ mortes anuais**
- Internações evitáveis custam ao SUS entre **R$ 600 milhões e R$ 1 bilhão/ano**
- O problema central não é a falta de medicação — é a **baixa adesão ao tratamento** e o **isolamento clínico**

### A Solução

O Afilaxy transforma a urgência individual em gestão contínua:

1. **Porta de entrada:** Localização P2P de inalador de resgate em emergências
2. **Engajamento:** Check-ins de sintomas, registro de medicação e fatores ambientais
3. **Prevenção:** Algoritmo que analisa o perfil do paciente e antecipa riscos de crise
4. **Dados:** Geração de Real-World Evidence (RWE) para otimizar a gestão populacional

### O Diferencial

> "Inovação não é criar uma molécula nova, é inovar no modelo de acesso."

Enquanto o mercado global vende mais medicamentos, o Afilaxy garante que o investimento público chegue ao paciente e seja usado corretamente — sendo a **única plataforma de gestão de asma em português no país**.

---

## 🏗️ Arquitetura

### Kotlin Multiplatform Mobile (KMM)

**80%+ de código compartilhado** entre Android e iOS usando Clean Architecture + MVVM.

```
shared/
├── commonMain/          # 📦 Código compartilhado (Android + iOS)
│   ├── domain/         # 🎯 Regras de negócio
│   │   ├── model/      # Emergency, CheckIn, RiskScore, HealthProfessional...
│   │   ├── repository/ # Interfaces (EmergencyRepository, LocationRepository...)
│   │   └── usecase/    # CreateEmergencyUseCase
│   ├── data/           # 💾 Implementações (Firestore, GPS, Preferences)
│   ├── presentation/   # 🎨 13 ViewModels compartilhados
│   ├── util/           # Logger multiplataforma, TimeUtils
│   └── di/             # 💉 Koin modules
├── androidMain/        # 🤖 AndroidLogger, AndroidPlatform
└── iosMain/            # 🍎 IOSLogger, IOSPlatform
```

📐 **Diagramas detalhados**: [ARCHITECTURE.md](ARCHITECTURE.md)

### Stack Tecnológico

| Tecnologia | Versão | Uso |
|-----------|--------|-----|
| **Kotlin** | 2.0.21 | Linguagem principal (shared + Android) |
| **Swift / SwiftUI** | — | UI iOS nativa |
| **Firebase** | BOM 33.9.0 | Auth, Firestore, Cloud Messaging, Analytics |
| **Koin** | 3.5.6 | Dependency Injection (Android + KMM) |
| **KMM-ViewModel** | 1.0.0-ALPHA-16 | ViewModels compartilhados Android/iOS |
| **Jetpack Compose** | BOM 2025.02 | UI Android (Material Design 3) |
| **Coroutines + Flow** | 1.7.3 | Async/reativo |
| **Ktor** | 2.3.7 | HTTP client (OpenMeteo + WAQI API) |
| **kotlinx-serialization** | 1.6.0 | JSON serialization (Firestore, APIs) |
| **kotlinx-datetime** | 0.5.0 | Datas multiplataforma |
| **Detekt** | — | Análise estática Kotlin (CI obrigatório) |
| **SwiftLint** | — | Análise estática Swift (CI obrigatório) |
| **WorkManager** | 2.9.0 | Check-ins agendados matinal/noturno |
| **Google Maps** | 4.3.3 | Mapa de helpers e UBS |

---

## 🚀 Quick Start

### Pré-requisitos

- JDK 17+
- Android Studio Hedgehog+
- Xcode 16+ (macOS, para iOS)

### Android

```bash
git clone https://github.com/herb-sin/afilaxy-kmm.git
cd afilaxy-kmm
./gradlew androidApp:assembleDebug
```

### iOS (macOS)

```bash
cd iosApp
xcodegen generate   # gera o .xcodeproj a partir do project.yml
pod install
open iosApp.xcworkspace
```

### Configurar Firebase

1. Adicione `google-services.json` em `androidApp/`
2. Adicione `GoogleService-Info.plist` em `iosApp/iosApp/`

### Configurar APIs de Dados Ambientais

1. Copie `local.properties.example` para `local.properties`
2. Adicione as chaves necessárias:
```properties
MAPS_API_KEY_ANDROID=SUA_KEY_AQUI
WAQI_API_TOKEN=SEU_TOKEN_AQUI
```

📖 **Guia completo**: [SETUP_MAPS.md](SETUP_MAPS.md) | [DEPLOYMENT.md](DEPLOYMENT.md)

---

## 📱 Estado Atual das Plataformas

### Android — v2.3.0 · Publicado na Google Play

| Feature | Status |
|---------|--------|
| Autenticação (Firebase Auth) | ✅ |
| Emergência P2P com geolocalização | ✅ |
| Chat em tempo real (Firestore) | ✅ |
| Push notifications (FCM) | ✅ |
| Modo Apoiador (helper toggle, raio 250m) | ✅ |
| Check-ins proativos (manhã/noite) | ✅ |
| Score de risco de crise (AQI + clima + perfil clínico) | ✅ |
| Notificações preditivas baseadas em risco | ✅ |
| Perfil médico (medicações, exames, protocolo) | ✅ |
| Feed da comunidade (posts, likes) | ✅ |
| Portal de profissionais de saúde | ✅ |
| Dashboard para pneumologistas | ✅ |
| Histórico de emergências | ✅ |
| Mapa com geolocalização e helpers | ✅ |
| Conteúdo educativo (autocuidado, UBS) | ✅ |
| Sessão única por dispositivo (kick-out automático) | ✅ |
| Persistência do histórico de risco diário | ✅ |
| **Total: 25 telas** | ✅ |

### iOS — TestFlight (beta) · Deploy automático via CI/CD

| Feature | Status |
|---------|--------|
| Autenticação (Firebase Auth) | ✅ |
| Emergência P2P | ✅ |
| Chat em tempo real | ✅ |
| Push notifications | ✅ |
| Modo Apoiador | ✅ |
| Check-ins proativos | ✅ |
| Score de risco de crise | ✅ |
| Perfil médico | ✅ |
| Feed da comunidade | ✅ |
| Portal de profissionais | ✅ |
| Design system nativo (SwiftUI + HIG) | ✅ |
| Layout adaptativo iPhone/iPad | ✅ |
| GPS / CoreLocation | 🚧 Em integração |
| **Total: 13 telas** | ✅ |
| **Distribuição** | TestFlight (beta) |

---

## 🔑 Funcionalidades

### ✅ Implementado

- **Emergência P2P:** Geolocalização + chat em tempo real com apoiadores próximos (raio 250m)
- **Score de Risco Preditivo:** Motor heurístico com escala não-linear calibrada pelas diretrizes GINA — combina AQI (WAQI), clima (OpenMeteo), perfil clínico, SAMU acionado e crises dos últimos 7 dias (janela deslizante) para gerar score 0–100 em 4 níveis (Baixo 🟢 / Moderado 🟡 / Alto 🟠 / Muito Alto 🔴). Histórico diário persistido em `risk_scores/{uid}/snapshots/{date}` para futura análise de tendência e ML
- **Check-ins Proativos:** Diário de sintomas, uso de medicação e contexto clínico (tipo de asma, gravidade) com notificações baseadas no score de risco
- **Perfil Médico Completo:** Tipo de asma, medicações por categoria, exames, protocolo de crise e contatos de emergência
- **Portal de Profissionais:** Listagem de pneumologistas e alergistas com filtro por especialidade e planos de assinatura
- **Dashboard Profissional:** Métricas de pacientes, alertas críticos e taxa de adesão
- **Feed da Comunidade:** Posts, likes e compartilhamento de experiências
- **Portal Web:** Landing page para adesão de profissionais com planos de assinatura (React + Vite)
- **Sessão Única por Dispositivo:** Login em um novo dispositivo encerra automaticamente a sessão anterior com alerta ao usuário — prevenção de acesso simultâneo à mesma conta
- **Contagem de Crises (Janela Deslizante):** Contador de pedidos de socorro usa os últimos 7 dias corridos (não semana ISO), garantindo que crises de domingo não sejam ignoradas na segunda-feira
- **Testes Automatizados:** Unit tests dos ViewModels e repositories (CI verde em shared + Android)

### 🚧 Em Desenvolvimento

- **GPS iOS (CoreLocation):** Integração de geolocalização nativa para iOS
- **Assinaturas (Stripe):** Planos pagos para profissionais de saúde

### 🔮 Roadmap

- Dashboard de RWE para gestores públicos e indústria farmacêutica
- Integração com DataSUS
- Hotspots de crises (clustering geoespacial)
- Mapa de UBS com disponibilidade de medicação gratuita
- Modelo ML personalizado (substituir heurística por predição supervisionada com dados dos check-ins)

---

## ⚙️ CI/CD

4 pipelines automáticos em todo `git push main`:

| Pipeline | Runner | O que faz |
|----------|--------|-----------|
| **Shared Tests** | ubuntu | Compila e roda testes unitários KMM |
| **Android CI** | ubuntu | Detekt + build debug Android |
| **iOS Build & Export** | macos-15 / Xcode 16 | SwiftLint + archive + upload TestFlight |
| **Secure Deploy** | ubuntu | Assina AAB + deploy interno Play Store |

---

## 💰 Modelo de Negócio

O modelo evolui em três estágios, do mais imediato ao mais estratégico:

### Estágio 1 — B2B · Ativo

Assinaturas para profissionais de saúde interessados no público asmático:

- Pneumologistas e alergistas
- Fisioterapeutas pulmonares
- Psicólogos e psicanalistas (pacientes asmáticos têm maior incidência de ansiedade)
- Outros especialistas que atendem esse perfil de paciente

Benefícios da assinatura:
- ✅ Visibilidade junto a pacientes qualificados e engajados
- ✅ Dashboard com dados de adesão e crises dos pacientes que vincularam o profissional no app
- ✅ Canal para publicação de conteúdo educativo
- ✅ Apoio a uma iniciativa de saúde pública

**Plano:**
- **Profissional da Saúde** (R$ 199/mês): Perfil listado com informações de especialidade, área de atuação, contato, completude do perfil, conteúdo publicado, atividade na plataforma e analytics do próprio perfil.

> ⚠️ **Nota regulatória:** A plataforma não exibe avaliações públicas de profissionais de saúde por pacientes, em conformidade com as resoluções do CFM, COFFITO e CFP que vedam essa prática. O critério de destaque é exclusivamente objetivo e não comercial.

---

### Estágio 2 — B2B2G · Em estruturação

Com a base de dados consolidada, parcerias com clínicas e operadoras de saúde para uso dos dados populacionais na previsão de demanda por medicamentos e internações, reduzindo custos operacionais e qualificando o atendimento.

---

### Estágio 3 — B2G · Visão de longo prazo

O Governo passa a ser cliente dos dados agregados para identificar municípios e bairros com maior incidência de asma, alocando recursos — UBS, medicamentos, campanhas — com muito maior assertividade. O objetivo é transformar o comportamento informal de socorro em dado clínico inteligente que retroalimenta o SUS.

---

## 🏆 Trajetória

| Programa | Resultado |
|----------|-----------|
| **Ideiaz** | Participante |
| **InovAtiva Brasil** | Graduado |
| **Parque Tecnológico de Santos** | Empresa apoiada |
| **InovAtiva Impacto 2026** | Candidato (mentorias regulatórias, tecnológicas e de mercado) |

---

## 🤝 Contribuindo

Contribuições são bem-vindas! Veja [CONTRIBUTING.md](CONTRIBUTING.md) para detalhes.

### Como Contribuir

1. Fork o projeto
2. Crie uma branch (`git checkout -b feature/nova-funcionalidade`)
3. Commit suas mudanças (`git commit -m 'feat: adiciona funcionalidade'`)
4. Push para a branch (`git push origin feature/nova-funcionalidade`)
5. Abra um Pull Request

### Qualidade de Código

- **Kotlin:** `./gradlew detekt` — deve retornar BUILD SUCCESSFUL sem violations
- **Swift:** SwiftLint roda automaticamente no CI iOS com `--strict`

---

## 📄 Licença

MIT License - veja [LICENSE](LICENSE) para detalhes.

---

## 🌟 Como Apoiar

- **Profissionais de Saúde:** Ajude a curar conteúdo educativo e valide o algoritmo de risco
- **Desenvolvedores:** Contribua com código
- **Gestores Públicos:** Use nossos dados para melhorar a governança da asma no SUS
- **Pacientes:** Compartilhe sua experiência e ajude a treinar o modelo

---

## 🙏 Agradecimentos

- [GitLive Firebase KMM](https://github.com/GitLiveApp/firebase-kotlin-sdk)
- [Koin](https://insert-koin.io/)
- [KMM-ViewModel](https://github.com/rickclephas/KMM-ViewModel)
- [Multiplatform Settings](https://github.com/russhwolf/multiplatform-settings)
- [WAQI](https://waqi.info/) — World Air Quality Index API
- [Open-Meteo](https://open-meteo.com/) — API de clima open source

---

**Desenvolvido com ❤️ para democratizar o acesso à saúde respiratória no Brasil** 🇧🇷
