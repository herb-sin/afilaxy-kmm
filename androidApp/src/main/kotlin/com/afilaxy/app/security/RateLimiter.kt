package com.afilaxy.app.security

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class RateLimiter(
    private val maxAttempts: Int,
    private val windowMillis: Long
) {
    private val mutex = Mutex()
    private val attempts = mutableMapOf<String, MutableList<Long>>()

    suspend fun checkLimit(key: String): RateLimitResult {
        return mutex.withLock {
            val now = System.currentTimeMillis()
            val userAttempts = attempts.getOrPut(key) { mutableListOf() }
            
            // Remove tentativas antigas
            userAttempts.removeAll { it < now - windowMillis }
            
            if (userAttempts.size >= maxAttempts) {
                val oldestAttempt = userAttempts.first()
                val waitTime = (oldestAttempt + windowMillis) - now
                return@withLock RateLimitResult.Limited(waitTime)
            }
            
            userAttempts.add(now)
            RateLimitResult.Allowed
        }
    }

    suspend fun reset(key: String) {
        mutex.withLock {
            attempts.remove(key)
        }
    }

    internal suspend fun resetAll() {
        mutex.withLock { attempts.clear() }
    }
}

sealed class RateLimitResult {
    object Allowed : RateLimitResult()
    data class Limited(val waitTimeMillis: Long) : RateLimitResult()
}
