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
    fun campaignSentMessageUsesCampaignCopy() {
        val receipt = testReceipt(
            recipientUid = "admin-1",
            targetScope = "self",
            testOnly = true,
            campaignId = "summer-test",
            targetAudience = "marketing_opt_in_users",
            marketingConsentRequired = true,
            sendBlockedReason = "campaign-send-not-implemented",
            sendState = "draft_only",
            deliveryState = "sent",
            tokenCount = 1,
            sentCount = 1,
        )

        assertEquals(
            "Teste de campanha enviado apenas para o administrador atual.",
            receipt.toSelfTestQueuedMessage("Teste de campanha"),
        )
    }

    @Test
    fun selfTestMessageConfirmsSentDelivery() {
        val receipt = testReceipt(
            recipientUid = "admin-1",
            targetScope = "self",
            testOnly = true,
            deliveryState = "sent",
            tokenCount = 1,
            sentCount = 1,
        )

        assertEquals(
            "Teste de notificação enviado apenas para o administrador atual.",
            receipt.toSelfTestQueuedMessage("Teste de notificação"),
        )
    }

    @Test
    fun selfTestMessageExplainsMissingDeviceToken() {
        val receipt = testReceipt(
            recipientUid = "admin-1",
            targetScope = "self",
            testOnly = true,
            deliveryState = "no_recipients",
            tokenCount = 0,
        )

        assertEquals(
            "Ative este dispositivo antes de enviar testes de notificação.",
            receipt.toSelfTestQueuedMessage("Teste de notificação"),
        )
    }

    @Test
    fun selfTestMessageExplainsDeliveryFailure() {
        val receipt = testReceipt(
            recipientUid = "admin-1",
            targetScope = "self",
            testOnly = true,
            deliveryState = "failed",
            tokenCount = 1,
            failedCount = 1,
        )

        assertEquals(
            "Não foi possível entregar o teste a este dispositivo.",
            receipt.toSelfTestQueuedMessage("Teste de notificação"),
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
        deliveryState: String = "queued",
        tokenCount: Int = 0,
        sentCount: Int = 0,
        failedCount: Int = 0,
        invalidatedCount: Int = 0,
    ): AdminNotificationTestReceipt = AdminNotificationTestReceipt(
        notificationId = "test-notification-1",
        templateKey = "booking_request",
        campaignId = campaignId,
        deliveryState = deliveryState,
        recipientUid = recipientUid,
        message = "queued",
        targetScope = targetScope,
        testOnly = testOnly,
        targetAudience = targetAudience,
        marketingConsentRequired = marketingConsentRequired,
        sendBlockedReason = sendBlockedReason,
        sendState = sendState,
        tokenCount = tokenCount,
        sentCount = sentCount,
        failedCount = failedCount,
        invalidatedCount = invalidatedCount,
    )
}
