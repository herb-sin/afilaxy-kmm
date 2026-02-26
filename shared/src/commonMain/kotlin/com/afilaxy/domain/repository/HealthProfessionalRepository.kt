package com.afilaxy.domain.repository

import com.afilaxy.domain.model.HealthProfessional
import com.afilaxy.domain.model.Location
import com.afilaxy.domain.model.Specialty
import com.afilaxy.domain.model.SubscriptionPlan

interface HealthProfessionalRepository {
    suspend fun getAll(): List<HealthProfessional>
    suspend fun getById(id: String): HealthProfessional?
    suspend fun findBySpecialty(specialty: Specialty): List<HealthProfessional>
    suspend fun findNearby(location: Location, radiusKm: Double): List<HealthProfessional>
    suspend fun updateSubscription(id: String, plan: SubscriptionPlan, expiryDate: Long)
}
