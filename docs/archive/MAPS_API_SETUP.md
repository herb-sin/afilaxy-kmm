# 🗺️ Configuração Google Maps API Key

## Passo 1: Criar API Key

1. Acesse: https://console.cloud.google.com/google/maps-apis
2. Selecione o projeto **afilaxy-kmm** (ou crie um novo)
3. Vá em **Credenciais** → **Criar Credenciais** → **Chave de API**
4. Copie a chave gerada

## Passo 2: Restringir API Key (Segurança)

1. Clique na chave criada
2. Em **Restrições de aplicativo**:
   - Selecione: **Aplicativos Android**
   - Adicione:
     - **Nome do pacote**: `com.afilaxy.app`
     - **Impressão digital SHA-1**: (obter com comando abaixo)

```bash
# Debug SHA-1
keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android

# Release SHA-1 (após criar keystore)
keytool -list -v -keystore release.keystore -alias afilaxy
```

3. Em **Restrições de API**:
   - Selecione: **Restringir chave**
   - Marque: **Maps SDK for Android**

## Passo 3: Ativar APIs Necessárias

Ative as seguintes APIs no projeto:
- ✅ Maps SDK for Android
- ✅ Places API (se usar busca de lugares)
- ✅ Geolocation API

## Passo 4: Configurar no Projeto

Substitua no arquivo `androidApp/src/main/AndroidManifest.xml`:

```xml
<meta-data
    android:name="com.google.android.geo.API_KEY"
    android:value="SUA_CHAVE_AQUI" />
```

## Passo 5: Testar

```bash
# Build e instalar
export JAVA_HOME="/home/afilaxy/Projetos/afilaxy Kotlin/afilaxy/jdk-17.0.2"
cd /home/afilaxy/Projetos/afilaxy-kmm
./gradlew installDebug

# Abrir MapScreen no app e verificar se o mapa carrega
```

## ⚠️ Importante

- **Nunca commite a API Key** no Git
- Use variáveis de ambiente ou `local.properties`
- Monitore uso em: https://console.cloud.google.com/google/maps-apis/quotas

## 💰 Custos

- **$200 de crédito grátis/mês**
- Maps SDK for Android: $7 por 1000 carregamentos
- Geolocation API: $5 por 1000 requisições

Para app pequeno/médio, fica dentro do free tier.
