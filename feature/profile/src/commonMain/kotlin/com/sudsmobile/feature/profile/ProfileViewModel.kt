package com.sudsmobile.feature.profile

import androidx.lifecycle.ViewModel
import com.sudsmobile.data.auth.AuthRepository
import com.sudsmobile.data.auth.AuthSessionState
import kotlinx.coroutines.flow.StateFlow

class ProfileViewModel(
    private val authRepository: AuthRepository,
) : ViewModel() {
    val sessionState: StateFlow<AuthSessionState> = authRepository.sessionState

    fun signOut() {
        authRepository.signOut()
    }
}
