#!/bin/bash
# Execute este script no TERMINAL DO SISTEMA (não no VSCodium)
# Abra um terminal Fedora normal e execute: bash P0_SETUP_COMMANDS.sh

set -e  # Para na primeira falha

echo "🚀 Configuração P0 - Afilaxy KMM"
echo "================================"
echo ""

# Cores
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

# Diretório do projeto
PROJECT_DIR="/home/afilaxy/Projetos/afilaxy-kmm"

# 1. Verificar Node.js
echo -e "${YELLOW}[1/5] Verificando Node.js...${NC}"
if command -v node &> /dev/null; then
    echo -e "${GREEN}✓ Node.js já instalado: $(node --version)${NC}"
else
    echo -e "${YELLOW}Instalando Node.js...${NC}"
    sudo dnf install -y nodejs npm
    echo -e "${GREEN}✓ Node.js instalado: $(node --version)${NC}"
fi
echo ""

# 2. Instalar Firebase CLI
echo -e "${YELLOW}[2/5] Instalando Firebase CLI...${NC}"
if command -v firebase &> /dev/null; then
    echo -e "${GREEN}✓ Firebase CLI já instalado${NC}"
else
    npm install -g firebase-tools
    echo -e "${GREEN}✓ Firebase CLI instalado${NC}"
fi
echo ""

# 3. Login Firebase
echo -e "${YELLOW}[3/5] Login no Firebase...${NC}"
echo "Abrirá o navegador para você fazer login..."
firebase login
echo ""

# 4. Deploy Functions
echo -e "${YELLOW}[4/5] Deploy Firebase Functions...${NC}"
cd "$PROJECT_DIR/functions"
npm install
cd "$PROJECT_DIR"
firebase deploy --only functions
echo -e "${GREEN}✓ Functions deployadas!${NC}"
echo ""

# 5. Verificar
echo -e "${YELLOW}[5/5] Verificando...${NC}"
firebase functions:list
echo ""

echo -e "${GREEN}================================${NC}"
echo -e "${GREEN}✅ Firebase Functions OK!${NC}"
echo -e "${GREEN}================================${NC}"
echo ""
echo "📋 Próximos passos:"
echo "1. Configurar Google Maps API Key"
echo "2. Criar Keystore"
echo "3. Build e testar"
echo ""
echo "Siga o arquivo: SETUP_GUIDE.md (Passo 3 em diante)"
