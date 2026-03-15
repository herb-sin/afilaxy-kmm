package com.afilaxy.util

import platform.Foundation.NSLog
import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.Platform

@OptIn(ExperimentalAtomicApi::class, ExperimentalNativeApi::class)
actual object Logger {
    private val hookRef = AtomicReference<((String, String, String) -> Unit)?>(null)
    var fileLogHook: ((String, String, String) -> Unit)?
        get() = hookRef.load()
        set(value) { hookRef.store(value) }

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
        hookRef.load()?.invoke("DEBUG", tag, s)
    }

    actual fun i(tag: String, message: String) {
        val s = sanitize(message)
        nslog("INFO", tag, s)
        hookRef.load()?.invoke("INFO", tag, s)
    }

    actual fun w(tag: String, message: String) {
        val s = sanitize(message)
        nslog("WARN", tag, s)
        hookRef.load()?.invoke("WARN", tag, s)
    }

    actual fun e(tag: String, message: String, throwable: Throwable?) {
        val s = sanitize(if (throwable != null) "$message\n${throwable.stackTraceToString()}" else message)
        nslog("ERROR", tag, s)
        hookRef.load()?.invoke("ERROR", tag, s)
    }

    actual fun sanitize(message: String): String {
        var s = message
        sensitivePatterns.forEach { s = s.replace(it, "[REDACTED]") }
        return s
    }
}
