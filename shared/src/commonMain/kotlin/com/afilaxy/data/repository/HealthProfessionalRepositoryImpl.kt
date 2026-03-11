package com.afilaxy.data.repository

import com.afilaxy.domain.model.HealthProfessional
import com.afilaxy.domain.model.Location
import com.afilaxy.domain.model.Specialty
import com.afilaxy.domain.model.SubscriptionPlan
import com.afilaxy.domain.model.getCurrentTimeMillis
import com.afilaxy.domain.repository.HealthProfessionalRepository
import dev.gitlive.firebase.firestore.FirebaseFirestore
import dev.gitlive.firebase.firestore.where
import kotlin.math.*

class HealthProfessionalRepositoryImpl(
    private val firestore: FirebaseFirestore
) : HealthProfessionalRepository {
    
    private val collection = firestore.collection("health_professionals")
    
    override suspend fun getAll(): List<HealthProfessional> {
        return try {
            collection
                .get()
                .documents
                .mapNotNull { doc ->
                    try {
                        doc.data<HealthProfessional>()
                    } catch (e: Exception) {
                        println("❌ HealthProfessional deserialization failed: ${e.message}")
                        null
                    }
                }
                .sortedByDescending { calculatePriority(it) }
        } catch (e: Exception) {
            println("❌ getAll professionals failed: ${e.message}")
            emptyList()
        }
    }
    
    override suspend fun getById(id: String): HealthProfessional? {
        return collection.document(id).get().data<HealthProfessional>()
    }
    
    override suspend fun findBySpecialty(specialty: Specialty): List<HealthProfessional> {
        return collection
            .where { 
                "specialty" equalTo specialty.name
                "subscriptionExpiry" greaterThan getCurrentTimeMillis()
            }
            .get()
            .documents
            .mapNotNull { it.data<HealthProfessional>() }
            .sortedByDescending { calculatePriority(it) }
    }
    
    override suspend fun findNearby(location: Location, radiusKm: Double): List<HealthProfessional> {
        // TODO: Implementar query geoespacial com GeoHash
        return getAll().filter { professional ->
            professional.location?.let { profLocation ->
                calculateDistance(location, profLocation) <= radiusKm
            } ?: false
        }
    }
    
    override suspend fun updateSubscription(id: String, plan: SubscriptionPlan, expiryDate: Long) {
        collection.document(id).update(
            "subscriptionPlan" to plan.name,
            "subscriptionExpiry" to expiryDate
        )
    }
    
    private fun calculatePriority(professional: HealthProfessional): Int {
        return when (professional.subscriptionPlan) {
            SubscriptionPlan.PREMIUM -> 3
            SubscriptionPlan.PRO -> 2
            SubscriptionPlan.BASIC -> 1
            SubscriptionPlan.NONE -> 0
        }
    }
    
    private fun calculateDistance(loc1: Location, loc2: Location): Double {
        // Fórmula Haversine
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
