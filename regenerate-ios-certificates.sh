#!/bin/bash
set -e

echo "🔐 REGENERAÇÃO COMPLETA DE CERTIFICADOS iOS - Afilaxy"
echo "======================================================"
echo ""

# Cores
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

PROJECT_DIR="$HOME/Projetos/afilaxy-kmm"
DOWNLOADS_DIR="$HOME/Downloads"

echo -e "${YELLOW}📋 PASSO 1: REVOGAR CERTIFICADO ANTIGO${NC}"
echo "----------------------------------------"
echo "1. Acesse: https://developer.apple.com/account/resources/certificates/list"
echo "2. Encontre o certificado 'Herbert Sin (iOS Distribution)'"
echo "3. Clique em 'Revoke' e confirme"
echo ""
read -p "Pressione ENTER após revogar o certificado..."

echo ""
echo -e "${YELLOW}📝 PASSO 2: GERAR NOVO CSR${NC}"
echo "----------------------------------------"
cd "$PROJECT_DIR"

# Remove arquivos antigos se existirem
rm -f ios_distribution_new.key ios_distribution_new.csr

# Gera novo CSR
openssl req -nodes -newkey rsa:2048 \
  -keyout ios_distribution_new.key \
  -out ios_distribution_new.csr \
  -subj "/emailAddress=afilaxy@gmail.com/CN=Afilaxy Distribution/C=BR"

echo -e "${GREEN}✅ CSR gerado com sucesso!${NC}"
echo ""
echo "📄 Conteúdo do CSR (copie isso):"
echo "================================"
cat ios_distribution_new.csr
echo "================================"
echo ""

echo -e "${YELLOW}📤 PASSO 3: CRIAR NOVO CERTIFICADO NO APPLE DEVELOPER${NC}"
echo "----------------------------------------"
echo "1. Acesse: https://developer.apple.com/account/resources/certificates/add"
echo "2. Selecione 'iOS Distribution (App Store and Ad Hoc)'"
echo "3. Clique em 'Continue'"
echo "4. Cole o conteúdo do CSR acima"
echo "5. Clique em 'Continue' e depois 'Download'"
echo "6. Salve o arquivo como 'distribution.cer' em ~/Downloads"
echo ""
read -p "Pressione ENTER após baixar o certificado..."

echo ""
echo -e "${YELLOW}🔄 PASSO 4: CONVERTER CERTIFICADO${NC}"
echo "----------------------------------------"
cd "$DOWNLOADS_DIR"

if [ ! -f "distribution.cer" ]; then
    echo -e "${RED}❌ Erro: distribution.cer não encontrado em ~/Downloads${NC}"
    exit 1
fi

# Converte .cer para .pem
openssl x509 -in distribution.cer -inform DER -out distribution_new.pem -outform PEM
echo -e "${GREEN}✅ Convertido para PEM${NC}"

# Cria .p12
openssl pkcs12 -export \
  -out distribution_new.p12 \
  -inkey "$PROJECT_DIR/ios_distribution_new.key" \
  -in distribution_new.pem \
  -password pass:afilaxy2026

echo -e "${GREEN}✅ Arquivo .p12 criado com sucesso!${NC}"
echo ""

echo -e "${YELLOW}📱 PASSO 5: CRIAR NOVO PROVISIONING PROFILE${NC}"
echo "----------------------------------------"
echo "1. Acesse: https://developer.apple.com/account/resources/profiles/add"
echo "2. Selecione 'App Store' em Distribution"
echo "3. Selecione o App ID: com.afilaxy.app"
echo "4. Selecione o NOVO certificado que você acabou de criar"
echo "5. Dê o nome: 'Afilaxy App Store Profile'"
echo "6. Clique em 'Generate' e depois 'Download'"
echo "7. Salve como 'Afilaxy_App_Store.mobileprovision' em ~/Downloads"
echo ""
read -p "Pressione ENTER após baixar o provisioning profile..."

if [ ! -f "$DOWNLOADS_DIR/Afilaxy_App_Store.mobileprovision" ]; then
    echo -e "${RED}❌ Erro: Afilaxy_App_Store.mobileprovision não encontrado${NC}"
    exit 1
fi

echo ""
echo -e "${YELLOW}🔐 PASSO 6: CODIFICAR EM BASE64 PARA GITHUB SECRETS${NC}"
echo "----------------------------------------"

# Codifica certificado
CERT_BASE64=$(base64 -w 0 "$DOWNLOADS_DIR/distribution_new.p12")
echo -e "${GREEN}✅ Certificado codificado${NC}"

# Codifica provisioning profile
PROFILE_BASE64=$(base64 -w 0 "$DOWNLOADS_DIR/Afilaxy_App_Store.mobileprovision")
echo -e "${GREEN}✅ Provisioning profile codificado${NC}"

# Salva em arquivo temporário
cat > "$PROJECT_DIR/github-secrets.txt" << EOF
🔐 GITHUB SECRETS - Atualize em:
https://github.com/seu-usuario/afilaxy-kmm/settings/secrets/actions

================================
Secret 1: IOS_CERTIFICATE_BASE64
================================
$CERT_BASE64

================================
Secret 2: IOS_PROVISION_PROFILE_BASE64
================================
$PROFILE_BASE64

================================
Secret 3: IOS_CERTIFICATE_PASSWORD
================================
afilaxy2026

================================
Secret 4: KEYCHAIN_PASSWORD
================================
afilaxy2026
EOF

echo ""
echo -e "${GREEN}✅ Secrets salvos em: $PROJECT_DIR/github-secrets.txt${NC}"
echo ""

echo -e "${YELLOW}📤 PASSO 7: ATUALIZAR GITHUB SECRETS${NC}"
echo "----------------------------------------"
echo "1. Acesse: https://github.com/seu-usuario/afilaxy-kmm/settings/secrets/actions"
echo "2. Atualize os seguintes secrets com os valores em github-secrets.txt:"
echo "   - IOS_CERTIFICATE_BASE64"
echo "   - IOS_PROVISION_PROFILE_BASE64"
echo "   - IOS_CERTIFICATE_PASSWORD (afilaxy2026)"
echo "   - KEYCHAIN_PASSWORD (afilaxy2026)"
echo ""

echo -e "${GREEN}✅ PROCESSO COMPLETO!${NC}"
echo ""
echo "📁 Arquivos gerados:"
echo "  - $PROJECT_DIR/ios_distribution_new.key (PRIVADA - NÃO COMMITAR)"
echo "  - $PROJECT_DIR/ios_distribution_new.csr"
echo "  - $DOWNLOADS_DIR/distribution_new.pem"
echo "  - $DOWNLOADS_DIR/distribution_new.p12"
echo "  - $PROJECT_DIR/github-secrets.txt (DELETAR APÓS USO)"
echo ""
echo -e "${RED}⚠️  IMPORTANTE:${NC}"
echo "  - NÃO commite ios_distribution_new.key no Git"
echo "  - DELETE github-secrets.txt após atualizar os secrets"
echo "  - Guarde distribution_new.p12 em local seguro"
echo ""
