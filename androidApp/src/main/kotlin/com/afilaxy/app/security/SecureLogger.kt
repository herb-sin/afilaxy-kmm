package com.afilaxy.app.security

import android.util.Log

object SecureLogger {
    
    private const val MAX_LOG_LENGTH = 4000
    
    fun d(tag: String, message: String) {
        Log.d(sanitize(tag), sanitize(message))
    }
    
    fun i(tag: String, message: String) {
        Log.i(sanitize(tag), sanitize(message))
    }
    
    fun w(tag: String, message: String) {
        Log.w(sanitize(tag), sanitize(message))
    }
    
    fun e(tag: String, message: String, throwable: Throwable? = null) {
        if (throwable != null) {
            Log.e(sanitize(tag), sanitize(message), throwable)
        } else {
            Log.e(sanitize(tag), sanitize(message))
        }
    }
    
    // Remove \n, \r e demais caracteres de controle de dados externos antes de logar (CWE-117).
    // Aplicar também ao sanitize() do SecureLogger para consistência.
    private fun sanitize(input: String): String {
        return try {
            input
                .replace(Regex("[\\r\\n\\t\\x00-\\x1F\\x7F]"), " ")
                .take(MAX_LOG_LENGTH)
                .filter { it.isLetterOrDigit() || it in "_-. :()[]{}/" }
                .ifBlank { "SANITIZED" }
        } catch (e: Exception) {
            Log.w("SecureLogger", "sanitize falhou: ${e::class.simpleName}")
            "SANITIZED"
        }
    }
}
