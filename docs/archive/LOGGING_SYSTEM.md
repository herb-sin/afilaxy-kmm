# 📝 Sistema de Logs iOS - Afilaxy

## 🎯 Objetivo

Sistema de logging multiplataforma que salva logs em arquivo local no iOS para facilitar debug remoto e análise de problemas reportados por usuários.

---

## 🏗️ Arquitetura

```
┌─────────────────────────────────────┐
│   SwiftUI Views (iOS)               │
│   - Eventos de UI                   │
│   - Navegação                       │
└──────────────┬──────────────────────┘
               │
┌──────────────▼──────────────────────┐
│   FileLogger.swift                  │
│   - Escreve em Cache/logs/          │
│   - Rotação automática (5MB)        │
│   - Limpeza (7 dias)                │
└──────────────┬──────────────────────┘
               │
┌──────────────▼──────────────────────┐
│   Logger.kt (Shared - KMM)          │
│   - expect/actual pattern           │
│   - Sanitização de dados sensíveis  │
└──────────────┬──────────────────────┘
               │
┌──────────────▼──────────────────────┐
│   ViewModels & Repositories         │
│   - logDebug(), logInfo()           │
│   - logWarning(), logError()        │
└─────────────────────────────────────┘
```

---

## 📁 Estrutura de Arquivos

### **Kotlin (Shared)**
```
shared/src/
├── commonMain/kotlin/com/afilaxy/util/
│   └── Logger.kt                    # Interface expect
├── androidMain/kotlin/com/afilaxy/util/
│   └── Logger.kt                    # Implementação Android (Logcat)
└── iosMain/kotlin/com/afilaxy/util/
    ├── Logger.kt                    # Implementação iOS
    └── IOSLogBridge.kt              # Bridge Kotlin → Swift
```

### **Swift (iOS)**
```
iosApp/iosApp/
├── Helpers/
│   └── FileLogger.swift             # Logger que salva em arquivo
└── Views/
    └── SettingsView.swift           # UI para exportar/limpar logs
```

### **Logs Salvos**
```
/var/mobile/Containers/Data/Application/{UUID}/
└── Library/
    └── Caches/
        └── logs/
            ├── afilaxy_2026-03-11.log  (hoje)
            ├── afilaxy_2026-03-10.log  (ontem)
            └── afilaxy_2026-03-09.log  (7 dias atrás)
```

---

## 🔧 Como Usar

### **1. Em Código Kotlin (ViewModels, Repositories)**

```kotlin
import com.afilaxy.util.logDebug
import com.afilaxy.util.logInfo
import com.afilaxy.util.logError

class EmergencyViewModel(...) : KMMViewModel() {
    
    fun onCreateEmergency() {
        logInfo("Creating new emergency")
        
        val location = locationRepository.getCurrentLocation()
        if (location == null) {
            logError("Failed to get location")
            return
        }
        
        logDebug("Location: lat=${location.latitude}, lon=${location.longitude}")
        
        createEmergencyUseCase.execute(emergency)
            .onSuccess { id ->
                logInfo("Emergency created: $id")
            }
            .onFailure { exception ->
                logError("Failed to create emergency", exception)
            }
    }
}
```

### **2. Em Código Swift (Views, Helpers)**

```swift
import os.log

// Logs automáticos via FileLogger
FileLogger.shared.write(level: "INFO", tag: "HomeView", message: "View appeared")

// Ou via OSLog (também salva em arquivo)
os_log("User tapped emergency button", type: .info)
```

### **3. Exportar Logs (Usuário)**

1. Abrir app → **Configurações**
2. Seção **Desenvolvedor**
3. Tocar em **"Exportar Logs"**
4. Escolher destino:
   - AirDrop
   - Mail
   - WhatsApp
   - Files (iCloud Drive)

---

## 📊 Formato de Log

```
[2026-03-11T13:31:22.169Z] [INFO] [FirebaseAuth] Logging in as afilaxy@gmail.com
[2026-03-11T13:31:22.926Z] [DEBUG] [EmergencyViewModel] Location obtained: lat=37.42, lon=-122.08
[2026-03-11T13:31:34.024Z] [ERROR] [Firestore] Query failed: FAILED_PRECONDITION
[2026-03-11T13:31:34.024Z] [ERROR] [Firestore] Index required: https://console.firebase.google.com/...
```

**Campos:**
- `[Timestamp ISO8601]` - Data/hora UTC com milissegundos
- `[Level]` - DEBUG, INFO, WARN, ERROR
- `[Tag]` - Classe/componente que gerou o log
- `Message` - Mensagem descritiva

---

## 🔒 Segurança & Privacidade

### **Sanitização Automática**

O Logger **remove automaticamente** dados sensíveis:

```kotlin
// ANTES (código)
Logger.i("Auth", "Token: Bearer abc123xyz")
Logger.d("User", "Email: user@example.com")
Logger.d("Payment", "CPF: 123.456.789-00")

// DEPOIS (arquivo de log)
[INFO] [Auth] Token: [REDACTED]
[DEBUG] [User] Email: [REDACTED]
[DEBUG] [Payment] CPF: [REDACTED]
```

**Padrões Sanitizados:**
- Senhas (`password=...`)
- Tokens (`token=...`, `Bearer ...`)
- Emails completos
- CPF
- Coordenadas GPS (truncadas para 2 decimais)

### **Armazenamento Local**

- ✅ Logs ficam **apenas no dispositivo** (Cache directory)
- ✅ **Não sincronizam** com iCloud
- ✅ **Não aparecem** em backups iTunes/Finder
- ✅ Sistema pode **limpar automaticamente** se espaço baixo
- ✅ Usuário controla exportação via Share Sheet

---

## ⚙️ Configurações

### **Limites Automáticos**

```swift
// FileLogger.swift
private let maxFileSize: Int64 = 5 * 1024 * 1024  // 5MB por arquivo
private let maxLogAge: TimeInterval = 7 * 24 * 60 * 60  // 7 dias
```

### **Rotação de Logs**

Quando arquivo atinge **5MB**:
1. Arquivo atual renomeado: `afilaxy_2026-03-11.log` → `afilaxy_2026-03-11.1710172800.log`
2. Novo arquivo criado: `afilaxy_2026-03-11.log`

### **Limpeza Automática**

Logs com mais de **7 dias** são deletados automaticamente no próximo launch do app.

---

## 🎨 UI - Menu Desenvolvedor

### **SettingsView.swift**

```
┌─────────────────────────────────┐
│ Desenvolvedor                   │
├─────────────────────────────────┤
│ 📤 Exportar Logs                │
│ 🗑️  Limpar Logs                 │
│ Tamanho dos Logs: 2.3 MB        │
└─────────────────────────────────┘
```

**Funcionalidades:**
- **Exportar Logs**: Abre Share Sheet com todos os arquivos de log
- **Limpar Logs**: Remove todos os logs (com confirmação)
- **Tamanho**: Mostra espaço ocupado pelos logs

---

## 🐛 Debug & Troubleshooting

### **Verificar se Logs Estão Sendo Salvos**

```swift
// No Xcode Console
print(FileLogger.shared.getLogFileURL()?.path ?? "No log file")
// Output: /var/mobile/.../Library/Caches/logs/afilaxy_2026-03-11.log
```

### **Inspecionar Logs no Simulador**

```bash
# Encontrar diretório do app
xcrun simctl get_app_container booted com.afilaxy.app data

# Navegar até logs
cd "$(xcrun simctl get_app_container booted com.afilaxy.app data)/Library/Caches/logs"

# Ver logs
cat afilaxy_*.log
```

### **Logs Não Aparecem?**

1. Verificar se `FileLogger.shared` está sendo inicializado
2. Verificar permissões do Cache directory
3. Verificar se há espaço em disco
4. Checar Xcode Console para erros do FileLogger

---

## 📈 Performance

### **Escrita Assíncrona**

```swift
private let queue = DispatchQueue(label: "com.afilaxy.filelogger", qos: .utility)

func write(level: String, tag: String, message: String) {
    queue.async { [weak self] in
        // Escrita em background thread
    }
}
```

**Impacto:**
- ✅ Não bloqueia UI thread
- ✅ QoS `.utility` (baixa prioridade)
- ✅ Escrita em batch (buffer)

### **Overhead Estimado**

- Escrita de log: **~0.1ms** (assíncrono)
- Rotação de arquivo: **~10ms** (raro, apenas quando atinge 5MB)
- Limpeza de logs antigos: **~50ms** (apenas no launch)

---

## 🚀 Próximos Passos (Opcional)

### **Fase 2: Melhorias**

1. **Toggle "Logs Detalhados"**
   - Desabilitar logs DEBUG em produção
   - Apenas INFO, WARN, ERROR

2. **Upload Automático em Crashes**
   - Integrar com Firebase Crashlytics
   - Enviar logs automaticamente em crashes

3. **Compressão de Logs**
   - Comprimir logs antigos (.zip)
   - Reduzir espaço em disco

4. **Filtros de Log**
   - Filtrar por nível (DEBUG, INFO, etc.)
   - Filtrar por tag (ViewModel, Repository, etc.)

---

## 📄 Licença

MIT License - Mesmo do projeto Afilaxy

---

## 🙏 Créditos

Implementado para facilitar debug remoto e melhorar qualidade do app Afilaxy iOS.

**Desenvolvido com ❤️ para democratizar o acesso à saúde respiratória no Brasil** 🇧🇷
