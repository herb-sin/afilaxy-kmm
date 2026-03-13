package com.afilaxy.util

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSString
import platform.darwin.NSObject

@OptIn(ExperimentalForeignApi::class)
@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
external class FileLoggerBridge : NSObject {
    companion object {
        fun logWithLevel(level: String, tag: String, message: String)
    }
}

object IOSFileLogger {
    fun log(level: String, tag: String, message: String) {
        try {
            FileLoggerBridge.logWithLevel(level, tag, message)
        } catch (e: Exception) {
            // Fallback silencioso se bridge falhar
        }
    }
}
