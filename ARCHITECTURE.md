# Afilaxy — Arquitetura do Sistema

> Documentação gerada a partir do código-fonte. Atualizar junto com mudanças de domínio.

---

## Visão Geral — Kotlin Multiplatform Mobile (KMM)

```mermaid
graph TD
    subgraph shared["📦 shared (commonMain)"]
        Domain["Domain Layer\n(Models, UseCases, Repositories)"]
        Presentation["Presentation Layer\n(ViewModels, States)"]
    end

    subgraph androidApp["🤖 androidApp"]
        AndroidUI["Jetpack Compose\n(Screens, NavGraph)"]
        AndroidInfra["Infra Android\n(FCM, WorkManager, Biometric)"]
    end

    subgraph iosApp["🍎 iosApp"]
        SwiftUI["SwiftUI\n(Views, ViewModelProvider)"]
    end

    subgraph firebase["☁️ Firebase"]
        Auth["Auth"]
        Firestore["Firestore"]
        FCM["Cloud Messaging"]
        Functions["Functions v2"]
    end

    AndroidUI --> Presentation
    SwiftUI --> Presentation
    Presentation --> Domain
    Domain --> firebase
    AndroidInfra --> FCM
```

---

## Fluxo de Emergência — Máquina de Estados

```mermaid
stateDiagram-v2
    direction LR

    [*] --> ACTIVE : createEmergency()\nUsuário solicita ajuda

    ACTIVE --> HELPER_RESPONDING : acceptEmergency()\nHelper aceita no raio de 250m
    ACTIVE --> CANCELLED : cancelEmergency()\nUsuário cancela
    ACTIVE --> CANCELLED : Timeout 3 min\nsem helper disponível

    HELPER_RESPONDING --> RESOLVED : resolveEmergency()\nAtendimento concluído
    HELPER_RESPONDING --> CANCELLED : cancelEmergency()\nHelper ou usuário cancela

    RESOLVED --> [*]
    CANCELLED --> [*]
```

### Estados no Firestore

| Estado (enum) | `status` no Firestore | Descrição |
|--------------|----------------------|-----------|
| `ACTIVE` | `"waiting"` | Aguardando helper no raio de 250m |
| `HELPER_RESPONDING` | `"matched"` | Helper aceito, a caminho |
| `RESOLVED` | `"resolved"` | Atendimento concluído |
| `CANCELLED` | `"cancelled"` | Cancelada (timeout, usuário ou helper) |

---

## Fluxo de Risco de Asma — Score e Notificação

```mermaid
flowchart TD
    A["Dados Ambientais\nOpenMeteo + WAQI"] --> C["Motor de Risco\nHeurístico"]
    B["Perfil Clínico\nTipo de asma, severidade"] --> C
    D["Check-in Matinal\ntem bombinha?"] --> C

    C --> E{"Score 0-100"}

    E -->|"0–44\nBaixo 🟢"| F["Sem notificação"]
    E -->|"45–64\nModerado 🟡"| G["Notificação matinal\npergunta sobre bombinha"]
    E -->|"65–84\nAlto 🟠"| G
    E -->|"85–100\nMuito Alto 🔴"| G

    G --> H{"Check-in Noturno\nTeve crise hoje?"}
    H -->|"Não"| I["ML label = 0"]
    H -->|"Sim"| J["ML label = 1\nSeveridade registrada"]

    I --> K[("Firestore\ncheckin_responses")]
    J --> K
    K --> L["Futuro modelo ML\nPrevisão personalizada"]
```

### Score de Risco — Faixas

| Faixa | Nível | Ação |
|-------|-------|------|
| 0–44 | 🟢 Baixo | Nenhuma |
| 45–64 | 🟡 Moderado | Notificação matinal |
| 65–84 | 🟠 Alto | Notificação matinal |
| 85–100 | 🔴 Muito Alto | Notificação matinal urgente |

---

## Fluxo de Check-in

```mermaid
sequenceDiagram
    participant W as WorkManager
    participant App as Afilaxy App
    participant U as Usuário
    participant FS as Firestore

    Note over W: 07:30 — MorningCheckInWorker
    W->>App: Notificação se score ≥ 45
    App->>U: "Você está com sua bombinha?"
    U->>App: ✅ Sim / ❌ Não
    App->>FS: CheckInResponse MORNING\n(hasRescueInhaler, riskScore, aqi...)

    Note over W: 21:00 — EveningCheckInWorker
    W->>App: Notificação diária
    App->>U: "Teve crise de asma hoje?"
    U->>App: ✅ Não tive / ⚠️ Tive uma crise
    App->>FS: CheckInResponse EVENING\n(hadCrisisToday, crisisSeverity...)
```

---

## Arquitetura de Camadas — shared module

```mermaid
graph BT
    subgraph data["Data Layer"]
        Repos["RepositoryImpl\n(Firestore, Location, Auth...)"]
    end

    subgraph domain["Domain Layer"]
        Models["Models\n(Emergency, CheckIn, RiskScore...)"]
        Interfaces["Repository Interfaces"]
        UseCases["Use Cases\n(CreateEmergency, SendChat,\nFindHelpers, ValidateInput...)"]
    end

    subgraph presentation["Presentation Layer"]
        VMs["ViewModels\n(Emergency, Home, Medical,\nCheckIn, Login...)"]
    end

    Repos --> Interfaces
    Interfaces --> UseCases
    Models --> UseCases
    UseCases --> VMs
```

---

## CI/CD Pipeline

```mermaid
flowchart LR
    Push["git push main"] --> A & B & C & D

    A["Shared Tests\nJUnit / KMM\n~36s"]
    B["Android CI\nDetekt + Build\n~4min"]
    C["iOS Build\nSwiftLint + Archive\n+ TestFlight\n~10min"]
    D["Secure Deploy\nAAB signing\n~2min"]

    A & B & C & D --> Green["✅ All Green"]
```

---

## Modelos de Domínio Principais

| Modelo | Responsabilidade |
|--------|----------------|
| `Emergency` | Pedido de socorro com status, localização e severidade |
| `EmergencyStatus` | Máquina de estados: ACTIVE → HELPER_RESPONDING → RESOLVED/CANCELLED |
| `CheckInResponse` | Resposta de check-in matinal/noturno com contexto ambiental e clínico |
| `RiskScore` | Score 0–100 com nível, fatores e recomendações |
| `AsthmaRiskLevel` | LOW / MODERATE / HIGH / VERY_HIGH |
| `EnvironmentalData` | AQI, temperatura, umidade, vento, UV, precipitação |
| `HealthProfessional` | Profissional cadastrado com especialidade, rating e planos |
| `UserProfile` | Perfil clínico: tipo de asma, severidade, medicamentos |
