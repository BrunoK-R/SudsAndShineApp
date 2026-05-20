package com.sudsmobile.data.preferences

import platform.Foundation.NSUserDefaults

class UserDefaultsOnboardingPreferenceStore(
    private val defaults: NSUserDefaults = NSUserDefaults.standardUserDefaults,
) : OnboardingPreferenceStore {
    override fun hasCompletedOnboarding(): Boolean {
        return defaults.boolForKey(KeyOnboardingCompleted)
    }

    override fun setOnboardingCompleted(completed: Boolean) {
        defaults.setBool(completed, KeyOnboardingCompleted)
    }

    private companion object {
        const val KeyOnboardingCompleted = "suds_onboarding_completed"
    }
}
