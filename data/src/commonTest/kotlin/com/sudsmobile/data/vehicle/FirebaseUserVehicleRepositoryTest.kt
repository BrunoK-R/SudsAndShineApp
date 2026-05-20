package com.sudsmobile.data.vehicle

import com.sudsmobile.data.auth.AuthActionResult
import com.sudsmobile.data.auth.AuthRepository
import com.sudsmobile.data.auth.AuthResult
import com.sudsmobile.data.auth.AuthSession
import com.sudsmobile.data.auth.AuthSessionState
import com.sudsmobile.data.auth.AuthUser
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runTest

class FirebaseUserVehicleRepositoryTest {
    @Test
    fun rejectsListWhenUnauthenticatedBeforeCallingApi() = runTest {
        val api = RecordingVehicleFunctionsApi()
        val repository = FirebaseUserVehicleRepository(api, FakeAuthRepository(authenticated = false))

        val result = repository.getMyVehicles()

        assertIs<UserVehicleListResult.Failure>(result)
        assertIs<UserVehicleError.Unauthenticated>(result.error)
        assertEquals(0, api.listCalls)
    }

    @Test
    fun normalizesCreateRequestAndPassesIdToken() = runTest {
        val api = RecordingVehicleFunctionsApi()
        val repository = FirebaseUserVehicleRepository(api, FakeAuthRepository(authenticated = true))

        val result = repository.createVehicle(
            UserVehicleSaveRequest(
                brand = "  BMW  ",
                model = "  320d  ",
                plate = " aa-00-bb ",
                color = " Preto ",
                type = "passageiros",
            ),
        )

        assertIs<UserVehicleMutationResult.Success>(result)
        assertEquals("id-token-1", api.lastIdToken)
        assertEquals("BMW", api.lastSaveRequest?.brand)
        assertEquals("AA-00-BB", api.lastSaveRequest?.plate)
        assertEquals("passenger", api.lastSaveRequest?.type)
    }

    @Test
    fun rejectsUpdateWithoutVehicleIdBeforeCallingApi() = runTest {
        val api = RecordingVehicleFunctionsApi()
        val repository = FirebaseUserVehicleRepository(api, FakeAuthRepository(authenticated = true))

        val result = repository.updateVehicle(validSaveRequest().copy(id = null))

        assertIs<UserVehicleMutationResult.Failure>(result)
        assertIs<UserVehicleError.Validation>(result.error)
        assertEquals(0, api.updateCalls)
    }

    @Test
    fun rejectsBlankPlateBeforeCallingApi() = runTest {
        val api = RecordingVehicleFunctionsApi()
        val repository = FirebaseUserVehicleRepository(api, FakeAuthRepository(authenticated = true))

        val result = repository.createVehicle(validSaveRequest().copy(plate = " "))

        assertIs<UserVehicleMutationResult.Failure>(result)
        assertIs<UserVehicleError.Validation>(result.error)
        assertEquals(0, api.createCalls)
    }

    @Test
    fun deletesWithAuthenticatedIdToken() = runTest {
        val api = RecordingVehicleFunctionsApi()
        val repository = FirebaseUserVehicleRepository(api, FakeAuthRepository(authenticated = true))

        val result = repository.deleteVehicle(" vehicle-1 ")

        assertIs<UserVehicleDeleteResult.Success>(result)
        assertEquals("vehicle-1", api.lastDeleteVehicleId)
        assertEquals("id-token-1", api.lastIdToken)
    }
}

private class RecordingVehicleFunctionsApi : VehicleFunctionsApi {
    var listCalls: Int = 0
        private set
    var createCalls: Int = 0
        private set
    var updateCalls: Int = 0
        private set
    var lastSaveRequest: UserVehicleSaveRequest? = null
        private set
    var lastDeleteVehicleId: String? = null
        private set
    var lastIdToken: String? = null
        private set

    override suspend fun getMyVehicles(idToken: String): UserVehicleListResult {
        listCalls += 1
        lastIdToken = idToken
        return UserVehicleListResult.Success(emptyList())
    }

    override suspend fun createVehicle(
        request: UserVehicleSaveRequest,
        idToken: String,
    ): UserVehicleMutationResult {
        createCalls += 1
        lastSaveRequest = request
        lastIdToken = idToken
        return UserVehicleMutationResult.Success(request.toVehicle(id = "vehicle-1"))
    }

    override suspend fun updateVehicle(
        request: UserVehicleSaveRequest,
        idToken: String,
    ): UserVehicleMutationResult {
        updateCalls += 1
        lastSaveRequest = request
        lastIdToken = idToken
        return UserVehicleMutationResult.Success(request.toVehicle(id = request.id.orEmpty()))
    }

    override suspend fun deleteVehicle(vehicleId: String, idToken: String): UserVehicleDeleteResult {
        lastDeleteVehicleId = vehicleId
        lastIdToken = idToken
        return UserVehicleDeleteResult.Success
    }
}

private class FakeAuthRepository(
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

    override suspend fun currentSession(): AuthSession? {
        return (sessionState.value as? AuthSessionState.Authenticated)?.session
    }

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

private fun validSaveRequest(): UserVehicleSaveRequest = UserVehicleSaveRequest(
    id = "vehicle-1",
    brand = "BMW",
    model = "320d",
    plate = "AA-00-BB",
    color = "Preto",
    type = "passenger",
)

private fun UserVehicleSaveRequest.toVehicle(id: String): UserVehicle = UserVehicle(
    id = id,
    brand = brand,
    model = model,
    plate = plate,
    color = color,
    type = type,
)
