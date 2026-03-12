package com.afilaxy.util

import android.util.Log

actual object Logger {
    private val sensitivePatterns = listOf(
        Regex("password[\"']?\\s*[:=]\\s*[\"']?([^\"',\\s}]+)", RegexOption.IGNORE_CASE),
        Regex("token[\"']?\\s*[:=]\\s*[\"']?([^\"',\\s}]+)", RegexOption.IGNORE_CASE),
        Regex("\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}\\b"),
        Regex("\\b\\d{3}\\.\\d{3}\\.\\d{3}-\\d{2}\\b"), // CPF
        Regex("Bearer\\s+[A-Za-z0-9\\-._~+/]+=*", RegexOption.IGNORE_CASE)
    )
    
    actual fun d(tag: String, message: String) {
        Log.d(tag, sanitize(message))
    }
    
    actual fun i(tag: String, message: String) {
        Log.i(tag, sanitize(message))
    }
    
    actual fun w(tag: String, message: String) {
        Log.w(tag, sanitize(message))
    }
    
    actual fun e(tag: String, message: String, throwable: Throwable?) {
        if (throwable != null) {
            Log.e(tag, sanitize(message), throwable)
        } else {
            Log.e(tag, sanitize(message))
        }
    }
    
    actual fun sanitize(message: String): String {
        var sanitized = message
        sensitivePatterns.forEach { pattern ->
            sanitized = sanitized.replace(pattern, "[REDACTED]")
        }
        return sanitized
    }
}
