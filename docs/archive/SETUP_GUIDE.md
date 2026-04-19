# 🚀 Configuração P0 - Guia Passo a Passo

Execute estes passos para deixar o app 100% pronto para produção.

---

## ✅ Passo 1: Instalar Node.js (para Firebase Functions)

### Opção A: Via Flatpak (Recomendado)
```bash
flatpak install flathub org.freedesktop.Sdk.Extension.node18
```

### Opção B: Via DNF (Fedora/RHEL)
```bash
sudo dnf install nodejs npm
```

### Opção C: Via NVM (Gerenciador de versões)
```bash
curl -o- https://raw.githubusercontent.com/nvm-sh/nvm/v0.39.0/install.sh | bash
source ~/.bashrc
nvm install 18
nvm use 18
```

### Verificar instalação:
```bash
node --version  # Deve mostrar v18.x.x
npm --version   # Deve mostrar 9.x.x ou superior
```

---

## ✅ Passo 2: Deploy Firebase Functions

```bash
cd /home/afilaxy/Projetos/afilaxy-kmm

# Instalar Firebase CLI
npm install -g firebase-tools

# Login no Firebase
firebase login

# Instalar dependências das functions
cd functions
npm install
cd ..

# Deploy
firebase deploy --only functions

# Verificar se funcionou
firebase functions:list
```

**Resultado esperado:**
```
✔ functions: sendEmergencyNotification(us-central1)
```

---

## ✅ Passo 3: Configurar Google Maps API Key

### 3.1 Criar API Key

1. Acesse: https://console.cloud.google.com/google/maps-apis/credentials

2. Clique em **"Criar Credenciais"** → **"Chave de API"**

3. Copie a chave gerada (ex: `AIzaSyA...`)

### 3.2 Obter SHA-1 Fingerprint

```bash
# SHA-1 Debug (para desenvolvimento)
keytool -list -v \
  -keystore ~/.android/debug.keystore \
  -alias androiddebugkey \
  -storepass android \
  -keypass android | grep SHA1

# Copie o SHA-1 que aparecer (ex: A1:B2:C3:...)
```

### 3.3 Restringir API Key (Segurança)

1. No Google Cloud Console, clique na chave criada
2. Em **"Restrições de aplicativo"**:
   - Selecione: **Aplicativos Android**
   - Clique em **"Adicionar um item"**
   - **Nome do pacote**: `com.afilaxy.app`
   - **Impressão digital SHA-1**: (cole o SHA-1 copiado)
   - Clique em **"Concluído"**

3. Em **"Restrições de API"**:
   - Selecione: **Restringir chave**
   - Marque: **Maps SDK for Android**
   - Clique em **"Salvar"**

### 3.4 Ativar APIs Necessárias

Acesse: https://console.cloud.google.com/apis/library

Ative:
- ✅ Maps SDK for Android
- ✅ Geolocation API

### 3.5 Adicionar ao Projeto

Edite o arquivo `local.properties`:

```bash
nano local.properties
```

Adicione:
```properties
MAPS_API_KEY=AIzaSyA...sua_chave_aqui
sdk.dir=/home/afilaxy/Android/Sdk
```

Salve (Ctrl+O, Enter, Ctrl+X)

---

## ✅ Passo 4: Criar Keystore de Release

```bash
cd /home/afilaxy/Projetos/afilaxy-kmm

# Executar script
./create-keystore.sh
```

**Preencha quando solicitado:**
- Nome: Afilaxy
- Unidade organizacional: Desenvolvimento
- Organização: Afilaxy
- Cidade: Sua cidade
- Estado: Seu estado
- Código do país: BR

**Anote a senha que você definir!**

### 4.1 Obter SHA-1 Release

```bash
keytool -list -v \
  -keystore release.keystore \
  -alias afilaxy \
  -storepass android | grep SHA1

# Copie o SHA-1 que aparecer
```

### 4.2 Adicionar SHA-1 Release no Google Cloud

1. Volte em: https://console.cloud.google.com/google/maps-apis/credentials
2. Clique na sua API Key
3. Em **"Restrições de aplicativo"** → **"Adicionar um item"**
4. **Nome do pacote**: `com.afilaxy.app`
5. **Impressão digital SHA-1**: (cole o SHA-1 release)
6. Clique em **"Salvar"**

### 4.3 Configurar Assinatura

Edite `local.properties`:

```bash
nano local.properties
```

Adicione:
```properties
KEYSTORE_FILE=release.keystore
KEYSTORE_PASSWORD=sua_senha_aqui
KEY_ALIAS=afilaxy
KEY_PASSWORD=sua_senha_aqui
```

---

## ✅ Passo 5: Build e Testar

### Build Debug
```bash
export JAVA_HOME="/home/afilaxy/Projetos/afilaxy Kotlin/afilaxy/jdk-17.0.2"
cd /home/afilaxy/Projetos/afilaxy-kmm
./gradlew assembleDebug
```

**APK em:** `androidApp/build/outputs/apk/debug/androidApp-debug.apk`

### Build Release
```bash
./gradlew assembleRelease
```

**APK em:** `androidApp/build/outputs/apk/release/androidApp-release.apk`

### Build AAB (Play Store)
```bash
./gradlew bundleRelease
```

**AAB em:** `androidApp/build/outputs/bundle/release/androidApp-release.aab`

---

## ✅ Passo 6: Testar no Dispositivo

### Instalar Debug
```bash
adb install androidApp/build/outputs/apk/debug/androidApp-debug.apk
```

### Checklist de Testes
- [ ] Login funciona
- [ ] Criar emergência funciona
- [ ] Notificação chega no helper
- [ ] Mapa carrega (Google Maps)
- [ ] Chat funciona
- [ ] GPS funciona
- [ ] Aceitar emergência funciona
- [ ] Resolver emergência funciona

---

## 🎉 Pronto!

Se todos os testes passaram, o app está **100% pronto para produção!**

### Próximos Passos (Opcional - P1)
- Rate Limiting
- Testes Automatizados
- Crashlytics

### Upload na Play Store
1. Acesse: https://play.google.com/console
2. Upload do AAB: `androidApp-release.aab`
3. Preencher listagem da loja
4. Publicar

---

## 🆘 Problemas?

### Firebase Functions não deployou
```bash
firebase login --reauth
firebase use afilaxy-kmm
firebase deploy --only functions --debug
```

### Maps não carrega
- Verificar `MAPS_API_KEY` em `local.properties`
- Verificar SHA-1 no Google Cloud Console
- Aguardar 5 minutos (propagação da API Key)

### Build falha
```bash
./gradlew clean
./gradlew --stop
./gradlew assembleDebug --stacktrace
```

---

**Tempo estimado total: 15-20 minutos**

Boa sorte! 🚀
