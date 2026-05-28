package com.sudsmobile.feature.profile

import com.sudsmobile.data.auth.AuthActionResult
import com.sudsmobile.data.auth.AuthError
import com.sudsmobile.data.auth.AuthRepository
import com.sudsmobile.data.auth.AuthResult
import com.sudsmobile.data.auth.AuthSession
import com.sudsmobile.data.auth.AuthSessionState
import com.sudsmobile.data.auth.AuthUser
import com.sudsmobile.data.vehicle.UserVehicle
import com.sudsmobile.data.vehicle.UserVehicleDeleteResult
import com.sudsmobile.data.vehicle.UserVehicleListResult
import com.sudsmobile.data.vehicle.UserVehicleMutationResult
import com.sudsmobile.data.vehicle.UserVehicleRepository
import com.sudsmobile.data.vehicle.UserVehicleSaveRequest
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
class VehiclesViewModelTest {
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
    fun loadVehiclesRequiresAuthenticatedSession() = runTest {
        val repository = FakeUserVehicleRepository(
            listResult = UserVehicleListResult.Success(emptyList()),
        )
        val viewModel = VehiclesViewModel(
            authRepository = FakeVehiclesAuthRepository(authenticated = false),
            vehicleRepository = repository,
        )

        viewModel.loadVehicles()
        runCurrent()

        assertIs<VehiclesUiState.Unauthenticated>(viewModel.uiState.value)
        assertEquals(0, repository.listCalls)
    }

    @Test
    fun refreshForSessionWaitsWhileSessionIsRestoring() = runTest {
        val repository = FakeUserVehicleRepository(
            listResult = UserVehicleListResult.Success(emptyList()),
        )
        val viewModel = VehiclesViewModel(
            authRepository = FakeVehiclesAuthRepository(
                authenticated = false,
                initialState = AuthSessionState.Restoring,
            ),
            vehicleRepository = repository,
        )

        viewModel.refreshForSession()
        runCurrent()

        assertIs<VehiclesUiState.Loading>(viewModel.uiState.value)
        assertEquals(0, repository.listCalls)
    }

    @Test
    fun refreshForSessionMapsRestoreFailureWithoutVehicleCall() = runTest {
        val repository = FakeUserVehicleRepository(
            listResult = UserVehicleListResult.Success(emptyList()),
        )
        val viewModel = VehiclesViewModel(
            authRepository = FakeVehiclesAuthRepository(
                authenticated = false,
                initialState = AuthSessionState.RestoreFailed(AuthError.Unavailable("Sessão indisponível.")),
            ),
            vehicleRepository = repository,
        )

        viewModel.refreshForSession()
        runCurrent()

        val error = assertIs<VehiclesUiState.Error>(viewModel.uiState.value)
        assertEquals("Sessão indisponível.", error.message)
        assertEquals(true, error.retryable)
        assertEquals(0, repository.listCalls)
    }

    @Test
    fun refreshForSessionLoadsAfterRestoreCompletes() = runTest {
        val authRepository = FakeVehiclesAuthRepository(
            authenticated = false,
            initialState = AuthSessionState.Restoring,
        )
        val repository = FakeUserVehicleRepository(
            listResult = UserVehicleListResult.Success(
                listOf(userVehicle(id = "vehicle-1", brand = "BMW")),
            ),
        )
        val viewModel = VehiclesViewModel(
            authRepository = authRepository,
            vehicleRepository = repository,
        )

        viewModel.refreshForSession()
        runCurrent()

        assertIs<VehiclesUiState.Loading>(viewModel.uiState.value)
        assertEquals(0, repository.listCalls)

        authRepository.authenticate(uid = "uid-1")
        viewModel.refreshForSession()
        runCurrent()

        assertIs<VehiclesUiState.Loaded>(viewModel.uiState.value)
        assertEquals(1, repository.listCalls)
    }

    @Test
    fun loadVehiclesKeepsRestoringStateWhenSessionChangesDuringListLoad() = runTest {
        val repository = DeferredUserVehicleRepository()
        val authRepository = FakeVehiclesAuthRepository(authenticated = true)
        val viewModel = VehiclesViewModel(
            authRepository = authRepository,
            vehicleRepository = repository,
        )

        viewModel.loadVehicles()
        runCurrent()
        authRepository.setSessionState(AuthSessionState.Restoring)
        repository.result.complete(
            UserVehicleListResult.Success(listOf(userVehicle(id = "vehicle-1", brand = "BMW"))),
        )
        runCurrent()

        assertIs<VehiclesUiState.Loading>(viewModel.uiState.value)
        assertEquals(1, repository.listCalls)
    }

    @Test
    fun loadVehiclesMapsRestoreFailureWhenSessionChangesDuringListLoad() = runTest {
        val repository = DeferredUserVehicleRepository()
        val authRepository = FakeVehiclesAuthRepository(authenticated = true)
        val viewModel = VehiclesViewModel(
            authRepository = authRepository,
            vehicleRepository = repository,
        )

        viewModel.loadVehicles()
        runCurrent()
        authRepository.setSessionState(
            AuthSessionState.RestoreFailed(AuthError.Backend("Falha ao validar sessão.")),
        )
        repository.result.complete(
            UserVehicleListResult.Success(listOf(userVehicle(id = "vehicle-1", brand = "BMW"))),
        )
        runCurrent()

        val error = assertIs<VehiclesUiState.Error>(viewModel.uiState.value)
        assertEquals("Falha ao validar sessão.", error.message)
        assertEquals(true, error.retryable)
        assertEquals(1, repository.listCalls)
    }

    @Test
    fun loadVehiclesMapsBackendListToLoadedState() = runTest {
        val viewModel = VehiclesViewModel(
            authRepository = FakeVehiclesAuthRepository(authenticated = true),
            vehicleRepository = FakeUserVehicleRepository(
                listResult = UserVehicleListResult.Success(
                    listOf(userVehicle(id = "vehicle-1", brand = "BMW", isDefault = true)),
                ),
            ),
        )

        viewModel.loadVehicles()
        runCurrent()

        val loaded = assertIs<VehiclesUiState.Loaded>(viewModel.uiState.value)
        assertEquals("vehicle-1", loaded.vehicles.single().id)
        assertEquals(VehicleTypeUi.Passenger, loaded.vehicles.single().type)
        assertEquals(true, loaded.vehicles.single().isDefault)
    }

    @Test
    fun saveVehicleValidatesDraftBeforeCallingRepository() = runTest {
        val repository = FakeUserVehicleRepository(
            listResult = UserVehicleListResult.Success(emptyList()),
        )
        val viewModel = VehiclesViewModel(
            authRepository = FakeVehiclesAuthRepository(authenticated = true),
            vehicleRepository = repository,
        )

        viewModel.saveVehicle(VehicleDraftUi(brand = "", model = "320d", plate = "AA-00-BB"))
        runCurrent()

        assertIs<VehicleMutationUiState.ValidationError>(viewModel.mutationState.value)
        assertEquals(0, repository.createCalls)
    }

    @Test
    fun saveVehicleWaitsForAuthenticatedSessionBeforeRepositoryCall() = runTest {
        val repository = FakeUserVehicleRepository(
            listResult = UserVehicleListResult.Success(emptyList()),
        )
        val viewModel = VehiclesViewModel(
            authRepository = FakeVehiclesAuthRepository(
                authenticated = false,
                initialState = AuthSessionState.Restoring,
            ),
            vehicleRepository = repository,
        )

        viewModel.saveVehicle(
            VehicleDraftUi(
                brand = "BMW",
                model = "320d",
                plate = "AA-00-BB",
            ),
        )
        runCurrent()

        assertIs<VehiclesUiState.Loading>(viewModel.uiState.value)
        assertIs<VehicleMutationUiState.Idle>(viewModel.mutationState.value)
        assertEquals(0, repository.createCalls)
    }

    @Test
    fun saveVehicleDiscardsResultWhenSessionChangesDuringMutation() = runTest {
        val repository = DeferredMutationUserVehicleRepository()
        val authRepository = FakeVehiclesAuthRepository(authenticated = true)
        val viewModel = VehiclesViewModel(
            authRepository = authRepository,
            vehicleRepository = repository,
        )

        viewModel.saveVehicle(
            VehicleDraftUi(
                brand = "BMW",
                model = "320d",
                plate = "AA-00-BB",
            ),
        )
        runCurrent()
        authRepository.signOut()
        repository.mutationResult.complete(
            UserVehicleMutationResult.Success(userVehicle(id = "vehicle-1", brand = "BMW")),
        )
        runCurrent()

        assertIs<VehiclesUiState.Unauthenticated>(viewModel.uiState.value)
        assertIs<VehicleMutationUiState.Idle>(viewModel.mutationState.value)
        assertEquals(1, repository.createCalls)
    }

    @Test
    fun saveVehiclePublishesSavedVehicle() = runTest {
        val repository = FakeUserVehicleRepository(
            listResult = UserVehicleListResult.Success(emptyList()),
            mutationResult = UserVehicleMutationResult.Success(userVehicle(id = "vehicle-1", brand = "BMW")),
        )
        val viewModel = VehiclesViewModel(
            authRepository = FakeVehiclesAuthRepository(authenticated = true),
            vehicleRepository = repository,
        )

        viewModel.saveVehicle(
            VehicleDraftUi(
                brand = "BMW",
                model = "320d",
                plate = "AA-00-BB",
                color = "Preto",
                type = VehicleTypeUi.Passenger,
            ),
        )
        runCurrent()

        assertIs<VehicleMutationUiState.Success>(viewModel.mutationState.value)
        val loaded = assertIs<VehiclesUiState.Loaded>(viewModel.uiState.value)
        assertEquals("vehicle-1", loaded.vehicles.single().id)
    }

    @Test
    fun deleteVehicleRemovesVehicleFromLoadedState() = runTest {
        val repository = FakeUserVehicleRepository(
            listResult = UserVehicleListResult.Success(emptyList()),
            deleteResult = UserVehicleDeleteResult.Success,
        )
        val viewModel = VehiclesViewModel(
            authRepository = FakeVehiclesAuthRepository(authenticated = true),
            vehicleRepository = repository,
        )

        viewModel.saveVehicle(
            VehicleDraftUi(
                brand = "BMW",
                model = "320d",
                plate = "AA-00-BB",
            ),
        )
        runCurrent()
        viewModel.deleteVehicle("vehicle-1")
        runCurrent()

        assertIs<VehicleMutationUiState.Success>(viewModel.mutationState.value)
        assertIs<VehiclesUiState.Empty>(viewModel.uiState.value)
    }

    @Test
    fun setDefaultVehiclePromotesSelectedVehicleAndClearsPreviousDefault() = runTest {
        val repository = FakeUserVehicleRepository(
            listResult = UserVehicleListResult.Success(
                listOf(
                    userVehicle(id = "vehicle-1", brand = "Audi", isDefault = true),
                    userVehicle(id = "vehicle-2", brand = "BMW"),
                ),
            ),
            mutationResult = UserVehicleMutationResult.Success(
                userVehicle(id = "vehicle-2", brand = "BMW", isDefault = true),
            ),
        )
        val viewModel = VehiclesViewModel(
            authRepository = FakeVehiclesAuthRepository(authenticated = true),
            vehicleRepository = repository,
        )

        viewModel.loadVehicles()
        runCurrent()
        val loaded = assertIs<VehiclesUiState.Loaded>(viewModel.uiState.value)

        viewModel.setDefaultVehicle(loaded.vehicles.first { it.id == "vehicle-2" })
        runCurrent()

        val updated = assertIs<VehiclesUiState.Loaded>(viewModel.uiState.value)
        assertEquals("vehicle-2", updated.vehicles.first().id)
        assertEquals(true, updated.vehicles.first().isDefault)
        assertEquals(false, updated.vehicles.first { it.id == "vehicle-1" }.isDefault)
        assertIs<VehicleMutationUiState.Success>(viewModel.mutationState.value)
    }

    @Test
    fun refreshForSessionLoadsAfterSignInAndClearsAfterSignOut() = runTest {
        val authRepository = FakeVehiclesAuthRepository(authenticated = false)
        val repository = FakeUserVehicleRepository(
            listResult = UserVehicleListResult.Success(
                listOf(userVehicle(id = "vehicle-1", brand = "BMW")),
            ),
        )
        val viewModel = VehiclesViewModel(
            authRepository = authRepository,
            vehicleRepository = repository,
        )

        viewModel.refreshForSession()
        runCurrent()

        assertIs<VehiclesUiState.Unauthenticated>(viewModel.uiState.value)
        assertEquals(0, repository.listCalls)

        authRepository.authenticate(uid = "uid-1")
        viewModel.refreshForSession()
        runCurrent()

        assertIs<VehiclesUiState.Loaded>(viewModel.uiState.value)
        assertEquals(1, repository.listCalls)

        authRepository.signOut()
        viewModel.refreshForSession()
        runCurrent()

        assertIs<VehiclesUiState.Unauthenticated>(viewModel.uiState.value)
    }
}

private class FakeUserVehicleRepository(
    private val listResult: UserVehicleListResult,
    private val mutationResult: UserVehicleMutationResult = UserVehicleMutationResult.Success(
        userVehicle(id = "vehicle-1", brand = "BMW"),
    ),
    private val deleteResult: UserVehicleDeleteResult = UserVehicleDeleteResult.Success,
) : UserVehicleRepository {
    var listCalls: Int = 0
        private set
    var createCalls: Int = 0
        private set

    override suspend fun getMyVehicles(): UserVehicleListResult {
        listCalls += 1
        return listResult
    }

    override suspend fun createVehicle(request: UserVehicleSaveRequest): UserVehicleMutationResult {
        createCalls += 1
        return mutationResult
    }

    override suspend fun updateVehicle(request: UserVehicleSaveRequest): UserVehicleMutationResult = mutationResult

    override suspend fun deleteVehicle(vehicleId: String): UserVehicleDeleteResult = deleteResult
}

private class DeferredUserVehicleRepository : UserVehicleRepository {
    val result = CompletableDeferred<UserVehicleListResult>()
    var listCalls: Int = 0
        private set

    override suspend fun getMyVehicles(): UserVehicleListResult {
        listCalls += 1
        return result.await()
    }

    override suspend fun createVehicle(request: UserVehicleSaveRequest): UserVehicleMutationResult {
        error("Not used")
    }

    override suspend fun updateVehicle(request: UserVehicleSaveRequest): UserVehicleMutationResult {
        error("Not used")
    }

    override suspend fun deleteVehicle(vehicleId: String): UserVehicleDeleteResult {
        error("Not used")
    }
}

private class DeferredMutationUserVehicleRepository : UserVehicleRepository {
    val mutationResult = CompletableDeferred<UserVehicleMutationResult>()
    var createCalls: Int = 0
        private set

    override suspend fun getMyVehicles(): UserVehicleListResult {
        error("Not used")
    }

    override suspend fun createVehicle(request: UserVehicleSaveRequest): UserVehicleMutationResult {
        createCalls += 1
        return mutationResult.await()
    }

    override suspend fun updateVehicle(request: UserVehicleSaveRequest): UserVehicleMutationResult {
        error("Not used")
    }

    override suspend fun deleteVehicle(vehicleId: String): UserVehicleDeleteResult {
        error("Not used")
    }
}

private class FakeVehiclesAuthRepository(
    authenticated: Boolean,
    initialState: AuthSessionState? = null,
) : AuthRepository {
    private val mutableSessionState = MutableStateFlow(
        initialState ?: if (authenticated) {
            authenticatedSession()
        } else {
            AuthSessionState.Unauthenticated
        },
    )
    override val sessionState: StateFlow<AuthSessionState> = mutableSessionState

    override suspend fun currentSession(): AuthSession? {
        return (mutableSessionState.value as? AuthSessionState.Authenticated)?.session
    }

    fun authenticate(uid: String = "uid-1") {
        mutableSessionState.value = authenticatedSession(uid)
    }

    fun setSessionState(state: AuthSessionState) {
        mutableSessionState.value = state
    }

    override suspend fun signIn(email: String, password: String): AuthResult {
        authenticate()
        return AuthResult.Success((mutableSessionState.value as AuthSessionState.Authenticated).session)
    }

    override suspend fun register(
        displayName: String,
        email: String,
        phoneNumber: String,
        password: String,
    ): AuthResult {
        authenticate()
        return AuthResult.Success((mutableSessionState.value as AuthSessionState.Authenticated).session)
    }

    override suspend fun sendPasswordReset(email: String): AuthActionResult {
        return AuthActionResult.Success
    }

    override fun signOut() {
        mutableSessionState.value = AuthSessionState.Unauthenticated
    }
}

private fun authenticatedSession(uid: String = "uid-1"): AuthSessionState.Authenticated {
    return AuthSessionState.Authenticated(
        AuthSession(
            user = AuthUser(
                uid = uid,
                email = "bruno@example.com",
                displayName = "Bruno",
                phoneNumber = "",
            ),
            idToken = "id-token-$uid",
            refreshToken = "refresh-token-$uid",
            expiresInSeconds = 3600,
        ),
    )
}

private fun userVehicle(
    id: String,
    brand: String,
    model: String = "320d",
    plate: String = "AA-00-BB",
    type: String = "passenger",
    isDefault: Boolean = false,
): UserVehicle = UserVehicle(
    id = id,
    brand = brand,
    model = model,
    plate = plate,
    color = "Preto",
    type = type,
    isDefault = isDefault,
)
