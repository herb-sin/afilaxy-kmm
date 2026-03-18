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
            "geohash": encodeGeohash(lat: lat, lon: lon),
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

    // Geohash precision=9 — compatível com geofire-common
    private func encodeGeohash(lat: Double, lon: Double, precision: Int = 9) -> String {
        let base32 = Array("0123456789bcdefghjkmnpqrstuvwxyz")
        var minLat = -90.0, maxLat = 90.0, minLon = -180.0, maxLon = 180.0
        var hash = "", bits = 0, bitsTotal = 0, hashValue = 0
        while hash.count < precision {
            if bitsTotal % 2 == 0 {
                let mid = (minLon + maxLon) / 2
                if lon >= mid { hashValue = (hashValue << 1) | 1; minLon = mid }
                else { hashValue = hashValue << 1; maxLon = mid }
            } else {
                let mid = (minLat + maxLat) / 2
                if lat >= mid { hashValue = (hashValue << 1) | 1; minLat = mid }
                else { hashValue = hashValue << 1; maxLat = mid }
            }
            bits += 1; bitsTotal += 1
            if bits == 5 { hash.append(base32[hashValue]); bits = 0; hashValue = 0 }
        }
        return hash
    }

    func disableHelperMode() {
        FileLogger.shared.write(level: "INFO", tag: "LocationBridge", message: "disableHelperMode")
        locationManager.stopUpdating()
        guard let uid = Auth.auth().currentUser?.uid else { return }
        Firestore.firestore().collection("helpers").document(uid).delete()
    }
}
