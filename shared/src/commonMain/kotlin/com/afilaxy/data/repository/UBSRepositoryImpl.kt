package com.afilaxy.data.repository

import com.afilaxy.domain.model.Location
import com.afilaxy.domain.model.UBS
import com.afilaxy.domain.repository.UBSRepository
import dev.gitlive.firebase.firestore.FirebaseFirestore
import dev.gitlive.firebase.firestore.GeoPoint
import kotlin.math.*

class UBSRepositoryImpl(
    private val firestore: FirebaseFirestore
) : UBSRepository {
    
    private val collection = firestore.collection("ubs")
    
    override suspend fun getNearby(location: Location, radiusKm: Double): List<UBS> {
        return try {
            val snapshot = collection.get()
            
            snapshot.documents
                .mapNotNull { doc ->
                    try {
                        val geoPoint = doc.get<GeoPoint>("location")
                        val ubsLocation = Location.create(geoPoint.latitude, geoPoint.longitude)
                        val distance = calculateDistance(location, ubsLocation)
                        
                        if (distance <= radiusKm) {
                            UBS(
                                id = doc.id,
                                name = doc.get("name"),
                                address = doc.get("address"),
                                location = ubsLocation,
                                phone = doc.get("phone"),
                                openingHours = doc.get("openingHours") ?: "Segunda a Sexta: 7h às 17h",
                                hasAsthmaProgram = doc.get("hasAsthmaProgram") ?: false,
                                hasFreemedication = doc.get("hasFreeMedication") ?: true,
                                distanceKm = distance
                            )
                        } else null
                    } catch (e: Exception) {
                        println("[UBSRepository] Erro ao processar documento ${doc.id}: ${e.message}")
                        null
                    }
                }
                .sortedBy { it.distanceKm }
        } catch (e: Exception) {
            println("[UBSRepository] Erro ao buscar UBS: ${e.message}")
            e.printStackTrace()
            emptyList()
        }
    }
    
    override suspend fun getById(id: String): UBS? {
        return try {
            val doc = collection.document(id).get()
            val geoPoint = doc.get<GeoPoint>("location")
            
            UBS(
                id = doc.id,
                name = doc.get("name"),
                address = doc.get("address"),
                location = Location.create(geoPoint.latitude, geoPoint.longitude),
                phone = doc.get("phone"),
                openingHours = doc.get("openingHours") ?: "Segunda a Sexta: 7h às 17h",
                hasAsthmaProgram = doc.get("hasAsthmaProgram") ?: false,
                hasFreemedication = doc.get("hasFreeMedication") ?: true
            )
        } catch (e: Exception) {
            null
        }
    }
    
    private fun calculateDistance(loc1: Location, loc2: Location): Double {
        val earthRadius = 6371.0 // km
        val dLat = (loc2.latitude - loc1.latitude).toRadians()
        val dLon = (loc2.longitude - loc1.longitude).toRadians()
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(loc1.latitude.toRadians()) * cos(loc2.latitude.toRadians()) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return earthRadius * c
    }

    private fun Double.toRadians() = this * PI / 180.0
}
