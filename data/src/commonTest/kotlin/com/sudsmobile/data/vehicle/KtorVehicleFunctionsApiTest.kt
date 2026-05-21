package com.sudsmobile.data.vehicle

import com.sudsmobile.data.booking.FirebaseFunctionsConfig
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestData
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.fullPath
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json

class KtorVehicleFunctionsApiTest {
    @Test
    fun mapsVehicleListResponse() = runTest {
        var requestedPath: String? = null
        val api = KtorVehicleFunctionsApi(
            httpClient = mockClient(
                """
                {
                  "result": {
                    "vehicles": [
                      {
                        "id": "vehicle-1",
                        "brand": "BMW",
                        "model": "320d",
                        "plate": "AA-00-BB",
                        "color": "Preto",
                        "type": "passenger",
                        "isDefault": true
                      }
                    ]
                  }
                }
                """.trimIndent(),
            ) { request ->
                requestedPath = request.url.fullPath
            },
            config = testConfig(),
        )

        val result = api.getMyVehicles("id-token-1")

        val success = assertIs<UserVehicleListResult.Success>(result)
        assertEquals("/test-project/europe-west1/getMyVehicles", requestedPath)
        assertEquals("vehicle-1", success.vehicles.first().id)
        assertEquals("BMW", success.vehicles.first().brand)
        assertEquals(true, success.vehicles.first().isDefault)
    }

    @Test
    fun sendsAuthorizationHeaderWhenCreatingVehicle() = runTest {
        var authorizationHeader: String? = null
        val api = KtorVehicleFunctionsApi(
            httpClient = mockClient(
                """
                {
                  "result": {
                    "vehicle": {
                      "id": "vehicle-1",
                      "brand": "BMW",
                      "model": "320d",
                      "plate": "AA-00-BB",
                      "color": "Preto",
                      "type": "passenger",
                      "isDefault": true
                    }
                  }
                }
                """.trimIndent(),
            ) { request ->
                authorizationHeader = request.headers[HttpHeaders.Authorization]
            },
            config = testConfig(),
        )

        val result = api.createVehicle(validSaveRequest(), idToken = "id-token-1")

        assertIs<UserVehicleMutationResult.Success>(result)
        assertEquals("Bearer id-token-1", authorizationHeader)
        val success = assertIs<UserVehicleMutationResult.Success>(result)
        assertEquals(true, success.vehicle.isDefault)
    }

    @Test
    fun mapsCallableValidationError() = runTest {
        val api = KtorVehicleFunctionsApi(
            httpClient = mockClient(
                """
                {
                  "error": {
                    "status": "INVALID_ARGUMENT",
                    "message": "Vehicle brand is required"
                  }
                }
                """.trimIndent(),
            ),
            config = testConfig(),
        )

        val result = api.updateVehicle(validSaveRequest(), idToken = "id-token-1")

        val failure = assertIs<UserVehicleMutationResult.Failure>(result)
        assertIs<UserVehicleError.Validation>(failure.error)
        assertEquals("Vehicle brand is required", failure.error.message)
    }

    @Test
    fun mapsDeleteSuccessResponse() = runTest {
        val api = KtorVehicleFunctionsApi(
            httpClient = mockClient(
                """
                {
                  "result": {
                    "ok": true,
                    "vehicleId": "vehicle-1"
                  }
                }
                """.trimIndent(),
            ),
            config = testConfig(),
        )

        val result = api.deleteVehicle("vehicle-1", idToken = "id-token-1")

        assertIs<UserVehicleDeleteResult.Success>(result)
    }
}

private fun mockClient(
    responseJson: String,
    onRequest: (HttpRequestData) -> Unit = {},
): HttpClient {
    val engine = MockEngine {
        onRequest(it)
        respond(
            content = responseJson,
            status = HttpStatusCode.OK,
            headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
        )
    }
    return HttpClient(engine) {
        expectSuccess = false
        install(ContentNegotiation) {
            json(
                Json {
                    ignoreUnknownKeys = true
                    explicitNulls = false
                },
            )
        }
    }
}

private fun testConfig(): FirebaseFunctionsConfig = FirebaseFunctionsConfig(
    projectId = "test-project",
    region = "europe-west1",
    useEmulator = true,
    emulatorHost = "127.0.0.1",
)

private fun validSaveRequest(): UserVehicleSaveRequest = UserVehicleSaveRequest(
    id = "vehicle-1",
    brand = "BMW",
    model = "320d",
    plate = "AA-00-BB",
    color = "Preto",
    type = "passenger",
    isDefault = true,
)
