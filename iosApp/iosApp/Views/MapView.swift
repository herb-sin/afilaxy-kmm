import SwiftUI
import MapKit
import shared

struct MapView: View {
    @EnvironmentObject var container: AppContainer
    @State private var region = MKCoordinateRegion(
        center: CLLocationCoordinate2D(latitude: -23.5505, longitude: -46.6333), // São Paulo
        span: MKCoordinateSpan(latitudeDelta: 0.05, longitudeDelta: 0.05)
    )
    @State private var showHelperMode = false
    @State private var trackingMode: MapUserTrackingMode = .none
    @State private var mapStyle: MapStyle = .standard
    
    var body: some View {
        ZStack {
            // Main Map
            Map(coordinateRegion: $region, 
                showsUserLocation: true,
                userTrackingMode: $trackingMode,
                annotationItems: nearbyHelpers) { helper in
                MapAnnotation(coordinate: helper.coordinate) {
                    HelperAnnotationView(helper: helper)
                }
            }
            .mapStyle(mapStyle)
            .ignoresSafeArea(.all, edges: .top)
            
            // Overlay UI
            VStack {
                // Top Status Bar
                HStack {
                    LocationStatusCard()
                    Spacer()
                    MapControlsCard()
                }
                .padding(.horizontal, 16)
                .padding(.top, 8)
                
                Spacer()
                
                // Bottom Action Cards
                VStack(spacing: 12) {
                    if container.emergency.state?.isHelperModeActive == true {
                        HelperModeActiveCard()
                    }
                    
                    HStack(spacing: 12) {
                        HelperToggleCard()
                        EmergencyMapButton()
                    }
                    
                    NearbyHelpersCard()
                }
                .padding(.horizontal, 16)
                .padding(.bottom, 100)
            }
        }
        .navigationTitle("Mapa")
        .navigationBarTitleDisplayMode(.inline)
        .onAppear {
            updateLocationIfNeeded()
        }
        .onReceive(LocationManager.shared.$currentLocation) { location in
            if let location = location {
                region.center = location.coordinate
            }
        }
    }
    
    private var nearbyHelpers: [MapHelper] {
        // Mock data - replace with actual helpers from container
        [
            MapHelper(id: "1", name: "Ana Silva", coordinate: CLLocationCoordinate2D(latitude: -23.5515, longitude: -46.6343), distance: 0.8),
            MapHelper(id: "2", name: "João Santos", coordinate: CLLocationCoordinate2D(latitude: -23.5495, longitude: -46.6323), distance: 1.2),
            MapHelper(id: "3", name: "Maria Costa", coordinate: CLLocationCoordinate2D(latitude: -23.5525, longitude: -46.6353), distance: 1.5)
        ]
    }
    
    private func updateLocationIfNeeded() {
        if LocationManager.shared.hasPermission {
            // LocationManager.shared.requestLocation()
        }
    }
}

// MARK: - Supporting Views

struct LocationStatusCard: View {
    var body: some View {
        AfilaxyCard {
            HStack(spacing: 8) {
                Image(systemName: "location.fill")
                    .foregroundColor(Color.afiprimary)
                    .font(.caption)
                
                VStack(alignment: .leading, spacing: 2) {
                    Text("Localização Atual")
                        .font(.caption2)
                        .foregroundColor(Color.afionSurface.opacity(0.6))
                    Text("São Paulo, SP")
                        .font(.caption)
                        .fontWeight(.medium)
                }
                
                Spacer()
                
                Circle()
                    .fill(Color.green)
                    .frame(width: 8, height: 8)
            }
        }
    }
}

struct MapControlsCard: View {
    @State private var showMapOptions = false
    
    var body: some View {
        AfilaxyCard {
            HStack(spacing: 12) {
                Button(action: { /* Center on user */ }) {
                    Image(systemName: "location.circle")
                        .font(.title3)
                        .foregroundColor(Color.afiprimary)
                }
                
                Button(action: { showMapOptions.toggle() }) {
                    Image(systemName: "map")
                        .font(.title3)
                        .foregroundColor(Color.afiprimary)
                }
            }
        }
        .actionSheet(isPresented: $showMapOptions) {
            ActionSheet(
                title: Text("Estilo do Mapa"),
                buttons: [
                    .default(Text("Padrão")) { /* Set standard */ },
                    .default(Text("Satélite")) { /* Set satellite */ },
                    .default(Text("Híbrido")) { /* Set hybrid */ },
                    .cancel()
                ]
            )
        }
    }
}

struct HelperModeActiveCard: View {
    var body: some View {
        AfilaxyCard {
            HStack {
                Circle()
                    .fill(Color.green)
                    .frame(width: 12, height: 12)
                    .overlay {
                        Circle()
                            .fill(Color.green.opacity(0.3))
                            .frame(width: 24, height: 24)
                            .scaleEffect(1.0)
                            .animation(.easeInOut(duration: 1.0).repeatForever(), value: true)
                    }
                
                VStack(alignment: .leading, spacing: 2) {
                    Text("Modo Helper Ativo")
                        .font(.subheadline)
                        .fontWeight(.semibold)
                        .foregroundColor(.green)
                    Text("Você está disponível para ajudar")
                        .font(.caption)
                        .foregroundColor(Color.afionSurface.opacity(0.6))
                }
                
                Spacer()
                
                Text("ONLINE")
                    .font(.caption2)
                    .fontWeight(.bold)
                    .foregroundColor(.green)
                    .padding(.horizontal, 8)
                    .padding(.vertical, 4)
                    .background(Color.green.opacity(0.1))
                    .clipShape(Capsule())
            }
        }
    }
}

struct HelperToggleCard: View {
    @EnvironmentObject var container: AppContainer
    
    var body: some View {
        AfilaxyCard {
            HStack {
                Image(systemName: "heart.fill")
                    .foregroundColor(Color.afiprimary)
                    .font(.title3)
                
                VStack(alignment: .leading, spacing: 2) {
                    Text("Modo Helper")
                        .font(.subheadline)
                        .fontWeight(.medium)
                    Text(container.emergency.state?.isHelperModeActive == true ? "Ativo" : "Inativo")
                        .font(.caption)
                        .foregroundColor(Color.afionSurface.opacity(0.6))
                }
                
                Spacer()
                
                Toggle("", isOn: Binding(
                    get: { container.emergency.state?.isHelperModeActive == true },
                    set: { isOn in
                        if isOn {
                            container.emergency.vm.activateHelperMode()
                        } else {
                            container.emergency.vm.deactivateHelperMode()
                        }
                    }
                ))
                .toggleStyle(SwitchToggleStyle(tint: Color.afiprimary))
            }
        }
        .frame(maxWidth: .infinity)
    }
}

struct EmergencyMapButton: View {
    var body: some View {
        Button(action: {
            // Handle emergency action
        }) {
            HStack {
                Image(systemName: "exclamationmark.triangle.fill")
                    .foregroundColor(.white)
                    .font(.title3)
                
                Text("SOS")
                    .font(.subheadline)
                    .fontWeight(.bold)
                    .foregroundColor(.white)
            }
            .frame(maxWidth: .infinity)
            .padding(.vertical, 16)
            .background(
                LinearGradient(
                    colors: [Color.red, Color.red.opacity(0.8)],
                    startPoint: .topLeading,
                    endPoint: .bottomTrailing
                )
            )
            .clipShape(RoundedRectangle(cornerRadius: 12))
        }
    }
}

struct NearbyHelpersCard: View {
    var body: some View {
        AfilaxyCard {
            VStack(alignment: .leading, spacing: 12) {
                HStack {
                    Text("Helpers Próximos")
                        .font(.subheadline)
                        .fontWeight(.semibold)
                    
                    Spacer()
                    
                    Text("3 disponíveis")
                        .font(.caption)
                        .foregroundColor(Color.afiprimary)
                        .padding(.horizontal, 8)
                        .padding(.vertical, 4)
                        .background(Color.afiprimary.opacity(0.1))
                        .clipShape(Capsule())
                }
                
                HStack(spacing: 12) {
                    ForEach(0..<3, id: \.self) { index in
                        VStack(spacing: 4) {
                            Circle()
                                .fill(Color.afiprimary.opacity(0.2))
                                .frame(width: 32, height: 32)
                                .overlay {
                                    Image(systemName: "person.fill")
                                        .font(.caption)
                                        .foregroundColor(Color.afiprimary)
                                }
                            
                            Text("\(0.8 + Double(index) * 0.4, specifier: "%.1f")km")
                                .font(.caption2)
                                .foregroundColor(Color.afionSurface.opacity(0.6))
                        }
                    }
                    
                    Spacer()
                    
                    Button("Ver Todos") {
                        // Show helpers list
                    }
                    .font(.caption)
                    .foregroundColor(Color.afiprimary)
                }
            }
        }
    }
}

struct HelperAnnotationView: View {
    let helper: MapHelper
    
    var body: some View {
        VStack(spacing: 4) {
            Circle()
                .fill(Color.afiprimary)
                .frame(width: 24, height: 24)
                .overlay {
                    Image(systemName: "heart.fill")
                        .font(.caption2)
                        .foregroundColor(.white)
                }
                .overlay {
                    Circle()
                        .stroke(Color.white, lineWidth: 2)
                }
            
            Text("\(helper.distance, specifier: "%.1f")km")
                .font(.caption2)
                .fontWeight(.medium)
                .foregroundColor(.white)
                .padding(.horizontal, 6)
                .padding(.vertical, 2)
                .background(Color.black.opacity(0.7))
                .clipShape(Capsule())
        }
    }
}

// MARK: - Data Models

struct MapHelper: Identifiable {
    let id: String
    let name: String
    let coordinate: CLLocationCoordinate2D
    let distance: Double
}