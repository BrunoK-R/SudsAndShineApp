package com.sudsmobile.data.catalog

import com.sudsmobile.data.booking.FirebaseFunctionsConfig
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json

class KtorCatalogFunctionsApiTest {
    @Test
    fun mapsCallableServiceCatalogResponse() = runTest {
        val api = KtorCatalogFunctionsApi(
            httpClient = mockClient(
                """
                {
                  "result": {
                    "services": [
                      {
                        "id": "premium",
                        "name": "Lavagem Premium",
                        "description": "Lavagem detalhada",
                        "durationMinutes": 45,
                        "passengerPriceCents": 3200,
                        "suvPriceCents": 3400,
                        "iconKey": "sparkles",
                        "popular": true
                      }
                    ]
                  }
                }
                """.trimIndent(),
            ),
            config = testConfig(),
        )

        val result = api.getServiceCatalog()

        val success = assertIs<ServiceCatalogResult.Success>(result)
        val service = success.catalog.services.single()
        assertEquals("premium", service.id)
        assertEquals("Lavagem Premium", service.name)
        assertEquals(45, service.durationMinutes)
        assertEquals(3200, service.passengerPriceCents)
        assertEquals(3400, service.suvPriceCents)
        assertEquals("sparkles", service.iconKey)
        assertEquals(true, service.popular)
    }

    @Test
    fun mapsCallableServiceCatalogError() = runTest {
        val api = KtorCatalogFunctionsApi(
            httpClient = mockClient(
                """
                {
                  "error": {
                    "status": "UNAVAILABLE",
                    "message": "catalog unavailable"
                  }
                }
                """.trimIndent(),
            ),
            config = testConfig(),
        )

        val result = api.getServiceCatalog()

        val failure = assertIs<ServiceCatalogResult.Failure>(result)
        assertIs<ServiceCatalogError.Unavailable>(failure.error)
        assertEquals("O serviço de catálogo está indisponível.", failure.error.message)
    }
}

private fun mockClient(responseJson: String): HttpClient {
    val engine = MockEngine {
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
