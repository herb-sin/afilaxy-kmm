# ✅ Checklist de Deploy - Afilaxy

## 🔴 CRITICAL - Antes do Deploy

### Segurança
- [x] Credenciais protegidas pelo .gitignore
- [x] Firebase Security Rules implementadas
- [x] Storage Rules implementadas
- [x] ProGuard/R8 configurado
- [ ] API Keys com restrições no Google Cloud Console
- [ ] Firebase Security Rules deployed
- [ ] Certificado de release configurado

### Validação
```bash
# 1. Verificar segurança
./scripts/check-security.sh

# 2. Build de release
./gradlew androidApp:assembleRelease

# 3. Verificar ProGuard mapping
ls -la androidApp/build/outputs/mapping/release/

# 4. Deploy Firebase Rules
firebase deploy --only firestore:rules,storage:rules
```

## 🟡 HIGH - Configurações

### Google Maps API
1. Acesse: https://console.cloud.google.com/google/maps-apis
2. Selecione sua API Key
3. Configurar restrições:
   - **Application restrictions:** Android apps
   - **Package name:** `com.afilaxy.app`
   - **SHA-1:** (do seu keystore de release)

```bash
# Obter SHA-1 do keystore
keytool -list -v -keystore /path/to/your/keystore.jks
```

### Firebase API
1. Acesse: https://console.firebase.google.com/
2. Project Settings → General
3. Verificar restrições por package name

## 🟢 MEDIUM - Testes

### Testes Locais
```bash
# Testes unitários
./gradlew shared:testDebugUnitTest

# Testes Android
./gradlew androidApp:testDebugUnitTest

# Build completo
./gradlew build
```

### Testes em Dispositivo
```bash
# Instalar APK de release
adb install androidApp/build/outputs/apk/release/app-release.apk

# Verificar logs
adb logcat | grep Afilaxy
```

## 📋 Pós-Deploy

### Monitoramento
```bash
# Firebase Functions logs
firebase functions:log

# Crashlytics (se configurado)
# Verificar no Firebase Console
```

### Rollback Plan
```bash
# Se necessário, fazer rollback
firebase hosting:rollback  # Se usar hosting
# Play Store: promover versão anterior
```

## 🔒 Segurança Implementada

### ✅ Concluído
- [x] Firestore Rules com autenticação
- [x] Storage Rules com validação de tipo/tamanho
- [x] Validação de entrada em ViewModels
- [x] Sanitização de inputs
- [x] Tratamento de exceções
- [x] ProGuard remove logs em release
- [x] Credenciais não commitadas

### 📝 Regras Implementadas

**Firestore:**
- Users: apenas owner pode ler/escrever
- Emergencies: validação de campos obrigatórios
- Messages: apenas participantes podem ler/escrever
- Professionals: owner pode gerenciar
- UBS/Education: read-only (admin via Functions)

**Storage:**
- Profile images: max 5MB, apenas imagens
- Apenas owner pode fazer upload

## 🚀 Deploy Commands

```bash
# 1. Deploy Firebase Rules
firebase deploy --only firestore:rules,storage:rules

# 2. Deploy Functions (se houver mudanças)
firebase deploy --only functions

# 3. Build Android Release
./gradlew androidApp:assembleRelease

# 4. Upload para Play Store
# Use Play Console ou fastlane
```

## 📊 Métricas para Monitorar

- Crashes (Firebase Crashlytics)
- Performance (Firebase Performance)
- Autenticação (Firebase Auth)
- Firestore reads/writes
- Storage usage
- Functions invocations

## 🆘 Troubleshooting

### ProGuard Issues
```bash
# Analisar mapping
cat androidApp/build/outputs/mapping/release/mapping.txt

# Adicionar regras se necessário
# androidApp/proguard-rules.pro
```

### Firebase Rules Issues
```bash
# Testar regras localmente
firebase emulators:start --only firestore

# Ver logs de denied
# Firebase Console → Firestore → Rules → Logs
```

---

**Última verificação:** Execute `./scripts/check-security.sh`
**Status:** 🟢 Pronto para deploy após configurar API restrictions
