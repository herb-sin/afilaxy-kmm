# ✅ Checklist P0 - Configuração Rápida

Marque cada item conforme completar:

---

## 📦 Pré-requisitos

- [ ] Node.js 18+ instalado (`node --version`)
- [ ] Firebase CLI instalado (`firebase --version`)
- [ ] Conta Google Cloud ativa
- [ ] Projeto Firebase criado (afilaxy-kmm)

---

## 🔥 Firebase Functions

- [ ] `cd functions && npm install`
- [ ] `firebase login`
- [ ] `firebase deploy --only functions`
- [ ] Verificar: `firebase functions:list` mostra `sendEmergencyNotification`

---

## 🗺️ Google Maps API

- [ ] Criar API Key em: https://console.cloud.google.com/google/maps-apis
- [ ] Copiar chave (AIzaSy...)
- [ ] Obter SHA-1 debug: `keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android`
- [ ] Adicionar restrição Android (pacote: `com.afilaxy.app`, SHA-1)
- [ ] Ativar "Maps SDK for Android"
- [ ] Adicionar em `local.properties`: `MAPS_API_KEY=sua_chave`

---

## 🔐 Keystore Release

- [ ] Executar: `./create-keystore.sh`
- [ ] Anotar senha em local seguro
- [ ] Obter SHA-1 release: `keytool -list -v -keystore release.keystore -alias afilaxy`
- [ ] Adicionar SHA-1 release no Google Cloud Console
- [ ] Configurar `local.properties`:
  ```
  KEYSTORE_FILE=release.keystore
  KEYSTORE_PASSWORD=sua_senha
  KEY_ALIAS=afilaxy
  KEY_PASSWORD=sua_senha
  ```

---

## 🏗️ Build

- [ ] Build debug: `./gradlew assembleDebug`
- [ ] Build release: `./gradlew assembleRelease`
- [ ] Build AAB: `./gradlew bundleRelease`

---

## 🧪 Testes

- [ ] Instalar no dispositivo: `adb install androidApp/build/outputs/apk/debug/androidApp-debug.apk`
- [ ] Login funciona
- [ ] Criar emergência funciona
- [ ] Notificação chega
- [ ] Mapa carrega
- [ ] Chat funciona
- [ ] GPS funciona

---

## 🎉 Conclusão

- [ ] Todos os testes passaram
- [ ] App pronto para produção
- [ ] AAB pronto para Play Store

---

**Quando completar tudo, volte aqui e me avise para implementar P1!** 🚀
