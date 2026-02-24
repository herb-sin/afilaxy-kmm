package com.afilaxy.di

import org.koin.core.Koin
import org.koin.core.context.startKoin

fun doInitKoin() {
    startKoin {
        modules(sharedModule(), platformModule())
    }
}

fun getKoin(): Koin = org.koin.core.context.GlobalContext.get()
