import CoreLocation
import Combine

/// Gerencia permissões e atualizações de localização via CLLocationManager
final class LocationManager: NSObject, ObservableObject, CLLocationManagerDelegate {

    static let shared = LocationManager()

    private let manager = CLLocationManager()
    private var locationContinuation: CheckedContinuation<CLLocation?, Never>?

    @Published private(set) var currentLocation: CLLocation?
    @Published private(set) var authorizationStatus: CLAuthorizationStatus = .notDetermined

    private override init() {
        super.init()
        manager.delegate = self
        manager.desiredAccuracy = kCLLocationAccuracyBest
        manager.distanceFilter = 50 // atualiza a cada 50m
        authorizationStatus = manager.authorizationStatus
    }

    // MARK: - Permissões

    /// Solicita permissão "ao usar" (modo emergência)
    func requestWhenInUse() {
        manager.requestWhenInUseAuthorization()
    }

    /// Solicita permissão "sempre" (modo helper com background location)
    func requestAlwaysAuthorization() {
        manager.requestAlwaysAuthorization()
    }

    var hasPermission: Bool {
        switch manager.authorizationStatus {
        case .authorizedAlways, .authorizedWhenInUse:
            return true
        default:
            return false
        }
    }

    // MARK: - Localização

    func startUpdating() {
        if hasPermission {
            manager.startUpdatingLocation()
        } else {
            manager.requestWhenInUseAuthorization()
        }
    }

    func startBackgroundUpdating() {
        guard manager.authorizationStatus == .authorizedAlways else {
            manager.requestAlwaysAuthorization()
            return
        }
        manager.allowsBackgroundLocationUpdates = true
        manager.pausesLocationUpdatesAutomatically = false
        manager.startUpdatingLocation()
    }

    func stopUpdating() {
        manager.stopUpdatingLocation()
    }

    /// Obtém a localização atual de forma assíncrona.
    /// Usa a última localização conhecida se disponível e recente (< 30s),
    /// caso contrário solicita uma nova leitura e aguarda até 5 segundos.
    func fetchCurrentLocation() async -> CLLocation? {
        // Se já temos localização recente, retornar diretamente
        if let loc = currentLocation, Date().timeIntervalSince(loc.timestamp) < 30 {
            return loc
        }

        if !hasPermission {
            manager.requestWhenInUseAuthorization()
            return nil
        }

        return await withCheckedContinuation { continuation in
            locationContinuation = continuation
            manager.requestLocation()

            // Timeout de 5 segundos
            Task {
                try? await Task.sleep(nanoseconds: 5_000_000_000)
                if let cont = locationContinuation {
                    locationContinuation = nil
                    cont.resume(returning: currentLocation)
                }
            }
        }
    }

    // MARK: - CLLocationManagerDelegate

    func locationManager(_ manager: CLLocationManager, didUpdateLocations locations: [CLLocation]) {
        guard let location = locations.last else { return }
        currentLocation = location
        
        // Atualizar bridge para Kotlin
        IOSLocationBridge.shared.latitude = location.coordinate.latitude
        IOSLocationBridge.shared.longitude = location.coordinate.longitude

        if let cont = locationContinuation {
            locationContinuation = nil
            cont.resume(returning: location)
        }
    }

    func locationManager(_ manager: CLLocationManager, didFailWithError error: Error) {
        print("LocationManager error: \(error.localizedDescription)")
        if let cont = locationContinuation {
            locationContinuation = nil
            cont.resume(returning: currentLocation) // retorna última conhecida ou nil
        }
    }

    func locationManagerDidChangeAuthorization(_ manager: CLLocationManager) {
        authorizationStatus = manager.authorizationStatus
        
        // Atualizar bridge para Kotlin
        IOSLocationBridge.shared.hasPermission = hasPermission
        
        if hasPermission {
            // Se tem permissão "sempre", ativar background updates
            if manager.authorizationStatus == .authorizedAlways {
                manager.allowsBackgroundLocationUpdates = true
                manager.pausesLocationUpdatesAutomatically = false
            }
            manager.startUpdatingLocation()
        }
    }
}
