package com.sudsmobile.feature.profile

import com.sudsmobile.data.admin.AdminAvailabilityResult
import com.sudsmobile.data.admin.AdminAvailabilityUpdateRequest
import com.sudsmobile.data.admin.AdminBookingDecisionRequest
import com.sudsmobile.data.admin.AdminBookingDecisionResult
import com.sudsmobile.data.admin.AdminBookingPolicyResult
import com.sudsmobile.data.admin.AdminBookingRequestsResult
import com.sudsmobile.data.admin.AdminBusinessInfoResult
import com.sudsmobile.data.admin.AdminBusinessInfoUpdateRequest
import com.sudsmobile.data.admin.AdminCapacityOverrideClearRequest
import com.sudsmobile.data.admin.AdminCapacityOverrideMutationResult
import com.sudsmobile.data.admin.AdminCapacityOverrideUpsertRequest
import com.sudsmobile.data.admin.AdminError
import com.sudsmobile.data.admin.AdminNotificationSettingsConfig
import com.sudsmobile.data.admin.AdminNotificationSettingsResult
import com.sudsmobile.data.admin.AdminNotificationSettingsUpdateRequest
import com.sudsmobile.data.admin.AdminNotificationTemplateConfig
import com.sudsmobile.data.admin.AdminRepository
import com.sudsmobile.data.admin.AdminRole
import com.sudsmobile.data.admin.AdminRoleResult
import com.sudsmobile.data.admin.AdminServiceCatalogArchiveRequest
import com.sudsmobile.data.admin.AdminServiceCatalogMutationRequest
import com.sudsmobile.data.admin.AdminServiceCatalogMutationResult
import com.sudsmobile.data.admin.AdminServiceCatalogResult
import com.sudsmobile.data.admin.AdminServiceExtraArchiveRequest
import com.sudsmobile.data.admin.AdminServiceExtraMutationRequest
import com.sudsmobile.data.admin.AdminServiceExtraMutationResult
import com.sudsmobile.data.admin.AdminServiceExtrasResult
import com.sudsmobile.data.auth.AuthActionResult
import com.sudsmobile.data.auth.AuthRepository
import com.sudsmobile.data.auth.AuthResult
import com.sudsmobile.data.auth.AuthSession
import com.sudsmobile.data.auth.AuthSessionState
import com.sudsmobile.data.auth.AuthUser
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
class AdminNotificationSettingsViewModelTest {
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
    fun loadConfigurationRequiresAuthenticatedSessionBeforeRepositoryCall() = runTest {
        val repository = FakeNotificationSettingsAdminRepository()
        val viewModel = AdminNotificationSettingsViewModel(
            authRepository = FakeNotificationSettingsAuthRepository(authenticated = false),
            adminRepository = repository,
        )

        viewModel.loadConfiguration()
        runCurrent()

        assertIs<AdminNotificationSettingsUiState.Unauthenticated>(viewModel.uiState.value)
        assertEquals(0, repository.loadCalls)
    }

    @Test
    fun loadConfigurationMapsPermissionFailureToNotAdmin() = runTest {
        val viewModel = AdminNotificationSettingsViewModel(
            authRepository = FakeNotificationSettingsAuthRepository(authenticated = true),
            adminRepository = FakeNotificationSettingsAdminRepository(
                loadResult = AdminNotificationSettingsResult.Failure(AdminError.Permission("denied")),
            ),
        )

        viewModel.loadConfiguration()
        runCurrent()

        assertIs<AdminNotificationSettingsUiState.NotAdmin>(viewModel.uiState.value)
    }

    @Test
    fun loadConfigurationIgnoresStaleResponseAfterSignOut() = runTest {
        val deferred = CompletableDeferred<AdminNotificationSettingsResult>()
        val authRepository = FakeNotificationSettingsAuthRepository(authenticated = true)
        val viewModel = AdminNotificationSettingsViewModel(
            authRepository = authRepository,
            adminRepository = FakeNotificationSettingsAdminRepository(loadResultDeferred = deferred),
        )

        viewModel.loadConfiguration()
        runCurrent()
        authRepository.signOut()
        deferred.complete(AdminNotificationSettingsResult.Success(adminNotificationSettingsConfig()))
        runCurrent()

        assertIs<AdminNotificationSettingsUiState.Unauthenticated>(viewModel.uiState.value)
    }

    @Test
    fun saveValidatesBeforeRepositoryCall() = runTest {
        val repository = FakeNotificationSettingsAdminRepository()
        val viewModel = AdminNotificationSettingsViewModel(
            authRepository = FakeNotificationSettingsAuthRepository(authenticated = true),
            adminRepository = repository,
        )

        viewModel.loadConfiguration()
        runCurrent()
        viewModel.updateForm(adminNotificationSettingsConfig().toTestForm(reminderLeadMinutes = "5"))
        viewModel.save()
        runCurrent()

        assertIs<AdminNotificationSettingsSaveState.Error>(viewModel.saveState.value)
        assertEquals(0, repository.updateRequests.size)
    }

    @Test
    fun saveSubmitsParsedNotificationSettings() = runTest {
        val repository = FakeNotificationSettingsAdminRepository()
        val viewModel = AdminNotificationSettingsViewModel(
            authRepository = FakeNotificationSettingsAuthRepository(authenticated = true),
            adminRepository = repository,
        )

        viewModel.loadConfiguration()
        runCurrent()
        viewModel.updateForm(
            adminNotificationSettingsConfig().toTestForm(
                reminderLeadMinutes = "60",
                templates = adminNotificationTemplates().map {
                    if (it.key == "booking_request") {
                        it.copy(title = "  Pedido   recebido  ")
                    } else {
                        it
                    }
                },
                adminPendingAlertEnabled = false,
            ),
        )
        viewModel.save()
        runCurrent()

        assertIs<AdminNotificationSettingsSaveState.Success>(viewModel.saveState.value)
        val request = repository.updateRequests.single()
        assertEquals(60, request.reminderLeadMinutes)
        assertEquals("Pedido recebido", request.templates.first { it.key == "booking_request" }.title)
        assertEquals(false, request.adminPendingAlertEnabled)
    }

    @Test
    fun saveStopsWhenSessionChangesBeforeRepositoryCall() = runTest {
        val authRepository = FakeNotificationSettingsAuthRepository(authenticated = true)
        val repository = FakeNotificationSettingsAdminRepository()
        val viewModel = AdminNotificationSettingsViewModel(
            authRepository = authRepository,
            adminRepository = repository,
        )

        viewModel.loadConfiguration()
        runCurrent()
        viewModel.save()
        authRepository.signOut()
        runCurrent()

        assertEquals(0, repository.updateRequests.size)
        assertIs<AdminNotificationSettingsUiState.Unauthenticated>(viewModel.uiState.value)
    }
}

private class FakeNotificationSettingsAdminRepository(
    var loadResult: AdminNotificationSettingsResult =
        AdminNotificationSettingsResult.Success(adminNotificationSettingsConfig()),
    var updateResult: AdminNotificationSettingsResult? = null,
    private val loadResultDeferred: CompletableDeferred<AdminNotificationSettingsResult>? = null,
) : AdminRepository {
    var loadCalls = 0
        private set
    val updateRequests = mutableListOf<AdminNotificationSettingsUpdateRequest>()

    override suspend fun syncMyRole(): AdminRoleResult {
        return AdminRoleResult.Success(AdminRole(uid = "uid-1", email = "admin@example.com", role = "admin"))
    }

    override suspend fun getPendingBookingRequests(): AdminBookingRequestsResult =
        AdminBookingRequestsResult.Failure(AdminError.Backend("unused"))

    override suspend fun getBusinessInfoConfiguration(): AdminBusinessInfoResult =
        AdminBusinessInfoResult.Failure(AdminError.Backend("unused"))

    override suspend fun getAvailabilityConfiguration(): AdminAvailabilityResult =
        AdminAvailabilityResult.Failure(AdminError.Backend("unused"))

    override suspend fun getServiceCatalogConfiguration(): AdminServiceCatalogResult =
        AdminServiceCatalogResult.Failure(AdminError.Backend("unused"))

    override suspend fun getServiceExtrasConfiguration(): AdminServiceExtrasResult =
        AdminServiceExtrasResult.Failure(AdminError.Backend("unused"))

    override suspend fun getNotificationSettingsConfiguration(): AdminNotificationSettingsResult {
        loadCalls += 1
        return loadResultDeferred?.await() ?: loadResult
    }

    override suspend fun updateNotificationSettingsConfiguration(
        request: AdminNotificationSettingsUpdateRequest,
    ): AdminNotificationSettingsResult {
        updateRequests += request
        return updateResult ?: AdminNotificationSettingsResult.Success(
            AdminNotificationSettingsConfig(
                bookingStatusEnabled = request.bookingStatusEnabled,
                appointmentReminderEnabled = request.appointmentReminderEnabled,
                loyaltyEnabled = request.loyaltyEnabled,
                adminPendingAlertEnabled = request.adminPendingAlertEnabled,
                marketingEnabled = request.marketingEnabled,
                reminderLeadMinutes = request.reminderLeadMinutes,
                quietHoursStart = request.quietHoursStart,
                quietHoursEnd = request.quietHoursEnd,
                templates = request.templates,
            ),
        )
    }

    override suspend fun updateBusinessInfoConfiguration(
        request: AdminBusinessInfoUpdateRequest,
    ): AdminBusinessInfoResult = AdminBusinessInfoResult.Failure(AdminError.Backend("unused"))

    override suspend fun updateAvailabilityConfiguration(
        request: AdminAvailabilityUpdateRequest,
    ): AdminAvailabilityResult = AdminAvailabilityResult.Failure(AdminError.Backend("unused"))

    override suspend fun upsertCapacityOverride(
        request: AdminCapacityOverrideUpsertRequest,
    ): AdminCapacityOverrideMutationResult = AdminCapacityOverrideMutationResult.Failure(AdminError.Backend("unused"))

    override suspend fun clearCapacityOverride(
        request: AdminCapacityOverrideClearRequest,
    ): AdminCapacityOverrideMutationResult = AdminCapacityOverrideMutationResult.Failure(AdminError.Backend("unused"))

    override suspend fun acceptBookingRequest(request: AdminBookingDecisionRequest): AdminBookingDecisionResult =
        AdminBookingDecisionResult.Failure(AdminError.Backend("unused"))

    override suspend fun rejectBookingRequest(request: AdminBookingDecisionRequest): AdminBookingDecisionResult =
        AdminBookingDecisionResult.Failure(AdminError.Backend("unused"))

    override suspend fun upsertServiceCatalogItem(
        request: AdminServiceCatalogMutationRequest,
    ): AdminServiceCatalogMutationResult = AdminServiceCatalogMutationResult.Failure(AdminError.Backend("unused"))

    override suspend fun archiveServiceCatalogItem(
        request: AdminServiceCatalogArchiveRequest,
    ): AdminServiceCatalogMutationResult = AdminServiceCatalogMutationResult.Failure(AdminError.Backend("unused"))

    override suspend fun upsertServiceExtra(
        request: AdminServiceExtraMutationRequest,
    ): AdminServiceExtraMutationResult = AdminServiceExtraMutationResult.Failure(AdminError.Backend("unused"))

    override suspend fun archiveServiceExtra(
        request: AdminServiceExtraArchiveRequest,
    ): AdminServiceExtraMutationResult = AdminServiceExtraMutationResult.Failure(AdminError.Backend("unused"))
}

private class FakeNotificationSettingsAuthRepository(authenticated: Boolean) : AuthRepository {
    private val authSession = AuthSession(
        user = AuthUser(
            uid = "uid-1",
            email = "admin@example.com",
            displayName = "Admin",
            phoneNumber = "",
        ),
        idToken = "id-token-1",
        refreshToken = "refresh-token-1",
        expiresInSeconds = 3600,
    )
    private val mutableSessionState = MutableStateFlow(
        if (authenticated) AuthSessionState.Authenticated(authSession) else AuthSessionState.Unauthenticated,
    )

    override val sessionState = mutableSessionState

    override suspend fun currentSession(): AuthSession? {
        return (sessionState.value as? AuthSessionState.Authenticated)?.session
    }

    override suspend fun signIn(email: String, password: String): AuthResult {
        mutableSessionState.value = AuthSessionState.Authenticated(authSession)
        return AuthResult.Success(authSession)
    }

    override suspend fun register(
        displayName: String,
        email: String,
        phoneNumber: String,
        password: String,
    ): AuthResult {
        mutableSessionState.value = AuthSessionState.Authenticated(authSession)
        return AuthResult.Success(authSession)
    }

    override suspend fun sendPasswordReset(email: String): AuthActionResult = AuthActionResult.Success

    override fun signOut() {
        mutableSessionState.value = AuthSessionState.Unauthenticated
    }
}

private fun adminNotificationSettingsConfig(): AdminNotificationSettingsConfig = AdminNotificationSettingsConfig(
    bookingStatusEnabled = true,
    appointmentReminderEnabled = true,
    loyaltyEnabled = true,
    adminPendingAlertEnabled = true,
    marketingEnabled = false,
    reminderLeadMinutes = 120,
    quietHoursStart = "22:00",
    quietHoursEnd = "08:00",
    templates = adminNotificationTemplates(),
)

private fun AdminNotificationSettingsConfig.toTestForm(
    bookingStatusEnabled: Boolean = this.bookingStatusEnabled,
    appointmentReminderEnabled: Boolean = this.appointmentReminderEnabled,
    loyaltyEnabled: Boolean = this.loyaltyEnabled,
    adminPendingAlertEnabled: Boolean = this.adminPendingAlertEnabled,
    marketingEnabled: Boolean = this.marketingEnabled,
    reminderLeadMinutes: String = this.reminderLeadMinutes.toString(),
    quietHoursStart: String = this.quietHoursStart,
    quietHoursEnd: String = this.quietHoursEnd,
    templates: List<AdminNotificationTemplateConfig> = this.templates,
): AdminNotificationSettingsForm = AdminNotificationSettingsForm(
    bookingStatusEnabled = bookingStatusEnabled,
    appointmentReminderEnabled = appointmentReminderEnabled,
    loyaltyEnabled = loyaltyEnabled,
    adminPendingAlertEnabled = adminPendingAlertEnabled,
    marketingEnabled = marketingEnabled,
    reminderLeadMinutes = reminderLeadMinutes,
    quietHoursStart = quietHoursStart,
    quietHoursEnd = quietHoursEnd,
    templates = templates.map {
        AdminNotificationTemplateForm(
            key = it.key,
            label = it.label,
            enabled = it.enabled,
            title = it.title,
            body = it.body,
        )
    },
)

private fun adminNotificationTemplates(): List<AdminNotificationTemplateConfig> = listOf(
    AdminNotificationTemplateConfig(
        key = "booking_request",
        label = "Pedido recebido",
        enabled = true,
        title = "Pedido de marcação recebido",
        body = "Recebemos o seu pedido de marcação.",
    ),
    AdminNotificationTemplateConfig(
        key = "booking_accepted",
        label = "Marcação aceite",
        enabled = true,
        title = "Marcação confirmada",
        body = "A sua marcação foi aceite.",
    ),
    AdminNotificationTemplateConfig(
        key = "booking_rejected",
        label = "Marcação rejeitada",
        enabled = true,
        title = "Marcação rejeitada",
        body = "Não foi possível aceitar a marcação.",
    ),
    AdminNotificationTemplateConfig(
        key = "booking_expired",
        label = "Pedido expirado",
        enabled = true,
        title = "Pedido expirado",
        body = "O pedido expirou antes da confirmação.",
    ),
    AdminNotificationTemplateConfig(
        key = "booking_cancelled",
        label = "Marcação cancelada",
        enabled = true,
        title = "Marcação cancelada",
        body = "A marcação foi cancelada.",
    ),
    AdminNotificationTemplateConfig(
        key = "booking_rescheduled",
        label = "Marcação remarcada",
        enabled = true,
        title = "Marcação remarcada",
        body = "A marcação foi remarcada.",
    ),
    AdminNotificationTemplateConfig(
        key = "booking_reminder",
        label = "Lembrete de marcação",
        enabled = true,
        title = "Lembrete",
        body = "Tem uma lavagem marcada em breve.",
    ),
    AdminNotificationTemplateConfig(
        key = "review_prompt",
        label = "Pedido de avaliação",
        enabled = true,
        title = "Avalie a lavagem",
        body = "Diga-nos como correu o serviço.",
    ),
    AdminNotificationTemplateConfig(
        key = "admin_pending_booking",
        label = "Alerta admin de pedido",
        enabled = true,
        title = "Novo pedido de marcação",
        body = "{{customerName}} pediu {{serviceName}} para {{slotStart}}.",
    ),
)
