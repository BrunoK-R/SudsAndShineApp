package com.sudsmobile.feature.products

import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val productsModule = module {
    viewModelOf(::ProductsBookingViewModel)
    viewModelOf(::ProductsCatalogViewModel)
}
