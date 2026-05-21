package com.sudsmobile.feature.payment.di

import com.sudsmobile.feature.payment.PaymentViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val paymentModule = module {
    viewModelOf(::PaymentViewModel)
}
val paymentPlatformModule = module {}
