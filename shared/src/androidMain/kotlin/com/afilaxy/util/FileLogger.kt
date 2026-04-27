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
    private const val MILLIS_PER_DAY = 24 * 60 * 60 * 1_000L
    private const val LOG_DIR = "logs"

    /** false em release builds — elimina todo IO de disco em produção */
    private var isEnabled: Boolean = false

    private lateinit var logDir: File
    private val lock = ReentrantReadWriteLock()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)

    fun initialize(context: Context) {
        isEnabled = (context.applicationInfo.flags and
            android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0
        if (!isEnabled) return  // Release: não cria diretório de logs em disco
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

                if (logFile.length() > MAX_FILE_SIZE) {
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
        val cutoffTime = System.currentTimeMillis() - (MAX_AGE_DAYS * MILLIS_PER_DAY)
        getAllLogs().filter { it.lastModified() < cutoffTime }.forEach { it.delete() }
    }
}
