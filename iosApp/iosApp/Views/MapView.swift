import SwiftUI
import MapKit
import shared

// MARK: - Air Quality Model

struct AirQualityData {
    let aqi: Int           // European AQI (0–500+)
    let pm25: Double       // µg/m³
    let humidity: Double   // %
    let label: String      // "Boa", "Moderada", "Ruim", "Muito Ruim"
    let color: Color

    static func from(aqi: Int, pm25: Double, humidity: Double) -> AirQualityData {
        let (label, color): (String, Color) = switch aqi {
        case 0...20:   ("Excelente", Color(red: 0.1, green: 0.75, blue: 0.4))
        case 21...40:  ("Boa",       Color(red: 0.4, green: 0.8,  blue: 0.2))
        case 41...60:  ("Moderada",  Color(red: 1.0, green: 0.75, blue: 0.0))
        case 61...80:  ("Ruim",      Color(red: 1.0, green: 0.45, blue: 0.0))
        default:       ("Muito Ruim",Color(red: 0.9, green: 0.1,  blue: 0.1))
        }
        return AirQualityData(aqi: aqi, pm25: pm25, humidity: humidity, label: label, color: color)
    }
}

// MARK: - MapView

struct MapView: View {
    @EnvironmentObject var container: AppContainer
    @StateObject private var locationManager = LocationManager.shared
    @State private var region = MKCoordinateRegion(
        center: CLLocationCoordinate2D(latitude: -23.5505, longitude: -46.6333),
        span: MKCoordinateSpan(latitudeDelta: 0.05, longitudeDelta: 0.05)
    )
    @State private var trackingMode: MapUserTrackingMode = .follow
    @State private var airQuality: AirQualityData? = nil
    @State private var airCardExpanded = false
    @State private var isFetchingAir = false

    var body: some View {
        ZStack {
            Map(coordinateRegion: $region,
                showsUserLocation: true,
                userTrackingMode: $trackingMode,
                annotationItems: nearbyHelpers) { helper in
                MapAnnotation(coordinate: helper.coordinate) {
                    HelperAnnotationView(helper: helper)
                }
            }
            .ignoresSafeArea(.all, edges: .top)

            // Single floating card — top-right
            VStack {
                HStack {
                    Spacer()
                    AirQualityCard(
                        data: airQuality,
                        isLoading: isFetchingAir,
                        expanded: $airCardExpanded
                    )
                    .padding(.trailing, 16)
                    .padding(.top, 8)
                }
                Spacer()
            }
        }
        .navigationTitle("Mapa")
        .navigationBarTitleDisplayMode(.inline)
        .onAppear {
            updateLocationIfNeeded()
        }
        .onReceive(LocationManager.shared.$currentLocation) { location in
            guard let location else { return }
            region.center = location.coordinate
            if airQuality == nil && !isFetchingAir {
                fetchAirQuality(lat: location.coordinate.latitude,
                                lon: location.coordinate.longitude)
            }
        }
    }

    // MARK: - Helpers list

    private var nearbyHelpers: [MapHelper] {
        guard let state = container.emergency.state else { return [] }
        let helpers = state.nearbyHelpers as [shared.Helper]
        return helpers.compactMap { helper -> MapHelper? in
            let lat = helper.latitude
            let lon = helper.longitude
            guard lat != 0 || lon != 0 else { return nil }
            return MapHelper(
                id: helper.id,
                name: helper.name.isEmpty ? "Ajudante" : helper.name,
                coordinate: CLLocationCoordinate2D(latitude: lat, longitude: lon),
                distance: helper.distance
            )
        }
    }

    // MARK: - Location init

    private func updateLocationIfNeeded() {
        if let current = LocationManager.shared.currentLocation {
            region.center = current.coordinate
            trackingMode = .follow
            let lat = current.coordinate.latitude
            let lon = current.coordinate.longitude
            container.emergency.startObservingNearbyHelpers(latitude: lat, longitude: lon)
            if airQuality == nil { fetchAirQuality(lat: lat, lon: lon) }
        }
        if LocationManager.shared.hasPermission {
            LocationManager.shared.startUpdating()
        } else {
            LocationManager.shared.requestWhenInUse()
        }
    }

    // MARK: - Air Quality fetch (Open-Meteo, sem chave de API)

    private func fetchAirQuality(lat: Double, lon: Double) {
        isFetchingAir = true
        let urlStr = "https://air-quality-api.open-meteo.com/v1/air-quality" +
            "?latitude=\(lat)&longitude=\(lon)" +
            "&current=european_aqi,pm2_5&hourly=relativehumidity_2m&forecast_days=1"
        guard let url = URL(string: urlStr) else { isFetchingAir = false; return }

        URLSession.shared.dataTask(with: url) { data, _, _ in
            defer { DispatchQueue.main.async { isFetchingAir = false } }
            guard let data,
                  let json = try? JSONSerialization.jsonObject(with: data) as? [String: Any]
            else { return }

            // current values
            let current  = json["current"] as? [String: Any]
            let aqi      = current?["european_aqi"] as? Int    ?? 0
            let pm25     = current?["pm2_5"]        as? Double ?? 0.0

            // humidity — primeiro valor horário
            let hourly   = json["hourly"] as? [String: Any]
            let humArr   = hourly?["relativehumidity_2m"] as? [Double] ?? []
            let humidity = humArr.first ?? 0.0

            DispatchQueue.main.async {
                airQuality = AirQualityData.from(aqi: aqi, pm25: pm25, humidity: humidity)
            }
        }.resume()
    }
}

// MARK: - Air Quality Floating Card

struct AirQualityCard: View {
    let data: AirQualityData?
    let isLoading: Bool
    @Binding var expanded: Bool

    var body: some View {
        VStack(alignment: .trailing, spacing: 0) {
            // Collapsed pill
            Button {
                withAnimation(.spring(response: 0.35, dampingFraction: 0.75)) {
                    expanded.toggle()
                }
            } label: {
                HStack(spacing: 8) {
                    Image(systemName: "wind")
                        .font(.caption)
                        .foregroundColor(data?.color ?? .secondary)

                    if isLoading {
                        ProgressView()
                            .scaleEffect(0.7)
                    } else if let d = data {
                        Text("Ar \(d.label)")
                            .font(.caption.bold())
                            .foregroundColor(d.color)
                        Circle()
                            .fill(d.color)
                            .frame(width: 8, height: 8)
                    } else {
                        Text("Qualidade do Ar")
                            .font(.caption)
                            .foregroundColor(.secondary)
                    }
                }
                .padding(.horizontal, 14)
                .padding(.vertical, 10)
                .background(.ultraThinMaterial, in: RoundedRectangle(cornerRadius: 20))
                .shadow(color: .black.opacity(0.12), radius: 6, x: 0, y: 3)
            }
            .buttonStyle(.plain)

            // Expanded detail panel
            if expanded, let d = data {
                VStack(alignment: .leading, spacing: 14) {
                    // AQI gauge
                    VStack(alignment: .leading, spacing: 6) {
                        HStack {
                            Text("Índice Europeu de Qualidade do Ar")
                                .font(.caption2)
                                .foregroundColor(.secondary)
                            Spacer()
                            Text("\(d.aqi)")
                                .font(.title2.bold())
                                .foregroundColor(d.color)
                        }
                        GeometryReader { geo in
                            ZStack(alignment: .leading) {
                                Capsule().fill(Color(.systemGray5)).frame(height: 6)
                                Capsule().fill(d.color)
                                    .frame(width: geo.size.width * min(Double(d.aqi) / 100.0, 1.0),
                                           height: 6)
                            }
                        }
                        .frame(height: 6)
                    }

                    Divider()

                    // Metrics
                    HStack(spacing: 20) {
                        MetricCell(
                            icon: "smoke.fill",
                            label: "PM2.5",
                            value: String(format: "%.1f µg/m³", d.pm25),
                            color: d.color
                        )
                        MetricCell(
                            icon: "humidity.fill",
                            label: "Humidade",
                            value: String(format: "%.0f%%", d.humidity),
                            color: .blue
                        )
                    }

                    Text("⚠️ Asmáticos devem evitar atividades ao ar livre quando o índice ultrapassar 50.")
                        .font(.caption2)
                        .foregroundColor(.secondary)
                        .multilineTextAlignment(.leading)
                }
                .padding(16)
                .background(.ultraThinMaterial, in: RoundedRectangle(cornerRadius: 16))
                .shadow(color: .black.opacity(0.15), radius: 10, x: 0, y: 5)
                .frame(width: 260)
                .transition(.asymmetric(
                    insertion: .opacity.combined(with: .move(edge: .top)),
                    removal:   .opacity.combined(with: .move(edge: .top))
                ))
                .padding(.top, 6)
            }
        }
    }
}

struct MetricCell: View {
    let icon: String
    let label: String
    let value: String
    let color: Color

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            HStack(spacing: 4) {
                Image(systemName: icon)
                    .font(.caption2)
                    .foregroundColor(color)
                Text(label)
                    .font(.caption2)
                    .foregroundColor(.secondary)
            }
            Text(value)
                .font(.caption.bold())
                .foregroundColor(.primary)
        }
    }
}

// MARK: - Helper Annotation

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
                    Circle().stroke(Color.white, lineWidth: 2)
                }

            Text(String(format: "%.0fm", helper.distance * 1000))
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

// MARK: - Data Model

struct MapHelper: Identifiable {
    let id: String
    let name: String
    let coordinate: CLLocationCoordinate2D
    let distance: Double
}