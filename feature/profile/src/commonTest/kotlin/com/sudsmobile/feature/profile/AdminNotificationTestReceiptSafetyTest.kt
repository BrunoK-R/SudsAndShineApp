package com.sudsmobile.feature.profile

import com.sudsmobile.data.admin.AdminNotificationTestReceipt
import kotlin.test.Test
import kotlin.test.assertEquals

class AdminNotificationTestReceiptSafetyTest {
    @Test
    fun acceptsOnlyCurrentAdminSelfTestReceipt() {
        val receipt = testReceipt(
            recipientUid = "admin-1",
            targetScope = "self",
            testOnly = true,
        )

        assertEquals(true, receipt.isCurrentAdminSelfTest("admin-1"))
    }

    @Test
    fun rejectsAudienceScopeEvenWhenMarkedTestOnly() {
        val receipt = testReceipt(
            recipientUid = "admin-1",
            targetScope = "audience",
            testOnly = true,
        )

        assertEquals(false, receipt.isCurrentAdminSelfTest("admin-1"))
    }

    @Test
    fun rejectsSelfScopeWithoutTestOnlyFlag() {
        val receipt = testReceipt(
            recipientUid = "admin-1",
            targetScope = "self",
            testOnly = false,
        )

        assertEquals(false, receipt.isCurrentAdminSelfTest("admin-1"))
    }

    @Test
    fun rejectsDifferentRecipient() {
        val receipt = testReceipt(
            recipientUid = "other-admin",
            targetScope = "self",
            testOnly = true,
        )

        assertEquals(false, receipt.isCurrentAdminSelfTest("admin-1"))
    }

    @Test
    fun rejectsBlankRecipientAndRequestedUid() {
        val receipt = testReceipt(
            recipientUid = "",
            targetScope = "self",
            testOnly = true,
        )

        assertEquals(false, receipt.isCurrentAdminSelfTest(""))
    }

    @Test
    fun campaignQueuedMessageIncludesSendLockAndConsentMetadata() {
        val receipt = testReceipt(
            recipientUid = "admin-1",
            targetScope = "self",
            testOnly = true,
            campaignId = "summer-test",
            targetAudience = "marketing_opt_in_users",
            marketingConsentRequired = true,
            sendBlockedReason = "campaign-send-not-implemented",
            sendState = "draft_only",
        )

        assertEquals(
            "Teste de campanha em fila apenas para o administrador atual. " +
                "Envio real bloqueado (draft_only): campaign-send-not-implemented. " +
                "Público requer opt-in marketing.",
            receipt.toSelfTestQueuedMessage("Teste de campanha"),
        )
    }

    private fun testReceipt(
        recipientUid: String,
        targetScope: String,
        testOnly: Boolean,
        campaignId: String = "",
        targetAudience: String = "",
        marketingConsentRequired: Boolean = false,
        sendBlockedReason: String = "",
        sendState: String = "",
    ): AdminNotificationTestReceipt = AdminNotificationTestReceipt(
        notificationId = "test-notification-1",
        templateKey = "booking_request",
        campaignId = campaignId,
        deliveryState = "queued",
        recipientUid = recipientUid,
        message = "queued",
        targetScope = targetScope,
        testOnly = testOnly,
        targetAudience = targetAudience,
        marketingConsentRequired = marketingConsentRequired,
        sendBlockedReason = sendBlockedReason,
        sendState = sendState,
    )
}
