package com.afilaxy.util

import android.content.Context
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

object FileLogger {
    private const val MAX_FILE_SIZE_DEBUG   = 5 * 1024 * 1024L // 5 MB em debug
    private const val MAX_FILE_SIZE_RELEASE = 3 * 1024 * 1024L // 3 MB em release
    private const val MAX_AGE_DAYS_DEBUG    = 7L
    private const val MAX_AGE_DAYS_RELEASE  = 3L
    private const val MILLIS_PER_DAY = 24 * 60 * 60 * 1_000L
    private const val LOG_DIR = "logs"

    /** Sempre true — habilitado em debug e release para diagnóstico em campo.
     *  Limites de tamanho e retenção são mais conservadores em release. */
    private var isEnabled: Boolean = false
    private var isDebug: Boolean = false

    private lateinit var logDir: File
    private val lock = ReentrantReadWriteLock()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)

    fun initialize(context: Context) {
        isDebug = (context.applicationInfo.flags and
            android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0
        isEnabled = true  // Habilitado em debug e release para diagnóstico em campo
        logDir = File(context.cacheDir, LOG_DIR)
        if (!logDir.exists()) {
            logDir.mkdirs()
        }
        cleanOldLogs()
    }
    
    fun log(level: String, tag: String, message: String) {
        if (!isEnabled) return  // no-op em release
        lock.write {
            try {
                val logFile = getCurrentLogFile()
                val timestamp = dateFormat.format(Date())
                val logEntry = "$timestamp [$level] $tag: $message\n"

                logFile.appendText(logEntry)

                val maxSize = if (isDebug) MAX_FILE_SIZE_DEBUG else MAX_FILE_SIZE_RELEASE
                if (logFile.length() > maxSize) {
                    rotateLogFile()
                }
            } catch (e: Exception) {
                android.util.Log.e("FileLogger", "Failed to write log", e)
            }
        }
    }
    
    fun getAllLogs(): List<File> {
        if (!isEnabled) return emptyList()
        return lock.read {
            if (!::logDir.isInitialized || !logDir.exists()) {
                emptyList()
            } else {
                logDir.listFiles()?.filter { it.extension == "log" }?.sortedByDescending { it.lastModified() } ?: emptyList()
            }
        }
    }

    fun getTotalSize(): Long {
        if (!isEnabled) return 0L
        return lock.read {
            getAllLogs().sumOf { it.length() }
        }
    }

    fun clearLogs() {
        if (!isEnabled) return
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
        val maxAge = if (isDebug) MAX_AGE_DAYS_DEBUG else MAX_AGE_DAYS_RELEASE
        val cutoffTime = System.currentTimeMillis() - (maxAge * MILLIS_PER_DAY)
        getAllLogs().filter { it.lastModified() < cutoffTime }.forEach { it.delete() }
    }
}
