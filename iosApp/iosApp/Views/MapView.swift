import SwiftUI
import MapKit
import shared

// MARK: - Air Quality Model

struct AirQualityData {
    let aqi: Int
    let pm25: Double
    let humidity: Double
    let label: String
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
    /// Quando true, busca farmácias via Overpass API em vez de exibir UPAs.
    var pharmacyMode: Bool = false

    @StateObject private var locationManager = LocationManager.shared
    @State private var region = MKCoordinateRegion(
        center: CLLocationCoordinate2D(latitude: -23.5505, longitude: -46.6333),
        span: MKCoordinateSpan(latitudeDelta: 0.05, longitudeDelta: 0.05)
    )
    @State private var trackingMode: MapUserTrackingMode = .follow
    @State private var airQuality: AirQualityData? = nil
    @State private var airCardExpanded = false
    @State private var isFetchingAir = false

    // Pharmacy mode state
    @State private var pharmacies: [PharmacyItem] = []
    @State private var pharmacySearchDone = false
    @State private var pharmacySearching = false

    // UPA mode state
    @State private var upas: [UpaItem] = []
    @State private var upaSearchDone = false
    @State private var upaSearching = false

    var body: some View {
        ZStack {
            Map(coordinateRegion: $region,
                showsUserLocation: true,
                userTrackingMode: $trackingMode,
                annotationItems: allPins) { pin in
                MapAnnotation(coordinate: pin.coordinate) {
                    switch pin.kind {
                    case .upa(let u):      UpaAnnotationView(upa: u)
                    case .pharmacy(let p): PharmacyAnnotationView(pharmacy: p)
                    }
                }
            }
            .ignoresSafeArea(.all, edges: .top)

            // Floating overlay
            VStack {
                HStack {
                    if pharmacyMode {
                        // Pharmacy count pill — top-left
                        HStack(spacing: 6) {
                            Image(systemName: "cross.fill")
                                .font(.caption.bold())
                                .foregroundColor(.green)
                            if pharmacySearching {
                                ProgressView().scaleEffect(0.7)
                                Text("Buscando...").font(.caption)
                            } else {
                                Text(pharmacies.isEmpty
                                     ? "Nenhuma farmácia encontrada"
                                     : "\(pharmacies.count) farmácias próximas")
                                    .font(.caption.bold())
                            }
                        }
                        .padding(.horizontal, 14)
                        .padding(.vertical, 10)
                        .background(.ultraThinMaterial, in: RoundedCornerShape20())
                        .shadow(color: .black.opacity(0.12), radius: 6, x: 0, y: 3)
                        .padding(.leading, 16)
                        .padding(.top, 8)
                        Spacer()
                    } else {
                        // UPA count pill — top-left
                        HStack(spacing: 6) {
                            Image(systemName: "staroflife.fill")
                                .font(.caption.bold())
                                .foregroundColor(.red)
                            if upaSearching {
                                ProgressView().scaleEffect(0.7)
                                Text("Buscando...").font(.caption)
                            } else {
                                Text(upas.isEmpty
                                     ? "Nenhuma UPA encontrada"
                                     : "\(upas.count) UPAs em 10 km")
                                    .font(.caption.bold())
                            }
                        }
                        .padding(.horizontal, 14)
                        .padding(.vertical, 10)
                        .background(.ultraThinMaterial, in: RoundedCornerShape20())
                        .shadow(color: .black.opacity(0.12), radius: 6, x: 0, y: 3)
                        .padding(.leading, 16)
                        .padding(.top, 8)

                        Spacer()

                        // Air quality card — top-right (mantido para contexto respiratório)
                        AirQualityCard(
                            data: airQuality,
                            isLoading: isFetchingAir,
                            expanded: $airCardExpanded
                        )
                        .padding(.trailing, 16)
                        .padding(.top, 8)
                    }
                }
                Spacer()
            }
        }
        .navigationTitle(pharmacyMode ? "Farmácias 24h" : "UPAs próximas")
        .navigationBarTitleDisplayMode(.inline)
        .onAppear { updateLocationIfNeeded() }
        .onReceive(LocationManager.shared.$currentLocation) { location in
            guard let location else { return }
            region.center = location.coordinate
            if pharmacyMode {
                searchPharmacies(near: location.coordinate)
            } else {
                searchUpas(near: location.coordinate)
                if airQuality == nil && !isFetchingAir {
                    fetchAirQuality(lat: location.coordinate.latitude,
                                    lon: location.coordinate.longitude)
                }
            }
        }
    }

    // MARK: - Combined annotation pins

    private var allPins: [MapPin] {
        let upaPins = upas.map { u in
            MapPin(id: "u_\(u.id)", coordinate: u.coordinate, kind: .upa(u))
        }
        let pharmacyPins = pharmacies.map { p in
            MapPin(id: "p_\(p.id)", coordinate: p.coordinate, kind: .pharmacy(p))
        }
        return upaPins + pharmacyPins
    }

    // MARK: - Location init

    private func updateLocationIfNeeded() {
        if let current = LocationManager.shared.currentLocation {
            region.center = current.coordinate
            trackingMode = .follow
            let lat = current.coordinate.latitude
            let lon = current.coordinate.longitude
            if pharmacyMode {
                searchPharmacies(near: current.coordinate)
            } else {
                searchUpas(near: current.coordinate)
                if airQuality == nil { fetchAirQuality(lat: lat, lon: lon) }
            }
        }
        if LocationManager.shared.hasPermission {
            LocationManager.shared.startUpdating()
        } else {
            LocationManager.shared.requestWhenInUse()
        }
    }

    // MARK: - UPA search (Overpass API — OpenStreetMap)

    private func searchUpas(near center: CLLocationCoordinate2D) {
        guard !pharmacyMode, !upaSearchDone else { return }
        upaSearchDone = true
        upaSearching = true
        let lat = center.latitude
        let lon = center.longitude
        let query = """
            [out:json];
            (
              node["amenity"="hospital"]["emergency"="yes"](around:10000,\(lat),\(lon));
              way["amenity"="hospital"]["emergency"="yes"](around:10000,\(lat),\(lon));
              node["amenity"="clinic"]["emergency"="yes"](around:10000,\(lat),\(lon));
              way["amenity"="clinic"]["emergency"="yes"](around:10000,\(lat),\(lon));
            );
            out center 20;
            """
        guard let encoded = query.addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed),
              let url = URL(string: "https://overpass-api.de/api/interpreter?data=\(encoded)")
        else { upaSearching = false; return }

        URLSession.shared.dataTask(with: url) { data, _, _ in
            defer { DispatchQueue.main.async { upaSearching = false } }
            guard let data,
                  let json = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
                  let elements = json["elements"] as? [[String: Any]]
            else { return }

            let result: [UpaItem] = elements.compactMap { el in
                let tags = el["tags"] as? [String: Any]
                let elLat: Double
                let elLon: Double
                if let lat = el["lat"] as? Double, let lon = el["lon"] as? Double {
                    elLat = lat; elLon = lon
                } else if let center = el["center"] as? [String: Any],
                          let lat = center["lat"] as? Double,
                          let lon = center["lon"] as? Double {
                    elLat = lat; elLon = lon
                } else { return nil }

                let name = (tags?["name"] as? String).flatMap { $0.isEmpty ? nil : $0 } ?? "UPA"
                let phone = tags?["phone"] as? String
                    ?? tags?["contact:phone"] as? String ?? ""
                return UpaItem(
                    id: UUID().uuidString,
                    name: name,
                    coordinate: CLLocationCoordinate2D(latitude: elLat, longitude: elLon),
                    phone: phone
                )
            }
            DispatchQueue.main.async { upas = result }
        }.resume()
    }

    // MARK: - Pharmacy search (Overpass API — OpenStreetMap, mesma fonte do Android)

    private func searchPharmacies(near center: CLLocationCoordinate2D) {
        guard pharmacyMode, !pharmacySearchDone else { return }
        pharmacySearchDone = true
        pharmacySearching = true
        let lat = center.latitude
        let lon = center.longitude
        let query = """
            [out:json];
            (
              node["amenity"="pharmacy"](around:5000,\(lat),\(lon));
              way["amenity"="pharmacy"](around:5000,\(lat),\(lon));
            );
            out center 20;
            """
        guard let encoded = query.addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed),
              let url = URL(string: "https://overpass-api.de/api/interpreter?data=\(encoded)")
        else { pharmacySearching = false; return }

        URLSession.shared.dataTask(with: url) { data, _, _ in
            defer { DispatchQueue.main.async { pharmacySearching = false } }
            guard let data,
                  let json = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
                  let elements = json["elements"] as? [[String: Any]]
            else { return }

            let result: [PharmacyItem] = elements.compactMap { el in
                let tags = el["tags"] as? [String: Any]
                let elLat: Double
                let elLon: Double
                if let lat = el["lat"] as? Double, let lon = el["lon"] as? Double {
                    elLat = lat; elLon = lon
                } else if let center = el["center"] as? [String: Any],
                          let lat = center["lat"] as? Double,
                          let lon = center["lon"] as? Double {
                    elLat = lat; elLon = lon
                } else { return nil }
                let name = (tags?["name"] as? String).flatMap { $0.isEmpty ? nil : $0 } ?? "Farmácia"
                let phone = tags?["phone"] as? String ?? tags?["contact:phone"] as? String ?? ""
                return PharmacyItem(
                    id: UUID().uuidString,
                    name: name,
                    coordinate: CLLocationCoordinate2D(latitude: elLat, longitude: elLon),
                    phone: phone
                )
            }
            DispatchQueue.main.async { pharmacies = result }
        }.resume()
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
            let current  = json["current"]  as? [String: Any]
            let aqi      = current?["european_aqi"] as? Int    ?? 0
            let pm25     = current?["pm2_5"]        as? Double ?? 0.0
            let hourly   = json["hourly"]   as? [String: Any]
            let humArr   = hourly?["relativehumidity_2m"] as? [Double] ?? []
            let humidity = humArr.first ?? 0.0
            DispatchQueue.main.async {
                airQuality = AirQualityData.from(aqi: aqi, pm25: pm25, humidity: humidity)
            }
        }.resume()
    }
}

// Helper shape alias to avoid long inline modifiers
private struct RoundedCornerShape20: Shape {
    func path(in rect: CGRect) -> Path {
        RoundedRectangle(cornerRadius: 20).path(in: rect)
    }
}

// MARK: - Air Quality Floating Card

struct AirQualityCard: View {
    let data: AirQualityData?
    let isLoading: Bool
    @Binding var expanded: Bool

    var body: some View {
        VStack(alignment: .trailing, spacing: 0) {
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
                        ProgressView().scaleEffect(0.7)
                    } else if let d = data {
                        Text("Ar \(d.label)").font(.caption.bold()).foregroundColor(d.color)
                        Circle().fill(d.color).frame(width: 8, height: 8)
                    } else {
                        Text("Qualidade do Ar").font(.caption).foregroundColor(.secondary)
                    }
                }
                .padding(.horizontal, 14)
                .padding(.vertical, 10)
                .background(.ultraThinMaterial, in: RoundedRectangle(cornerRadius: 20))
                .shadow(color: .black.opacity(0.12), radius: 6, x: 0, y: 3)
            }
            .buttonStyle(.plain)

            if expanded, let d = data {
                VStack(alignment: .leading, spacing: 14) {
                    VStack(alignment: .leading, spacing: 6) {
                        HStack {
                            Text("Índice Europeu de Qualidade do Ar")
                                .font(.caption2).foregroundColor(.secondary)
                            Spacer()
                            Text("\(d.aqi)").font(.title2.bold()).foregroundColor(d.color)
                        }
                        GeometryReader { geo in
                            ZStack(alignment: .leading) {
                                Capsule().fill(Color(.systemGray5)).frame(height: 6)
                                Capsule().fill(d.color)
                                    .frame(width: geo.size.width * min(Double(d.aqi) / 100.0, 1.0), height: 6)
                            }
                        }
                        .frame(height: 6)
                    }
                    Divider()
                    HStack(spacing: 20) {
                        MetricCell(icon: "smoke.fill", label: "PM2.5",
                                   value: String(format: "%.1f µg/m³", d.pm25), color: d.color)
                        MetricCell(icon: "humidity.fill", label: "Humidade",
                                   value: String(format: "%.0f%%", d.humidity), color: .blue)
                    }
                    Text("⚠️ Asmáticos devem evitar atividades ao ar livre quando o índice ultrapassar 50.")
                        .font(.caption2).foregroundColor(.secondary).multilineTextAlignment(.leading)
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
    let icon: String; let label: String; let value: String; let color: Color
    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            HStack(spacing: 4) {
                Image(systemName: icon).font(.caption2).foregroundColor(color)
                Text(label).font(.caption2).foregroundColor(.secondary)
            }
            Text(value).font(.caption.bold()).foregroundColor(.primary)
        }
    }
}

// MARK: - UPA Annotation

struct UpaAnnotationView: View {
    let upa: UpaItem
    @State private var showCallout = false

    var body: some View {
        VStack(spacing: 4) {
            Circle()
                .fill(Color.red)
                .frame(width: 32, height: 32)
                .overlay { Image(systemName: "staroflife.fill").font(.caption.bold()).foregroundColor(.white) }
                .overlay { Circle().stroke(Color.white, lineWidth: 2) }
                .shadow(color: .black.opacity(0.2), radius: 4, x: 0, y: 2)
                .onTapGesture {
                    withAnimation(.spring(response: 0.3)) { showCallout.toggle() }
                }

            if showCallout {
                VStack(alignment: .leading, spacing: 4) {
                    Text(upa.name)
                        .font(.caption.bold()).lineLimit(2).multilineTextAlignment(.leading)
                    if !upa.phone.isEmpty {
                        Button {
                            let digits = upa.phone.filter { $0.isNumber || $0 == "+" }
                            if let url = URL(string: "tel://\(digits)") {
                                UIApplication.shared.open(url)
                            }
                        } label: {
                            HStack(spacing: 4) {
                                Image(systemName: "phone.fill").font(.caption2)
                                Text(upa.phone).font(.caption2)
                            }
                            .foregroundColor(.red)
                        }
                    }
                }
                .padding(10)
                .background(.ultraThinMaterial, in: RoundedRectangle(cornerRadius: 10))
                .shadow(color: .black.opacity(0.15), radius: 6, x: 0, y: 3)
                .frame(maxWidth: 180)
                .transition(.scale.combined(with: .opacity))
            }
        }
    }
}

// MARK: - Pharmacy Annotation

struct PharmacyAnnotationView: View {
    let pharmacy: PharmacyItem
    @State private var showCallout = false

    var body: some View {
        VStack(spacing: 4) {
            Circle()
                .fill(Color.green)
                .frame(width: 32, height: 32)
                .overlay { Image(systemName: "cross.fill").font(.caption.bold()).foregroundColor(.white) }
                .overlay { Circle().stroke(Color.white, lineWidth: 2) }
                .shadow(color: .black.opacity(0.2), radius: 4, x: 0, y: 2)
                .onTapGesture {
                    withAnimation(.spring(response: 0.3)) { showCallout.toggle() }
                }

            if showCallout {
                VStack(alignment: .leading, spacing: 4) {
                    Text(pharmacy.name)
                        .font(.caption.bold()).lineLimit(2).multilineTextAlignment(.leading)
                    if !pharmacy.phone.isEmpty {
                        Button {
                            let digits = pharmacy.phone.filter { $0.isNumber || $0 == "+" }
                            if let url = URL(string: "tel://\(digits)") {
                                UIApplication.shared.open(url)
                            }
                        } label: {
                            HStack(spacing: 4) {
                                Image(systemName: "phone.fill").font(.caption2)
                                Text(pharmacy.phone).font(.caption2)
                            }
                            .foregroundColor(.green)
                        }
                    }
                }
                .padding(10)
                .background(.ultraThinMaterial, in: RoundedRectangle(cornerRadius: 10))
                .shadow(color: .black.opacity(0.15), radius: 6, x: 0, y: 3)
                .frame(maxWidth: 180)
                .transition(.scale.combined(with: .opacity))
            }
        }
    }
}

// MARK: - Data Models

struct UpaItem: Identifiable {
    let id: String
    let name: String
    let coordinate: CLLocationCoordinate2D
    let phone: String
}

struct PharmacyItem: Identifiable {
    let id: String
    let name: String
    let coordinate: CLLocationCoordinate2D
    let phone: String
}

struct MapPin: Identifiable {
    enum Kind {
        case upa(UpaItem)
        case pharmacy(PharmacyItem)
    }
    let id: String
    let coordinate: CLLocationCoordinate2D
    let kind: Kind
}
