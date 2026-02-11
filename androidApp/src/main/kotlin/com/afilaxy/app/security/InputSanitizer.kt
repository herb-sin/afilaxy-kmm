package com.afilaxy.app.security

import java.util.regex.Pattern

object InputSanitizer {
    
    private val EMAIL_PATTERN = Pattern.compile("^[a-zA-Z0-9._%+-]{1,64}@[a-zA-Z0-9.-]{1,253}\\.[a-zA-Z]{2,6}$")
    private val NAME_PATTERN = Pattern.compile("^[a-zA-Zà-ÿÀ-Ÿ\\s]{1,50}$")
    private val SAFE_TEXT_PATTERN = Pattern.compile("^[a-zA-Z0-9\\s._-]{1,200}$")
    
    private val NOSQL_OPERATORS = setOf(
        "\$where", "\$ne", "\$gt", "\$lt", "\$regex", "\$or", "\$and", 
        "\$exists", "\$in", "\$nin", "javascript:", "eval(", "function("
    )
    
    private val DANGEROUS_CHARS = "\${}[]();'\"\\/*<>=&|!~`^%".toCharArray().toSet()
    
    fun sanitizeEmail(email: String?): String {
        if (email.isNullOrBlank()) return ""
        val cleaned = email.trim().lowercase()
        return if (EMAIL_PATTERN.matcher(cleaned).matches()) cleaned else ""
    }
    
    fun sanitizeName(name: String?): String {
        if (name.isNullOrBlank()) return ""
        val cleaned = name.trim()
        return if (NAME_PATTERN.matcher(cleaned).matches()) cleaned else ""
    }
    
    fun sanitizeText(text: String?): String {
        if (text.isNullOrBlank()) return ""
        var cleaned = text.trim().take(200)
        
        NOSQL_OPERATORS.forEach { pattern ->
            cleaned = cleaned.replace(pattern, "", ignoreCase = true)
        }
        
        DANGEROUS_CHARS.forEach { char ->
            cleaned = cleaned.replace(char.toString(), "")
        }
        
        return if (SAFE_TEXT_PATTERN.matcher(cleaned).matches()) cleaned else ""
    }
    
    fun isValidEmail(email: String?): Boolean {
        return !email.isNullOrBlank() && EMAIL_PATTERN.matcher(email.trim().lowercase()).matches()
    }
    
    fun isValidName(name: String?): Boolean {
        return !name.isNullOrBlank() && NAME_PATTERN.matcher(name.trim()).matches()
    }
}
