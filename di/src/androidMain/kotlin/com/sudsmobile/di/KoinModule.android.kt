package com.sudsmobile.di

import org.koin.dsl.module

actual val platformModule = module {
    single { "android" }
}
