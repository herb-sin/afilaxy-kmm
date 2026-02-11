# Afilaxy - Kotlin Multiplatform Mobile (KMM)

[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-blue.svg)](https://kotlinlang.org)
[![KMM](https://img.shields.io/badge/KMM-Ready-brightgreen.svg)](https://kotlinlang.org/docs/multiplatform.html)
[![Platform](https://img.shields.io/badge/Platform-Android%20%7C%20iOS-lightgrey.svg)](https://kotlinlang.org/docs/multiplatform.html)

Sistema de emergência médica com **80%+ de código compartilhado** entre Android e iOS.

---

## 🎯 Sobre o Projeto

Aplicativo de emergência que conecta pessoas em situações críticas com helpers próximos, oferecendo:

- 🚨 **Criação de emergências** com localização em tempo real
- 💬 **Chat em tempo real** entre solicitante e helper
- 📍 **Busca de helpers próximos** (geolocalização)
- 🔐 **Autenticação segura** com Firebase
- 🌍 **Cross-platform**: Android e iOS com código compartilhado

---

## 🏗️ Arquitetura

### Clean Architecture + MVVM + KMM

```
shared/
├── commonMain/          # 📦 Código compartilhado (Android + iOS)
│   ├── domain/         # 🎯 Regras de negócio
│   │   ├── model/      # 10 models
│   │   ├── repository/ # 5 interfaces
│   │   └── usecase/    # 3 use cases
│   ├── data/           # 💾 Implementações
│   │   └── repository/ # 5 repositórios
│   ├── presentation/   # 🎨 ViewModels compartilhados
│   │   ├── login/
│   │   ├── chat/
│   │   └── emergency/
│   └── di/             # 💉 Dependency Injection (Koin)
├── androidMain/        # 🤖 Código específico Android
│   ├── data/
│   │   └── repository/ # LocationRepositoryImpl (GPS)
│   └── di/             # Koin Android module
└── iosMain/            # 🍎 Código específico iOS
    ├── data/
    │   └── repository/ # LocationRepositoryImpl (CoreLocation)
    └── di/             # Koin iOS module
```

**30 arquivos compartilhados = 80%+ de reutilização!** 🎉

---

## 🚀 Tecnologias

| Tecnologia | Versão | Uso |
|-----------|--------|-----|
| **Kotlin** | 2.0.21 | Linguagem principal |
| **Firebase KMM** | 1.11.1 | Auth + Firestore |
| **Koin** | 3.5.0 | Dependency Injection |
| **KMM-ViewModel** | 1.0.0-ALPHA-16 | ViewModels compartilhados |
| **Multiplatform Settings** | 1.1.1 | SharedPreferences/UserDefaults |
| **Kotlinx Serialization** | 1.6.0 | JSON |
| **Coroutines** | 1.7.3 | Async/Await |

---

## 📦 Módulos

### Domain Layer (18 arquivos)
- **Models**: `User`, `Emergency`, `ChatMessage`, `Helper`, `Location`, `HealthData`, etc.
- **Repositories**: `AuthRepository`, `EmergencyRepository`, `ChatRepository`, `LocationRepository`, `PreferencesRepository`
- **Use Cases**: `CreateEmergencyUseCase`, `FindHelpersUseCase`, `SendChatMessageUseCase`

### Data Layer (5 repositórios)
- **AuthRepositoryImpl**: Firebase Authentication
- **ChatRepositoryImpl**: Firestore Real-Time Chat
- **EmergencyRepositoryImpl**: Gestão de emergências com transações atômicas
- **LocationRepositoryImpl**: GPS nativo (Android/iOS com expect/actual)
- **PreferencesRepositoryImpl**: Persistência de dados

### Presentation Layer (3 ViewModels)
- **LoginViewModel**: Autenticação
- **ChatViewModel**: Chat real-time
- **EmergencyViewModel**: Emergências completas

---

## 🛠️ Setup

### Pré-requisitos

- **JDK 17+**
- **Android Studio** (Arctic Fox ou superior)
- **Xcode 14+** (para build iOS, apenas macOS)
- **CocoaPods** (para iOS)

### 1. Clone o repositório

```bash
git clone https://github.com/herb-sin/afilaxy.git
cd afilaxy-kmm
```

### 2. Configurar Firebase

1. Adicione `google-services.json` em `androidApp/`
2. Adicione `GoogleService-Info.plist` em `iosApp/`

### 3. Build Android

```bash
./gradlew build
```

Ou abra no Android Studio e clique em **Build → Make Project**.

### 4. Build iOS (macOS apenas)

```bash
cd iosApp
pod install
open iosApp.xcworkspace
```

---

## 💻 Como Usar

### Exemplo: LoginViewModel

```kotlin
// Android (Compose)
@Composable
fun LoginScreen() {
    val viewModel: LoginViewModel = koinViewModel()
    val state by viewModel.state.collectAsState()
    
    Column {
        TextField(
            value = state.email,
            onValueChange = viewModel::onEmailChange
        )
        
        TextField(
            value = state.password,
            onValueChange = viewModel::onPasswordChange,
            visualTransformation = PasswordVisualTransformation()
        )
        
        Button(
            onClick = viewModel::onLoginClick,
            enabled = !state.isLoading
        ) {
            Text(if (state.isLoading) "Entrando..." else "Login")
        }
        
        state.error?.let { Text(it, color = Color.Red) }
    }
}
```

```swift
// iOS (SwiftUI)
struct LoginView: View {
    @StateObject var viewModel = LoginViewModel()
    
    var body: some View {
        VStack {
            TextField("Email", text: $viewModel.email)
            SecureField("Senha", text: $viewModel.password)
            
            Button(action: { viewModel.onLoginClick() }) {
                Text(viewModel.isLoading ? "Entrando..." : "Login")
            }
            .disabled(viewModel.isLoading)
            
            if let error = viewModel.error {
                Text(error).foregroundColor(.red)
            }
        }
    }
}
```

**Mesmo ViewModel, duas plataformas!** 🎯

---

## 🔑 Features Implementadas

### ✅ Autenticação
- Login/Logout com Firebase Auth
- Registro de novos usuários
- Gerenciamento de sessão
- Atualização de FCM token

### ✅ Chat Real-Time
- Envio de mensagens
- Observação real-time com Flow
- Suporte para helper/requester
- Limpeza de chat

### ✅ Emergências
- Criar emergência com GPS automático
- Cancelar emergência
- Aceitar emergência (com transação atômica para evitar race condition)
- Resolver emergência
- Buscar helpers próximos (raio 5km)
- Cálculo de distância (fórmula Haversine)
- Modo Helper (ativar/desativar)

### ✅ Geolocalização
- GPS Android (Google Play Services)
- GPS iOS (CoreLocation)
- Verificação de permissões cross-platform
- Alta precisão

---

## 📱 Estrutura do App

### Android
```
androidApp/
├── src/main/
│   ├── kotlin/com/afilaxy/
│   │   ├── MainActivity.kt
│   │   ├── Application.kt (inicializa Koin)
│   │   └── ui/            # Compose screens
│   └── AndroidManifest.xml
└── build.gradle.kts
```

### iOS
```
iosApp/
├── iosApp/
│   ├── ContentView.swift
│   ├── KoinHelper.swift
│   └── Views/            # SwiftUI views
└── Podfile
```

---

## 🧪 Testes

### Executar testes compartilhados
```bash
./gradlew shared:test
```

### Executar testes Android
```bash
./gradlew androidApp:testDebugUnitTest
```

---

## 🤝 Contribuindo

1. Fork o projeto
2. Crie uma branch (`git checkout -b feature/amazing-feature`)
3. Commit suas mudanças (`git commit -m 'Add amazing feature'`)
4. Push para a branch (`git push origin feature/amazing-feature`)
5. Abra um Pull Request

---

## 📄 Licença

Este projeto está sob a licença MIT. Veja o arquivo [LICENSE](LICENSE) para mais detalhes.

---

## 🙏 Agradecimentos

- [GitLive Firebase KMM](https://github.com/GitLiveApp/firebase-kotlin-sdk)
- [Koin](https://insert-koin.io/)
- [KMM-ViewModel](https://github.com/rickclephas/KMM-ViewModel)
- [Multiplatform Settings](https://github.com/russhwolf/multiplatform-settings)

---

## 📊 Status do Projeto

- ✅ Domain Layer (100%)
- ✅ Data Layer (100%)
- ✅ Presentation Layer (ViewModels compartilhados)
- 🚧 UI Android (em desenvolvimento)
- 🚧 UI iOS (planejado)

---

**Desenvolvido com ❤️ usando Kotlin Multiplatform** 🚀
