package com.sudsmobile.feature.blog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sudsmobile.data.auth.AuthError
import com.sudsmobile.data.auth.AuthRepository
import com.sudsmobile.data.auth.AuthSessionState
import com.sudsmobile.data.entitlement.ServiceEntitlement
import com.sudsmobile.data.entitlement.ServiceEntitlementError
import com.sudsmobile.data.entitlement.ServiceEntitlementListResult
import com.sudsmobile.data.entitlement.ServiceEntitlementRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal data class ServiceEntitlementUi(
    val id: String,
    val code: String,
    val kindLabel: String,
    val name: String,
    val status: String,
    val statusLabel: String,
    val totalUses: Int,
    val usedUses: Int,
    val remainingUses: Int,
    val eligibleServicesLabel: String,
    val validUntilLabel: String,
)

internal sealed interface ServiceEntitlementsUiState {
    data object Idle : ServiceEntitlementsUiState
    data object Loading : ServiceEntitlementsUiState
    data object Unauthenticated : ServiceEntitlementsUiState
    data class Loaded(val entitlements: List<ServiceEntitlementUi>) : ServiceEntitlementsUiState
    data class Error(val message: String, val retryable: Boolean) : ServiceEntitlementsUiState
}

internal class ServiceEntitlementsViewModel(
    private val authRepository: AuthRepository,
    private val repository: ServiceEntitlementRepository,
) : ViewModel() {
    val sessionState: StateFlow<AuthSessionState> = authRepository.sessionState

    private val _uiState = MutableStateFlow<ServiceEntitlementsUiState>(ServiceEntitlementsUiState.Idle)
    val uiState: StateFlow<ServiceEntitlementsUiState> = _uiState.asStateFlow()

    private var loadedUid: String? = null
    private var loadingUid: String? = null
    private var requestSequence = 0L

    fun refreshForSession(force: Boolean = false) {
        when (val session = sessionState.value) {
            AuthSessionState.Restoring -> {
                clearSession()
                _uiState.value = ServiceEntitlementsUiState.Loading
            }
            is AuthSessionState.RestoreFailed -> {
                clearSession()
                _uiState.value = session.error.toEntitlementsUiState()
            }
            AuthSessionState.Unauthenticated -> {
                clearSession()
                _uiState.value = ServiceEntitlementsUiState.Unauthenticated
            }
            is AuthSessionState.Authenticated -> {
                val uid = session.session.user.uid
                if (!force && loadedUid == uid && _uiState.value is ServiceEntitlementsUiState.Loaded) return
                load()
            }
        }
    }

    fun load() {
        val session = sessionState.value as? AuthSessionState.Authenticated ?: run {
            clearSession()
            _uiState.value = ServiceEntitlementsUiState.Unauthenticated
            return
        }
        val uid = session.session.user.uid
        if (loadingUid == uid) return
        val sequence = ++requestSequence
        loadingUid = uid
        _uiState.value = ServiceEntitlementsUiState.Loading
        viewModelScope.launch {
            try {
                val state = when (val result = repository.getMyEntitlements()) {
                    is ServiceEntitlementListResult.Success -> ServiceEntitlementsUiState.Loaded(
                        result.value.entitlements.map(ServiceEntitlement::toUi),
                    )
                    is ServiceEntitlementListResult.Failure -> result.error.toEntitlementsUiState()
                }
                if (sequence != requestSequence || !sessionStillMatches(uid)) return@launch
                loadedUid = uid
                _uiState.value = state
            } finally {
                if (sequence == requestSequence) loadingUid = null
            }
        }
    }

    private fun sessionStillMatches(uid: String): Boolean {
        val current = sessionState.value
        if (current is AuthSessionState.Authenticated && current.session.user.uid == uid) return true
        refreshForSession(force = true)
        return false
    }

    private fun clearSession() {
        loadedUid = null
        loadingUid = null
        requestSequence += 1
    }
}

private fun ServiceEntitlement.toUi(): ServiceEntitlementUi = ServiceEntitlementUi(
    id = id,
    code = code,
    kindLabel = if (kind == "membership") "Plano" else "Pacote",
    name = name,
    status = status,
    statusLabel = when (status) {
        "active" -> "Ativo"
        "scheduled" -> "Agendado"
        "exhausted" -> "Esgotado"
        "expired" -> "Expirado"
        "revoked" -> "Revogado"
        else -> "Indisponível"
    },
    totalUses = totalUses,
    usedUses = usedUses,
    remainingUses = remainingUses,
    eligibleServicesLabel = eligibleServiceNames.ifEmpty { eligibleServiceIds }.joinToString(", "),
    validUntilLabel = validUntilIso.take(10),
)

private fun ServiceEntitlementError.toEntitlementsUiState(): ServiceEntitlementsUiState.Error =
    ServiceEntitlementsUiState.Error(message, this is ServiceEntitlementError.Unavailable || this is ServiceEntitlementError.Backend)

private fun AuthError.toEntitlementsUiState(): ServiceEntitlementsUiState.Error =
    ServiceEntitlementsUiState.Error(message, this is AuthError.Unavailable || this is AuthError.Backend)
