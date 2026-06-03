package com.sudsmobile.feature.profile

import com.sudsmobile.data.admin.AdminAvailabilityResult
import com.sudsmobile.data.admin.AdminAvailabilityUpdateRequest
import com.sudsmobile.data.admin.AdminBookingDecisionRequest
import com.sudsmobile.data.admin.AdminBookingDecisionResult
import com.sudsmobile.data.admin.AdminBookingRequestsResult
import com.sudsmobile.data.admin.AdminBusinessInfoConfig
import com.sudsmobile.data.admin.AdminBusinessInfoResult
import com.sudsmobile.data.admin.AdminBusinessInfoUpdateRequest
import com.sudsmobile.data.admin.AdminBusinessOpeningHours
import com.sudsmobile.data.admin.AdminBusinessSocialLink
import com.sudsmobile.data.admin.AdminCapacityOverrideClearRequest
import com.sudsmobile.data.admin.AdminCapacityOverrideMutationResult
import com.sudsmobile.data.admin.AdminCapacityOverrideUpsertRequest
import com.sudsmobile.data.admin.AdminError
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
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
class AdminBusinessInfoViewModelTest {
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
        val repository = FakeBusinessInfoAdminRepository()
        val viewModel = AdminBusinessInfoViewModel(
            authRepository = FakeBusinessInfoAuthRepository(authenticated = false),
            adminRepository = repository,
        )

        viewModel.loadConfiguration()
        runCurrent()

        assertIs<AdminBusinessInfoUiState.Unauthenticated>(viewModel.uiState.value)
        assertEquals(0, repository.loadCalls)
    }

    @Test
    fun loadConfigurationMapsPermissionFailureToNotAdmin() = runTest {
        val repository = FakeBusinessInfoAdminRepository(
            loadResult = AdminBusinessInfoResult.Failure(AdminError.Permission("denied")),
        )
        val viewModel = AdminBusinessInfoViewModel(
            authRepository = FakeBusinessInfoAuthRepository(authenticated = true),
            adminRepository = repository,
        )

        viewModel.loadConfiguration()
        runCurrent()

        assertIs<AdminBusinessInfoUiState.NotAdmin>(viewModel.uiState.value)
    }

    @Test
    fun loadConfigurationShowsBusinessInfoAuditLabel() = runTest {
        val repository = FakeBusinessInfoAdminRepository(
            loadResult = AdminBusinessInfoResult.Success(
                adminBusinessInfoConfig(
                    updatedAtIso = "2026-06-01T10:15:00.000Z",
                    updatedByUid = "admin-business-long",
                ),
            ),
        )
        val viewModel = AdminBusinessInfoViewModel(
            authRepository = FakeBusinessInfoAuthRepository(authenticated = true),
            adminRepository = repository,
        )

        viewModel.loadConfiguration()
        runCurrent()

        val loaded = assertIs<AdminBusinessInfoUiState.Loaded>(viewModel.uiState.value)
        assertEquals("Atualizado 2026-06-01 10:15 UTC por admin-bu...", loaded.form.updatedAuditLabel)
    }

    @Test
    fun loadConfigurationIgnoresStaleResponseAfterSignOut() = runTest {
        val deferred = CompletableDeferred<AdminBusinessInfoResult>()
        val authRepository = FakeBusinessInfoAuthRepository(authenticated = true)
        val repository = FakeBusinessInfoAdminRepository(loadResultDeferred = deferred)
        val viewModel = AdminBusinessInfoViewModel(
            authRepository = authRepository,
            adminRepository = repository,
        )

        viewModel.loadConfiguration()
        runCurrent()
        authRepository.signOut()
        deferred.complete(AdminBusinessInfoResult.Success(adminBusinessInfoConfig()))
        runCurrent()

        assertIs<AdminBusinessInfoUiState.Unauthenticated>(viewModel.uiState.value)
    }

    @Test
    fun saveSendsBusinessInfoUpdateAndShowsSuccess() = runTest {
        val repository = FakeBusinessInfoAdminRepository(
            loadResult = AdminBusinessInfoResult.Success(adminBusinessInfoConfig()),
            updateResult = AdminBusinessInfoResult.Success(adminBusinessInfoConfig(phone = "244 000 222")),
        )
        val viewModel = AdminBusinessInfoViewModel(
            authRepository = FakeBusinessInfoAuthRepository(authenticated = true),
            adminRepository = repository,
        )

        viewModel.loadConfiguration()
        runCurrent()
        val loaded = assertIs<AdminBusinessInfoUiState.Loaded>(viewModel.uiState.value)
        viewModel.updateForm(
            loaded.form.copy(
                phone = " 244 000 222 ",
                openingHoursText = "Dias úteis | 10:00 - 18:00\nDomingo | Encerrado | fechado",
                socialLinksText = "Instagram | https://instagram.com/sudsshine",
            ),
        )
        viewModel.save()
        runCurrent()

        val request = repository.updateRequests.single()
        assertEquals(" 244 000 222 ", request.phone)
        assertEquals(2, request.openingHours.size)
        assertEquals(true, request.openingHours[1].closed)
        assertEquals("Instagram", request.socialLinks.single().label)
        assertIs<AdminBusinessInfoSaveState.Success>(viewModel.saveState.value)
        val reloaded = assertIs<AdminBusinessInfoUiState.Loaded>(viewModel.uiState.value)
        assertEquals("244 000 222", reloaded.form.phone)
    }

    @Test
    fun saveValidationErrorDoesNotCallRepository() = runTest {
        val repository = FakeBusinessInfoAdminRepository(
            loadResult = AdminBusinessInfoResult.Success(adminBusinessInfoConfig()),
        )
        val viewModel = AdminBusinessInfoViewModel(
            authRepository = FakeBusinessInfoAuthRepository(authenticated = true),
            adminRepository = repository,
        )

        viewModel.loadConfiguration()
        runCurrent()
        val loaded = assertIs<AdminBusinessInfoUiState.Loaded>(viewModel.uiState.value)
        viewModel.updateForm(loaded.form.copy(openingHoursText = "Sem separador"))
        viewModel.save()
        runCurrent()

        assertEquals(0, repository.updateRequests.size)
        assertIs<AdminBusinessInfoSaveState.Error>(viewModel.saveState.value)
    }

    @Test
    fun saveRechecksSessionBeforeRepositoryCall() = runTest {
        val authRepository = FakeBusinessInfoAuthRepository(authenticated = true)
        val repository = FakeBusinessInfoAdminRepository(
            loadResult = AdminBusinessInfoResult.Success(adminBusinessInfoConfig()),
        )
        val viewModel = AdminBusinessInfoViewModel(
            authRepository = authRepository,
            adminRepository = repository,
        )

        viewModel.loadConfiguration()
        runCurrent()
        assertIs<AdminBusinessInfoUiState.Loaded>(viewModel.uiState.value)

        viewModel.save()
        authRepository.signOut()
        runCurrent()

        assertEquals(0, repository.updateRequests.size)
        assertIs<AdminBusinessInfoUiState.Unauthenticated>(viewModel.uiState.value)
    }

    @Test
    fun saveIgnoresStaleFailureAfterUserSwitchAndReloadsCurrentSession() = runTest {
        val deferred = CompletableDeferred<AdminBusinessInfoResult>()
        val authRepository = FakeBusinessInfoAuthRepository(authenticated = true)
        val repository = FakeBusinessInfoAdminRepository(
            updateResultDeferred = deferred,
            loadResults = ArrayDeque(
                listOf(
                    AdminBusinessInfoResult.Success(adminBusinessInfoConfig(phone = "913 005 855")),
                    AdminBusinessInfoResult.Success(adminBusinessInfoConfig(phone = "244 222 333")),
                ),
            ),
        )
        val viewModel = AdminBusinessInfoViewModel(
            authRepository = authRepository,
            adminRepository = repository,
        )

        viewModel.loadConfiguration()
        runCurrent()
        viewModel.save()
        runCurrent()
        authRepository.switchTo("uid-2")
        deferred.complete(AdminBusinessInfoResult.Failure(AdminError.Permission("old admin denied")))
        runCurrent()

        assertEquals("913 005 855", repository.updateRequests.single().phone)
        assertEquals(2, repository.loadCalls)
        assertIs<AdminBusinessInfoSaveState.Idle>(viewModel.saveState.value)
        val loaded = assertIs<AdminBusinessInfoUiState.Loaded>(viewModel.uiState.value)
        assertEquals("244 222 333", loaded.form.phone)
    }
}

private class FakeBusinessInfoAdminRepository(
    var loadResult: AdminBusinessInfoResult = AdminBusinessInfoResult.Success(adminBusinessInfoConfig()),
    var updateResult: AdminBusinessInfoResult = AdminBusinessInfoResult.Success(adminBusinessInfoConfig()),
    private val loadResultDeferred: CompletableDeferred<AdminBusinessInfoResult>? = null,
    private val loadResults: ArrayDeque<AdminBusinessInfoResult>? = null,
    private val updateResultDeferred: CompletableDeferred<AdminBusinessInfoResult>? = null,
) : AdminRepository {
    var loadCalls = 0
        private set
    val updateRequests = mutableListOf<AdminBusinessInfoUpdateRequest>()

    override suspend fun syncMyRole(): AdminRoleResult {
        return AdminRoleResult.Success(AdminRole(uid = "uid-1", email = "admin@example.com", role = "admin"))
    }

    override suspend fun getPendingBookingRequests(): AdminBookingRequestsResult {
        return AdminBookingRequestsResult.Failure(AdminError.Backend("unused"))
    }

    override suspend fun getBusinessInfoConfiguration(): AdminBusinessInfoResult {
        loadCalls += 1
        return loadResultDeferred?.await() ?: loadResults?.removeFirstOrNull() ?: loadResult
    }

    override suspend fun getAvailabilityConfiguration(): AdminAvailabilityResult {
        return AdminAvailabilityResult.Failure(AdminError.Backend("unused"))
    }

    override suspend fun getServiceCatalogConfiguration(): AdminServiceCatalogResult {
        return AdminServiceCatalogResult.Failure(AdminError.Backend("unused"))
    }

    override suspend fun getServiceExtrasConfiguration(): AdminServiceExtrasResult {
        return AdminServiceExtrasResult.Failure(AdminError.Backend("unused"))
    }

    override suspend fun updateBusinessInfoConfiguration(
        request: AdminBusinessInfoUpdateRequest,
    ): AdminBusinessInfoResult {
        updateRequests += request
        return updateResultDeferred?.await() ?: updateResult
    }

    override suspend fun updateAvailabilityConfiguration(
        request: AdminAvailabilityUpdateRequest,
    ): AdminAvailabilityResult {
        return AdminAvailabilityResult.Failure(AdminError.Backend("unused"))
    }

    override suspend fun upsertCapacityOverride(
        request: AdminCapacityOverrideUpsertRequest,
    ): AdminCapacityOverrideMutationResult {
        return AdminCapacityOverrideMutationResult.Failure(AdminError.Backend("unused"))
    }

    override suspend fun clearCapacityOverride(
        request: AdminCapacityOverrideClearRequest,
    ): AdminCapacityOverrideMutationResult {
        return AdminCapacityOverrideMutationResult.Failure(AdminError.Backend("unused"))
    }

    override suspend fun acceptBookingRequest(
        request: AdminBookingDecisionRequest,
    ): AdminBookingDecisionResult {
        return AdminBookingDecisionResult.Failure(AdminError.Backend("unused"))
    }

    override suspend fun rejectBookingRequest(
        request: AdminBookingDecisionRequest,
    ): AdminBookingDecisionResult {
        return AdminBookingDecisionResult.Failure(AdminError.Backend("unused"))
    }

    override suspend fun upsertServiceCatalogItem(
        request: AdminServiceCatalogMutationRequest,
    ): AdminServiceCatalogMutationResult {
        return AdminServiceCatalogMutationResult.Failure(AdminError.Backend("unused"))
    }

    override suspend fun archiveServiceCatalogItem(
        request: AdminServiceCatalogArchiveRequest,
    ): AdminServiceCatalogMutationResult {
        return AdminServiceCatalogMutationResult.Failure(AdminError.Backend("unused"))
    }

    override suspend fun upsertServiceExtra(
        request: AdminServiceExtraMutationRequest,
    ): AdminServiceExtraMutationResult {
        return AdminServiceExtraMutationResult.Failure(AdminError.Backend("unused"))
    }

    override suspend fun archiveServiceExtra(
        request: AdminServiceExtraArchiveRequest,
    ): AdminServiceExtraMutationResult {
        return AdminServiceExtraMutationResult.Failure(AdminError.Backend("unused"))
    }
}

private class FakeBusinessInfoAuthRepository(
    authenticated: Boolean,
) : AuthRepository {
    private val mutableSessionState = MutableStateFlow(
        if (authenticated) businessInfoAuthenticatedSession() else AuthSessionState.Unauthenticated,
    )
    override val sessionState: StateFlow<AuthSessionState> = mutableSessionState

    override suspend fun currentSession(): AuthSession? {
        return (mutableSessionState.value as? AuthSessionState.Authenticated)?.session
    }

    override fun signOut() {
        mutableSessionState.value = AuthSessionState.Unauthenticated
    }

    fun switchTo(uid: String) {
        mutableSessionState.value = businessInfoAuthenticatedSession(uid)
    }

    override suspend fun signIn(email: String, password: String): AuthResult {
        mutableSessionState.value = businessInfoAuthenticatedSession()
        return AuthResult.Success((mutableSessionState.value as AuthSessionState.Authenticated).session)
    }

    override suspend fun register(
        displayName: String,
        email: String,
        phoneNumber: String,
        password: String,
    ): AuthResult {
        mutableSessionState.value = businessInfoAuthenticatedSession()
        return AuthResult.Success((mutableSessionState.value as AuthSessionState.Authenticated).session)
    }

    override suspend fun sendPasswordReset(email: String): AuthActionResult {
        return AuthActionResult.Success
    }
}

private fun businessInfoAuthenticatedSession(uid: String = "uid-1"): AuthSessionState.Authenticated {
    return AuthSessionState.Authenticated(
        AuthSession(
            user = AuthUser(
                uid = uid,
                email = "admin@example.com",
                displayName = "Admin",
                phoneNumber = "",
            ),
            idToken = "id-token-uid-1",
            refreshToken = "refresh-token-uid-1",
            expiresInSeconds = 3600,
        ),
    )
}

private fun adminBusinessInfoConfig(
    phone: String = "913 005 855",
    updatedAtIso: String = "",
    updatedByUid: String = "",
): AdminBusinessInfoConfig = AdminBusinessInfoConfig(
    phone = phone,
    email = "info@sudsshine.pt",
    addressLine1 = "Shopping Norte Sul",
    addressLine2 = "Leiria, Portugal",
    mapsUri = "https://maps.example.test",
    whatsappUri = "https://wa.me/351913005855",
    openingHours = listOf(
        AdminBusinessOpeningHours(
            dayLabel = "Segunda a Sexta",
            hoursLabel = "09:00 - 19:00",
            closed = false,
        ),
    ),
    socialLinks = listOf(
        AdminBusinessSocialLink(
            label = "Instagram",
            uri = "https://instagram.com/sudsshine",
        ),
    ),
    updatedAtIso = updatedAtIso,
    updatedByUid = updatedByUid,
)
