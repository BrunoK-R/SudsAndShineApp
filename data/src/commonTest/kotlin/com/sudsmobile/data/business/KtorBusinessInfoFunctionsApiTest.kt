package com.sudsmobile.data.business

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

class KtorBusinessInfoFunctionsApiTest {
    @Test
    fun mapsCallableBusinessInfoResponse() = runTest {
        val api = KtorBusinessInfoFunctionsApi(
            httpClient = mockClient(
                """
                {
                  "result": {
                    "phone": "244 000 111",
                    "phoneUri": "tel:244000111",
                    "email": "geral@example.pt",
                    "emailUri": "mailto:geral@example.pt",
                    "addressLine1": "Rua Nova 10",
                    "addressLine2": "Leiria, Portugal",
                    "mapsUri": "https://maps.example.test",
                    "whatsappUri": "https://wa.me/351244000111",
                    "openingHours": [
                      {
                        "dayLabel": "Dias úteis",
                        "hoursLabel": "10:00 - 18:00",
                        "closed": false
                      }
                    ],
                    "faq": [
                      {
                        "question": "Pergunta?",
                        "answer": "Resposta."
                      }
                    ],
                    "stats": [
                      {
                        "value": "900+",
                        "label": "Clientes"
                      }
                    ]
                  }
                }
                """.trimIndent(),
            ),
            config = testConfig(),
        )

        val result = api.getBusinessInfo()

        val success = assertIs<BusinessInfoResult.Success>(result)
        assertEquals("244 000 111", success.info.phone)
        assertEquals("geral@example.pt", success.info.email)
        assertEquals("Rua Nova 10", success.info.addressLine1)
        assertEquals("https://maps.example.test", success.info.mapsUri)
        assertEquals("Dias úteis", success.info.openingHours.single().dayLabel)
        assertEquals("Pergunta?", success.info.faq.single().question)
        assertEquals("900+", success.info.stats.single().value)
    }

    @Test
    fun fallsBackForMissingOptionalBusinessInfoLists() = runTest {
        val api = KtorBusinessInfoFunctionsApi(
            httpClient = mockClient(
                """
                {
                  "result": {
                    "phone": "",
                    "email": "",
                    "addressLine1": ""
                  }
                }
                """.trimIndent(),
            ),
            config = testConfig(),
        )

        val result = api.getBusinessInfo()

        val success = assertIs<BusinessInfoResult.Success>(result)
        assertEquals(DefaultBusinessInfo.phone, success.info.phone)
        assertEquals(DefaultBusinessInfo.email, success.info.email)
        assertEquals(DefaultBusinessInfo.openingHours, success.info.openingHours)
        assertEquals(DefaultBusinessInfo.faq, success.info.faq)
        assertEquals(DefaultBusinessInfo.stats, success.info.stats)
    }

    @Test
    fun mapsCallableBusinessInfoError() = runTest {
        val api = KtorBusinessInfoFunctionsApi(
            httpClient = mockClient(
                """
                {
                  "error": {
                    "status": "UNAVAILABLE",
                    "message": "business info unavailable"
                  }
                }
                """.trimIndent(),
            ),
            config = testConfig(),
        )

        val result = api.getBusinessInfo()

        val failure = assertIs<BusinessInfoResult.Failure>(result)
        assertIs<BusinessInfoError.Unavailable>(failure.error)
        assertEquals("O serviço de contactos está indisponível.", failure.error.message)
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
