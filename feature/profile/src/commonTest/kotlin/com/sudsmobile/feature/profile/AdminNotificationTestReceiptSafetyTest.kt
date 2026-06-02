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

    private fun testReceipt(
        recipientUid: String,
        targetScope: String,
        testOnly: Boolean,
    ): AdminNotificationTestReceipt = AdminNotificationTestReceipt(
        notificationId = "test-notification-1",
        templateKey = "booking_request",
        deliveryState = "queued",
        recipientUid = recipientUid,
        message = "queued",
        targetScope = targetScope,
        testOnly = testOnly,
    )
}
