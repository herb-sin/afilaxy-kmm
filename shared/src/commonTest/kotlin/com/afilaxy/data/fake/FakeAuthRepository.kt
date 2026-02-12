package com.afilaxy.data.fake

import com.afilaxy.domain.model.User
import com.afilaxy.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * Fake implementation of AuthRepository for testing
 */
class FakeAuthRepository(
    private var currentUser: User? = null,
    private var shouldSucceed: Boolean = true
) : AuthRepository {
    
    override suspend fun login(email: String, password: String): Result<User> {
        return if (shouldSucceed) {
            val user = User(
                uid = "test-uid",
                email = email,
                name = "Test User",
                fcmToken = null,
                isHelper = false
            )
            currentUser = user
            Result.success(user)
        } else {
            Result.failure(Exception("Login failed"))
        }
    }
    
    override suspend fun register(email: String, password: String, name: String): Result<User> {
        return if (shouldSucceed) {
            val user = User(
                uid = "test-uid",
                email = email,
                name = name,
                fcmToken = null,
                isHelper = false
            )
            currentUser = user
            Result.success(user)
        } else {
            Result.failure(Exception("Registration failed"))
        }
    }
    
    override suspend fun logout() {
        currentUser = null
    }
    
    override suspend fun getCurrentUser(): User? {
        return currentUser
    }
    
    override fun getCurrentUserId(): String? {
        return currentUser?.uid
    }
    
    override suspend fun updateFcmToken(token: String) {
        // No-op for testing
    }
    
    override suspend fun updateUserLocation(latitude: Double, longitude: Double) {
        // No-op for testing
    }
    
    override fun observeAuthState(): Flow<User?> {
        return flowOf(currentUser)
    }
    
    // Test helpers
    fun setCurrentUser(user: User?) {
        currentUser = user
    }
    
    fun setShouldSucceed(shouldSucceed: Boolean) {
        this.shouldSucceed = shouldSucceed
    }
}
