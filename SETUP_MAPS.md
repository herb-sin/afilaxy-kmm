# 🗺️ Configuração do Google Maps

## Problema: Mapa não aparece no Android

Se você está vendo uma tela em branco ou erro ao tentar visualizar o mapa, é porque a API key do Google Maps não está configurada.

## Solução: Configurar API Key

### 1. Obter API Key do Google Maps

1. Acesse o [Google Cloud Console](https://console.cloud.google.com/)
2. Crie um novo projeto ou selecione um existente
3. Ative a **Maps SDK for Android** API
4. Vá em **Credenciais** → **Criar Credenciais** → **Chave de API**
5. Copie a chave gerada

### 2. Configurar no Projeto

1. Copie o arquivo de exemplo:
```bash
cp local.properties.example local.properties
```

2. Edite o arquivo `local.properties` e substitua:
```properties
MAPS_API_KEY=SUA_API_KEY_AQUI
```

3. **IMPORTANTE**: Nunca commite o arquivo `local.properties` (já está no .gitignore)

### 3. Testar

1. Limpe e recompile o projeto:
```bash
./gradlew clean
./gradlew androidApp:assembleDebug
```

2. Execute o app e navegue para a aba "Mapa"

## Troubleshooting

### Erro: "API key not found"
- Verifique se o arquivo `local.properties` existe na raiz do projeto
- Confirme que a chave `MAPS_API_KEY` está definida corretamente

### Erro: "This API project is not authorized"
- No Google Cloud Console, adicione o SHA-1 fingerprint do seu app
- Para debug: `./gradlew signingReport`

### Mapa aparece cinza
- Verifique se a API "Maps SDK for Android" está ativada
- Confirme que não há restrições na API key

## Estrutura de Arquivos

```
afilaxy-kmm/
├── local.properties          # Suas chaves (NÃO COMMITAR)
├── local.properties.example  # Exemplo das chaves necessárias
└── androidApp/
    └── src/main/
        ├── AndroidManifest.xml  # Usa ${MAPS_API_KEY}
        └── kotlin/.../MapScreen.kt  # Implementação do mapa
```

## Fallback

Se o mapa não carregar, o app mostra:
- ✅ Mensagem de erro explicativa
- ✅ Coordenadas da localização atual
- ✅ Botão para tentar novamente
- ✅ Instruções para configurar a API key

Isso garante que o app continue funcional mesmo sem o mapa configurado.