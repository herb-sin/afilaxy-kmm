import Foundation
import shared

/// Bridge que conecta LocationManager (Swift) com IOSLocationBridge (Kotlin)
final class LocationManagerBridge {
    static let shared = LocationManagerBridge()
    
    private let locationManager = LocationManager.shared
    
    private init() {}
    
    /// Inicia monitoramento de localização e sincroniza com Kotlin
    func start() {
        // Solicitar permissão inicial
        locationManager.requestWhenInUse()
        
        // Inicializar bridge com estado atual
        updateBridge()
        
        // Observar mudanças de localização
        locationManager.startUpdating()
    }
    
    /// Atualiza o bridge Kotlin com estado atual
    private func updateBridge() {
        IOSLocationBridge.shared.hasPermission = locationManager.hasPermission
        
        if let location = locationManager.currentLocation {
            IOSLocationBridge.shared.latitude = location.coordinate.latitude
            IOSLocationBridge.shared.longitude = location.coordinate.longitude
        }
    }
    
    /// Ativa modo helper (solicita permissão "sempre" se necessário)
    func enableHelperMode() {
        // Se já tem permissão "sempre", apenas iniciar background updates
        if locationManager.hasPermission {
            locationManager.startBackgroundUpdating()
            return
        }
        
        // Caso contrário, solicitar permissão "sempre"
        locationManager.requestAlwaysAuthorization()
        // startBackgroundUpdating será chamado automaticamente em didChangeAuthorization
    }
    
    /// Desativa modo helper
    func disableHelperMode() {
        locationManager.stopUpdating()
    }
}
