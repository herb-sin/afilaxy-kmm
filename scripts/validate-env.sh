#!/bin/bash

# Script de validação de variáveis de ambiente para build seguro
# Este script verifica se todas as variáveis necessárias estão definidas

echo "🔍 Validando variáveis de ambiente..."

# Lista de variáveis obrigatórias
required_vars=(
    "VITE_FIREBASE_API_KEY"
    "VITE_FIREBASE_AUTH_DOMAIN"
    "VITE_FIREBASE_PROJECT_ID"
    "VITE_FIREBASE_STORAGE_BUCKET"
    "VITE_FIREBASE_MESSAGING_SENDER_ID"
    "VITE_FIREBASE_APP_ID"
)

# Verificar se o arquivo .env.local existe
if [ ! -f ".env.local" ]; then
    echo "❌ Arquivo .env.local não encontrado!"
    echo "📋 Execute: cp .env.example .env.local"
    echo "✏️  Em seguida, edite .env.local com suas credenciais reais"
    exit 1
fi

# Carregar variáveis do .env.local
source .env.local

# Verificar cada variável obrigatória
missing_vars=()
for var in "${required_vars[@]}"; do
    if [ -z "${!var}" ] || [ "${!var}" = "your_firebase_api_key_here" ]; then
        missing_vars+=("$var")
    fi
done

# Reportar resultados
if [ ${#missing_vars[@]} -eq 0 ]; then
    echo "✅ Todas as variáveis de ambiente estão configuradas!"
    echo "🚀 Prosseguindo com o build..."
    exit 0
else
    echo "❌ Variáveis de ambiente faltando ou não configuradas:"
    for var in "${missing_vars[@]}"; do
        echo "   - $var"
    done
    echo ""
    echo "📝 Edite o arquivo .env.local e configure as variáveis necessárias"
    exit 1
fi