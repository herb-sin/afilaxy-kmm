#!/bin/bash

# Script para sincronizar projeto Stitch
PROJECT_ID="14004449678458817852"
API_KEY="AQ.Ab8RN6JxJBi5PlAW4yTGfofMozPR8BQiohBlGUSSL05WRvoPMg"

# Criar diretório para dados do Stitch
mkdir -p .stitch/data

# Baixar layout do projeto
curl -H "X-Goog-Api-Key: $API_KEY" \
     -H "Content-Type: application/json" \
     "https://stitch.googleapis.com/v1/projects/$PROJECT_ID/layout" \
     -o .stitch/data/layout.json

# Baixar estrutura de arquivos
curl -H "X-Goog-Api-Key: $API_KEY" \
     -H "Content-Type: application/json" \
     "https://stitch.googleapis.com/v1/projects/$PROJECT_ID/files" \
     -o .stitch/data/files.json

echo "Dados do Stitch sincronizados em .stitch/data/"