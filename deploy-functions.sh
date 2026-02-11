#!/bin/bash

echo "🚀 Deploy Firebase Functions - Afilaxy KMM"
echo "=========================================="

# Verificar Firebase CLI
if ! command -v firebase &> /dev/null; then
    echo "❌ Firebase CLI não instalado"
    echo "📦 Instalando Firebase CLI..."
    npm install -g firebase-tools
fi

# Verificar login
echo "🔐 Verificando autenticação..."
firebase login:list

# Instalar dependências
echo "📦 Instalando dependências..."
cd functions
npm install

# Deploy
echo "🚀 Fazendo deploy..."
cd ..
firebase deploy --only functions

echo "✅ Deploy concluído!"
echo ""
echo "📋 Para verificar logs:"
echo "   firebase functions:log"
