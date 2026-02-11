package com.afilaxy.data.repository

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.afilaxy.domain.model.Location
import com.afilaxy.domain.repository.LocationRepository
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.tasks.await

/**
 * Implementação Android do LocationRepository
 * Usa Google Play Services Location API
 */
actual class LocationRepositoryImpl(
    private val context: Context
) : LocationRepository {
    
    private val fusedLocationClient: FusedLocationProviderClient by lazy {
        LocationServices.getFusedLocationProviderClient(context)
    }
    
    @SuppressLint("MissingPermission")
    actual override suspend fun getCurrentLocation(): Location? {
        return try {
            if (!hasLocationPermission()) {
                return null
            }
            
            val cancellationToken = CancellationTokenSource()
            
            val location = fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                cancellationToken.token
            ).await()
            
            if (location != null) {
                Location(
                    latitude = location.latitude,
                    longitude = location.longitude,
                    address = "", // Geocoding pode ser adicionado depois
                    timestamp = System.currentTimeMillis(),
                    accuracy = location.accuracy
                )
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
    
    actual override fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }
}
