package com.sudsmobile.feature.blog

import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val blogModule = module {
    viewModelOf(::LoyaltyViewModel)
}
