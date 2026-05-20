package com.sudsmobile.feature.cart

import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val cartModule = module {
    viewModelOf(::CartBookingsViewModel)
    viewModelOf(::RatingViewModel)
}
