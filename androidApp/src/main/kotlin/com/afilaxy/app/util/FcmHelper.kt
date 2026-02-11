package com.afilaxy.app.util

import android.util.Log
import com.afilaxy.domain.repository.AuthRepository
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

object FcmHelper {
    
    fun requestFcmToken(authRepository: AuthRepository) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val token = FirebaseMessaging.getInstance().token.await()
                Log.d("FCM", "Token obtained: $token")
                authRepository.updateFcmToken(token)
            } catch (e: Exception) {
                Log.e("FCM", "Error getting token", e)
            }
        }
    }
}
