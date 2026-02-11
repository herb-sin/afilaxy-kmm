# Deployment - Afilaxy KMM

## 🚀 Build e Publicação

---

## 📱 Android

### Pré-requisitos
- JDK 17+
- Android Studio Arctic Fox+
- `google-services.json` configurado

### Build Debug
```bash
./gradlew assembleDebug
```

APK gerado em: `androidApp/build/outputs/apk/debug/`

### Build Release
```bash
# 1. Configurar keystore
keytool -genkey -v -keystore afilaxy-release.keystore \
  -alias afilaxy -keyalg RSA -keysize 2048 -validity 10000

# 2. Adicionar em gradle.properties
RELEASE_STORE_FILE=../afilaxy-release.keystore
RELEASE_STORE_PASSWORD=sua_senha
RELEASE_KEY_ALIAS=afilaxy
RELEASE_KEY_PASSWORD=sua_senha

# 3. Build
./gradlew assembleRelease
```

APK gerado em: `androidApp/build/outputs/apk/release/`

### Google Play Console

#### 1. Criar App
- Acesse [Google Play Console](https://play.google.com/console)
- Criar aplicativo
- Preencher informações

#### 2. Upload APK/AAB
```bash
# Gerar AAB (recomendado)
./gradlew bundleRelease
```

AAB gerado em: `androidApp/build/outputs/bundle/release/`

#### 3. Configurar Listagem
- Título: Afilaxy
- Descrição curta: Sistema de emergência médica
- Categoria: Saúde e fitness
- Screenshots (mínimo 2)
- Ícone 512x512

#### 4. Classificação de Conteúdo
- Responder questionário
- Classificação: Livre

#### 5. Publicar
- Teste interno → Teste fechado → Produção

---

## 🍎 iOS

### Pré-requisitos
- macOS 12+
- Xcode 14+
- Apple Developer Account ($99/ano)
- `GoogleService-Info.plist` configurado

### Build Shared Framework
```bash
cd shared
./gradlew :shared:assembleXCFramework
```

Framework gerado em: `shared/build/XCFrameworks/release/`

### Instalar Dependências
```bash
cd iosApp
pod install
```

### Build Debug
```bash
open iosApp.xcworkspace
# Xcode: Product → Build (Cmd+B)
# Selecionar simulador
# Product → Run (Cmd+R)
```

### Build Release

#### 1. Configurar Signing
```
Xcode → iosApp target → Signing & Capabilities
- Team: Selecionar sua conta
- Bundle Identifier: com.afilaxy.app
- Provisioning Profile: Automatic
```

#### 2. Archive
```
Xcode → Product → Archive
```

#### 3. Distribuir
```
Window → Organizer → Archives
- Selecionar archive
- Distribute App
- App Store Connect
- Upload
```

### App Store Connect

#### 1. Criar App
- Acesse [App Store Connect](https://appstoreconnect.apple.com)
- My Apps → + → New App
- Platform: iOS
- Name: Afilaxy
- Bundle ID: com.afilaxy.app

#### 2. Preencher Informações
- Subtitle: Sistema de emergência
- Description: [descrição completa]
- Keywords: emergência, saúde, ajuda
- Screenshots (obrigatório)
- App Icon 1024x1024

#### 3. Configurar Build
- TestFlight → Selecionar build
- Adicionar testers
- Testar

#### 4. Submeter para Revisão
- App Store → Prepare for Submission
- Pricing: Grátis
- Availability: Todos países
- Submit for Review

---

## 🔧 CI/CD (Opcional)

### GitHub Actions

#### Android
```yaml
# .github/workflows/android.yml
name: Android CI

on: [push, pull_request]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - name: Set up JDK 17
        uses: actions/setup-java@v3
        with:
          java-version: '17'
      - name: Build with Gradle
        run: ./gradlew assembleDebug
      - name: Upload APK
        uses: actions/upload-artifact@v3
        with:
          name: app-debug
          path: androidApp/build/outputs/apk/debug/*.apk
```

#### iOS
```yaml
# .github/workflows/ios.yml
name: iOS CI

on: [push, pull_request]

jobs:
  build:
    runs-on: macos-latest
    steps:
      - uses: actions/checkout@v3
      - name: Build Shared Framework
        run: cd shared && ./gradlew assembleXCFramework
      - name: Install Pods
        run: cd iosApp && pod install
      - name: Build iOS
        run: xcodebuild -workspace iosApp/iosApp.xcworkspace -scheme iosApp -sdk iphonesimulator
```

---

## 🔐 Configuração Firebase

### 1. Criar Projeto
- Acesse [Firebase Console](https://console.firebase.google.com)
- Adicionar projeto
- Nome: Afilaxy

### 2. Adicionar Apps

#### Android
```
Project Settings → Add app → Android
- Package name: com.afilaxy.app
- Download google-services.json
- Colocar em androidApp/
```

#### iOS
```
Project Settings → Add app → iOS
- Bundle ID: com.afilaxy.app
- Download GoogleService-Info.plist
- Colocar em iosApp/iosApp/
```

### 3. Configurar Firestore
```
Firestore Database → Create database
- Start in production mode
- Location: us-central1
```

### 4. Security Rules
```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /users/{userId} {
      allow read, write: if request.auth.uid == userId;
    }
    
    match /emergency_requests/{emergencyId} {
      allow read: if request.auth != null;
      allow create: if request.auth != null;
      allow update: if request.auth.uid == resource.data.requesterId
                    || request.auth.uid == resource.data.helperId;
    }
    
    match /chats/{chatId} {
      allow read, write: if request.auth != null;
    }
    
    match /ratings/{ratingId} {
      allow read: if request.auth != null;
      allow create: if request.auth != null;
    }
  }
}
```

### 5. Configurar FCM (Android)
```
Cloud Messaging → Android apps
- Upload SHA-1 certificate
```

### 6. Configurar APNs (iOS)
```
Cloud Messaging → iOS apps
- Upload APNs certificate
```

---

## 📊 Versionamento

### Semantic Versioning
```
MAJOR.MINOR.PATCH-suffix

Exemplo: 2.1.0-kmm
- MAJOR: 2 (breaking changes)
- MINOR: 1 (new features)
- PATCH: 0 (bug fixes)
- suffix: kmm (Kotlin Multiplatform)
```

### Android (build.gradle.kts)
```kotlin
android {
    defaultConfig {
        versionCode = 16
        versionName = "2.1.0-kmm"
    }
}
```

### iOS (Info.plist)
```xml
<key>CFBundleShortVersionString</key>
<string>2.1.0</string>
<key>CFBundleVersion</key>
<string>16</string>
```

---

## 🧪 Testes Antes de Publicar

### Checklist Android
- [ ] Build release sem erros
- [ ] ProGuard/R8 configurado
- [ ] Permissões corretas no Manifest
- [ ] Firebase configurado
- [ ] FCM funcionando
- [ ] GPS funcionando
- [ ] Testar em device real
- [ ] Testar em Android 10+

### Checklist iOS
- [ ] Build archive sem erros
- [ ] Signing configurado
- [ ] Permissões no Info.plist
- [ ] Firebase configurado
- [ ] Testar em device real
- [ ] Testar em iOS 14+

---

## 📝 Notas

### Android
- **versionCode** deve ser incrementado a cada release
- **Keystore** deve ser guardado com segurança
- **SHA-1** necessário para Firebase Auth

### iOS
- **Bundle ID** deve ser único
- **Provisioning Profile** necessário
- **APNs certificate** necessário para push

### Firebase
- **google-services.json** e **GoogleService-Info.plist** não devem ser commitados
- Adicionar ao `.gitignore`

---

## 🚀 Deploy Rápido

### Android
```bash
# 1. Build
./gradlew bundleRelease

# 2. Upload para Play Console
# Manual ou via fastlane
```

### iOS
```bash
# 1. Build framework
cd shared && ./gradlew assembleXCFramework

# 2. Archive
open iosApp.xcworkspace
# Xcode: Product → Archive

# 3. Upload
# Window → Organizer → Distribute
```

---

**Desenvolvido com ❤️ usando Kotlin Multiplatform** 🚀
