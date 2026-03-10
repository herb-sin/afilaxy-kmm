import Foundation
import CoreLocation
import shared

/// Ponte unidirecional: Swift → Kotlin
/// Swift lê a localização do CLLocationManager e escreve em IOSLocationBridge (Kotlin object)
/// para que o LocationRepositoryImpl (iosMain) possa ler sem dependência circular.
final class LocationManagerBridge: NSObject, CLLocationManagerDelegate {

    static let shared = LocationManagerBridge()
    private let manager = CLLocationManager()

    private override init() {
        super.init()
        manager.delegate = self
        manager.desiredAccuracy = kCLLocationAccuracyBest
        manager.distanceFilter = 50
        updatePermissionBridge()
    }

    func start() {
        switch manager.authorizationStatus {
        case .authorizedAlways, .authorizedWhenInUse:
            manager.startUpdatingLocation()
        default:
            manager.requestWhenInUseAuthorization()
        }
    }

    func requestBackground() {
        manager.requestAlwaysAuthorization()
        if manager.authorizationStatus == .authorizedAlways {
            manager.allowsBackgroundLocationUpdates = true
            manager.startUpdatingLocation()
        }
    }

    // MARK: - CLLocationManagerDelegate

    func locationManager(_ manager: CLLocationManager, didUpdateLocations locations: [CLLocation]) {
        guard let loc = locations.last else { return }
        IOSLocationBridge.shared.latitude = loc.coordinate.latitude
        IOSLocationBridge.shared.longitude = loc.coordinate.longitude
        IOSLocationBridge.shared.hasPermission = true
    }

    func locationManager(_ manager: CLLocationManager, didFailWithError error: Error) {
        print("LocationManagerBridge error: \(error.localizedDescription)")
    }

    func locationManagerDidChangeAuthorization(_ manager: CLLocationManager) {
        updatePermissionBridge()
        if manager.authorizationStatus == .authorizedWhenInUse ||
           manager.authorizationStatus == .authorizedAlways {
            manager.startUpdatingLocation()
        }
    }

    private func updatePermissionBridge() {
        let granted = manager.authorizationStatus == .authorizedAlways ||
                      manager.authorizationStatus == .authorizedWhenInUse
        IOSLocationBridge.shared.hasPermission = granted
    }
}
