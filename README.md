# 🫁 Afilaxy — Plataforma de Gestão de Asma

[![Android CI](https://github.com/herb-sin/afilaxy-kmm/actions/workflows/android-build.yml/badge.svg)](https://github.com/herb-sin/afilaxy-kmm/actions)
[![Shared Tests](https://github.com/herb-sin/afilaxy-kmm/actions/workflows/shared-tests.yml/badge.svg)](https://github.com/herb-sin/afilaxy-kmm/actions)
[![iOS Build](https://github.com/herb-sin/afilaxy-kmm/actions/workflows/ios-build.yml/badge.svg)](https://github.com/herb-sin/afilaxy-kmm/actions)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-blue.svg)](https://kotlinlang.org)
[![KMM](https://img.shields.io/badge/KMM-Ready-brightgreen.svg)](https://kotlinlang.org/docs/multiplatform.html)
[![Version](https://img.shields.io/badge/version-2.1.0--kmm-orange.svg)](https://github.com/herb-sin/afilaxy-kmm/releases)

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
│   │   └── usecase/    # CreateEmergency, FindHelpers, SendChatMessage...
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

### Android — v2.1.0 · Publicado na Google Play

| Feature | Status |
|---------|--------|
| Autenticação (Firebase Auth) | ✅ |
| Emergência P2P com geolocalização | ✅ |
| Chat em tempo real (Firestore) | ✅ |
| Push notifications (FCM) | ✅ |
| Modo Apoiador (helper toggle, raio 250m) | ✅ |
| Check-ins proativos (manhã/noite) | ✅ |
| Score de risco de crise (AQI + clima + perfil) | ✅ |
| Notificações preditivas baseadas em risco | ✅ |
| Perfil médico (medicações, exames, protocolo) | ✅ |
| Feed da comunidade (posts, likes) | ✅ |
| Portal de profissionais de saúde | ✅ |
| Dashboard para pneumologistas | ✅ |
| Histórico de emergências | ✅ |
| Mapa com geolocalização e helpers | ✅ |
| Conteúdo educativo (autocuidado, UBS) | ✅ |
| **Total: 25 telas** | ✅ |

### iOS — Publicado na App Store · Deploy automático via TestFlight

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

---

## 🔑 Funcionalidades

### ✅ Implementado

- **Emergência P2P:** Geolocalização + chat em tempo real com apoiadores próximos (raio 250m)
- **Score de Risco Preditivo:** Motor heurístico que combina AQI (WAQI), clima (OpenMeteo), perfil clínico e histórico de crises para gerar score 0–100 em 4 níveis (Baixo 🟢 / Moderado 🟡 / Alto 🟠 / Muito Alto 🔴)
- **Check-ins Proativos:** Diário de sintomas e uso de medicação (manhã/noite) com notificações baseadas no score de risco
- **Perfil Médico Completo:** Tipo de asma, medicações por categoria, exames, protocolo de crise e contatos de emergência
- **Portal de Profissionais:** Listagem de pneumologistas e alergistas com filtro por especialidade e planos de assinatura
- **Dashboard Profissional:** Métricas de pacientes, alertas críticos e taxa de adesão
- **Feed da Comunidade:** Posts, likes e compartilhamento de experiências
- **Portal Web:** Landing page para adesão de profissionais com planos de assinatura (React + Vite)
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

### Assinaturas B2B para Profissionais de Saúde

Pneumologistas, alergistas e fisioterapeutas pagam mensalidade para:

- ✅ Ganhar visibilidade junto a pacientes qualificados
- ✅ Acesso ao dashboard de monitoramento de pacientes
- ✅ Gerar conteúdo educativo
- ✅ Apoiar iniciativa de saúde pública

**Planos:**
- **Básico** (R$ 99/mês): Perfil listado
- **Pro** (R$ 199/mês): Destaque + badge
- **Premium** (R$ 399/mês): Topo da lista + analytics

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
