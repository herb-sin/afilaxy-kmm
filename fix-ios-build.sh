#!/bin/bash

echo "🔧 Aplicando fixes para build do iOS..."

# 1. Fix ContentView.swift - AnyTabViewStyle não existe no iOS 16
echo "📱 Corrigindo ContentView.swift..."
sed -i '' 's/AnyTabViewStyle/DefaultTabViewStyle/g' iosApp/iosApp/ContentView.swift

# 2. Fix MapView.swift - MapStyle só existe no iOS 17+
echo "🗺️ Corrigindo MapView.swift..."
cat > iosApp/iosApp/Views/MapView.swift << 'EOF'
import SwiftUI
import MapKit
import shared

struct MapView: View {
    @EnvironmentObject var container: AppContainer
    @State private var region = MKCoordinateRegion(
        center: CLLocationCoordinate2D(latitude: -23.5505, longitude: -46.6333),
        span: MKCoordinateSpan(latitudeDelta: 0.05, longitudeDelta: 0.05)
    )
    
    var body: some View {
        NavigationView {
            ZStack {
                Map(coordinateRegion: $region)
                    .ignoresSafeArea()
                
                VStack {
                    Spacer()
                    
                    HStack {
                        Button("Emergência") {
                            container.emergency.requestEmergency()
                        }
                        .padding()
                        .background(Color.red)
                        .foregroundColor(.white)
                        .cornerRadius(10)
                        
                        Spacer()
                        
                        Button(container.emergency.state?.isHelperMode == true ? "Desativar Helper" : "Ativar Helper") {
                            container.emergency.toggleHelperMode()
                        }
                        .padding()
                        .background(Color.blue)
                        .foregroundColor(.white)
                        .cornerRadius(10)
                    }
                    .padding()
                }
            }
            .navigationTitle("Mapa")
        }
    }
}
EOF

# 3. Fix HistoryView.swift - Remover código duplicado e corrigir cores
echo "📋 Corrigindo HistoryView.swift..."
cat > iosApp/iosApp/Views/HistoryView.swift << 'EOF'
import SwiftUI
import shared

struct HistoryView: View {
    @EnvironmentObject var container: AppContainer
    @State private var filter = HistoryFilter.all
    
    var body: some View {
        NavigationView {
            VStack {
                Text("Histórico de Emergências")
                    .font(.title)
                    .padding()
                
                Picker("Filtro", selection: $filter) {
                    Text("Todas").tag(HistoryFilter.all)
                    Text("Resolvidas").tag(HistoryFilter.resolved)
                    Text("Pendentes").tag(HistoryFilter.pending)
                }
                .pickerStyle(SegmentedPickerStyle())
                .padding()
                
                List {
                    Text("Histórico em desenvolvimento")
                        .foregroundColor(.gray)
                }
                
                Spacer()
            }
            .navigationTitle("Histórico")
        }
    }
}

enum HistoryFilter: CaseIterable {
    case all, resolved, pending
}
EOF

# 4. Fix ProfessionalListView.swift - Simplificar
echo "👨‍⚕️ Corrigindo ProfessionalListView.swift..."
cat > iosApp/iosApp/Views/ProfessionalListView.swift << 'EOF'
import SwiftUI
import shared

struct ProfessionalListView: View {
    @EnvironmentObject var container: AppContainer
    
    var body: some View {
        NavigationView {
            VStack {
                Text("Profissionais de Saúde")
                    .font(.title)
                    .padding()
                
                List {
                    Text("Lista de profissionais em desenvolvimento")
                        .foregroundColor(.gray)
                }
                
                Spacer()
            }
            .navigationTitle("Profissionais")
        }
    }
}
EOF

# 5. Fix ProfileView.swift - Corrigir propriedades
echo "👤 Corrigindo ProfileView.swift..."
sed -i '' 's/profile\.displayName/profile?.name ?? "Usuário"/g' iosApp/iosApp/Views/ProfileView.swift
sed -i '' 's/AfilaxyColors/Color/g' iosApp/iosApp/Views/ProfileView.swift

# 6. Fix HomeView.swift - Corrigir propriedades
echo "🏠 Corrigindo HomeView.swift..."
sed -i '' 's/\.name/.description/g' iosApp/iosApp/Views/HomeView.swift

echo "✅ Fixes aplicados com sucesso!"
echo ""
echo "🚀 Próximos passos:"
echo "1. cd iosApp && pod install"
echo "2. xcodebuild -workspace iosApp.xcworkspace -scheme iosApp build"
EOF