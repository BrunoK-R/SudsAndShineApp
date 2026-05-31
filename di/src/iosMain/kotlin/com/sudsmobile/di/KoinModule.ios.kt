package com.sudsmobile.di

import com.sudsmobile.data.auth.AuthSessionStore
import com.sudsmobile.data.auth.UserDefaultsAuthSessionStore
import com.sudsmobile.data.notification.NotificationDeviceRegistrar
import com.sudsmobile.data.notification.UnsupportedNotificationDeviceRegistrar
import com.sudsmobile.data.preferences.OnboardingPreferenceStore
import com.sudsmobile.data.preferences.UserDefaultsOnboardingPreferenceStore
import org.koin.dsl.module

actual val platformModule = module {
    single { "ios" }
    single<AuthSessionStore> { UserDefaultsAuthSessionStore() }
    single<OnboardingPreferenceStore> { UserDefaultsOnboardingPreferenceStore() }
    single<NotificationDeviceRegistrar> { UnsupportedNotificationDeviceRegistrar() }
}
