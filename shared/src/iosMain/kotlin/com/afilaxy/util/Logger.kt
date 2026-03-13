package com.afilaxy.util

import platform.Foundation.NSLog

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
        NSLog("[DEBUG] [$tag] $sanitized")
        // FileLogger Swift será chamado via NSLog interceptor
    }
    
    actual fun i(tag: String, message: String) {
        val sanitized = sanitize(message)
        NSLog("[INFO] [$tag] $sanitized")
    }
    
    actual fun w(tag: String, message: String) {
        val sanitized = sanitize(message)
        NSLog("[WARN] [$tag] $sanitized")
    }
    
    actual fun e(tag: String, message: String, throwable: Throwable?) {
        val fullMessage = if (throwable != null) {
            "$message\n${throwable.stackTraceToString()}"
        } else {
            message
        }
        val sanitized = sanitize(fullMessage)
        NSLog("[ERROR] [$tag] $sanitized")
    }
    
    actual fun sanitize(message: String): String {
        var sanitized = message
        sensitivePatterns.forEach { pattern ->
            sanitized = sanitized.replace(pattern, "[REDACTED]")
        }
        return sanitized
    }
}
