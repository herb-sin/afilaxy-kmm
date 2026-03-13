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
        val sanitized = sanitize(message)
        Log.d(tag, sanitized)
        FileLogger.log("DEBUG", tag, sanitized)
    }
    
    actual fun i(tag: String, message: String) {
        val sanitized = sanitize(message)
        Log.i(tag, sanitized)
        FileLogger.log("INFO", tag, sanitized)
    }
    
    actual fun w(tag: String, message: String) {
        val sanitized = sanitize(message)
        Log.w(tag, sanitized)
        FileLogger.log("WARN", tag, sanitized)
    }
    
    actual fun e(tag: String, message: String, throwable: Throwable?) {
        val sanitized = sanitize(message)
        if (throwable != null) {
            Log.e(tag, sanitized, throwable)
            FileLogger.log("ERROR", tag, "$sanitized\n${throwable.stackTraceToString()}")
        } else {
            Log.e(tag, sanitized)
            FileLogger.log("ERROR", tag, sanitized)
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
