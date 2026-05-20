package com.sudsmobile.feature.profile

import com.sudsmobile.data.auth.AuthActionResult
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
    fun loadVehiclesMapsBackendListToLoadedState() = runTest {
        val viewModel = VehiclesViewModel(
            authRepository = FakeVehiclesAuthRepository(authenticated = true),
            vehicleRepository = FakeUserVehicleRepository(
                listResult = UserVehicleListResult.Success(
                    listOf(userVehicle(id = "vehicle-1", brand = "BMW")),
                ),
            ),
        )

        viewModel.loadVehicles()
        runCurrent()

        val loaded = assertIs<VehiclesUiState.Loaded>(viewModel.uiState.value)
        assertEquals("vehicle-1", loaded.vehicles.single().id)
        assertEquals(VehicleTypeUi.Passenger, loaded.vehicles.single().type)
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

private class FakeVehiclesAuthRepository(
    authenticated: Boolean,
) : AuthRepository {
    override val sessionState: StateFlow<AuthSessionState> = MutableStateFlow(
        if (authenticated) {
            AuthSessionState.Authenticated(
                AuthSession(
                    user = AuthUser(
                        uid = "uid-1",
                        email = "bruno@example.com",
                        displayName = "Bruno",
                        phoneNumber = "",
                    ),
                    idToken = "id-token-1",
                    refreshToken = "refresh-token-1",
                    expiresInSeconds = 3600,
                ),
            )
        } else {
            AuthSessionState.Unauthenticated
        },
    )

    override suspend fun signIn(email: String, password: String): AuthResult {
        error("Not used")
    }

    override suspend fun register(
        displayName: String,
        email: String,
        phoneNumber: String,
        password: String,
    ): AuthResult {
        error("Not used")
    }

    override suspend fun sendPasswordReset(email: String): AuthActionResult {
        error("Not used")
    }

    override fun signOut() = Unit
}

private fun userVehicle(
    id: String,
    brand: String,
    model: String = "320d",
    plate: String = "AA-00-BB",
    type: String = "passenger",
): UserVehicle = UserVehicle(
    id = id,
    brand = brand,
    model = model,
    plate = plate,
    color = "Preto",
    type = type,
)
