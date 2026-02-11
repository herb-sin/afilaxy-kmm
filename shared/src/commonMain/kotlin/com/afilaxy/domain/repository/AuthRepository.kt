package com.afilaxy.domain.repository

import com.afilaxy.domain.model.User
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    suspend fun login(email: String, password: String): Result<User>
    suspend fun register(email: String, password: String, name: String): Result<User>
    suspend fun logout()
    suspend fun getCurrentUser(): User?
    suspend fun updateFcmToken(token: String)
    suspend fun updateUserLocation(latitude: Double, longitude: Double)
    fun observeAuthState(): Flow<User?>
}

