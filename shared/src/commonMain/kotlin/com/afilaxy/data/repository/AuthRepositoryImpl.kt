package com.afilaxy.data.repository

import com.afilaxy.domain.model.User
import com.afilaxy.domain.model.getCurrentTimeMillis
import com.afilaxy.domain.repository.AuthRepository
import dev.gitlive.firebase.auth.FirebaseAuth
import dev.gitlive.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Implementação do AuthRepository usando Firebase Authentication
 * Compatível com KMM (comum para Android e iOS)
 */
class AuthRepositoryImpl(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : AuthRepository {
    
    override suspend fun login(email: String, password: String): Result<User> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password)
            val firebaseUser = result.user
            
            if (firebaseUser == null) {
                return Result.failure(Exception("Usuário não encontrado"))
            }
            
            // Buscar dados do perfil no Firestore
            val userDoc = firestore
                .collection("users")
                .document(firebaseUser.uid)
                .get()
            
            val user = User(
                uid = firebaseUser.uid,
                email = firebaseUser.email ?: "",
                name = userDoc.get<String?>("name") ?: firebaseUser.displayName,
                fcmToken = userDoc.get<String?>("fcmToken"),
                isHelper = userDoc.get<Boolean?>("isHelper") ?: false
            )
            
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(Exception(mapAuthError(e.message)))
        }
    }
    
    override suspend fun register(email: String, password: String, name: String): Result<User> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password)
            val firebaseUser = result.user
            
            if (firebaseUser == null) {
                return Result.failure(Exception("Erro ao criar usuário"))
            }
            
            // Criar perfil no Firestore
            val userData = mapOf(
                "uid" to firebaseUser.uid,
                "email" to email,
                "name" to name,
                "isHelper" to false,
                "createdAt" to getCurrentTimeMillis()
            )
            
            firestore
                .collection("users")
                .document(firebaseUser.uid)
                .set(userData)
            
            val user = User(
                uid = firebaseUser.uid,
                email = email,
                name = name,
                fcmToken = null,
                isHelper = false
            )
            
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(Exception(mapAuthError(e.message)))
        }
    }
    
    override suspend fun logout() {
        try {
            auth.signOut()
        } catch (e: Exception) {
            // Log error but don't throw - logout should always succeed
            println("Error during logout: ${e.message}")
        }
    }
    
    override suspend fun getCurrentUser(): User? {
        val firebaseUser = auth.currentUser ?: return null
        
        return try {
            val userDoc = firestore
                .collection("users")
                .document(firebaseUser.uid)
                .get()
            
            User(
                uid = firebaseUser.uid,
                email = firebaseUser.email ?: "",
                name = userDoc.get<String?>("name") ?: firebaseUser.displayName,
                fcmToken = userDoc.get<String?>("fcmToken"),
                isHelper = userDoc.get<Boolean?>("isHelper") ?: false
            )
        } catch (e: Exception) {
            null
        }
    }
    
    override fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }
    
    override suspend fun updateFcmToken(token: String) {
        val firebaseUser = auth.currentUser ?: return
        
        try {
            firestore
                .collection("users")
                .document(firebaseUser.uid)
                .update("fcmToken" to token)
        } catch (e: Exception) {
            // Silently fail
        }
    }
    
    override fun observeAuthState(): Flow<User?> {
        return auth.authStateChanged.map { firebaseUser ->
            if (firebaseUser != null) {
                try {
                    val userDoc = firestore
                        .collection("users")
                        .document(firebaseUser.uid)
                        .get()
                    
                    User(
                        uid = firebaseUser.uid,
                        email = firebaseUser.email ?: "",
                        name = userDoc.get<String?>("name") ?: firebaseUser.displayName,
                        fcmToken = userDoc.get<String?>("fcmToken"),
                        isHelper = userDoc.get<Boolean?>("isHelper") ?: false
                    )
                } catch (e: Exception) {
                    null
                }
            } else {
                null
            }
        }
    }
    
    override suspend fun updateUserLocation(latitude: Double, longitude: Double) {
        val firebaseUser = auth.currentUser ?: return
        
        try {
            firestore
                .collection("users")
                .document(firebaseUser.uid)
                .update(mapOf(
                    "latitude" to latitude,
                    "longitude" to longitude
                ))
        } catch (e: Exception) {
            // Silently fail
        }
    }
    
    /**
     * Mapeia mensagens de erro do Firebase para mensagens amigáveis
     */
    private fun mapAuthError(message: String?): String {
        return when {
            message?.contains("password", ignoreCase = true) == true -> "Senha incorreta"
            message?.contains("email", ignoreCase = true) == true -> "Email inválido"
            message?.contains("user", ignoreCase = true) == true -> "Usuário não encontrado"
            message?.contains("network", ignoreCase = true) == true -> "Erro de conexão"
            else -> "Erro ao autenticar: ${message ?: "desconhecido"}"
        }
    }
}
