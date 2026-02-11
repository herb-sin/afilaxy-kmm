# 🚀 Checklist de Produção - Afilaxy KMM

## ✅ P0 - Crítico (Obrigatório)

### 1. Firebase Functions Deploy
```bash
# Instalar dependências
cd functions
npm install

# Login no Firebase
firebase login

# Deploy
firebase deploy --only functions

# Verificar logs
firebase functions:log
```

**Status**: ⏳ Pendente  
**Script**: `./deploy-functions.sh`

---

### 2. Google Maps API Key

#### 2.1 Criar API Key
1. Acesse: https://console.cloud.google.com/google/maps-apis
2. Crie chave de API
3. Copie a chave

#### 2.2 Configurar Restrições
```bash
# Obter SHA-1 Debug
keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android

# Obter SHA-1 Release (após criar keystore)
keytool -list -v -keystore release.keystore -alias afilaxy -storepass android
```

Adicione no Google Cloud Console:
- **Pacote**: `com.afilaxy.app`
- **SHA-1**: (do comando acima)

#### 2.3 Adicionar ao Projeto
Edite `local.properties`:
```properties
MAPS_API_KEY=AIzaSy...sua_chave_aqui
```

**Status**: ⏳ Pendente  
**Guia**: `MAPS_API_SETUP.md`

---

### 3. Keystore & Assinatura

#### 3.1 Criar Keystore
```bash
./create-keystore.sh
```

Ou manualmente:
```bash
keytool -genkey -v \
    -keystore release.keystore \
    -alias afilaxy \
    -keyalg RSA \
    -keysize 2048 \
    -validity 10000
```

#### 3.2 Configurar local.properties
```properties
KEYSTORE_FILE=release.keystore
KEYSTORE_PASSWORD=sua_senha_forte
KEY_ALIAS=afilaxy
KEY_PASSWORD=sua_senha_forte
```

#### 3.3 Build Release
```bash
export JAVA_HOME="/home/afilaxy/Projetos/afilaxy Kotlin/afilaxy/jdk-17.0.2"
./gradlew assembleRelease
```

APK em: `androidApp/build/outputs/apk/release/androidApp-release.apk`

**Status**: ⏳ Pendente  
**Script**: `./create-keystore.sh`

---

## 🔐 Segurança

### Verificar .gitignore
```bash
# Arquivos que NÃO devem estar no Git:
local.properties
*.keystore
*.jks
google-services.json (opcional)
GoogleService-Info.plist (opcional)
```

### Backup Seguro
```bash
# Fazer backup do keystore em local seguro
# SE PERDER O KEYSTORE, NÃO PODERÁ ATUALIZAR O APP NA PLAY STORE!
cp release.keystore ~/Backups/afilaxy-keystore-$(date +%Y%m%d).keystore
```

---

## 📱 Google Play Console

### 1. Criar App
1. Acesse: https://play.google.com/console
2. Criar aplicativo
3. Preencher informações básicas

### 2. Upload APK/AAB
```bash
# Gerar AAB (recomendado)
./gradlew bundleRelease

# AAB em: androidApp/build/outputs/bundle/release/androidApp-release.aab
```

### 3. Configurar Listagem
- Screenshots (mínimo 2)
- Ícone (512x512)
- Banner (1024x500)
- Descrição curta/longa
- Categoria: Medicina

### 4. Classificação de Conteúdo
- Responder questionário
- Classificação: Livre (provavelmente)

### 5. Política de Privacidade
- URL obrigatória
- Hospedar em: GitHub Pages, Firebase Hosting, etc.

---

## 🧪 Testes Finais

### Checklist de Testes
- [ ] Login/Registro funciona
- [ ] Email verification funciona
- [ ] Criar emergência funciona
- [ ] Notificação chega no helper
- [ ] EmergencyOverlayActivity abre
- [ ] Aceitar emergência funciona
- [ ] Chat real-time funciona
- [ ] GPS funciona
- [ ] Mapa carrega corretamente
- [ ] Resolver emergência funciona
- [ ] Avaliação funciona
- [ ] Modo Helper ativa/desativa
- [ ] Logout funciona

### Testar em Dispositivos
- [ ] Android 6.0 (API 23) - mínimo
- [ ] Android 10 (API 29) - background location
- [ ] Android 13+ (API 33+) - notificações
- [ ] Diferentes tamanhos de tela

---

## 📊 Monitoramento

### Firebase Console
- Analytics: https://console.firebase.google.com/project/afilaxy-kmm/analytics
- Crashlytics: (adicionar depois)
- Performance: (adicionar depois)

### Google Cloud Console
- Maps API Usage: https://console.cloud.google.com/google/maps-apis/quotas
- Functions Logs: `firebase functions:log`

---

## 🚀 Deploy

### Comandos Finais
```bash
# 1. Build release
export JAVA_HOME="/home/afilaxy/Projetos/afilaxy Kotlin/afilaxy/jdk-17.0.2"
./gradlew clean
./gradlew bundleRelease

# 2. Verificar AAB
ls -lh androidApp/build/outputs/bundle/release/

# 3. Deploy Functions
./deploy-functions.sh

# 4. Upload no Play Console
# (manual via web)
```

---

## 📝 Notas Importantes

1. **Versão**: Sempre incrementar `versionCode` e `versionName` em `build.gradle.kts`
2. **Keystore**: NUNCA perder o keystore de release
3. **API Keys**: NUNCA commitar no Git
4. **Testes**: Testar em dispositivos reais antes de publicar
5. **Rollout**: Começar com 10% dos usuários, aumentar gradualmente

---

## 🆘 Troubleshooting

### Build falha
```bash
./gradlew clean
./gradlew --stop
./gradlew assembleRelease --stacktrace
```

### Maps não carrega
- Verificar API Key no `local.properties`
- Verificar SHA-1 no Google Cloud Console
- Verificar APIs ativadas

### Notificações não chegam
- Verificar Functions deployadas: `firebase functions:list`
- Verificar logs: `firebase functions:log`
- Verificar FCM token no Firestore

---

**Última atualização**: $(date +%Y-%m-%d)
