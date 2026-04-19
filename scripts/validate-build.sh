#!/bin/bash

echo "🔍 Validação de Build - Afilaxy KMM"
echo "===================================="
echo ""

# Cores
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Contadores
PASSED=0
FAILED=0

# Função de teste
test_build() {
    local name=$1
    local command=$2
    
    echo -n "Testing $name... "
    
    if eval "$command" > /dev/null 2>&1; then
        echo -e "${GREEN}✓ PASS${NC}"
        ((PASSED++))
        return 0
    else
        echo -e "${RED}✗ FAIL${NC}"
        ((FAILED++))
        return 1
    fi
}

# 1. Verificar estrutura de arquivos críticos
echo "📁 Verificando estrutura de arquivos..."
test_build "Android Koin.kt existe" "[ -f shared/src/androidMain/kotlin/com/afilaxy/di/Koin.kt ]"
test_build "iOS Koin.kt existe" "[ -f shared/src/iosMain/kotlin/com/afilaxy/di/Koin.kt ]"
test_build "LocationRepository Android" "[ -f shared/src/androidMain/kotlin/com/afilaxy/data/repository/LocationRepositoryImpl.kt ]"
test_build "LocationRepository iOS" "[ -f shared/src/iosMain/kotlin/com/afilaxy/data/repository/LocationRepositoryImpl.kt ]"
test_build "LegalDisclaimer criado" "[ -f shared/src/commonMain/kotlin/com/afilaxy/domain/model/LegalDisclaimer.kt ]"
echo ""

# 2. Verificar que iOS não tem referências Android
echo "🍎 Verificando isolamento iOS..."
if grep -r "android.content.Context" shared/src/iosMain/ 2>/dev/null; then
    echo -e "${RED}✗ FAIL: Context Android encontrado no iOS${NC}"
    ((FAILED++))
else
    echo -e "${GREEN}✓ PASS: Nenhuma referência Android no iOS${NC}"
    ((PASSED++))
fi

if grep -r "SharedPreferences" shared/src/iosMain/ 2>/dev/null; then
    echo -e "${RED}✗ FAIL: SharedPreferences encontrado no iOS${NC}"
    ((FAILED++))
else
    echo -e "${GREEN}✓ PASS: Nenhuma referência SharedPreferences no iOS${NC}"
    ((PASSED++))
fi
echo ""

# 3. Compilação
echo "🔨 Testando compilação..."
test_build "Shared module (common)" "./gradlew shared:compileKotlinMetadata --quiet"
test_build "Android app" "./gradlew androidApp:assembleDebug --quiet"

# iOS só funciona no macOS
if [[ "$OSTYPE" == "darwin"* ]]; then
    test_build "iOS framework" "./gradlew shared:linkDebugFrameworkIosSimulatorArm64 --quiet"
else
    echo -e "${YELLOW}⊘ SKIP: iOS build (requer macOS)${NC}"
fi
echo ""

# 4. Testes unitários
echo "🧪 Executando testes..."
test_build "Testes compartilhados" "./gradlew shared:allTests --quiet"
echo ""

# Resumo
echo "===================================="
echo -e "Resultados: ${GREEN}$PASSED passed${NC}, ${RED}$FAILED failed${NC}"
echo ""

if [ $FAILED -eq 0 ]; then
    echo -e "${GREEN}✓ Todas as mitigações aplicadas com sucesso!${NC}"
    echo ""
    echo "Próximos passos:"
    echo "1. Testar no macOS: ./gradlew shared:linkDebugFrameworkIosSimulatorArm64"
    echo "2. Implementar tela de disclaimer legal"
    echo "3. Consultar advogado sobre LGPD"
    exit 0
else
    echo -e "${RED}✗ Algumas verificações falharam. Revise os erros acima.${NC}"
    exit 1
fi
