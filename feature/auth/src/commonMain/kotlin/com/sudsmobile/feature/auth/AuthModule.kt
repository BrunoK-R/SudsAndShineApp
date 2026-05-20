package com.sudsmobile.feature.auth

import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val authFeatureModule = module {
    viewModelOf(::AuthViewModel)
}
