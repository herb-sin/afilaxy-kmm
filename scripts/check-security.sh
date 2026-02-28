#!/bin/bash
# Script para verificar problemas de segurança no projeto Afilaxy

echo "🔒 Verificando Segurança do Projeto Afilaxy..."
echo ""

# Cores
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

ISSUES=0

# 1. Verificar se credenciais estão no .gitignore
echo "1️⃣ Verificando .gitignore..."
if grep -q "google-services.json" .gitignore && \
   grep -q "GoogleService-Info.plist" .gitignore && \
   grep -q "local.properties" .gitignore && \
   grep -q "*firebase-adminsdk*.json" .gitignore; then
    echo -e "${GREEN}✅ Arquivos de credenciais estão no .gitignore${NC}"
else
    echo -e "${RED}❌ Alguns arquivos de credenciais não estão no .gitignore${NC}"
    ISSUES=$((ISSUES+1))
fi
echo ""

# 2. Verificar se credenciais estão commitadas
echo "2️⃣ Verificando se credenciais estão no git..."
CREDS=$(git ls-files | grep -E "google-services\.json$|GoogleService-Info\.plist$|local\.properties$|firebase-adminsdk.*\.json$" | grep -v "\.example")
if [ -n "$CREDS" ]; then
    echo -e "${RED}❌ CRÍTICO: Arquivos de credenciais encontrados no git!${NC}"
    echo "$CREDS"
    ISSUES=$((ISSUES+1))
else
    echo -e "${GREEN}✅ Nenhum arquivo de credenciais commitado${NC}"
fi
echo ""

# 3. Verificar se templates existem
echo "3️⃣ Verificando templates de exemplo..."
TEMPLATES_OK=true
if [ ! -f "androidApp/google-services.json.example" ]; then
    echo -e "${YELLOW}⚠️  Template google-services.json.example não encontrado${NC}"
    TEMPLATES_OK=false
fi
if [ ! -f "iosApp/GoogleService-Info.plist.example" ]; then
    echo -e "${YELLOW}⚠️  Template GoogleService-Info.plist.example não encontrado${NC}"
    TEMPLATES_OK=false
fi
if [ ! -f "local.properties.example" ]; then
    echo -e "${YELLOW}⚠️  Template local.properties.example não encontrado${NC}"
    TEMPLATES_OK=false
fi
if [ "$TEMPLATES_OK" = true ]; then
    echo -e "${GREEN}✅ Todos os templates existem${NC}"
fi
echo ""

# 4. Verificar ProGuard
echo "4️⃣ Verificando configuração ProGuard..."
if [ -f "androidApp/proguard-rules.pro" ]; then
    echo -e "${GREEN}✅ ProGuard configurado${NC}"
else
    echo -e "${YELLOW}⚠️  ProGuard não configurado${NC}"
fi
echo ""

# 5. Verificar dependências npm (se existir)
if [ -d "functions" ]; then
    echo "5️⃣ Verificando vulnerabilidades npm..."
    cd functions
    if command -v npm &> /dev/null; then
        AUDIT_OUTPUT=$(npm audit 2>&1)
        if echo "$AUDIT_OUTPUT" | grep -q "found 0 vulnerabilities"; then
            echo -e "${GREEN}✅ Nenhuma vulnerabilidade npm encontrada${NC}"
        else
            echo -e "${RED}❌ Vulnerabilidades npm encontradas:${NC}"
            npm audit
            ISSUES=$((ISSUES+1))
        fi
    else
        echo -e "${YELLOW}⚠️  npm não instalado, pulando verificação${NC}"
    fi
    cd ..
    echo ""
fi

# 6. Verificar se há TODOs de segurança
echo "6️⃣ Verificando TODOs de segurança no código..."
TODO_COUNT=$(grep -r "TODO.*security\|TODO.*SECURITY\|FIXME.*security" --include="*.kt" --include="*.swift" shared/ androidApp/ iosApp/ 2>/dev/null | wc -l)
if [ "$TODO_COUNT" -gt 0 ]; then
    echo -e "${YELLOW}⚠️  $TODO_COUNT TODOs de segurança encontrados${NC}"
    grep -r "TODO.*security\|TODO.*SECURITY\|FIXME.*security" --include="*.kt" --include="*.swift" shared/ androidApp/ iosApp/ 2>/dev/null | head -5
else
    echo -e "${GREEN}✅ Nenhum TODO de segurança pendente${NC}"
fi
echo ""

# Resumo
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
if [ $ISSUES -eq 0 ]; then
    echo -e "${GREEN}✅ Verificação de segurança concluída com sucesso!${NC}"
    echo -e "${GREEN}   Nenhum problema crítico encontrado.${NC}"
else
    echo -e "${RED}❌ Verificação de segurança encontrou $ISSUES problema(s)!${NC}"
    echo -e "${RED}   Corrija os problemas antes de fazer deploy.${NC}"
fi
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
echo "📚 Para mais informações, consulte: SECURITY_MITIGATION.md"

exit $ISSUES
