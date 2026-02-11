package com.afilaxy.app.security

object RateLimitManager {
    // Login: 5 tentativas por minuto
    val loginLimiter = RateLimiter(
        maxAttempts = 5,
        windowMillis = 60_000L
    )
    
    // Emergências: 3 por hora
    val emergencyLimiter = RateLimiter(
        maxAttempts = 3,
        windowMillis = 3_600_000L
    )
    
    // Chat: 30 mensagens por minuto
    val chatLimiter = RateLimiter(
        maxAttempts = 30,
        windowMillis = 60_000L
    )
    
    // Registro: 3 tentativas por hora
    val registerLimiter = RateLimiter(
        maxAttempts = 3,
        windowMillis = 3_600_000L
    )
}
