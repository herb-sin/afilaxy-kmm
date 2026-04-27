package com.afilaxy.app.performance

import android.util.Log

object LogOptimizer {

    private const val MAX_LOG_LENGTH = 4000
    private const val MAX_TAG_LENGTH = 23   // limite de caracteres de tag do logcat Android

    fun d(tag: String, message: String) {
        Log.d(sanitizeTag(tag), sanitize(message))
    }

    fun i(tag: String, message: String) {
        Log.i(sanitizeTag(tag), sanitize(message))
    }

    fun w(tag: String, message: String) {
        Log.w(sanitizeTag(tag), sanitize(message))
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        val t = sanitizeTag(tag)
        val m = sanitize(message)
        if (throwable != null) Log.e(t, m, throwable) else Log.e(t, m)
    }

    // Remove caracteres de controle que permitem log injection (CWE-117):
    // \n e \r forjam novas entradas de log; \t e outros distorcem a formatação.
    private fun sanitize(message: String): String =
        message
            .replace(Regex("[\\r\\n\\t]"), " ")
            .replace(Regex("[\\x00-\\x1F\\x7F]"), "")
            .let { if (it.length > MAX_LOG_LENGTH) it.substring(0, MAX_LOG_LENGTH) + "..." else it }

    // Tag tem limite de 23 chars no Android e não deve conter caracteres de controle.
    private fun sanitizeTag(tag: String): String =
        tag
            .replace(Regex("[\\r\\n\\t\\x00-\\x1F\\x7F]"), "")
            .take(MAX_TAG_LENGTH)
            .ifEmpty { "App" }
}
