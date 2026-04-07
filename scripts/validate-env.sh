#!/bin/bash

# Script de validação de variáveis de ambiente para build seguro
# Suporta dois modos:
#   - Desenvolvimento local: lê de .env.local
#   - CI (GitHub Actions): lê variáveis já injetadas no ambiente via Secrets

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

# Em CI, as variáveis são injetadas via GitHub Secrets (env: no workflow).
# Não é necessário — nem possível — ler .env.local em CI.
if [ "${CI}" = "true" ]; then
    echo "ℹ️  Modo CI detectado — usando variáveis do ambiente (GitHub Secrets)"
else
    # Desenvolvimento local: carrega .env.local se existir
    if [ -f ".env.local" ]; then
        # shellcheck source=/dev/null
        source .env.local
        echo "ℹ️  Variáveis carregadas de .env.local"
    elif [ -f "web-src/.env.local" ]; then
        source web-src/.env.local
        echo "ℹ️  Variáveis carregadas de web-src/.env.local"
    else
        echo "❌ Arquivo .env.local não encontrado!"
        echo "📋 Execute: cp .env.example .env.local"
        echo "✏️  Em seguida, edite .env.local com suas credenciais reais"
        exit 1
    fi
fi

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
    if [ "${CI}" = "true" ]; then
        echo "📝 Configure os GitHub Secrets no repositório:"
        echo "   Settings → Secrets and variables → Actions → New repository secret"
    else
        echo "📝 Edite o arquivo .env.local e configure as variáveis necessárias"
    fi
    exit 1
fi