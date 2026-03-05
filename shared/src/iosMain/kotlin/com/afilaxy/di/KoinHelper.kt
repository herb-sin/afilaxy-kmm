package com.afilaxy.di

import org.koin.core.Koin
import org.koin.core.context.startKoin

private var koinInstance: Koin? = null

fun doInitKoin() {
    try {
        koinInstance = startKoin {
            modules(sharedModule(), platformModule())
        }.koin
        println("✅ Koin initialized successfully")
    } catch (e: Exception) {
        println("❌ Koin initialization failed: ${e.message}")
        e.printStackTrace()
        throw e
    }
}

fun getKoin(): Koin = koinInstance ?: error("Koin not initialized. Call doInitKoin() first.")
