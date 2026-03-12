# 🚀 Quick Start - Sistema de Logs

## Para Desenvolvedores

### 1. Usar Logger em ViewModels/Repositories

```kotlin
import com.afilaxy.util.logDebug
import com.afilaxy.util.logInfo
import com.afilaxy.util.logError

class MyViewModel : KMMViewModel() {
    
    fun doSomething() {
        logInfo("Starting operation")
        
        try {
            // código
            logDebug("Operation details: ...")
        } catch (e: Exception) {
            logError("Operation failed", e)
        }
    }
}
```

### 2. Exportar Logs (Usuário Final)

1. Abrir app
2. Ir em **Configurações**
3. Seção **Desenvolvedor**
4. Tocar **"Exportar Logs"**
5. Compartilhar via AirDrop/Mail/WhatsApp

### 3. Analisar Logs

```
[2026-03-11T13:31:22.169Z] [INFO] [EmergencyViewModel] Creating emergency
[2026-03-11T13:31:22.926Z] [DEBUG] [LocationRepository] Location: lat=37.42, lon=-122.08
[2026-03-11T13:31:34.024Z] [ERROR] [Firestore] Query failed: FAILED_PRECONDITION
```

## Níveis de Log

- **DEBUG**: Detalhes técnicos (coordenadas, IDs, estados)
- **INFO**: Eventos importantes (login, criar emergência, match)
- **WARN**: Situações anormais mas não críticas
- **ERROR**: Erros que impedem funcionalidade

## Sanitização Automática

✅ Senhas, tokens, emails, CPF são **automaticamente removidos** dos logs.

## Localização dos Logs

iOS: `Library/Caches/logs/afilaxy_YYYY-MM-DD.log`

## Limites

- **5MB** por arquivo (rotação automática)
- **7 dias** de histórico (limpeza automática)
