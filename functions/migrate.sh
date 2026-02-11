#!/bin/bash

# Script para executar migração de geohashes
# Execute este script após fazer deploy das Cloud Functions

echo "🚀 Executando migração de geohashes..."
echo ""

# Executar a function de migração
MIGRATION_URL="https://us-central1-afilaxy-app.cloudfunctions.net/migrateHelperLocations"

echo "Chamando: $MIGRATION_URL"
echo ""

# Fazer requisição HTTP
response=$(curl -s -w "\n%{http_code}" "$MIGRATION_URL")
http_code=$(echo "$response" | tail -n1)
body=$(echo "$response" | sed '$d')

echo "Status HTTP: $http_code"
echo ""
echo "Resposta:"
echo "$body" | jq '.' 2>/dev/null || echo "$body"
echo ""

if [ "$http_code" = "200" ]; then
    echo "✅ Migração concluída com sucesso!"
else
    echo "❌ Erro na migração (HTTP $http_code)"
    exit 1
fi
