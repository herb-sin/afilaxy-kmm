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
                // Token não logado — dado sensível (CWE-117)
                Log.d("FCM", "Token obtido com sucesso")
                authRepository.updateFcmToken(token)
            } catch (e: Exception) {
                Log.e("FCM", "Erro ao obter token")
            }
        }
    }
}
