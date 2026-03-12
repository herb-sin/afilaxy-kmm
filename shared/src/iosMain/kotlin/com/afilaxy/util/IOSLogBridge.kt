package com.afilaxy.util

import platform.Foundation.NSLog

actual object IOSLogBridge {
    actual fun log(level: String, tag: String, message: String) {
        // Fallback para NSLog caso FileLogger Swift não esteja disponível
        NSLog("[$level] [$tag] $message")
        
        // Chamar FileLogger Swift via interop
        try {
            FileLoggerBridge.shared.write(level, tag, message)
        } catch (e: Exception) {
            NSLog("Failed to write to file logger: ${e.message}")
        }
    }
}

/**
 * Interface para chamar FileLogger Swift
 * A implementação real está em FileLogger.swift
 */
@OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
private object FileLoggerBridge {
    val shared: FileLoggerProtocol
        get() = platform.darwin.NSObject() as FileLoggerProtocol
}

/**
 * Protocol que deve ser implementado no Swift
 */
interface FileLoggerProtocol {
    fun write(level: String, tag: String, message: String)
}
