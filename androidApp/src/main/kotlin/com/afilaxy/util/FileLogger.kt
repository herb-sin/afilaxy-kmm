package com.afilaxy.util

import android.content.Context
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object FileLogger {

    private var logDir: File? = null
    private var sessionFile: File? = null

    private val tsFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
    private val fileFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())

    fun initialize(context: Context) {
        val dir = File(context.cacheDir, "logs").also { it.mkdirs() }
        logDir = dir
        sessionFile = File(dir, "afilaxy_${fileFormat.format(Date())}.log").also {
            it.appendText("=== Sessão iniciada ${tsFormat.format(Date())} ===\n")
        }
    }

    fun log(level: String, tag: String, message: String) {
        val file = sessionFile ?: return
        try {
            file.appendText("${tsFormat.format(Date())} $level/$tag: $message\n")
        } catch (_: Exception) {}
    }

    // Chamado pelo botão de exportação — despeja logcat recente no arquivo da sessão
    // antes de retornar a lista, para capturar tudo inclusive erros do mapa/Firebase.
    fun getAllLogs(): List<File> {
        sessionFile?.let { dumpLogcat(it) }
        val dir = logDir ?: return emptyList()
        return dir.listFiles()
            ?.filter { it.extension == "log" }
            ?.sortedByDescending { it.lastModified() }
            ?: emptyList()
    }

    fun clearLogs() {
        logDir?.listFiles()?.forEach { it.delete() }
    }

    private fun dumpLogcat(file: File) {
        try {
            // -t 800: últimas 800 linhas; -v time: timestamp por linha
            val proc = Runtime.getRuntime().exec(arrayOf("logcat", "-d", "-t", "800", "-v", "time", "*:V"))
            val output = proc.inputStream.bufferedReader().readText()
            proc.destroy()
            file.appendText("\n=== LOGCAT ${tsFormat.format(Date())} ===\n$output\n")
        } catch (_: Exception) {}
    }
}
