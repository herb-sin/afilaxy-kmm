#!/bin/bash

echo "🗺️ Configurando Google Maps API no Afilaxy KMM..."

# Verificar se a API key foi configurada
if grep -q "YOUR_MAPS_API_KEY_HERE" local.properties; then
    echo "❌ API key não configurada em local.properties"
    exit 1
fi

echo "✅ API key configurada no Android"

# Instalar dependências do iOS
echo "📱 Instalando dependências do iOS..."
cd iosApp
pod install --repo-update
cd ..

echo "✅ Configuração concluída!"
echo ""
echo "📋 Próximos passos:"
echo "1. Build Android: ./gradlew androidApp:assembleDebug"
echo "2. Build iOS: cd iosApp && xcodebuild -workspace iosApp.xcworkspace -scheme iosApp build"
echo ""
echo "🔧 Para testar os mapas:"
echo "- Android: Verifique se os mapas carregam no EmergencyScreen"
echo "- iOS: Verifique se os mapas carregam no EmergencyView"