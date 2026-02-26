# 🫁 Afilaxy - Health Equity para Asma

[![Android CI](https://github.com/seu-usuario/afilaxy-kmm/workflows/Android%20CI/badge.svg)](https://github.com/seu-usuario/afilaxy-kmm/actions)
[![Shared Tests](https://github.com/seu-usuario/afilaxy-kmm/workflows/Shared%20Module%20Tests/badge.svg)](https://github.com/seu-usuario/afilaxy-kmm/actions)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-blue.svg)](https://kotlinlang.org)
[![KMM](https://img.shields.io/badge/KMM-Ready-brightgreen.svg)](https://kotlinlang.org/docs/multiplatform.html)

Plataforma de Health Equity que usa a urgência da crise de asma como porta de entrada para reinserir pacientes no tratamento preventivo do SUS.

---

## 🎯 Visão

### O Problema

- **20 milhões** de brasileiros têm asma
- **80%** não seguem tratamento preventivo
- SUS oferece medicação gratuita, mas falta adesão
- Crises evitáveis sobrecarregam emergências

### A Solução

**1. Hook Emocional:** Encontrar "bombinha" via P2P durante crise  
**2. Educação:** Explicar resgate vs. manutenção (24h pós-crise)  
**3. Reinserção:** Indicar UBS + tratamento gratuito SUS  
**4. Dados:** Gerar BI georreferenciado para gestores públicos

### O Diferencial

> "Inovação não é criar uma molécula nova, é inovar no modelo de acesso"

Enquanto o mercado global vende mais medicamentos, o Afilaxy garante que o investimento público chegue ao paciente e seja usado corretamente.

---

## 🏗️ Arquitetura

### Kotlin Multiplatform Mobile (KMM)

**80%+ de código compartilhado** entre Android e iOS usando Clean Architecture + MVVM.

```
shared/
├── commonMain/          # 📦 Código compartilhado
│   ├── domain/         # 🎯 Regras de negócio
│   │   ├── model/      # User, Emergency, ChatMessage, Location, etc.
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
| **Kotlin 2.0.21** | Linguagem principal |
| **Firebase** | Auth, Firestore, Cloud Messaging |
| **Koin 3.5.0** | Dependency Injection |
| **KMM-ViewModel** | ViewModels compartilhados |
| **Jetpack Compose** | UI Android |
| **SwiftUI** | UI iOS |
| **Coroutines** | Async/Await |

---

## 🚀 Quick Start

### Pré-requisitos

- JDK 17+
- Android Studio Arctic Fox+
- Xcode 14+ (macOS, para iOS)

### Android

```bash
git clone https://github.com/seu-usuario/afilaxy-kmm.git
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

---

## 🔑 Features

### ✅ Implementado

- **Emergência P2P:** Geolocalização + chat em tempo real
- **Autenticação:** Firebase Auth
- **Modo Helper:** Ativar/desativar disponibilidade
- **Busca de Helpers:** Raio 5km com cálculo Haversine
- **Chat Real-Time:** Firestore + Flow
- **Histórico:** Emergências passadas

### 🚧 Em Desenvolvimento (MVP)

- **Profissionais de Saúde:** Listagem de pneumologistas/alergistas
- **Assinaturas:** Planos Básico/Pro/Premium (Stripe)
- **Conteúdo Educativo:** Resgate vs. Manutenção
- **Mapa de UBS:** Localização de postos de saúde

### 🔮 Roadmap

- Notificações pós-emergência (24h)
- Dashboard BI para gestores públicos
- Integração com DataSUS
- Sistema de lembretes de medicação
- Hotspots de crises (clustering geoespacial)

---

## 💰 Modelo de Negócio

### Assinaturas para Profissionais de Saúde

Pneumologistas, alergistas e fisioterapeutas pagam mensalidade para:

- ✅ Ganhar visibilidade junto a pacientes
- ✅ Gerar conteúdo educativo
- ✅ Apoiar iniciativa de saúde pública
- ✅ Receber leads qualificados

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

## 🌟 Apoiadores

Este projeto é open source e depende de contribuições da comunidade:

- **Profissionais de Saúde:** Ajude a curar conteúdo educativo
- **Desenvolvedores:** Contribua com código
- **Gestores Públicos:** Use nossos dados para melhorar o SUS
- **Pacientes:** Compartilhe sua experiência

---

## 📊 Status do Projeto

- ✅ **Domain Layer:** 100%
- ✅ **Data Layer:** 100%
- ✅ **Presentation Layer:** ViewModels compartilhados
- ✅ **UI Android:** 90%
- 🚧 **UI iOS:** 70%
- 🚧 **MVP Profissionais:** Em desenvolvimento

---

## 🙏 Agradecimentos

- [GitLive Firebase KMM](https://github.com/GitLiveApp/firebase-kotlin-sdk)
- [Koin](https://insert-koin.io/)
- [KMM-ViewModel](https://github.com/rickclephas/KMM-ViewModel)
- [Multiplatform Settings](https://github.com/russhwolf/multiplatform-settings)

---

**Desenvolvido com ❤️ para democratizar o acesso à saúde respiratória no Brasil** 🇧🇷
