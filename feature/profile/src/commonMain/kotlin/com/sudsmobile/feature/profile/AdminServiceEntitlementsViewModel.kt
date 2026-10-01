package com.sudsmobile.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sudsmobile.data.auth.AuthRepository
import com.sudsmobile.data.auth.AuthSessionState
import com.sudsmobile.data.catalog.ServiceCatalogRepository
import com.sudsmobile.data.catalog.ServiceCatalogResult
import com.sudsmobile.data.entitlement.AdminBookingLookup
import com.sudsmobile.data.entitlement.AdminOperationLookupResult
import com.sudsmobile.data.entitlement.AdminPackageTemplate
import com.sudsmobile.data.entitlement.AdminPackageTemplateResult
import kotlinx.coroutines.CancellationException
import com.sudsmobile.data.entitlement.AdminEntitlementCustomer
import com.sudsmobile.data.entitlement.AdminServiceEntitlementListResult
import com.sudsmobile.data.entitlement.AdjustServiceEntitlementUsageRequest
import com.sudsmobile.data.entitlement.IssueServiceEntitlementRequest
import com.sudsmobile.data.entitlement.RevokeServiceEntitlementRequest
import com.sudsmobile.data.entitlement.ServiceEntitlement
import com.sudsmobile.data.entitlement.ServiceEntitlementError
import com.sudsmobile.data.entitlement.ServiceEntitlementMutationResult
import com.sudsmobile.data.entitlement.ServiceEntitlementRepository
import kotlin.random.Random
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal data class AdminEntitlementServiceOption(val id: String, val name: String)

internal data class AdminServiceEntitlementForm(
    val customerEmail: String = "",
    val selectedTemplateId: String = "",
    val kind: String = "package",
    val name: String = "Pacote 5 lavagens",
    val totalUses: String = "5",
    val validDays: String = "180",
    val amountPaidEuros: String = "",
    val selectedServiceIds: Set<String> = emptySet(),
    val issueNote: String = "Venda registada ao balcão",
    val usageReservationCode: String = "",
    val usageNote: String = "Utilização confirmada ao balcão",
)

internal sealed interface AdminServiceEntitlementsUiState {
    data object Idle : AdminServiceEntitlementsUiState
    data object Loading : AdminServiceEntitlementsUiState
    data object Unauthenticated : AdminServiceEntitlementsUiState
    data class Loaded(
        val form: AdminServiceEntitlementForm,
        val services: List<AdminEntitlementServiceOption>,
        val customer: AdminEntitlementCustomer? = null,
        val entitlements: List<ServiceEntitlement> = emptyList(),
        val pendingRevocationId: String? = null,
        val templates: List<AdminPackageTemplate> = emptyList(),
        val bookings: List<AdminBookingLookup> = emptyList(),
        val lookupError: String? = null,
    ) : AdminServiceEntitlementsUiState
    data class Error(val message: String, val retryable: Boolean) : AdminServiceEntitlementsUiState
}

internal sealed interface AdminServiceEntitlementActionState {
    data object Idle : AdminServiceEntitlementActionState
    data object Working : AdminServiceEntitlementActionState
    data class Success(val message: String) : AdminServiceEntitlementActionState
    data class Error(val message: String, val retryable: Boolean) : AdminServiceEntitlementActionState
}

internal class AdminServiceEntitlementsViewModel(
    private val authRepository: AuthRepository,
    private val catalogRepository: ServiceCatalogRepository,
    private val entitlementRepository: ServiceEntitlementRepository,
) : ViewModel() {
    val sessionState: StateFlow<AuthSessionState> = authRepository.sessionState

    private val _uiState = MutableStateFlow<AdminServiceEntitlementsUiState>(AdminServiceEntitlementsUiState.Idle)
    val uiState: StateFlow<AdminServiceEntitlementsUiState> = _uiState.asStateFlow()
    private val _actionState = MutableStateFlow<AdminServiceEntitlementActionState>(AdminServiceEntitlementActionState.Idle)
    val actionState: StateFlow<AdminServiceEntitlementActionState> = _actionState.asStateFlow()
    private var loadedUid: String? = null
    private var loadingUid: String? = null
    private var requestSequence = 0L

    fun refreshForSession(force: Boolean = false) {
        when (val session = sessionState.value) {
            AuthSessionState.Restoring -> {
                clearSession()
                _uiState.value = AdminServiceEntitlementsUiState.Loading
            }
            is AuthSessionState.RestoreFailed -> {
                clearSession()
                _uiState.value = AdminServiceEntitlementsUiState.Error(
                    session.error.message,
                    retryable = true,
                )
            }
            AuthSessionState.Unauthenticated -> {
                clearSession()
                _uiState.value = AdminServiceEntitlementsUiState.Unauthenticated
            }
            is AuthSessionState.Authenticated -> {
                val uid = session.session.user.uid
                if (!force && loadedUid == uid && _uiState.value is AdminServiceEntitlementsUiState.Loaded) return
                loadCatalog(uid)
            }
        }
    }

    fun updateForm(form: AdminServiceEntitlementForm) {
        val current = _uiState.value as? AdminServiceEntitlementsUiState.Loaded ?: return
        val customerChanged = form.customerEmail.trim().lowercase() != current.form.customerEmail.trim().lowercase()
        _uiState.value = current.copy(
            customer = if (customerChanged) null else current.customer,
            entitlements = if (customerChanged) emptyList() else current.entitlements,
            bookings = if (customerChanged) emptyList() else current.bookings,
            form = if (customerChanged) form.copy(usageReservationCode = "") else form,
        )
        _actionState.value = AdminServiceEntitlementActionState.Idle
    }

    fun findCustomer() {
        val current = _uiState.value as? AdminServiceEntitlementsUiState.Loaded ?: return
        if (_actionState.value == AdminServiceEntitlementActionState.Working) return
        val requestedSequence = requestSequence
        val requestedUid = authenticatedUid() ?: return
        runAction {
            when (val result = entitlementRepository.getAdminEntitlements(current.form.customerEmail)) {
                is AdminServiceEntitlementListResult.Success -> {
                    if (requestSequence != requestedSequence || authenticatedUid() != requestedUid) return@runAction AdminServiceEntitlementActionState.Idle
                    val latest = _uiState.value as? AdminServiceEntitlementsUiState.Loaded
                        ?: return@runAction AdminServiceEntitlementActionState.Error(
                            "O ecrã deixou de estar disponível.",
                            retryable = true,
                        )
                    val lookups = entitlementRepository.getAdminOperationLookups(customerEmail = result.value.customer.email)
                    val activeUid = authenticatedUid()
                    if (activeUid != requestedUid || requestSequence != requestedSequence) return@runAction AdminServiceEntitlementActionState.Idle
                    _uiState.value = latest.copy(
                        bookings = (lookups as? AdminOperationLookupResult.Success)?.value?.reservations.orEmpty(),
                        templates = (lookups as? AdminOperationLookupResult.Success)?.value?.templates ?: latest.templates,
                        lookupError = (lookups as? AdminOperationLookupResult.Failure)?.error?.message,
                        form = latest.form.copy(customerEmail = result.value.customer.email),
                        customer = result.value.customer,
                        entitlements = result.value.entitlements,
                    )
                    AdminServiceEntitlementActionState.Success("Conta e planos atualizados.")
                }
                is AdminServiceEntitlementListResult.Failure -> result.error.toActionState()
            }
        }
    }

    suspend fun searchCustomers(query: String): List<AdminLookupOption> {
        val uid = authenticatedUid() ?: error("Inicie sessão como administrador.")
        val result = entitlementRepository.getAdminOperationLookups(query = query)
        if (authenticatedUid() != uid) throw CancellationException("Sessão alterada")
        return when (result) {
            is AdminOperationLookupResult.Success -> result.value.customers.map {
                AdminLookupOption(it.email, it.displayName.ifBlank { "Cliente registado" },
                    listOf(it.email, it.phoneNumber).filter(String::isNotBlank).joinToString(" · "))
            }
            is AdminOperationLookupResult.Failure -> error(result.error.message)
        }
    }

    fun selectTemplate(id: String) {
        val current = _uiState.value as? AdminServiceEntitlementsUiState.Loaded ?: return
        val template = current.templates.firstOrNull { it.id == id }
        updateForm(if (template == null) current.form.copy(selectedTemplateId = "") else current.form.applyTemplate(template))
    }

    fun saveTemplate() {
        val current = _uiState.value as? AdminServiceEntitlementsUiState.Loaded ?: return
        val form = current.form
        val uses = form.totalUses.toIntOrNull()?.takeIf { it in 1..100 }
        val days = form.validDays.toIntOrNull()?.takeIf { it in 1..730 }
        if (form.name.trim().length < 3 || uses == null || days == null || form.selectedServiceIds.isEmpty()) {
            _actionState.value = AdminServiceEntitlementActionState.Error("Revise o nome, utilizações, validade e serviços do modelo.", false)
            return
        }
        val template = AdminPackageTemplate(form.selectedTemplateId, form.name.trim(), form.kind, uses, days,
            form.selectedServiceIds.toList().sorted())
        runAction {
            when (val result = entitlementRepository.saveAdminPackageTemplate(template)) {
                is AdminPackageTemplateResult.Success -> {
                    val latest = _uiState.value as? AdminServiceEntitlementsUiState.Loaded
                        ?: return@runAction AdminServiceEntitlementActionState.Idle
                    _uiState.value = latest.copy(
                        templates = (latest.templates.filterNot { it.id == result.template.id } + result.template).sortedBy { it.name },
                        form = latest.form.copy(selectedTemplateId = result.template.id), lookupError = null,
                    )
                    AdminServiceEntitlementActionState.Success("Modelo guardado. Não foi emitido nenhum plano.")
                }
                is AdminPackageTemplateResult.Failure -> result.error.toActionState()
            }
        }
    }

    fun issue() {
        val current = _uiState.value as? AdminServiceEntitlementsUiState.Loaded ?: return
        val customer = current.customer ?: run {
            _actionState.value = AdminServiceEntitlementActionState.Error(
                "Consulte primeiro a conta do cliente.",
                retryable = false,
            )
            return
        }
        val request = current.form.toIssueRequest(customer.email) ?: run {
            _actionState.value = AdminServiceEntitlementActionState.Error(
                "Revise o nome, utilizações, validade, valor, serviços incluídos e nota de emissão.",
                retryable = false,
            )
            return
        }
        runMutation("Plano emitido e registado no histórico.") { entitlementRepository.issueEntitlement(request) }
    }

    fun adjustUsage(entitlementId: String, deltaUses: Int) {
        val current = _uiState.value as? AdminServiceEntitlementsUiState.Loaded ?: return
        val customer = current.customer ?: return
        val note = current.form.usageNote.trim()
        if (note.isBlank()) {
            _actionState.value = AdminServiceEntitlementActionState.Error(
                "Indique uma nota para o histórico da utilização.",
                retryable = false,
            )
            return
        }
        val request = AdjustServiceEntitlementUsageRequest(
            operationId = operationId(if (deltaUses > 0) "use" else "correct"),
            customerEmail = customer.email,
            entitlementId = entitlementId,
            deltaUses = deltaUses,
            reservationCode = current.form.usageReservationCode.trim().uppercase(),
            staffNote = note,
        )
        val message = if (deltaUses > 0) "Utilização registada." else "Última utilização corrigida."
        runMutation(message) { entitlementRepository.adjustUsage(request) }
    }

    fun requestRevoke(entitlementId: String) {
        val current = _uiState.value as? AdminServiceEntitlementsUiState.Loaded ?: return
        _uiState.value = current.copy(pendingRevocationId = entitlementId)
    }

    fun cancelRevoke() {
        val current = _uiState.value as? AdminServiceEntitlementsUiState.Loaded ?: return
        _uiState.value = current.copy(pendingRevocationId = null)
    }

    fun confirmRevoke() {
        val current = _uiState.value as? AdminServiceEntitlementsUiState.Loaded ?: return
        val customer = current.customer ?: return
        val entitlementId = current.pendingRevocationId ?: return
        val reason = current.form.usageNote.trim()
        if (reason.isBlank()) {
            _actionState.value = AdminServiceEntitlementActionState.Error(
                "Indique o motivo da revogação na nota de operação.",
                retryable = false,
            )
            return
        }
        _uiState.value = current.copy(pendingRevocationId = null)
        runMutation("Plano revogado com histórico preservado.") {
            entitlementRepository.revokeEntitlement(
                RevokeServiceEntitlementRequest(
                    operationId = operationId("revoke"),
                    customerEmail = customer.email,
                    entitlementId = entitlementId,
                    reason = reason,
                ),
            )
        }
    }

    fun clearActionState() {
        if (_actionState.value != AdminServiceEntitlementActionState.Working) {
            _actionState.value = AdminServiceEntitlementActionState.Idle
        }
    }

    private fun loadCatalog(uid: String) {
        if (loadingUid == uid) return
        val sequence = ++requestSequence
        loadingUid = uid
        _uiState.value = AdminServiceEntitlementsUiState.Loading
        viewModelScope.launch {
            try {
                val state = when (val result = catalogRepository.getServiceCatalog()) {
                    is ServiceCatalogResult.Success -> {
                        val services = result.catalog.services.map { AdminEntitlementServiceOption(it.id, it.name) }
                        val lookup = entitlementRepository.getAdminOperationLookups()
                        AdminServiceEntitlementsUiState.Loaded(
                            form = AdminServiceEntitlementForm(selectedServiceIds = services.map { it.id }.toSet()),
                            services = services,
                            templates = (lookup as? AdminOperationLookupResult.Success)?.value?.templates.orEmpty(),
                            lookupError = (lookup as? AdminOperationLookupResult.Failure)?.error?.message,
                        )
                    }
                    is ServiceCatalogResult.Failure -> AdminServiceEntitlementsUiState.Error(
                        result.error.message,
                        retryable = true,
                    )
                }
                if (sequence != requestSequence || !sessionStillMatches(uid)) return@launch
                loadedUid = uid
                _uiState.value = state
            } finally {
                if (sequence == requestSequence) loadingUid = null
            }
        }
    }

    private fun runMutation(
        successMessage: String,
        mutation: suspend () -> ServiceEntitlementMutationResult,
    ) {
        if (_actionState.value == AdminServiceEntitlementActionState.Working) return
        val uid = authenticatedUid() ?: run {
            refreshForSession(force = true)
            return
        }
        viewModelScope.launch {
            if (!sessionStillMatches(uid)) return@launch
            _actionState.value = AdminServiceEntitlementActionState.Working
            val result = mutation()
            if (!sessionStillMatches(uid)) return@launch
            when (result) {
                is ServiceEntitlementMutationResult.Success -> {
                    val current = _uiState.value as? AdminServiceEntitlementsUiState.Loaded
                    if (current != null) {
                        val updated = listOf(result.entitlement) + current.entitlements.filterNot {
                            it.id == result.entitlement.id
                        }
                        _uiState.value = current.copy(entitlements = updated)
                    }
                    _actionState.value = AdminServiceEntitlementActionState.Success(successMessage)
                }
                is ServiceEntitlementMutationResult.Failure -> _actionState.value = result.error.toActionState()
            }
        }
    }

    private fun runAction(action: suspend () -> AdminServiceEntitlementActionState) {
        if (_actionState.value == AdminServiceEntitlementActionState.Working) return
        val uid = authenticatedUid() ?: run {
            refreshForSession(force = true)
            return
        }
        viewModelScope.launch {
            if (!sessionStillMatches(uid)) return@launch
            _actionState.value = AdminServiceEntitlementActionState.Working
            val result = action()
            if (!sessionStillMatches(uid)) return@launch
            _actionState.value = result
        }
    }

    private fun authenticatedUid(): String? =
        (sessionState.value as? AuthSessionState.Authenticated)?.session?.user?.uid

    private fun sessionStillMatches(uid: String): Boolean {
        if (authenticatedUid() == uid) return true
        refreshForSession(force = true)
        return false
    }

    private fun clearSession() {
        loadedUid = null
        loadingUid = null
        requestSequence += 1
        _actionState.value = AdminServiceEntitlementActionState.Idle
    }
}

private fun AdminServiceEntitlementForm.toIssueRequest(customerEmail: String): IssueServiceEntitlementRequest? {
    val uses = totalUses.toIntOrNull()?.takeIf { it in 1..100 } ?: return null
    val days = validDays.toIntOrNull()?.takeIf { it in 1..730 } ?: return null
    val cents = amountPaidEuros.toEuroCentsOrNull() ?: return null
    if (name.trim().length < 3 || selectedServiceIds.isEmpty() || issueNote.isBlank()) return null
    return IssueServiceEntitlementRequest(
        operationId = operationId("issue"),
        customerEmail = customerEmail,
        kind = kind,
        name = name.trim(),
        totalUses = uses,
        validDays = days,
        amountPaidCents = cents,
        eligibleServiceIds = selectedServiceIds.toList().sorted(),
        staffNote = issueNote.trim(),
    )
}

private fun String.toEuroCentsOrNull(): Int? {
    val value = trim().replace(',', '.')
    if (value.isBlank()) return 0
    val parts = value.split('.')
    if (parts.size > 2 || parts[0].any { !it.isDigit() } || parts.getOrNull(1)?.any { !it.isDigit() } == true) return null
    val euros = parts[0].toIntOrNull() ?: return null
    val decimals = parts.getOrNull(1).orEmpty()
    if (decimals.length > 2) return null
    val cents = decimals.padEnd(2, '0').ifBlank { "00" }.toIntOrNull() ?: return null
    return (euros * 100 + cents).takeIf { it in 0..1_000_000 }
}

private fun operationId(prefix: String): String {
    val suffix = Random.nextBytes(12).joinToString("") { byte ->
        (byte.toInt() and 0xff).toString(16).padStart(2, '0')
    }
    return "$prefix-$suffix"
}

private fun ServiceEntitlementError.toActionState(): AdminServiceEntitlementActionState.Error =
    AdminServiceEntitlementActionState.Error(
        message = message,
        retryable = this is ServiceEntitlementError.Unavailable || this is ServiceEntitlementError.Backend,
    )

internal fun AdminServiceEntitlementForm.applyTemplate(template: AdminPackageTemplate): AdminServiceEntitlementForm = copy(
    selectedTemplateId = template.id, kind = template.kind, name = template.name,
    totalUses = template.totalUses.toString(), validDays = template.validDays.toString(),
    selectedServiceIds = template.eligibleServiceIds.toSet(),
)
