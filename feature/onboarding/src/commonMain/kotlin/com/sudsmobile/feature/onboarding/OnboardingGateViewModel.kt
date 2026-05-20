package com.sudsmobile.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sudsmobile.data.preferences.OnboardingPreferenceStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface OnboardingGateUiState {
    data object Loading : OnboardingGateUiState
    data object ShowOnboarding : OnboardingGateUiState
    data object Main : OnboardingGateUiState
}

class OnboardingGateViewModel(
    private val preferenceStore: OnboardingPreferenceStore,
) : ViewModel() {
    private val _uiState = MutableStateFlow<OnboardingGateUiState>(OnboardingGateUiState.Loading)
    val uiState: StateFlow<OnboardingGateUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val completed = runCatching {
                preferenceStore.hasCompletedOnboarding()
            }.getOrDefault(false)

            _uiState.value = if (completed) {
                OnboardingGateUiState.Main
            } else {
                OnboardingGateUiState.ShowOnboarding
            }
        }
    }

    suspend fun completeOnboarding() {
        runCatching {
            preferenceStore.setOnboardingCompleted(true)
        }
        _uiState.value = OnboardingGateUiState.Main
    }

    suspend fun resetOnboardingPreference() {
        runCatching {
            preferenceStore.setOnboardingCompleted(false)
        }
        _uiState.value = OnboardingGateUiState.ShowOnboarding
    }
}
