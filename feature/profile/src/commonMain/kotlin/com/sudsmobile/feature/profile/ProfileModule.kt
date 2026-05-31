package com.sudsmobile.feature.profile

import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val profileModule = module {
    viewModelOf(::ProfileViewModel)
    viewModelOf(::ProfileHistoryViewModel)
    viewModelOf(::AdminAccessViewModel)
    viewModelOf(::AdminBookingsViewModel)
    viewModelOf(::AdminAvailabilityViewModel)
    viewModelOf(::AdminBookingPolicyViewModel)
    viewModelOf(::AdminLoyaltySettingsViewModel)
    viewModelOf(::AdminNotificationSettingsViewModel)
    viewModelOf(::AdminBusinessInfoViewModel)
    viewModelOf(::AdminServiceCatalogViewModel)
    viewModelOf(::AdminServiceExtrasViewModel)
    viewModelOf(::NotificationPreferencesViewModel)
    viewModelOf(::PersonalDataViewModel)
    viewModelOf(::VehiclesViewModel)
    viewModelOf(::ContactViewModel)
}
