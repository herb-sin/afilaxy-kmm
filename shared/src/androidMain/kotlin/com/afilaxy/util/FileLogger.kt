package com.afilaxy.util

import android.content.Context
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

object FileLogger {
    private const val MAX_FILE_SIZE = 5 * 1024 * 1024L // 5MB
    private const val MAX_AGE_DAYS = 7L
    private const val LOG_DIR = "logs"
    
    private lateinit var logDir: File
    private val lock = ReentrantReadWriteLock()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)
    
    fun initialize(context: Context) {
        logDir = File(context.cacheDir, LOG_DIR)
        if (!logDir.exists()) {
            logDir.mkdirs()
        }
        cleanOldLogs()
    }
    
    fun log(level: String, tag: String, message: String) {
        lock.write {
            try {
                val logFile = getCurrentLogFile()
                val timestamp = dateFormat.format(Date())
                val logEntry = "$timestamp [$level] $tag: $message\n"
                
                logFile.appendText(logEntry)
                
                if (logFile.length() > MAX_FILE_SIZE) {
                    rotateLogFile()
                }
            } catch (e: Exception) {
                android.util.Log.e("FileLogger", "Failed to write log", e)
            }
        }
    }
    
    fun getAllLogs(): List<File> {
        return lock.read {
            if (!::logDir.isInitialized || !logDir.exists()) {
                emptyList()
            } else {
                logDir.listFiles()?.filter { it.extension == "log" }?.sortedByDescending { it.lastModified() } ?: emptyList()
            }
        }
    }
    
    fun getTotalSize(): Long {
        return lock.read {
            getAllLogs().sumOf { it.length() }
        }
    }
    
    fun clearLogs() {
        lock.write {
            getAllLogs().forEach { it.delete() }
        }
    }
    
    private fun getCurrentLogFile(): File {
        val fileName = "afilaxy_${SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())}.log"
        return File(logDir, fileName)
    }
    
    private fun rotateLogFile() {
        val currentFile = getCurrentLogFile()
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val rotatedFile = File(logDir, "afilaxy_$timestamp.log")
        currentFile.renameTo(rotatedFile)
    }
    
    private fun cleanOldLogs() {
        val cutoffTime = System.currentTimeMillis() - (MAX_AGE_DAYS * 24 * 60 * 60 * 1000)
        getAllLogs().filter { it.lastModified() < cutoffTime }.forEach { it.delete() }
    }
}
