#!/bin/bash
# 🚀 Quick Start - Afilaxy Production Setup

echo "🚀 Afilaxy KMM - Production Setup"
echo "=================================="
echo ""

# Garante execução a partir da raiz do projeto
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
cd "$PROJECT_ROOT"

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Check Java
echo -e "${YELLOW}Verificando Java...${NC}"
export JAVA_HOME="/home/afilaxy/Projetos/afilaxy Kotlin/afilaxy/jdk-17.0.2"
if [ -d "$JAVA_HOME" ]; then
    echo -e "${GREEN}✓ Java OK${NC}"
else
    echo -e "${RED}✗ Java não encontrado${NC}"
    exit 1
fi

echo ""
echo "📋 Escolha uma opção:"
echo ""
echo "1) 🔥 Deploy Firebase Functions"
echo "2) 🔐 Criar Keystore"
echo "3) 🏗️  Build Debug APK"
echo "4) 🚀 Build Release APK"
echo "5) 📦 Build Release AAB (Play Store)"
echo "6) 🧪 Run Tests"
echo "7) 📊 Ver Status do Projeto"
echo "0) Sair"
echo ""
read -p "Opção: " option

case $option in
    1)
        echo -e "${YELLOW}Deploying Firebase Functions...${NC}"
        bash scripts/deploy-functions.sh
        ;;
    2)
        echo -e "${YELLOW}Criando Keystore...${NC}"
        bash scripts/create-keystore.sh
        ;;
    3)
        echo -e "${YELLOW}Building Debug APK...${NC}"
        ./gradlew assembleDebug
        echo -e "${GREEN}✓ APK: androidApp/build/outputs/apk/debug/androidApp-debug.apk${NC}"
        ;;
    4)
        echo -e "${YELLOW}Building Release APK...${NC}"
        ./gradlew assembleRelease
        echo -e "${GREEN}✓ APK: androidApp/build/outputs/apk/release/androidApp-release.apk${NC}"
        ;;
    5)
        echo -e "${YELLOW}Building Release AAB...${NC}"
        ./gradlew bundleRelease
        echo -e "${GREEN}✓ AAB: androidApp/build/outputs/bundle/release/androidApp-release.aab${NC}"
        ;;
    6)
        echo -e "${YELLOW}Running Tests...${NC}"
        ./gradlew test
        ;;
    7)
        echo ""
        echo "📊 Status do Projeto Afilaxy KMM"
        echo "================================"
        echo ""
        echo "Versão: 2.1.0-kmm (versionCode 16)"
        echo "Package: com.afilaxy.app"
        echo ""
        echo "✅ Features Implementadas:"
        echo "  - Autenticação (Login/Registro/Email Verification)"
        echo "  - Emergências (Criar/Aceitar/Resolver)"
        echo "  - Chat Real-Time"
        echo "  - Notificações FCM"
        echo "  - Geolocalização"
        echo "  - Google Maps"
        echo "  - Helper Mode"
        echo "  - 14 Telas UI"
        echo ""
        echo "⏳ Pendente (Configuração):"
        echo "  - Firebase Functions Deploy"
        echo "  - Google Maps API Key"
        echo "  - Keystore Release"
        echo ""
        echo "📁 Documentação:"
        echo "  - ARCHITECTURE.md"
        echo "  - DEPLOYMENT.md"
        echo "  - docs/archive/ (histórico)"
        echo ""
        ;;
    0)
        echo "Saindo..."
        exit 0
        ;;
    *)
        echo -e "${RED}Opção inválida${NC}"
        exit 1
        ;;
esac
