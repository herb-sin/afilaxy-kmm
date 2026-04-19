# 🔧 Guia de Configuração - Produção

## ✅ Correções P0 Implementadas

### 1. Validação de Input ✅
- LoginScreen agora valida email antes de enviar
- InputSanitizer integrado

### 2. ProGuard Configurado ✅
- Arquivo `proguard-rules.pro` criado
- Build release com minify e shrink habilitados
- Logs removidos em produção

### 3. Permissão Background Location ✅
- Android Q+ solicita ACCESS_BACKGROUND_LOCATION
- Rationale atualizado

### 4. Firebase Functions Preparadas ✅
- Arquivos de configuração criados
- Script de deploy pronto

---

## 📋 Checklist Pré-Produção

### Configurações Obrigatórias

#### 1. Google Maps API Key
**Arquivo**: `androidApp/src/main/AndroidManifest.xml`
**Linha**: 19

```xml
<meta-data
    android:name="com.google.android.geo.API_KEY"
    android:value="SUA_CHAVE_AQUI" />
```

**Como obter**:
1. Acesse: https://console.cloud.google.com/
2. Ative Maps SDK for Android
3. Crie credencial (API Key)
4. Restrinja por package name: `com.afilaxy.app`

---

#### 2. Deploy Firebase Functions
**Comando**:
```bash
./deploy-functions.sh
```

**Ou manualmente**:
```bash
cd functions
npm install
firebase deploy --only functions
```

**Verificar**:
```bash
firebase functions:log
```

---

#### 3. Configurar Firebase Project
**Arquivo**: `.firebaserc`
**Alterar**: `"default": "SEU_PROJECT_ID"`

**Como obter Project ID**:
1. Firebase Console: https://console.firebase.google.com/
2. Project Settings → General
3. Copiar "Project ID"

---

#### 4. Keystore para Release
**Criar keystore**:
```bash
keytool -genkey -v -keystore afilaxy-release.keystore \
  -alias afilaxy -keyalg RSA -keysize 2048 -validity 10000
```

**Configurar no build.gradle**:
```kotlin
signingConfigs {
    create("release") {
        storeFile = file("../afilaxy-release.keystore")
        storePassword = "SUA_SENHA"
        keyAlias = "afilaxy"
        keyPassword = "SUA_SENHA"
    }
}
```

---

### Configurações Recomendadas

#### 5. Firebase Crashlytics
**build.gradle**:
```kotlin
plugins {
    id("com.google.firebase.crashlytics")
}

dependencies {
    implementation("com.google.firebase:firebase-crashlytics-ktx")
}
```

**Inicializar**:
```kotlin
// AflixyApplication.kt
FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true)
```

---

#### 6. Rate Limiting (Firebase Security Rules)
**Firestore Rules**:
```javascript
match /emergency_requests/{emergencyId} {
  allow create: if request.auth != null 
    && request.time > resource.data.lastRequest + duration.value(1, 'm');
}
```

---

#### 7. Network Security Config
**Arquivo**: `androidApp/src/main/res/xml/network_security_config.xml`
```xml
<?xml version="1.0" encoding="utf-8"?>
<network-security-config>
    <base-config cleartextTrafficPermitted="false">
        <trust-anchors>
            <certificates src="system" />
        </trust-anchors>
    </base-config>
</network-security-config>
```

**AndroidManifest.xml**:
```xml
<application
    android:networkSecurityConfig="@xml/network_security_config">
```

---

## 🚀 Build Release

### 1. Build APK Release
```bash
export JAVA_HOME="/home/afilaxy/Projetos/afilaxy Kotlin/afilaxy/jdk-17.0.2"
./gradlew assembleRelease
```

**Output**: `androidApp/build/outputs/apk/release/androidApp-release.apk`

### 2. Build AAB (Google Play)
```bash
./gradlew bundleRelease
```

**Output**: `androidApp/build/outputs/bundle/release/androidApp-release.aab`

---

## ✅ Testes Pré-Produção

### Testes Obrigatórios
- [ ] Login/Logout
- [ ] Criar emergência
- [ ] Aceitar emergência
- [ ] Chat em tempo real
- [ ] Notificações push
- [ ] Modo helper
- [ ] Permissões de localização
- [ ] Maps funcionando
- [ ] Tela de emergência overlay

### Testes de Segurança
- [ ] Input validation (SQL injection)
- [ ] ProGuard funcionando (código ofuscado)
- [ ] Logs não aparecem em release
- [ ] HTTPS only

### Testes de Performance
- [ ] App não trava (ANR)
- [ ] Consumo de bateria aceitável
- [ ] Uso de memória normal
- [ ] Notificações chegam rápido (<5s)

---

## 📊 Monitoramento

### Firebase Console
- Analytics: https://console.firebase.google.com/project/SEU_PROJECT/analytics
- Crashlytics: https://console.firebase.google.com/project/SEU_PROJECT/crashlytics
- Functions Logs: https://console.firebase.google.com/project/SEU_PROJECT/functions/logs

### Comandos Úteis
```bash
# Ver logs do app
adb logcat -s com.afilaxy.app

# Ver logs Firebase Functions
firebase functions:log

# Testar notificações
firebase messaging:send --token TOKEN_FCM
```

---

## 🔒 Segurança Final

### Checklist
- [x] ProGuard habilitado
- [x] Input sanitization
- [x] Logs removidos em release
- [ ] Certificate pinning (opcional)
- [ ] Root detection (opcional)
- [ ] Crashlytics configurado
- [ ] Rate limiting configurado
- [ ] Network security config

---

## 📝 Notas Importantes

1. **Nunca commitar**:
   - `google-services.json` (já no .gitignore)
   - `keystore` files
   - API keys
   - Senhas

2. **Antes de cada release**:
   - Incrementar `versionCode` e `versionName`
   - Testar em múltiplos dispositivos
   - Verificar ProGuard não quebrou nada
   - Testar notificações push

3. **Após deploy**:
   - Monitorar Crashlytics
   - Verificar Analytics
   - Checar logs Firebase Functions
   - Testar em produção

---

## 🆘 Troubleshooting

### Maps não funciona
- Verificar API Key no manifest
- Verificar restrições no Google Cloud Console
- Verificar billing habilitado

### Notificações não chegam
- Verificar Firebase Functions deployadas
- Verificar FCM token salvo no Firestore
- Verificar logs: `firebase functions:log`

### App crasha em release
- Verificar ProGuard rules
- Testar com `./gradlew assembleRelease`
- Verificar Crashlytics

---

**Status**: ✅ Pronto para configuração e testes finais
