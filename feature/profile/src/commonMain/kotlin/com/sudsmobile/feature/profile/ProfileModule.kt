package com.sudsmobile.feature.profile

import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val profileModule = module {
    viewModel {
        ProfileViewModel(
            authRepository = get(),
            bookingRepository = get(),
            userVehicleRepository = get(),
            userProfileRepository = get(),
            bookingChangeNotifier = get(),
            userVehicleChangeNotifier = get(),
            userProfileChangeNotifier = get(),
            notificationRepository = get(),
            notificationDeviceRegistrar = get(),
            userProfilePhotoRepository = get(),
        )
    }
    viewModelOf(::ProfileHistoryViewModel)
    viewModelOf(::AdminAccessViewModel)
    viewModelOf(::AdminBookingsViewModel)
    viewModelOf(::AdminAvailabilityViewModel)
    viewModelOf(::AdminBookingPolicyViewModel)
    viewModelOf(::AdminLoyaltySettingsViewModel)
    viewModelOf(::AdminNotificationSettingsViewModel)
    viewModelOf(::AdminNotificationCampaignDraftsViewModel)
    viewModelOf(::AdminBusinessInfoViewModel)
    viewModelOf(::AdminServiceCatalogViewModel)
    viewModelOf(::AdminServiceExtrasViewModel)
    viewModelOf(::NotificationPreferencesViewModel)
    viewModelOf(::PersonalDataViewModel)
    viewModelOf(::VehiclesViewModel)
    viewModelOf(::ContactViewModel)
}
