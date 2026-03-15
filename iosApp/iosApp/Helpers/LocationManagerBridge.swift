import Foundation
import shared

/// Bridge que conecta LocationManager (Swift) com IOSLocationBridge (Kotlin)
final class LocationManagerBridge {
    static let shared = LocationManagerBridge()
    
    private let locationManager = LocationManager.shared
    
    private init() {}
    
    /// Inicia monitoramento de localização e sincroniza com Kotlin
    func start() {
        FileLogger.shared.write(level: "INFO", tag: "LocationBridge", message: "start() hasPermission=\(locationManager.hasPermission) authStatus=\(locationManager.authorizationStatus.rawValue)")
        locationManager.requestWhenInUse()
        updateBridge()
        locationManager.startUpdating()
    }
    
    /// Atualiza o bridge Kotlin com estado atual
    private func updateBridge() {
        IOSLocationBridge.shared.hasPermission = locationManager.hasPermission
        
        if let location = locationManager.currentLocation {
            IOSLocationBridge.shared.latitude = location.coordinate.latitude
            IOSLocationBridge.shared.longitude = location.coordinate.longitude
            FileLogger.shared.write(level: "INFO", tag: "LocationBridge", message: "updateBridge lat=\(location.coordinate.latitude) lon=\(location.coordinate.longitude)")
        } else {
            FileLogger.shared.write(level: "WARN", tag: "LocationBridge", message: "updateBridge: no location yet, hasPermission=\(locationManager.hasPermission)")
        }
    }
    
    /// Ativa modo helper (solicita permissão "sempre" se necessário)
    func enableHelperMode() {
        FileLogger.shared.write(level: "INFO", tag: "LocationBridge", message: "enableHelperMode hasPermission=\(locationManager.hasPermission)")
        if locationManager.hasPermission {
            locationManager.startBackgroundUpdating()
            return
        }
        locationManager.requestAlwaysAuthorization()
    }
    
    /// Desativa modo helper
    func disableHelperMode() {
        FileLogger.shared.write(level: "INFO", tag: "LocationBridge", message: "disableHelperMode")
        locationManager.stopUpdating()
    }
}
