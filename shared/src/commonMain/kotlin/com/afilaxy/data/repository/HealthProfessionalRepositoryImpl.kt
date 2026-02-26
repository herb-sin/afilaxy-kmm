package com.afilaxy.data.repository

import com.afilaxy.domain.model.HealthProfessional
import com.afilaxy.domain.model.Location
import com.afilaxy.domain.model.Specialty
import com.afilaxy.domain.model.SubscriptionPlan
import com.afilaxy.domain.repository.HealthProfessionalRepository
import dev.gitlive.firebase.firestore.FirebaseFirestore
import dev.gitlive.firebase.firestore.where

class HealthProfessionalRepositoryImpl(
    private val firestore: FirebaseFirestore
) : HealthProfessionalRepository {
    
    private val collection = firestore.collection("health_professionals")
    
    override suspend fun getAll(): List<HealthProfessional> {
        return collection
            .where { "subscriptionExpiry" greaterThan System.currentTimeMillis() }
            .get()
            .documents
            .mapNotNull { it.data() }
            .sortedByDescending { calculatePriority(it) }
    }
    
    override suspend fun getById(id: String): HealthProfessional? {
        return collection.document(id).get().data()
    }
    
    override suspend fun findBySpecialty(specialty: Specialty): List<HealthProfessional> {
        return collection
            .where { 
                "specialty" equalTo specialty.name
                "subscriptionExpiry" greaterThan System.currentTimeMillis()
            }
            .get()
            .documents
            .mapNotNull { it.data() }
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
        val dLat = Math.toRadians(loc2.latitude - loc1.latitude)
        val dLon = Math.toRadians(loc2.longitude - loc1.longitude)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(loc1.latitude)) * Math.cos(Math.toRadians(loc2.latitude)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return earthRadius * c
    }
}
