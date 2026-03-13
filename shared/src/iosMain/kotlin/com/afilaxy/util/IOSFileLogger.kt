package com.afilaxy.util

import platform.Foundation.NSLog

object IOSFileLogger {
    fun log(level: String, tag: String, message: String) {
        try {
            // Chama FileLogger Swift via NSLog formatado
            // FileLogger.swift intercepta logs via OSLog
            NSLog("[AFILAXY_LOG] [$level] [$tag] $message")
        } catch (e: Exception) {
            // Fallback silencioso
        }
    }
}
