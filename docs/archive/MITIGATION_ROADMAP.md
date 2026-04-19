# 🎯 Roadmap de Mitigação - Prioridades

## ✅ CONCLUÍDO

### 🔴 CRITICAL - Credenciais Hardcoded
- [x] Proteger `google-services.json` (Android Firebase)
- [x] Proteger `GoogleService-Info.plist` (iOS Firebase)
- [x] Proteger `local.properties` (Google Maps API)
- [x] Proteger `*firebase-adminsdk*.json` (Firebase Admin SDK)
- [x] Criar templates `.example`
- [x] Atualizar `.gitignore`
- [x] Remover arquivos do git (quando commitados)
- [x] Documentar no README

### 🟡 HIGH - Vulnerabilidades de Pacotes
- [x] Executar `npm audit` - 0 vulnerabilidades
- [x] Atualizar Node.js engine requirement

---

## 🔄 PRÓXIMAS AÇÕES (Por Prioridade)

### 🔴 PRIORITY 1: Segurança Crítica

#### 1.1 Firebase Security Rules
**Impacto:** CRITICAL  
**Esforço:** 2-4 horas

```bash
# Verificar regras atuais
firebase firestore:rules:get

# Implementar regras seguras
# Ver: SECURITY_MITIGATION.md
```

**Arquivos:**
- `firestore.rules`
- `storage.rules`

#### 1.2 ProGuard/R8 para Produção
**Impacto:** HIGH  
**Esforço:** 1-2 horas

```kotlin
// androidApp/proguard-rules.pro
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

# Firebase
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }

# Koin
-keep class org.koin.** { *; }
```

#### 1.3 API Key Restrictions
**Impacto:** HIGH  
**Esforço:** 30 min

**Google Maps API:**
1. Google Cloud Console → Credentials
2. Restringir por:
   - Application restrictions: Android apps
   - Package name: `com.afilaxy.app`
   - SHA-1: (do seu keystore)

**Firebase API:**
1. Firebase Console → Project Settings
2. Restringir por package name

---

### 🟡 PRIORITY 2: Validação e Tratamento de Erros

#### 2.1 Validação de Entrada
**Impacto:** MEDIUM  
**Esforço:** 4-6 horas

**Arquivos a revisar:**
- `shared/src/commonMain/kotlin/com/afilaxy/presentation/*/ViewModel.kt`

**Exemplo:**
```kotlin
// Antes
fun updateEmail(email: String) {
    _email.value = email
}

// Depois
fun updateEmail(email: String) {
    if (email.isValidEmail()) {
        _email.value = email
        _emailError.value = null
    } else {
        _emailError.value = "Email inválido"
    }
}

private fun String.isValidEmail(): Boolean {
    return android.util.Patterns.EMAIL_ADDRESS.matcher(this).matches()
}
```

#### 2.2 Tratamento de Exceções
**Impacto:** MEDIUM  
**Esforço:** 3-4 horas

```kotlin
// Criar sealed class para resultados
sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val exception: Throwable) : Result<Nothing>()
    object Loading : Result<Nothing>()
}

// Usar em ViewModels
viewModelScope.launch {
    _state.value = Result.Loading
    try {
        val data = repository.getData()
        _state.value = Result.Success(data)
    } catch (e: Exception) {
        _state.value = Result.Error(e)
        // Log sem dados sensíveis
        logger.error("Failed to load data", e)
    }
}
```

---

### 🟢 PRIORITY 3: Arquitetura e Qualidade

#### 3.1 Mover Lógica para UseCases
**Impacto:** MEDIUM  
**Esforço:** 6-8 horas

**ViewModels com lógica de negócio:**
- `EmergencyViewModel` - Cálculo de distância
- `ChatViewModel` - Formatação de mensagens
- `ProfileViewModel` - Validações complexas

**Refatoração:**
```kotlin
// Criar UseCase
class CalculateDistanceUseCase {
    operator fun invoke(
        lat1: Double, lon1: Double,
        lat2: Double, lon2: Double
    ): Double {
        // Lógica Haversine aqui
    }
}

// Injetar no ViewModel
class EmergencyViewModel(
    private val calculateDistance: CalculateDistanceUseCase
) : ViewModel() {
    fun findNearbyHelpers() {
        val distance = calculateDistance(userLat, userLon, helperLat, helperLon)
    }
}
```

#### 3.2 Null Safety
**Impacto:** LOW  
**Esforço:** 2-3 horas

**Buscar e substituir:**
```bash
# Encontrar uso de !!
grep -r "!!" shared/src/commonMain/kotlin/ --include="*.kt"

# Substituir por safe calls
user?.name ?: "Unknown"
```

#### 3.3 Coroutines Lifecycle
**Impacto:** MEDIUM  
**Esforço:** 2-3 horas

```kotlin
// Garantir cancelamento
class MyViewModel : ViewModel() {
    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.Main + job)
    
    override fun onCleared() {
        super.onCleared()
        job.cancel()
    }
}
```

---

### 🔵 PRIORITY 4: Testes

#### 4.1 Testes Unitários
**Impacto:** LOW  
**Esforço:** 8-12 horas

**Cobertura mínima:**
- UseCases: 80%
- ViewModels: 60%
- Repositories: 70%

```kotlin
class CalculateDistanceUseCaseTest {
    @Test
    fun `calculate distance between two points`() {
        val useCase = CalculateDistanceUseCase()
        val distance = useCase(
            -23.550520, -46.633308,  // São Paulo
            -22.906847, -43.172896   // Rio de Janeiro
        )
        assertEquals(357.0, distance, 10.0) // ~357km ±10km
    }
}
```

---

## 📊 Estimativa de Tempo Total

| Prioridade | Tarefas | Tempo Estimado |
|-----------|---------|----------------|
| P1 - Critical | 3 | 4-7 horas |
| P2 - High | 2 | 7-10 horas |
| P3 - Medium | 3 | 10-14 horas |
| P4 - Low | 1 | 8-12 horas |
| **TOTAL** | **9** | **29-43 horas** |

---

## 🚀 Plano de Execução Recomendado

### Sprint 1 (1 semana) - Segurança
- [ ] Firebase Security Rules
- [ ] ProGuard/R8
- [ ] API Key Restrictions
- [ ] Validação de entrada básica

### Sprint 2 (1 semana) - Qualidade
- [ ] Tratamento de exceções
- [ ] Refatoração de ViewModels
- [ ] Null safety

### Sprint 3 (1 semana) - Testes
- [ ] Testes unitários UseCases
- [ ] Testes ViewModels
- [ ] CI/CD com testes

---

## ✅ Checklist Antes do Deploy

```bash
# 1. Executar verificação de segurança
./scripts/check-security.sh

# 2. Executar testes
./gradlew test

# 3. Build de release
./gradlew androidApp:assembleRelease

# 4. Verificar ProGuard
# Analisar androidApp/build/outputs/mapping/release/

# 5. Testar em dispositivo real
adb install androidApp/build/outputs/apk/release/app-release.apk

# 6. Verificar Firebase Security Rules
firebase firestore:rules:get

# 7. Monitorar logs
firebase functions:log
```

---

## 📞 Suporte

Para dúvidas sobre implementação:
1. Consulte `SECURITY_MITIGATION.md`
2. Revise `ARCHITECTURE.md`
3. Veja exemplos em `shared/src/commonMain/kotlin/`

**Última atualização:** $(date)
