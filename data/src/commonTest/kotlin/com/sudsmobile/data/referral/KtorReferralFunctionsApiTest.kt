package com.sudsmobile.data.referral

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

class KtorReferralFunctionsApiTest {
    @Test
    fun mapsReferralProgramAndPrivacySafeStats() = runTest {
        val api = KtorReferralFunctionsApi(
            httpClient = referralMockClient(
                """
                {
                  "result": {
                    "code": "SUDS-ABCD123456",
                    "shareMessage": "Partilhe SUDS-ABCD123456",
                    "rewardPoints": 1,
                    "attributionDays": 30,
                    "canClaimCode": false,
                    "claimIneligibleReason": "account_too_old",
                    "referredBy": null,
                    "stats": {
                      "claimedCount": 2,
                      "qualifiedCount": 1,
                      "pendingCount": 1,
                      "bonusPointsEarned": 1
                    },
                    "invitations": [
                      {
                        "id": "anonymous-invite-1",
                        "status": "qualified",
                        "claimedAt": "2026-07-01T09:00:00.000Z",
                        "qualifiedAt": "2026-07-05T09:00:00.000Z"
                      }
                    ]
                  }
                }
                """.trimIndent(),
            ),
            config = referralTestConfig(),
        )

        val result = api.getMyReferral("id-token-1")

        val success = assertIs<ReferralProgramResult.Success>(result)
        assertEquals("SUDS-ABCD123456", success.program.code)
        assertEquals(2, success.program.stats.claimedCount)
        assertEquals(1, success.program.stats.qualifiedCount)
        assertEquals(false, success.program.canClaimCode)
        assertEquals("account_too_old", success.program.claimIneligibleReason)
        assertEquals("qualified", success.program.invitations.single().status)
    }

    @Test
    fun mapsClaimResponseAndEligibilityError() = runTest {
        val successApi = KtorReferralFunctionsApi(
            httpClient = referralMockClient(
                """
                {
                  "result": {
                    "ok": true,
                    "referral": {
                      "code": "SUDS-AABBCCDDEE",
                      "shareMessage": "Convite",
                      "referredBy": {
                        "code": "SUDS-ABCD123456",
                        "status": "claimed",
                        "claimedAt": "2026-07-22T09:00:00.000Z"
                      }
                    }
                  }
                }
                """.trimIndent(),
            ),
            config = referralTestConfig(),
        )
        val success = assertIs<ReferralProgramResult.Success>(
            successApi.claimMyReferralCode("SUDS-ABCD123456", "id-token-1"),
        )
        assertEquals("SUDS-ABCD123456", success.program.referredBy?.code)

        val errorApi = KtorReferralFunctionsApi(
            httpClient = referralMockClient(
                """
                {
                  "error": {
                    "status": "FAILED_PRECONDITION",
                    "message": "Referral is no longer eligible"
                  }
                }
                """.trimIndent(),
            ),
            config = referralTestConfig(),
        )
        val failure = assertIs<ReferralProgramResult.Failure>(
            errorApi.claimMyReferralCode("SUDS-ABCD123456", "id-token-1"),
        )
        val notEligible = assertIs<ReferralError.NotEligible>(failure.error)
        assertEquals(true, notEligible.message.contains("30 dias"))
    }
}

private fun referralMockClient(responseJson: String): HttpClient {
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
            json(Json { ignoreUnknownKeys = true; explicitNulls = false })
        }
    }
}

private fun referralTestConfig(): FirebaseFunctionsConfig = FirebaseFunctionsConfig(
    projectId = "test-project",
    region = "europe-west1",
    useEmulator = true,
    emulatorHost = "127.0.0.1",
)
