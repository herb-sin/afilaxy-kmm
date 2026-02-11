package com.afilaxy.domain.repository

import com.afilaxy.domain.model.UserProfile

interface ProfileRepository {
    suspend fun getProfile(userId: String): Result<UserProfile?>
    suspend fun updateProfile(profile: UserProfile): Result<Unit>
    suspend fun updateHealthData(userId: String, healthData: com.afilaxy.domain.model.UserHealthData): Result<Unit>
    suspend fun updateEmergencyContact(userId: String, contact: com.afilaxy.domain.model.EmergencyContact): Result<Unit>
}
