import CoreLocation
import Combine
import shared

/// Gerencia permissões e atualizações de localização via CLLocationManager
final class LocationManager: NSObject, ObservableObject, CLLocationManagerDelegate {

    static let shared: LocationManager = {
        if Thread.isMainThread {
            return LocationManager()
        } else {
            var instance: LocationManager!
            DispatchQueue.main.sync { instance = LocationManager() }
            return instance
        }
    }()

    private let manager = CLLocationManager()
    private var locationContinuation: CheckedContinuation<CLLocation?, Never>?

    @Published private(set) var currentLocation: CLLocation?
    @Published private(set) var authorizationStatus: CLAuthorizationStatus = .notDetermined

    private override init() {
        super.init()
        manager.delegate = self
        manager.desiredAccuracy = kCLLocationAccuracyBest
        manager.distanceFilter = 50
        // NÃO lê authorizationStatus aqui — o iOS ainda não carregou o estado.
        // O delegate locationManagerDidChangeAuthorization é chamado automaticamente
        // pelo iOS logo após o delegate ser atribuído, com o valor correto.
        FileLogger.shared.write(level: "INFO", tag: "LocationManager", message: "init — aguardando delegate para authStatus")
    }

    // MARK: - Permissões

    /// Solicita permissão "ao usar" (modo emergência)
    func requestWhenInUse() {
        let desc = Bundle.main.object(forInfoDictionaryKey: "NSLocationWhenInUseUsageDescription") as? String
        FileLogger.shared.write(level: "INFO", tag: "LocationManager", message: "requestWhenInUse authStatus=\(manager.authorizationStatus.rawValue) plistKey=\(desc != nil ? "OK" : "MISSING")")
        manager.requestWhenInUseAuthorization()
    }

    /// Solicita permissão "sempre" (modo helper com background location)
    func requestAlwaysAuthorization() {
        manager.requestAlwaysAuthorization()
    }

    var hasPermission: Bool {
        switch authorizationStatus {
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
        // Ativa atualizações em segundo plano — necessário para o Modo Ajudante
        // funcionar quando o app não está em foreground.
        // allowsBackgroundLocationUpdates requer UIBackgroundModes: location no Info.plist (já configurado).
        // showsBackgroundLocationIndicator exibe a barra azul do iOS (exigida pela Apple).
        if hasPermission {
            manager.allowsBackgroundLocationUpdates = true
            manager.showsBackgroundLocationIndicator = true
            manager.startUpdatingLocation()
        }
    }

    /// Solicita upgrade de WhenInUse para Always (necessário para helper em background).
    /// Chamado com asyncAfter(0.5s) para não conflitar com rerender SwiftUI.
    func requestAlwaysIfNeeded() {
        let status = manager.authorizationStatus
        guard status == .authorizedWhenInUse else { return }
        FileLogger.shared.write(level: "INFO", tag: "LocationManager",
            message: "requestAlways upgrade (status=\(status.rawValue))")
        manager.requestAlwaysAuthorization()
    }

    func stopUpdating() {
        manager.stopUpdatingLocation()
    }

    /// Obtém a localização atual de forma assíncrona.
    /// Usa a última localização conhecida se disponível e recente (< 30s),
    /// caso contrário solicita uma nova leitura e aguarda até 5 segundos.
    func fetchCurrentLocation() async -> CLLocation? {
        // Usa localização recente (< 120s) diretamente sem aguardar GPS
        if let loc = currentLocation, Date().timeIntervalSince(loc.timestamp) < 120 {
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
        FileLogger.shared.write(level: "INFO", tag: "LocationManager", message: "didUpdateLocations lat=\(location.coordinate.latitude) lon=\(location.coordinate.longitude) acc=\(location.horizontalAccuracy)")
        
        // Atualizar bridge para Kotlin
        IOSLocationBridge.shared.latitude = location.coordinate.latitude
        IOSLocationBridge.shared.longitude = location.coordinate.longitude

        if let cont = locationContinuation {
            locationContinuation = nil
            cont.resume(returning: location)
        }
    }

    func locationManager(_ manager: CLLocationManager, didFailWithError error: Error) {
        FileLogger.shared.write(level: "ERROR", tag: "LocationManager", message: "Location update failed")
        if let cont = locationContinuation {
            locationContinuation = nil
            cont.resume(returning: currentLocation)
        }
    }

    func locationManagerDidChangeAuthorization(_ manager: CLLocationManager) {
        let status = manager.authorizationStatus
        // Nunca regredir para notDetermined se já tínhamos permissão
        if status == .notDetermined && hasPermission { return }
        authorizationStatus = status
        FileLogger.shared.write(level: "INFO", tag: "LocationManager", message: "authorizationChanged status=\(status.rawValue) hasPermission=\(hasPermission) accuracy=\(manager.accuracyAuthorization.rawValue)")
        
        // Atualizar bridge para Kotlin
        IOSLocationBridge.shared.hasPermission = hasPermission
        
        if hasPermission {
            manager.startUpdatingLocation()
        }
    }
}
