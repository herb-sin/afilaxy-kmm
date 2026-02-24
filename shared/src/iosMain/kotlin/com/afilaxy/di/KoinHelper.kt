package com.afilaxy.di

import org.koin.core.Koin
import org.koin.core.context.startKoin

private var koinInstance: Koin? = null

fun doInitKoin() {
    koinInstance = startKoin {
        modules(sharedModule(), platformModule())
    }.koin
}

fun getKoin(): Koin = koinInstance ?: error("Koin not initialized. Call doInitKoin() first.")
