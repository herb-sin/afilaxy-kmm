package com.afilaxy.di

import org.koin.core.Koin
import org.koin.core.context.startKoin
import org.koin.core.context.GlobalContext

fun doInitKoin() {
    startKoin {
        modules(sharedModule(), platformModule())
    }
}

fun getKoin(): Koin = GlobalContext.getOrNull() ?: error("Koin not initialized")
