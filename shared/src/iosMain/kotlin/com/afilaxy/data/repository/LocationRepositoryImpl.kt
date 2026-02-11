package com.afilaxy.data.repository

import com.afilaxy.domain.model.Location
import com.afilaxy.domain.repository.LocationRepository
import platform.CoreLocation.*
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Implementação iOS do LocationRepository
 * Usa CoreLocation framework
 */
actual class LocationRepositoryImpl : LocationRepository {
    
    private val locationManager = CLLocationManager()
    
    actual override suspend fun getCurrentLocation(): Location? {
        return suspendCancellableCoroutine { continuation ->
            try {
                if (!hasLocationPermission()) {
                    continuation.resume(null)
                    return@suspendCancellableCoroutine
                }
                
                val delegate = object : NSObject(), CLLocationManagerDelegateProtocol {
                    override fun locationManager(
                        manager: CLLocationManager,
                        didUpdateLocations: List<*>
                    ) {
                        val clLocation = didUpdateLocations.lastOrNull() as? CLLocation
                        if (clLocation != null) {
                            val location = Location(
                                latitude = clLocation.coordinate.latitude,
                                longitude = clLocation.coordinate.longitude,
                                address = "",
                                timestamp = (clLocation.timestamp.timeIntervalSince1970 * 1000).toLong(),
                                accuracy = clLocation.horizontalAccuracy.toFloat()
                            )
                            continuation.resume(location)
                        } else {
                            continuation.resume(null)
                        }
                        manager.stopUpdatingLocation()
                    }
                    
                    override fun locationManager(
                        manager: CLLocationManager,
                        didFailWithError: NSError
                    ) {
                        continuation.resume(null)
                        manager.stopUpdatingLocation()
                    }
                }
                
                locationManager.delegate = delegate
                locationManager.requestLocation()
                
            } catch (e: Exception) {
                continuation.resume(null)
            }
        }
    }
    
    actual override fun hasLocationPermission(): Boolean {
        val status = CLLocationManager.authorizationStatus()
        return status == kCLAuthorizationStatusAuthorizedWhenInUse ||
               status == kCLAuthorizationStatusAuthorizedAlways
    }
}
