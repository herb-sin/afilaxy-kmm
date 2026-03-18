import Foundation
import shared
import FirebaseFirestore
import FirebaseAuth

/// Bridge que conecta LocationManager (Swift) com IOSLocationBridge (Kotlin)
final class LocationManagerBridge {
    static let shared = LocationManagerBridge()
    
    private let locationManager = LocationManager.shared
    
    private init() {}
    
    func start() {
        FileLogger.shared.write(level: "INFO", tag: "LocationBridge", message: "start() hasPermission=\(locationManager.hasPermission) authStatus=\(locationManager.authorizationStatus.rawValue)")
        updateBridge()
        locationManager.startUpdating()
    }
    
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

    /// Ativa helper mode: escreve no Firestore via SDK iOS (evita crash no Kotlin/Native)
    /// e chama completion na main thread quando concluído.
    func enableHelperMode(lat: Double, lon: Double, completion: @escaping (Bool) -> Void) {
        FileLogger.shared.write(level: "INFO", tag: "LocationBridge", message: "enableHelperMode hasPermission=\(locationManager.hasPermission)")
        locationManager.startUpdating()
        guard let uid = Auth.auth().currentUser?.uid else {
            completion(false)
            return
        }
        let data: [String: Any] = [
            "id": uid,
            "location": GeoPoint(latitude: lat, longitude: lon),
            "latitude": lat,
            "longitude": lon,
            "isActive": true,
            "lastUpdate": FieldValue.serverTimestamp()
        ]
        Firestore.firestore().collection("helpers").document(uid).setData(data, merge: true) { error in
            DispatchQueue.main.async {
                if let error = error {
                    FileLogger.shared.write(level: "ERROR", tag: "LocationBridge", message: "enableHelperMode Firestore error: \(error.localizedDescription)")
                    completion(false)
                } else {
                    completion(true)
                }
            }
        }
    }

    func disableHelperMode() {
        FileLogger.shared.write(level: "INFO", tag: "LocationBridge", message: "disableHelperMode")
        locationManager.stopUpdating()
        guard let uid = Auth.auth().currentUser?.uid else { return }
        Firestore.firestore().collection("helpers").document(uid).delete()
    }
}
