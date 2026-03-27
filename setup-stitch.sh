#!/bin/bash

# Instalar Node.js se não estiver instalado
if ! command -v node &> /dev/null; then
    curl -fsSL https://deb.nodesource.com/setup_lts.x | sudo -E bash -
    sudo apt-get install -y nodejs
fi

# Instalar servidor MCP filesystem localmente
npm install @modelcontextprotocol/server-filesystem

echo "MCP configurado! Reinicie o VSCodium para ativar."