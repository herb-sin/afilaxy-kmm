# 🫁 Afilaxy — Plataforma de Gestão de Asma

[![Android CI](https://github.com/afilaxy/afilaxy-kmm/workflows/Android%20CI/badge.svg)](https://github.com/afilaxy/afilaxy-kmm/actions)
[![Shared Tests](https://github.com/afilaxy/afilaxy-kmm/workflows/Shared%20Module%20Tests/badge.svg)](https://github.com/afilaxy/afilaxy-kmm/actions)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-blue.svg)](https://kotlinlang.org)
[![KMM](https://img.shields.io/badge/KMM-Ready-brightgreen.svg)](https://kotlinlang.org/docs/multiplatform.html)

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
├── commonMain/          # 📦 Código compartilhado
│   ├── domain/         # 🎯 Regras de negócio
│   │   ├── model/      # User, Emergency, ChatMessage, MedicalProfile, etc.
│   │   ├── repository/ # Interfaces
│   │   └── usecase/    # Lógica de negócio
│   ├── data/           # 💾 Implementações
│   │   └── repository/ # Firebase, GPS, Preferences
│   ├── presentation/   # 🎨 ViewModels compartilhados
│   └── di/             # 💉 Koin
├── androidMain/        # 🤖 Android específico
└── iosMain/            # 🍎 iOS específico
```

### Stack Tecnológico

| Tecnologia | Uso |
|-----------|-----|
| **Kotlin 2.0.21** | Linguagem principal (shared + Android) |
| **Swift / SwiftUI** | UI iOS nativa |
| **Firebase** | Auth, Firestore, Cloud Messaging |
| **Koin 3.5.0** | Dependency Injection |
| **KMM-ViewModel** | ViewModels compartilhados |
| **Jetpack Compose** | UI Android (Material Design 3) |
| **Coroutines + Flow** | Async/reativo |

---

## 🚀 Quick Start

### Pré-requisitos

- JDK 17+
- Android Studio Hedgehog+
- Xcode 15+ (macOS, para iOS)

### Android

```bash
git clone https://github.com/afilaxy/afilaxy-kmm.git
cd afilaxy-kmm
./gradlew androidApp:assembleDebug
```

### iOS (macOS)

```bash
cd iosApp
pod install
open iosApp.xcworkspace
```

### Configurar Firebase

1. Adicione `google-services.json` em `androidApp/`
2. Adicione `GoogleService-Info.plist` em `iosApp/`

### Configurar Google Maps

1. Copie `local.properties.example` para `local.properties`
2. Adicione sua API key do Google Maps:
```properties
MAPS_API_KEY=SUA_API_KEY_AQUI
```

📖 **Guia completo**: [SETUP_MAPS.md](SETUP_MAPS.md)

---

## 📱 Estado Atual das Plataformas

### Android — Funcional e Publicado na Google Play

| Feature | Status |
|---------|--------|
| Autenticação (Firebase Auth) | ✅ |
| Emergência P2P com geolocalização (GPS) | ✅ |
| Chat em tempo real (Firestore) | ✅ |
| Push notifications (FCM) | ✅ |
| Modo Apoiador (helper toggle) | ✅ |
| Busca de apoiadores no raio de 5km | ✅ |
| Check-ins proativos (manhã/noite) | ✅ |
| Perfil médico (medicações, exames, protocolo) | ✅ |
| Feed da comunidade (posts, likes) | ✅ |
| Portal de profissionais de saúde | ✅ |
| Dashboard para pneumologistas | ✅ |
| Sistema de avaliação (rating) | ✅ |
| Histórico de emergências | ✅ |
| **Total: 20 telas** | ✅ |

### iOS — Publicado na App Store

| Feature | Status |
|---------|--------|
| Autenticação (Firebase Auth) | ✅ |
| Emergência P2P | ✅ |
| Chat em tempo real | ✅ |
| Push notifications | ✅ |
| Modo Apoiador | ✅ |
| Check-ins proativos | ✅ |
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

- **Emergência P2P:** Geolocalização + chat em tempo real com apoiadores próximos
- **Check-ins Proativos:** Diário de sintomas e uso de medicação (manhã/noite)
- **Perfil Médico Completo:** Tipo de asma, medicações por categoria, exames, protocolo de crise e contatos de emergência
- **Portal de Profissionais:** Listagem de pneumologistas e alergistas com filtro por especialidade
- **Dashboard Profissional:** Métricas de pacientes, alertas críticos e taxa de adesão
- **Feed da Comunidade:** Posts, likes e compartilhamento de experiências
- **Sistema de Rating:** Avaliação de apoiadores após emergência resolvida
- **Portal Web (React + Vite):** Landing page para adesão de profissionais com planos de assinatura

### 🚧 Em Desenvolvimento

- **GPS iOS (CoreLocation):** Integração de geolocalização nativa para iOS
- **Assinaturas (Stripe):** Planos pagos para profissionais de saúde
- **Testes automatizados:** Unit e integration tests dos ViewModels e repositories

### 🔮 Roadmap

- Algoritmo preditivo com dados ambientais (qualidade do ar, clima)
- Dashboard de RWE para gestores públicos e indústria farmacêutica
- Integração com DataSUS
- Hotspots de crises (clustering geoespacial)
- Notificações preditivas personalizadas
- Mapa de UBS com disponibilidade de medicação gratuita

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

---

**Desenvolvido com ❤️ para democratizar o acesso à saúde respiratória no Brasil** 🇧🇷
