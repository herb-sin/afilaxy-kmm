# Afilaxy — Plataforma de Bem-Estar Comunitário

[![Android CI](https://github.com/herb-sin/afilaxy-kmm/actions/workflows/android-build.yml/badge.svg)](https://github.com/herb-sin/afilaxy-kmm/actions)
[![Shared Tests](https://github.com/herb-sin/afilaxy-kmm/actions/workflows/shared-tests.yml/badge.svg)](https://github.com/herb-sin/afilaxy-kmm/actions)
[![iOS Build](https://github.com/herb-sin/afilaxy-kmm/actions/workflows/ios-build.yml/badge.svg)](https://github.com/herb-sin/afilaxy-kmm/actions)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-blue.svg)](https://kotlinlang.org)
[![KMM](https://img.shields.io/badge/KMM-Ready-brightgreen.svg)](https://kotlinlang.org/docs/multiplatform.html)
[![Version](https://img.shields.io/badge/version-2.3.1-orange.svg)](https://github.com/herb-sin/afilaxy-kmm/releases)

Somos uma plataforma de bem-estar comunitário que conecta pessoas em emergências. O Afilaxy cria uma rede P2P de localização em tempo real: quem precisa de ajuda aciona o app, e voluntários próximos são notificados e podem responder.

---

## Visão

### O Problema

Emergências do dia a dia acontecem onde menos se espera — e geralmente quando estamos sozinhos. Quem precisa de ajuda não sabe quem está por perto e disposto a auxiliar. O resultado é isolamento no momento mais crítico.

### A Solução

O Afilaxy resolve isso em quatro camadas:

1. **Emergência P2P:** Quem precisa de ajuda aciona o app e voluntários próximos (raio 250 m) são notificados em tempo real
2. **Check-ins de bem-estar:** Acompanhamento contínuo do estado do usuário, matinal e noturno
3. **Score de risco contextual:** Algoritmo que combina qualidade do ar, clima e perfil do usuário para antecipar situações de risco
4. **Dados para a comunidade:** Dados agregados úteis para gestores e profissionais de saúde

### O Diferencial

A rede de voluntários transforma o comportamento informal de socorro — que já acontece espontaneamente — em infraestrutura organizada, rastreável e útil para toda a comunidade.

---

## Arquitetura

### Kotlin Multiplatform Mobile (KMM)

**80%+ de código compartilhado** entre Android e iOS usando Clean Architecture + MVVM.

```
shared/
├── commonMain/          # Código compartilhado (Android + iOS)
│   ├── domain/         # Regras de negócio
│   │   ├── model/      # Emergency, CheckIn, RiskScore, HealthProfessional...
│   │   ├── repository/ # Interfaces (EmergencyRepository, LocationRepository...)
│   │   └── usecase/    # CreateEmergencyUseCase
│   ├── data/           # Implementações (Firestore, GPS, Preferences)
│   ├── presentation/   # 13 ViewModels compartilhados
│   ├── util/           # Logger multiplataforma, TimeUtils
│   └── di/             # Koin modules
├── androidMain/        # AndroidLogger, AndroidPlatform
└── iosMain/            # IOSLogger, IOSPlatform
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
| **Google Maps** | 4.3.3 | Mapa de voluntários e estabelecimentos |

---

## Quick Start

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

## Estado Atual das Plataformas

### Android — v2.3.1 · Publicado na Google Play

| Feature | Status |
|---------|--------|
| Autenticação (Firebase Auth) | ✅ |
| Emergência P2P com geolocalização | ✅ |
| Chat em tempo real (Firestore) | ✅ |
| Push notifications (FCM) | ✅ |
| Modo Ajudante (voluntário, raio 250m) | ✅ |
| Check-ins de bem-estar (manhã/noite) | ✅ |
| Score de risco contextual (AQI + clima + perfil) | ✅ |
| Notificações preditivas baseadas em risco | ✅ |
| Perfil de saúde autodeclarado | ✅ |
| Feed da comunidade (posts, likes) | ✅ |
| Portal de profissionais de saúde | ✅ |
| Histórico de emergências | ✅ |
| Mapa com geolocalização e voluntários | ✅ |
| Conteúdo educativo | ✅ |
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
| Modo Ajudante | ✅ |
| Check-ins de bem-estar | ✅ |
| Score de risco contextual | ✅ |
| Perfil de saúde autodeclarado | ✅ |
| Feed da comunidade | ✅ |
| Portal de profissionais | ✅ |
| Design system nativo (SwiftUI + HIG) | ✅ |
| Layout adaptativo iPhone/iPad | ✅ |
| **Total: 13 telas** | ✅ |
| **Distribuição** | TestFlight (beta) |

---

## Funcionalidades

### Implementado

- **Emergência P2P:** Geolocalização + chat em tempo real com voluntários próximos (raio 250m)
- **Score de Risco Contextual:** Motor heurístico com escala não-linear — combina AQI (WAQI), clima (OpenMeteo), perfil autodeclarado e histórico de crises dos últimos 7 dias para gerar score 0–100 em 4 níveis (Baixo / Moderado / Alto / Muito Alto). Histórico diário persistido para futura análise de tendência e ML
- **Check-ins de Bem-Estar:** Acompanhamento diário matinal e noturno com notificações baseadas no score de risco
- **Perfil de Saúde Autodeclarado:** Medicações, alergias, condições e contatos de emergência
- **Portal de Profissionais:** Listagem de profissionais de saúde com filtro por especialidade e planos de assinatura
- **Feed da Comunidade:** Posts, likes e compartilhamento de experiências
- **Portal Web:** Landing page para adesão de profissionais com planos de assinatura (React + Vite)
- **Sessão Única por Dispositivo:** Login em novo dispositivo encerra automaticamente a sessão anterior
- **Contagem de Crises (Janela Deslizante):** Contador usa os últimos 7 dias corridos para garantir que crises de fim de semana não sejam ignoradas
- **Testes Automatizados:** Unit tests dos ViewModels e repositories (CI verde em shared + Android)

### Em Desenvolvimento

- **Assinaturas (Stripe):** Planos pagos para profissionais de saúde

### Roadmap

- Dashboard de dados populacionais para gestores públicos
- Hotspots de crises (clustering geoespacial)
- Mapa de estabelecimentos com disponibilidade de medicação gratuita
- Modelo ML personalizado (substituir heurística por predição supervisionada com dados reais dos check-ins)

---

## CI/CD

4 pipelines automáticos em todo `git push main`:

| Pipeline | Runner | O que faz |
|----------|--------|-----------|
| **Shared Tests** | ubuntu | Compila e roda testes unitários KMM |
| **Android CI** | ubuntu | Detekt + build debug Android |
| **iOS Build & Export** | macos-latest | SwiftLint + archive + upload TestFlight |
| **Secure Deploy** | ubuntu | Assina AAB + deploy interno Play Store |

---

## Modelo de Negócio

O modelo evolui em três estágios, do mais imediato ao mais estratégico:

### Estágio 1 — B2B · Ativo

Assinaturas para profissionais de saúde que atendem a comunidade de usuários da plataforma:

Benefícios da assinatura:
- ✅ Visibilidade junto a usuários engajados
- ✅ Canal para publicação de conteúdo educativo
- ✅ Apoio a uma iniciativa de impacto comunitário

**Plano Profissional da Saúde** (R$ 199/mês): Perfil listado com informações de especialidade, área de atuação, contato e analytics do próprio perfil.

### Estágio 2 — B2B2G · Em estruturação

Com a base de dados consolidada, parcerias com clínicas e operadoras de saúde para uso dos dados populacionais na previsão de demanda e alocação de recursos.

### Estágio 3 — B2G · Visão de longo prazo

Dados agregados para gestores públicos identificarem regiões com maior concentração de emergências comunitárias, alocando recursos com maior assertividade.

---

## Trajetória

| Programa | Resultado |
|----------|-----------|
| **Ideiaz** | Participante |
| **InovAtiva Brasil** | Graduado |
| **Parque Tecnológico de Santos** | Empresa apoiada |
| **InovAtiva Impacto 2026** | Candidato |

---

## Contribuindo

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

## Licença

MIT License — veja [LICENSE](LICENSE) para detalhes.

---

## Agradecimentos

- [GitLive Firebase KMM](https://github.com/GitLiveApp/firebase-kotlin-sdk)
- [Koin](https://insert-koin.io/)
- [KMM-ViewModel](https://github.com/rickclephas/KMM-ViewModel)
- [Multiplatform Settings](https://github.com/russhwolf/multiplatform-settings)
- [WAQI](https://waqi.info/) — World Air Quality Index API
- [Open-Meteo](https://open-meteo.com/) — API de clima open source

---

**Desenvolvido com ❤️ para conectar pessoas quando mais precisam** 🇧🇷
