package com.sudsmobile.di

import com.sudsmobile.data.auth.AuthSessionStore
import com.sudsmobile.data.auth.UserDefaultsAuthSessionStore
import org.koin.dsl.module

actual val platformModule = module {
    single { "ios" }
    single<AuthSessionStore> { UserDefaultsAuthSessionStore() }
}
