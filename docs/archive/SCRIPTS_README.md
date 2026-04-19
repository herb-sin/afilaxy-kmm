# 🛠️ Scripts de Produção

Scripts automatizados para facilitar o deploy e configuração do Afilaxy KMM.

---

## 🚀 Quick Start (Recomendado)

Menu interativo com todas as opções:

```bash
./quick-start.sh
```

---

## 📜 Scripts Disponíveis

### 1. `deploy-functions.sh`
Deploy automático das Firebase Cloud Functions.

```bash
./deploy-functions.sh
```

**O que faz:**
- Verifica Firebase CLI
- Instala dependências (npm install)
- Faz deploy das functions
- Mostra logs

---

### 2. `create-keystore.sh`
Cria keystore para assinatura de release.

```bash
./create-keystore.sh
```

**O que faz:**
- Cria `release.keystore`
- Configura alias `afilaxy`
- Mostra SHA-1 fingerprint (para Maps API)

**Importante:** Guarde a senha em local seguro!

---

### 3. `quick-start.sh`
Menu interativo com todas as opções.

```bash
./quick-start.sh
```

**Opções:**
1. Deploy Firebase Functions
2. Criar Keystore
3. Build Debug APK
4. Build Release APK
5. Build Release AAB
6. Run Tests
7. Ver Status do Projeto

---

## 📋 Comandos Manuais

### Build Debug
```bash
export JAVA_HOME="/home/afilaxy/Projetos/afilaxy Kotlin/afilaxy/jdk-17.0.2"
./gradlew assembleDebug
```

### Build Release
```bash
export JAVA_HOME="/home/afilaxy/Projetos/afilaxy Kotlin/afilaxy/jdk-17.0.2"
./gradlew assembleRelease
```

### Build AAB (Play Store)
```bash
export JAVA_HOME="/home/afilaxy/Projetos/afilaxy Kotlin/afilaxy/jdk-17.0.2"
./gradlew bundleRelease
```

### Clean Build
```bash
./gradlew clean
./gradlew --stop
./gradlew assembleDebug
```

---

## 🔐 Configuração

### local.properties
Crie o arquivo `local.properties` baseado em `local.properties.example`:

```properties
# Google Maps
MAPS_API_KEY=AIzaSy...sua_chave

# Release Signing
KEYSTORE_FILE=release.keystore
KEYSTORE_PASSWORD=sua_senha
KEY_ALIAS=afilaxy
KEY_PASSWORD=sua_senha
```

**Importante:** Este arquivo NÃO vai para o Git (.gitignore)

---

## 📁 Outputs

### Debug APK
```
androidApp/build/outputs/apk/debug/androidApp-debug.apk
```

### Release APK
```
androidApp/build/outputs/apk/release/androidApp-release.apk
```

### Release AAB
```
androidApp/build/outputs/bundle/release/androidApp-release.aab
```

---

## 🧪 Testes

### Unit Tests
```bash
./gradlew test
```

### Android Tests
```bash
./gradlew connectedAndroidTest
```

---

## 📚 Documentação

- `PRODUCTION_CHECKLIST.md` - Checklist completo de produção
- `MAPS_API_SETUP.md` - Configuração Google Maps
- `P0_IMPLEMENTATION_SUMMARY.md` - Resumo das implementações P0
- `QUALITY_ANALYSIS.md` - Análise de qualidade do projeto

---

## 🆘 Troubleshooting

### Erro: JAVA_HOME not set
```bash
export JAVA_HOME="/home/afilaxy/Projetos/afilaxy Kotlin/afilaxy/jdk-17.0.2"
```

### Erro: Firebase CLI not found
```bash
npm install -g firebase-tools
firebase login
```

### Erro: Maps não carrega
1. Verificar `MAPS_API_KEY` em `local.properties`
2. Verificar SHA-1 no Google Cloud Console
3. Verificar APIs ativadas

### Build lento
```bash
./gradlew --stop
./gradlew clean
```

---

## 📞 Suporte

Para mais informações, consulte:
- README.md principal
- PRODUCTION_CHECKLIST.md
- Issues no GitHub

---

**Desenvolvido com ❤️ usando Kotlin Multiplatform** 🚀
