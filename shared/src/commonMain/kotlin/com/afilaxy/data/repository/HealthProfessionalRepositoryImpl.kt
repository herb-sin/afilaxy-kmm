package com.afilaxy.data.repository

import com.afilaxy.domain.model.HealthProfessional
import com.afilaxy.domain.model.Location
import com.afilaxy.domain.model.Specialty
import com.afilaxy.util.Logger
import com.afilaxy.domain.model.SubscriptionPlan
import com.afilaxy.domain.model.getCurrentTimeMillis
import com.afilaxy.domain.repository.HealthProfessionalRepository
import dev.gitlive.firebase.auth.FirebaseAuth
import dev.gitlive.firebase.firestore.FirebaseFirestore
import dev.gitlive.firebase.firestore.where
import com.afilaxy.util.haversineDistance

class HealthProfessionalRepositoryImpl(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : HealthProfessionalRepository {
    
    private val collection = firestore.collection("health_professionals")
    
    override suspend fun getAll(): List<HealthProfessional> {
        // Guard: Firebase auth token ainda não está disponível (warmup do Koin antes do
        // authStateDidChange). Retorna lista vazia sem query — o caller (ViewModel) vai
        // recarregar após authStateDidChange confirmar o usuário.
        if (auth.currentUser == null) {
            Logger.w("HealthProfessionalRepo", "getAll: currentUser == null — aguardando auth")
            return emptyList()
        }
        return try {
            collection
                .get()
                .documents
                .mapNotNull { doc ->
                    try {
                        mapDocToHealthProfessional(doc)
                    } catch (e: Exception) {
                        Logger.e("HealthProfessionalRepo", "Deserialization failed for ${doc.id}", e)
                        null
                    }
                }
                .sortedByDescending { calculatePriority(it) }
        } catch (e: Exception) {
            Logger.e("HealthProfessionalRepo", "getAll failed", e)
            emptyList()
        }
    }
    
    override suspend fun getById(id: String): HealthProfessional? {
        return try {
            val doc = collection.document(id).get()
            if (doc.exists) mapDocToHealthProfessional(doc) else null
        } catch (e: Exception) {
            Logger.e("HealthProfessionalRepo", "getById failed for $id", e)
            null
        }
    }
    
    override suspend fun findBySpecialty(specialty: Specialty): List<HealthProfessional> {
        return try {
            collection
                .where {
                    "specialty" equalTo specialty.name
                    "subscriptionExpiry" greaterThan getCurrentTimeMillis()
                }
                .get()
                .documents
                .mapNotNull { doc ->
                    try { mapDocToHealthProfessional(doc) } catch (e: Exception) { null }
                }
                .sortedByDescending { calculatePriority(it) }
        } catch (e: Exception) {
            Logger.e("HealthProfessionalRepo", "findBySpecialty failed", e)
            emptyList()
        }
    }
    
    override suspend fun findNearby(location: Location, radiusKm: Double): List<HealthProfessional> {
        // Backlog: substituir por query geoespacial com GeoHash (como onHelperWrite faz para helpers).
        // A implementação atual (getAll + filtro Haversine client-side) funciona para <100 profissionais.
        return getAll().filter { professional ->
            professional.location?.let { profLocation ->
                calculateDistance(location, profLocation) <= radiusKm
            } ?: false
        }
    }
    
    override suspend fun updateSubscription(id: String, plan: SubscriptionPlan, expiryDate: Long) {
        auth.currentUser ?: error("User not authenticated")
        collection.document(id).update(
            "subscriptionPlan" to plan.name,
            "subscriptionExpiry" to expiryDate
        )
    }
    
    private fun mapDocToHealthProfessional(doc: dev.gitlive.firebase.firestore.DocumentSnapshot): HealthProfessional {
        val planStr = doc.get<String?>("subscriptionPlan") ?: "NONE"
        val plan = try { SubscriptionPlan.valueOf(planStr) } catch (e: Exception) { SubscriptionPlan.NONE }

        val specialtyStr = doc.get<String?>("specialty") ?: "PNEUMOLOGIST"
        val specialty = try { Specialty.valueOf(specialtyStr) } catch (e: Exception) { Specialty.PNEUMOLOGIST }

        // GeoPoint salvo pelo seed — extrair lat/lon sem depender de deserialização automática
        val geoPoint = try { doc.get<dev.gitlive.firebase.firestore.GeoPoint?>("location") } catch (e: Exception) { null }
        val location = geoPoint?.let {
            Location(latitude = it.latitude, longitude = it.longitude, address = "", timestamp = 0L)
        }

        return HealthProfessional(
            id = doc.get<String?>("id") ?: doc.id,
            name = doc.get<String?>("name") ?: "",
            specialty = specialty,
            crm = doc.get<String?>("crm") ?: "",
            subscriptionPlan = plan,
            subscriptionExpiry = doc.get<Long?>("subscriptionExpiry") ?: 0L,
            profilePhoto = doc.get<String?>("profilePhoto"),
            bio = doc.get<String?>("bio") ?: "",
            phone = doc.get<String?>("phone"),
            whatsapp = doc.get<String?>("whatsapp"),
            clinicAddress = doc.get<String?>("clinicAddress"),
            location = location,
            rating = doc.get<Double?>("rating") ?: 0.0,
            totalReviews = doc.get<Int?>("totalReviews") ?: 0,
            isVerified = doc.get<Boolean?>("isVerified") ?: false
        )
    }

    // Ordenação por critério técnico — plano de assinatura não influencia posição (CFM)
    private fun calculatePriority(professional: HealthProfessional): Double {
        val hasSubscription = if (professional.subscriptionPlan.tier() != com.afilaxy.domain.model.PlanTier.NONE) 0.001 else 0.0
        return professional.rating + hasSubscription
    }
    
    private fun calculateDistance(loc1: Location, loc2: Location): Double =
        haversineDistance(loc1.latitude, loc1.longitude, loc2.latitude, loc2.longitude)
}
