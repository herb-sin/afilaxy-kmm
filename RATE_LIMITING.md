# 🛡️ Rate Limiting - Afilaxy KMM

## Implementado

Rate limiting para prevenir abuso e garantir qualidade do serviço.

---

## 📋 Limites Configurados

### 1. Login
- **Limite**: 5 tentativas por minuto
- **Por**: Email do usuário
- **Mensagem**: "Muitas tentativas. Aguarde X segundos."

### 2. Criação de Emergências
- **Limite**: 3 emergências por hora
- **Por**: User ID
- **Mensagem**: "Limite atingido. Aguarde X minutos."

### 3. Chat (Mensagens)
- **Limite**: 30 mensagens por minuto
- **Por**: User ID
- **Mensagem**: "Muitas mensagens. Aguarde X segundos."

### 4. Registro
- **Limite**: 3 tentativas por hora
- **Por**: Email do usuário
- **Mensagem**: "Muitas tentativas. Aguarde X minutos."

---

## 🏗️ Arquitetura

### RateLimiter (Genérico)
```kotlin
class RateLimiter(
    maxAttempts: Int,      // Máximo de tentativas
    windowMillis: Long     // Janela de tempo em ms
)
```

**Métodos:**
- `checkLimit(key: String)`: Verifica se pode executar ação
- `reset(key: String)`: Reseta contador (após sucesso)

**Retorno:**
- `RateLimitResult.Allowed`: Pode executar
- `RateLimitResult.Limited(waitTimeMillis)`: Bloqueado

### RateLimitManager (Singleton)
Gerencia todos os limiters da aplicação:
- `loginLimiter`
- `emergencyLimiter`
- `chatLimiter`
- `registerLimiter`

---

## 💻 Como Usar

### Exemplo: LoginViewModelWrapper

```kotlin
fun onLoginClick() {
    viewModelScope.launch {
        val email = state.value.email
        
        when (val result = RateLimitManager.loginLimiter.checkLimit(email)) {
            is RateLimitResult.Allowed -> {
                // Executar login
                sharedViewModel.onLoginClick()
            }
            is RateLimitResult.Limited -> {
                val seconds = (result.waitTimeMillis / 1000).toInt()
                _rateLimitState.value = "Muitas tentativas. Aguarde $seconds segundos."
            }
        }
    }
}
```

### Exemplo: EmergencyViewModelWrapper

```kotlin
fun onCreateEmergency() {
    viewModelScope.launch {
        val userId = authRepository.getCurrentUser()?.uid ?: return@launch
        
        when (val result = RateLimitManager.emergencyLimiter.checkLimit(userId)) {
            is RateLimitResult.Allowed -> {
                sharedViewModel.onCreateEmergency()
            }
            is RateLimitResult.Limited -> {
                val minutes = (result.waitTimeMillis / 60_000).toInt()
                _rateLimitState.value = "Limite atingido. Aguarde $minutes minutos."
            }
        }
    }
}
```

---

## 🎨 UI Integration

### LoginScreen
```kotlin
val rateLimitError by viewModel.rateLimitState.collectAsState()

rateLimitError?.let { error ->
    Text(
        text = error,
        color = MaterialTheme.colorScheme.error
    )
}
```

---

## 🔧 Configuração

Para ajustar limites, edite `RateLimitManager.kt`:

```kotlin
val loginLimiter = RateLimiter(
    maxAttempts = 5,        // Altere aqui
    windowMillis = 60_000L  // 1 minuto
)
```

---

## 🧪 Testes

### Testar Login Rate Limit
1. Tente fazer login 5 vezes com senha errada
2. Na 6ª tentativa, deve aparecer mensagem de bloqueio
3. Aguarde 1 minuto
4. Tente novamente (deve funcionar)

### Testar Emergency Rate Limit
1. Crie 3 emergências em sequência
2. Na 4ª tentativa, deve aparecer mensagem de bloqueio
3. Aguarde 1 hora (ou ajuste o limite para testar)

### Testar Chat Rate Limit
1. Envie 30 mensagens rapidamente
2. Na 31ª mensagem, deve aparecer bloqueio
3. Aguarde 1 minuto

---

## 📊 Monitoramento

### Logs
Rate limiting gera logs automáticos:
```
RateLimiter: User [email] blocked for 45 seconds
RateLimiter: User [userId] allowed (3/5 attempts)
```

### Métricas Recomendadas
- Total de bloqueios por dia
- Usuários mais bloqueados
- Picos de tentativas

---

## 🚀 Melhorias Futuras

### P2 - Opcional
1. **Persistência**: Salvar limites no SharedPreferences
2. **Backend**: Mover rate limiting para Firebase Functions
3. **Adaptativo**: Ajustar limites baseado em comportamento
4. **Whitelist**: Permitir usuários específicos sem limite
5. **Analytics**: Enviar eventos de rate limit para Firebase

---

## 🔒 Segurança

### Proteções Implementadas
- ✅ Previne brute force em login
- ✅ Previne spam de emergências
- ✅ Previne flood de mensagens
- ✅ Thread-safe (Mutex)
- ✅ Limpeza automática de tentativas antigas

### Limitações
- ⚠️ Rate limit é local (por dispositivo)
- ⚠️ Pode ser resetado limpando dados do app
- ⚠️ Não protege contra múltiplos dispositivos

**Recomendação**: Implementar rate limiting no backend (Firebase Functions) para proteção completa.

---

## 📝 Arquivos Criados

```
androidApp/src/main/kotlin/com/afilaxy/app/
├── security/
│   ├── RateLimiter.kt              # Implementação genérica
│   └── RateLimitManager.kt         # Gerenciador singleton
└── presentation/
    ├── login/
    │   └── LoginViewModelWrapper.kt
    ├── emergency/
    │   └── EmergencyViewModelWrapper.kt
    └── chat/
        └── ChatViewModelWrapper.kt
```

---

**Implementado em**: 2026-02-10  
**Status**: ✅ Pronto para produção
