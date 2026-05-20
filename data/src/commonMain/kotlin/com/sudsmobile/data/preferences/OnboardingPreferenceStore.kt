package com.sudsmobile.data.preferences

interface OnboardingPreferenceStore {
    fun hasCompletedOnboarding(): Boolean
    fun setOnboardingCompleted(completed: Boolean)
}

object NoopOnboardingPreferenceStore : OnboardingPreferenceStore {
    override fun hasCompletedOnboarding(): Boolean = false
    override fun setOnboardingCompleted(completed: Boolean) = Unit
}
