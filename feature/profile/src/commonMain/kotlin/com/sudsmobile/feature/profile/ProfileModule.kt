package com.sudsmobile.feature.profile

import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val profileModule = module {
    viewModelOf(::ProfileViewModel)
    viewModelOf(::ProfileHistoryViewModel)
    viewModelOf(::PersonalDataViewModel)
    viewModelOf(::VehiclesViewModel)
}
