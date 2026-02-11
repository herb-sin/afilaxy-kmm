# Arquitetura - Afilaxy KMM

## 🏗️ Visão Geral

O Afilaxy utiliza **Clean Architecture** + **MVVM** + **Kotlin Multiplatform Mobile** para maximizar o reuso de código entre Android e iOS.

---

## 📊 Camadas

### 1. Domain Layer (Shared)
**Responsabilidade**: Regras de negócio puras, independentes de framework

```
domain/
├── model/          # Entidades de negócio
├── repository/     # Interfaces (contratos)
└── usecase/        # Casos de uso
```

**Princípios**:
- ✅ Sem dependências externas
- ✅ Testável isoladamente
- ✅ Imutável (data classes)

### 2. Data Layer (Shared)
**Responsabilidade**: Implementação de repositórios, acesso a dados

```
data/
└── repository/     # Implementações concretas
    ├── AuthRepositoryImpl
    ├── EmergencyRepositoryImpl
    ├── ChatRepositoryImpl
    ├── LocationRepositoryImpl (expect/actual)
    └── PreferencesRepositoryImpl
```

**Tecnologias**:
- Firebase KMM (Auth, Firestore)
- Multiplatform Settings
- Coroutines

### 3. Presentation Layer (Shared)
**Responsabilidade**: ViewModels com lógica de apresentação

```
presentation/
├── login/LoginViewModel
├── auth/AuthViewModel
├── emergency/EmergencyViewModel
├── chat/ChatViewModel
├── profile/ProfileViewModel
└── rating/RatingViewModel
```

**Padrão**:
- StateFlow para estados reativos
- Eventos via métodos públicos
- Sem referências a UI

### 4. UI Layer (Platform-Specific)
**Responsabilidade**: Interface do usuário nativa

**Android**: Jetpack Compose
**iOS**: SwiftUI

---

## 🔄 Fluxo de Dados

```
UI → ViewModel → UseCase → Repository → DataSource (Firebase)
                    ↓
                  State
                    ↓
                   UI
```

### Exemplo: Login

```kotlin
// 1. UI (Android Compose)
Button(onClick = { viewModel.onLoginClick() })

// 2. ViewModel (Shared)
fun onLoginClick() {
    viewModelScope.launch {
        _state.value = state.value.copy(isLoading = true)
        authRepository.login(email, password)
    }
}

// 3. Repository (Shared)
suspend fun login(email: String, password: String): Result<User> {
    return try {
        val result = firebaseAuth.signInWithEmailAndPassword(email, password)
        Result.success(result.user)
    } catch (e: Exception) {
        Result.failure(e)
    }
}

// 4. State atualizado
_state.value = state.value.copy(isLoading = false, user = user)

// 5. UI reage automaticamente
val state by viewModel.state.collectAsState()
if (state.user != null) { /* navegar */ }
```

---

## 🎯 Decisões Arquiteturais

### 1. Por que Clean Architecture?
- ✅ Separação de responsabilidades
- ✅ Testabilidade
- ✅ Independência de frameworks
- ✅ Facilita manutenção

### 2. Por que MVVM?
- ✅ Binding reativo (StateFlow)
- ✅ Lifecycle-aware
- ✅ Suportado nativamente (KMM-ViewModel)

### 3. Por que Koin?
- ✅ Leve e simples
- ✅ Suporte KMM nativo
- ✅ Sem geração de código
- ✅ DSL Kotlin idiomático

### 4. Por que Firebase KMM?
- ✅ Backend-as-a-Service
- ✅ Real-time database
- ✅ Autenticação pronta
- ✅ SDK multiplataforma oficial

---

## 🔧 Padrões Utilizados

### Repository Pattern
```kotlin
interface EmergencyRepository {
    suspend fun createEmergency(emergency: Emergency): Result<String>
    suspend fun getActiveEmergency(): Result<String?>
}

class EmergencyRepositoryImpl(
    private val firestore: FirebaseFirestore
) : EmergencyRepository {
    override suspend fun createEmergency(...) { /* impl */ }
}
```

### Observer Pattern (StateFlow)
```kotlin
class LoginViewModel {
    private val _state = MutableStateFlow(LoginState())
    val state: StateFlow<LoginState> = _state.asStateFlow()
}
```

### Dependency Injection
```kotlin
val sharedModule = module {
    single<AuthRepository> { AuthRepositoryImpl(get()) }
    factory { LoginViewModel(get()) }
}
```

### Expect/Actual (Platform-Specific)
```kotlin
// commonMain
expect class LocationRepositoryImpl : LocationRepository

// androidMain
actual class LocationRepositoryImpl : LocationRepository {
    // Google Play Services
}

// iosMain
actual class LocationRepositoryImpl : LocationRepository {
    // CoreLocation
}
```

---

## 📱 Platform-Specific

### Android
- **UI**: Jetpack Compose
- **DI**: Koin Android
- **Navigation**: Compose Navigation
- **Permissions**: Accompanist
- **Location**: Google Play Services

### iOS
- **UI**: SwiftUI
- **Binding**: ObservableObject wrappers
- **Navigation**: NavigationView
- **Location**: CoreLocation (pendente)
- **Push**: APNs (pendente)

---

## 🔐 Segurança

### Firebase Security Rules
```javascript
match /emergency_requests/{emergencyId} {
  allow read: if request.auth != null;
  allow create: if request.auth != null;
  allow update: if request.auth.uid == resource.data.requesterId
                || request.auth.uid == resource.data.helperId;
}
```

### Validações
- Email/senha no cliente
- Token JWT no servidor (Firebase)
- Transações atômicas (race conditions)

---

## 🚀 Performance

### Otimizações
- ✅ StateFlow (cold flow, apenas subscribers ativos)
- ✅ LazyColumn/LazyVStack (virtualização)
- ✅ Firestore snapshots (apenas deltas)
- ✅ Coroutines (não bloqueia UI)

### Caching
- ✅ Firestore offline persistence
- ✅ Multiplatform Settings (preferências)

---

## 📊 Métricas

```
Código Compartilhado: 85%
- Domain: 100%
- Data: 95% (LocationRepository é platform-specific)
- Presentation: 100%
- UI: 0% (nativo por design)

Linhas de Código:
- Kotlin (shared): 3.500 linhas
- Kotlin (Android): 450 linhas
- Swift (iOS): 650 linhas

Arquivos:
- Shared: 35 arquivos
- Android: 25 arquivos
- iOS: 20 arquivos
```

---

## 🎯 Benefícios Alcançados

### Reuso de Código
- ✅ Lógica de negócio única
- ✅ Validações centralizadas
- ✅ Menos bugs
- ✅ Manutenção simplificada

### Desenvolvimento
- ✅ Equipe única (Kotlin)
- ✅ Testes compartilhados
- ✅ Deploy sincronizado

### Qualidade
- ✅ Type-safe
- ✅ Null-safe
- ✅ Testável
- ✅ Escalável

---

**Desenvolvido com ❤️ usando Kotlin Multiplatform** 🚀
