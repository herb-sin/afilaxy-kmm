# Configuração Stitch MCP

## 1. Obter API Key real do Google Cloud:

1. Acesse: https://console.cloud.google.com/
2. Crie/selecione projeto
3. Ative "Stitch API" 
4. Vá em "Credenciais" > "Criar credenciais" > "Chave de API"
5. Copie a chave gerada

## 2. Substituir no arquivo:

Edite `.vscode/settings.json` e substitua:
```
"X-Goog-Api-Key": "SUA_API_KEY_REAL_AQUI"
```

## 3. Segurança:

- Adicione `.vscode/settings.json` ao `.gitignore`
- Use variáveis de ambiente para produção