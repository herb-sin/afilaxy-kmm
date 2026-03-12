package com.afilaxy.util

/**
 * Logger multiplataforma para debug e análise de problemas
 * 
 * Android: Usa Logcat
 * iOS: Salva em arquivo local + OSLog
 */
expect object Logger {
    fun d(tag: String, message: String)
    fun i(tag: String, message: String)
    fun w(tag: String, message: String)
    fun e(tag: String, message: String, throwable: Throwable? = null)
    
    /**
     * Sanitiza dados sensíveis antes de logar
     */
    fun sanitize(message: String): String
}

/**
 * Extension functions para facilitar uso
 */
inline fun <reified T> T.logDebug(message: String) {
    Logger.d(T::class.simpleName ?: "Unknown", message)
}

inline fun <reified T> T.logInfo(message: String) {
    Logger.i(T::class.simpleName ?: "Unknown", message)
}

inline fun <reified T> T.logWarning(message: String) {
    Logger.w(T::class.simpleName ?: "Unknown", message)
}

inline fun <reified T> T.logError(message: String, throwable: Throwable? = null) {
    Logger.e(T::class.simpleName ?: "Unknown", message, throwable)
}
