package com.sudsmobile.feature.onboarding

import com.sudsmobile.data.preferences.OnboardingPreferenceStore
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingGateViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun startsAtMainWhenOnboardingWasAlreadyCompleted() = runTest {
        val viewModel = OnboardingGateViewModel(FakeOnboardingPreferenceStore(completed = true))

        runCurrent()

        assertEquals(OnboardingGateUiState.Main, viewModel.uiState.value)
    }

    @Test
    fun startsAtOnboardingWhenPreferenceIsMissing() = runTest {
        val viewModel = OnboardingGateViewModel(FakeOnboardingPreferenceStore(completed = false))

        runCurrent()

        assertEquals(OnboardingGateUiState.ShowOnboarding, viewModel.uiState.value)
    }

    @Test
    fun completeOnboardingPersistsAndOpensMainApp() = runTest {
        val store = FakeOnboardingPreferenceStore(completed = false)
        val viewModel = OnboardingGateViewModel(store)
        runCurrent()

        viewModel.completeOnboarding()

        assertEquals(true, store.completed)
        assertEquals(OnboardingGateUiState.Main, viewModel.uiState.value)
    }

    @Test
    fun readFailureFallsBackToOnboarding() = runTest {
        val viewModel = OnboardingGateViewModel(
            FakeOnboardingPreferenceStore(
                completed = true,
                failReads = true,
            ),
        )

        runCurrent()

        assertEquals(OnboardingGateUiState.ShowOnboarding, viewModel.uiState.value)
    }
}

private class FakeOnboardingPreferenceStore(
    var completed: Boolean,
    private val failReads: Boolean = false,
) : OnboardingPreferenceStore {
    override fun hasCompletedOnboarding(): Boolean {
        if (failReads) error("Preference read failed")
        return completed
    }

    override fun setOnboardingCompleted(completed: Boolean) {
        this.completed = completed
    }
}
