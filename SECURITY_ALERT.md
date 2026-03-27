# 🚨 ALERTA DE SEGURANÇA - CREDENCIAL EXPOSTA

## Problema Identificado
- **API Key do Google Maps exposta** no arquivo `.env`
- **Chave:** `AIzaSyA1RVZ1EXBjACjCgf_f-hpnGdnf2FzNWvc`
- **Arquivo:** `/home/afilaxy/Projetos/afilaxy-kmm/.env`

## Ações Imediatas Necessárias

### 1. Revogar a API Key
1. Acesse o [Google Cloud Console](https://console.cloud.google.com/)
2. Vá para "APIs & Services" > "Credentials"
3. Encontre a API Key: `AIzaSyA1RVZ1EXBjACjCgf_f-hpnGdnf2FzNWvc`
4. **REVOGUE IMEDIATAMENTE** esta chave

### 2. Criar Nova API Key
1. Crie uma nova API Key no Google Cloud Console
2. Configure restrições adequadas:
   - **Restrições de aplicativo:** HTTP referrers
   - **Restrições de API:** Apenas APIs necessárias
   - **Domínios permitidos:** Apenas seus domínios

### 3. Configurar Variáveis de Ambiente
1. Adicione a nova chave ao arquivo `.env.local` (não commitado)
2. Remova a chave do arquivo `.env` commitado

## Status
- [ ] API Key revogada
- [ ] Nova API Key criada
- [ ] Configuração corrigida
- [ ] Commit com correção

**Data:** $(date)
**Prioridade:** CRÍTICA