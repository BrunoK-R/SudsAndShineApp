package com.sudsmobile.feature.profile

import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val profileModule = module {
    viewModelOf(::ProfileViewModel)
    viewModelOf(::ProfileHistoryViewModel)
    viewModelOf(::AdminAccessViewModel)
    viewModelOf(::AdminBookingsViewModel)
    viewModelOf(::AdminBusinessInfoViewModel)
    viewModelOf(::AdminServiceCatalogViewModel)
    viewModelOf(::PersonalDataViewModel)
    viewModelOf(::VehiclesViewModel)
    viewModelOf(::ContactViewModel)
}
