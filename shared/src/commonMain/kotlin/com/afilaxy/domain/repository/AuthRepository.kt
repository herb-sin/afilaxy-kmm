package com.afilaxy.domain.repository

import com.afilaxy.domain.model.User
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    suspend fun login(email: String, password: String): Result<User>
    suspend fun register(email: String, password: String, name: String): Result<User>
    suspend fun logout()
    suspend fun getCurrentUser(): User?
    fun getCurrentUserId(): String?
    suspend fun updateFcmToken(token: String)
    suspend fun updateUserLocation(latitude: Double, longitude: Double)
    fun observeAuthState(): Flow<User?>
    suspend fun sendPasswordResetEmail(email: String): Result<Unit>
    suspend fun sendEmailVerification(): Result<Unit>
    suspend fun isEmailVerified(): Boolean
    suspend fun reloadUser()
    /** Grava um sessionId único no Firestore e localmente. Deve ser chamado após login bem-sucedido. */
    suspend fun createSession()
    /** Emite true quando outro dispositivo sobrescreve o sessionId — sessão atual deve ser encerrada. */
    fun observeSessionInvalidation(): Flow<Boolean>
}

