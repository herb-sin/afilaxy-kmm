# 🗺️ Google Maps API Setup - Afilaxy KMM

## ✅ Configuração Concluída

### API Key Configurada
- **API Key**: `AIzaSyCujufLrPYhqCL6_ObFO9GFnWz9LOopd9s`
- **Android**: Configurada via `local.properties` → `AndroidManifest.xml`
- **iOS**: Configurada no `Info.plist` + inicialização no `AfilaxyApp.swift`

### Restrições de Segurança
- **Application restrictions**: Websites (para desenvolvimento web)
- **API restrictions**: Maps SDK for Android, Maps SDK for iOS, Geocoding API

## 🚀 Como Usar

### Setup Automático
```bash
./setup-maps.sh
```

### Setup Manual

#### Android
1. API key já configurada em `local.properties`
2. Build: `./gradlew androidApp:assembleDebug`

#### iOS
1. Instalar dependências: `cd iosApp && pod install`
2. Build: `xcodebuild -workspace iosApp.xcworkspace -scheme iosApp build`

## 🔧 Arquivos Modificados

### Android
- `local.properties`: API key adicionada
- `androidApp/src/main/AndroidManifest.xml`: Já configurado com `${MAPS_API_KEY}`
- `androidApp/build.gradle.kts`: Já configurado para ler do `local.properties`

### iOS
- `iosApp/Podfile`: Google Maps SDK adicionado
- `iosApp/iosApp/Info.plist`: API key adicionada
- `iosApp/iosApp/AfilaxyApp.swift`: Inicialização do GMSServices

## 🧪 Teste

### Verificar se os mapas carregam:
- **Android**: `EmergencyScreen` → Mapa deve aparecer
- **iOS**: `EmergencyView` → Mapa deve aparecer

### Debug
- Verifique logs para erros de autenticação
- Confirme que as restrições da API key permitem seu package/bundle ID

## 🔒 Segurança

### Produção
- Criar API keys separadas para cada ambiente
- Restringir por package name (Android) e bundle ID (iOS)
- Monitorar uso no Google Cloud Console

### Desenvolvimento
- API key atual permite desenvolvimento local
- Não commitar API keys no repositório
- Usar variáveis de ambiente em CI/CD