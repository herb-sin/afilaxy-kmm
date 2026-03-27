#!/bin/bash

# Script para corrigir erros de build iOS

echo "🔧 Corrigindo erros de compilação iOS..."

# 1. Corrigir MapView - remover iOS 17+ features
sed -i 's/@available(iOS 17.0, \*)/\/\/ @available(iOS 17.0, \*)/g' /home/afilaxy/Projetos/afilaxy-kmm/iosApp/iosApp/Views/MapView.swift

# 2. Corrigir referências de propriedades não existentes
sed -i 's/\.createdAt/.timestamp/g' /home/afilaxy/Projetos/afilaxy-kmm/iosApp/iosApp/Views/HistoryView.swift
sed -i 's/container\.helper/container.emergency/g' /home/afilaxy/Projetos/afilaxy-kmm/iosApp/iosApp/Views/MapView.swift
sed -i 's/LocationManager\.shared\.requestLocation()/\/\/ LocationManager.shared.requestLocation()/g' /home/afilaxy/Projetos/afilaxy-kmm/iosApp/iosApp/Views/MapView.swift

# 3. Adicionar tipos faltantes
echo "// Placeholder types for missing models" >> /home/afilaxy/Projetos/afilaxy-kmm/iosApp/iosApp/Views/ProfessionalListView.swift
echo "typealias ProfessionalSpecialty = String" >> /home/afilaxy/Projetos/afilaxy-kmm/iosApp/iosApp/Views/ProfessionalListView.swift

echo "✅ Correções aplicadas!"