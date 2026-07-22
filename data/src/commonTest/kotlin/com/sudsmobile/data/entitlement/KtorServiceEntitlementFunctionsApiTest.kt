package com.sudsmobile.data.entitlement

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

class KtorServiceEntitlementFunctionsApiTest {
    @Test
    fun mapsCustomerPlansAndKeepsOnlinePurchaseDisabled() = runTest {
        val api = KtorServiceEntitlementFunctionsApi(
            entitlementClient(
                """
                {
                  "result": {
                    "purchaseMode": "staff_issued",
                    "onlinePurchaseAvailable": false,
                    "entitlements": [{
                      "id": "issue-1",
                      "code": "SS-PLAN-ABC123",
                      "kind": "package",
                      "name": "Pacote 5",
                      "status": "active",
                      "totalUses": 5,
                      "usedUses": 2,
                      "remainingUses": 3,
                      "eligibleServiceIds": ["standard"],
                      "eligibleServiceNames": ["Lavagem Standard"],
                      "validFrom": "2026-07-01T00:00:00.000Z",
                      "validUntil": "2026-12-31T00:00:00.000Z",
                      "amountPaidCents": 10000,
                      "purchaseMode": "staff_issued",
                      "onlinePurchaseAvailable": false
                    }]
                  }
                }
                """.trimIndent(),
            ),
            entitlementTestConfig(),
        )

        val success = assertIs<ServiceEntitlementListResult.Success>(api.getMyEntitlements("token"))

        assertEquals(false, success.value.onlinePurchaseAvailable)
        assertEquals(3, success.value.entitlements.single().remainingUses)
        assertEquals("Lavagem Standard", success.value.entitlements.single().eligibleServiceNames.single())
    }

    @Test
    fun mapsAdminMutationAndUnavailablePlanError() = runTest {
        val successApi = KtorServiceEntitlementFunctionsApi(
            entitlementClient(
                """
                {"result":{"ok":true,"entitlement":{
                  "id":"issue-1","code":"SS-PLAN-1","kind":"membership","name":"Plano mensal",
                  "status":"active","totalUses":4,"usedUses":1,"remainingUses":3,
                  "validFrom":"2026-07-01T00:00:00.000Z","validUntil":"2026-08-01T00:00:00.000Z"
                }}}
                """.trimIndent(),
            ),
            entitlementTestConfig(),
        )
        val mutation = successApi.adjustUsage(
            AdjustServiceEntitlementUsageRequest(
                operationId = "usage-12345678",
                customerEmail = "client@example.com",
                entitlementId = "issue-1",
                deltaUses = 1,
                reservationCode = "SS-ONE",
                staffNote = "Usado ao balcão",
            ),
            "token",
        )
        assertEquals(3, assertIs<ServiceEntitlementMutationResult.Success>(mutation).entitlement.remainingUses)

        val errorApi = KtorServiceEntitlementFunctionsApi(
            entitlementClient("""{"error":{"status":"FAILED_PRECONDITION","message":"Plan expired"}}"""),
            entitlementTestConfig(),
        )
        val failure = assertIs<ServiceEntitlementMutationResult.Failure>(
            errorApi.adjustUsage(
                AdjustServiceEntitlementUsageRequest(
                    "usage-12345678",
                    "client@example.com",
                    "issue-1",
                    1,
                    "",
                    "Uso",
                ),
                "token",
            ),
        )
        assertIs<ServiceEntitlementError.NotEligible>(failure.error)
    }
}

private fun entitlementClient(json: String): HttpClient = HttpClient(MockEngine {
    respond(
        json,
        HttpStatusCode.OK,
        headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
    )
}) {
    expectSuccess = false
    install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true; explicitNulls = false }) }
}

private fun entitlementTestConfig() = FirebaseFunctionsConfig(
    projectId = "test-project",
    region = "europe-west1",
    useEmulator = true,
    emulatorHost = "127.0.0.1",
)
