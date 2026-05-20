package com.sudsmobile.data.preferences

import android.content.Context

class SharedPreferencesOnboardingPreferenceStore(
    context: Context,
) : OnboardingPreferenceStore {
    private val preferences = context.applicationContext.getSharedPreferences(
        "suds_app_preferences",
        Context.MODE_PRIVATE,
    )

    override fun hasCompletedOnboarding(): Boolean {
        return preferences.getBoolean(KeyOnboardingCompleted, false)
    }

    override fun setOnboardingCompleted(completed: Boolean) {
        preferences.edit()
            .putBoolean(KeyOnboardingCompleted, completed)
            .apply()
    }

    private companion object {
        const val KeyOnboardingCompleted = "onboarding_completed"
    }
}
