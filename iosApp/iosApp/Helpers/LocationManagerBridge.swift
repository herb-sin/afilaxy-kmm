import Foundation
import shared
import FirebaseFirestore
import FirebaseAuth

/// Bridge que conecta LocationManager (Swift) com IOSLocationBridge (Kotlin)
final class LocationManagerBridge {
    static let shared = LocationManagerBridge()
    
    private let locationManager = LocationManager.shared
    /// Controla se uma ativação de helper mode está em andamento.
    /// Setado para false por `cancelPendingHelperActivation()` antes de criar emergência.
    private var pendingHelperActivation = false
    
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
        pendingHelperActivation = true
        // Usa precisão reduzida para poupar bateria — 100m é suficiente para o raio de 5km
        locationManager.startUpdatingHelperMode()
        guard let uid = Auth.auth().currentUser?.uid else {
            pendingHelperActivation = false
            completion(false)
            return
        }
        // LGPD: arredonda coordenadas para 0.001° ≈ 111m — sem expor endereço exato
        let obfuscatedLat = (lat * 1000).rounded() / 1000.0
        let obfuscatedLon = (lon * 1000).rounded() / 1000.0
        // Nome de exibição para o marker do mapa (sem dados pessoais sensíveis)
        let displayName = Auth.auth().currentUser?.displayName ?? "Ajudante"
        let data: [String: Any] = [
            "id": uid,
            "name": displayName,
            "location": GeoPoint(latitude: obfuscatedLat, longitude: obfuscatedLon),
            "latitude": obfuscatedLat,
            "longitude": obfuscatedLon,
            "geohash": encodeGeohash(lat: obfuscatedLat, lon: obfuscatedLon),
            "isActive": true,
            "lastUpdate": FieldValue.serverTimestamp()
        ]
        Firestore.firestore().collection("helpers").document(uid).setData(data, merge: true) { error in
            DispatchQueue.main.async {
                // Se foi cancelado (emergência criada durante a espera), ignora o resultado
                guard self.pendingHelperActivation else {
                    FileLogger.shared.write(level: "INFO", tag: "LocationBridge",
                        message: "enableHelperMode ignorada — operação cancelada antes do Firestore confirmar")
                    completion(false)
                    return
                }
                self.pendingHelperActivation = false
                if let error = error {
                    FileLogger.shared.write(level: "ERROR", tag: "LocationBridge", message: "enableHelperMode Firestore error: \(error.localizedDescription)")
                    completion(false)
                } else {
                    FileLogger.shared.write(level: "INFO", tag: "LocationBridge", message: "enableHelperMode success uid=\(uid) lat=\(obfuscatedLat) lon=\(obfuscatedLon)")
                    completion(true)
                }
            }
        }
    }

    /// Ativa/reinicia o GPS em modo econômico para helper mode (foreground ou retorno do background).
    func startHelperModeGPS() {
        locationManager.startUpdatingHelperMode()
    }

    /// Cancela uma ativação de helper mode ainda pendente no Firestore.
    /// Deve ser chamado antes de `onCreateEmergency()` para evitar race condition
    /// onde o dispositivo se registra como helper da própria emergência.
    func cancelPendingHelperActivation() {
        guard pendingHelperActivation else { return }
        pendingHelperActivation = false
        FileLogger.shared.write(level: "INFO", tag: "LocationBridge",
            message: "cancelPendingHelperActivation — callback de helper mode será ignorado")
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
        // Para as atualizações de localização — GPS não fica ativo após desativar o toggle
        locationManager.stopUpdating()
        // Desativa background location — não mais necessário sem helper mode
        guard let uid = Auth.auth().currentUser?.uid else { return }
        Firestore.firestore().collection("helpers").document(uid).delete()
    }

    /// Aceita emergência via iOS SDK nativo (evita crash Kotlin/Native em thread não-main)
    func acceptEmergency(emergencyId: String, completion: @escaping (Bool, String?) -> Void) {
        guard let uid = Auth.auth().currentUser?.uid else {
            completion(false, "Usuário não autenticado")
            return
        }
        let db = Firestore.firestore()
        let ref = db.collection("emergency_requests").document(emergencyId)
        db.runTransaction({ transaction, errorPointer in
            let doc: DocumentSnapshot
            do { doc = try transaction.getDocument(ref) }
            catch let e as NSError { errorPointer?.pointee = e; return nil }
            guard doc.exists,
                  let active = doc.data()?["active"] as? Bool, active,
                  let status = doc.data()?["status"] as? String, status == "waiting",
                  doc.data()?["helperId"] == nil else {
                let e = NSError(domain: "Afilaxy", code: 409,
                    userInfo: [NSLocalizedDescriptionKey: "Emergência não disponível"])
                errorPointer?.pointee = e
                return nil
            }
            transaction.updateData([
                "status": "matched",
                "helperId": uid,
                "matchedAt": FieldValue.serverTimestamp()
            ], forDocument: ref)
            return nil
        }) { _, error in
            DispatchQueue.main.async {
                if let error = error {
                    FileLogger.shared.write(level: "ERROR", tag: "LocationBridge", message: "acceptEmergency error: \(error.localizedDescription)")
                    completion(false, error.localizedDescription)
                } else {
                    FileLogger.shared.write(level: "INFO", tag: "LocationBridge", message: "acceptEmergency success emergencyId=\(emergencyId)")
                    completion(true, nil)
                }
            }
        }
    }
}
