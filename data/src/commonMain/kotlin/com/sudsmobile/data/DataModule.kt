package com.sudsmobile.data

import org.koin.dsl.module

val dataModule = module {
    single<AppRepository> { AppRepositoryImpl() }
}
