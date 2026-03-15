package com.afilaxy.util

import platform.Foundation.NSLog
import kotlin.native.Platform
import kotlin.native.concurrent.AtomicReference

actual object Logger {
    // AtomicReference de kotlin.native.concurrent garante thread-safety sem dependência extra
    private val hookRef = AtomicReference<((String, String, String) -> Unit)?>(null)
    var fileLogHook: ((String, String, String) -> Unit)?
        get() = hookRef.value
        set(value) { hookRef.value = value }

    private val sensitivePatterns = listOf(
        Regex("password[\"']?\\s*[:=]\\s*[\"']?([^\"',\\s}]+)", RegexOption.IGNORE_CASE),
        Regex("token[\"']?\\s*[:=]\\s*[\"']?([^\"',\\s}]+)", RegexOption.IGNORE_CASE),
        Regex("\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}\\b"),
        Regex("\\b\\d{3}\\.\\d{3}\\.\\d{3}-\\d{2}\\b"),
        Regex("Bearer\\s+[A-Za-z0-9\\-._~+/]+=*", RegexOption.IGNORE_CASE)
    )

    private fun nslog(level: String, tag: String, message: String) {
        if (Platform.isDebugBinary) NSLog("[$level] [$tag] $message")
    }

    actual fun d(tag: String, message: String) {
        val s = sanitize(message)
        nslog("DEBUG", tag, s)
        hookRef.value?.invoke("DEBUG", tag, s)
    }

    actual fun i(tag: String, message: String) {
        val s = sanitize(message)
        nslog("INFO", tag, s)
        hookRef.value?.invoke("INFO", tag, s)
    }

    actual fun w(tag: String, message: String) {
        val s = sanitize(message)
        nslog("WARN", tag, s)
        hookRef.value?.invoke("WARN", tag, s)
    }

    actual fun e(tag: String, message: String, throwable: Throwable?) {
        val s = sanitize(if (throwable != null) "$message\n${throwable.stackTraceToString()}" else message)
        nslog("ERROR", tag, s)
        hookRef.value?.invoke("ERROR", tag, s)
    }

    actual fun sanitize(message: String): String {
        var s = message
        sensitivePatterns.forEach { s = s.replace(it, "[REDACTED]") }
        return s
    }
}
