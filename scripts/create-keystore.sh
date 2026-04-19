#!/bin/bash

echo "🔐 Criação de Keystore para Release - Afilaxy"
echo "=============================================="

KEYSTORE_FILE="release.keystore"
ALIAS="afilaxy"

if [ -f "$KEYSTORE_FILE" ]; then
    echo "⚠️  Keystore já existe: $KEYSTORE_FILE"
    read -p "Deseja sobrescrever? (y/N): " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        echo "❌ Operação cancelada"
        exit 1
    fi
    rm "$KEYSTORE_FILE"
fi

echo ""
echo "📝 Preencha as informações:"
echo ""

keytool -genkey -v \
    -keystore "$KEYSTORE_FILE" \
    -alias "$ALIAS" \
    -keyalg RSA \
    -keysize 2048 \
    -validity 10000 \
    -storepass android \
    -keypass android

if [ $? -eq 0 ]; then
    echo ""
    echo "✅ Keystore criado com sucesso: $KEYSTORE_FILE"
    echo ""
    echo "📋 Próximos passos:"
    echo "1. Adicione ao .gitignore (já está)"
    echo "2. Configure no build.gradle.kts"
    echo "3. Guarde a senha em local seguro"
    echo ""
    echo "🔑 SHA-1 Fingerprint (para Google Maps):"
    keytool -list -v -keystore "$KEYSTORE_FILE" -alias "$ALIAS" -storepass android | grep SHA1
else
    echo "❌ Erro ao criar keystore"
    exit 1
fi
