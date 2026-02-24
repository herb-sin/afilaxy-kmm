package com.afilaxy.data.repository

import com.afilaxy.domain.model.Location
import com.afilaxy.domain.repository.LocationRepository
import platform.CoreLocation.*
import platform.Foundation.NSError
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.cinterop.ExperimentalForeignApi
import kotlin.coroutines.resume
import platform.darwin.NSObject

/**
 * Implementação iOS do LocationRepository
 * Usa CoreLocation framework
 */
actual class LocationRepositoryImpl : LocationRepository {
    
    private val locationManager = CLLocationManager()
    
    @OptIn(ExperimentalForeignApi::class)
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
                                latitude = clLocation.coordinate.useContents { latitude },
                                longitude = clLocation.coordinate.useContents { longitude },
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
