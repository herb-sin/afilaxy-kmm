package com.afilaxy.app.security

import java.util.regex.Pattern

object InputSanitizer {

    // Regex de email com ponto escapado no domínio e sem backtracking catastrófico
    private val EMAIL_PATTERN = Pattern.compile(
        "^[a-zA-Z0-9._%+\\-]{1,64}@[a-zA-Z0-9\\-]{1,63}(?:\\.[a-zA-Z0-9\\-]{1,63})*\\.[a-zA-Z]{2,6}$"
    )
    private val NAME_PATTERN = Pattern.compile("^[a-zA-Z\u00e0-\u00ff\u00c0-\u0178\\s]{1,50}$")
    private val SAFE_TEXT_PATTERN = Pattern.compile("^[a-zA-Z0-9\\s._\\-]{1,200}$")

    // Detecta operadores NoSQL — usado para rejeitar o input, não para sanitizar por remoção
    private val NOSQL_PATTERN = Pattern.compile(
        """\$(?:where|ne|gt|gte|lt|lte|regex|or|and|exists|in|nin|not|nor|all|size|type|mod|text|search)|javascript:|eval\s*\(|function\s*\(""",
        Pattern.CASE_INSENSITIVE
    )

    // Allowlist: apenas caracteres seguros para texto livre
    private val UNSAFE_CHARS = Regex("[^a-zA-Z0-9\u00e0-\u00ff\u00c0-\u0178\\s._\\-]")

    fun sanitizeEmail(email: String?): String {
        if (email.isNullOrBlank()) return ""
        val cleaned = email.trim().lowercase()
        return if (EMAIL_PATTERN.matcher(cleaned).matches()) cleaned else ""
    }

    fun sanitizeName(name: String?): String {
        if (name.isNullOrBlank()) return ""
        val trimmed = name.trim()
        
        // Check length before processing
        if (trimmed.length > 50) return ""
        
        if (NOSQL_PATTERN.matcher(trimmed).find()) return ""
        val cleaned = trimmed.replace(UNSAFE_CHARS, "")
        return if (NAME_PATTERN.matcher(cleaned).matches()) cleaned else ""
    }

    fun sanitizeText(text: String?): String {
        if (text.isNullOrBlank()) return ""
        val trimmed = text.trim().take(200)

        // Rejeita qualquer input que contenha operadores NoSQL — sem tentar "limpar"
        if (NOSQL_PATTERN.matcher(trimmed).find()) return ""

        // Remove tudo que não está na allowlist de caracteres seguros
        val cleaned = trimmed.replace(UNSAFE_CHARS, "")

        return if (SAFE_TEXT_PATTERN.matcher(cleaned).matches()) cleaned else ""
    }

    fun isValidEmail(email: String?): Boolean =
        !email.isNullOrBlank() && EMAIL_PATTERN.matcher(email.trim().lowercase()).matches()

    fun isValidName(name: String?): Boolean =
        !name.isNullOrBlank() && NAME_PATTERN.matcher(name.trim()).matches()
}
