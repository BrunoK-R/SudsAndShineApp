package com.sudsmobile.di

import android.content.Context
import com.sudsmobile.data.auth.AuthSessionStore
import com.sudsmobile.data.auth.SharedPreferencesAuthSessionStore
import com.sudsmobile.data.preferences.OnboardingPreferenceStore
import com.sudsmobile.data.preferences.SharedPreferencesOnboardingPreferenceStore
import org.koin.dsl.module

private var applicationContext: Context? = null

fun configureAndroidPlatform(context: Context) {
    applicationContext = context.applicationContext
}

actual val platformModule = module {
    single { "android" }
    single<AuthSessionStore> {
        SharedPreferencesAuthSessionStore(
            requireNotNull(applicationContext) {
                "Call configureAndroidPlatform(context) before initializeKoin()."
            },
        )
    }
    single<OnboardingPreferenceStore> {
        SharedPreferencesOnboardingPreferenceStore(
            requireNotNull(applicationContext) {
                "Call configureAndroidPlatform(context) before initializeKoin()."
            },
        )
    }
}
