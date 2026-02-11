package com.afilaxy.app.performance

import android.util.Log

object LogOptimizer {
    
    private const val MAX_LOG_LENGTH = 4000
    
    fun d(tag: String, message: String) {
        Log.d(tag, truncate(message))
    }
    
    fun i(tag: String, message: String) {
        Log.i(tag, truncate(message))
    }
    
    fun w(tag: String, message: String) {
        Log.w(tag, truncate(message))
    }
    
    fun e(tag: String, message: String, throwable: Throwable? = null) {
        if (throwable != null) {
            Log.e(tag, truncate(message), throwable)
        } else {
            Log.e(tag, truncate(message))
        }
    }
    
    private fun truncate(message: String): String {
        return if (message.length > MAX_LOG_LENGTH) {
            message.substring(0, MAX_LOG_LENGTH) + "..."
        } else {
            message
        }
    }
}
