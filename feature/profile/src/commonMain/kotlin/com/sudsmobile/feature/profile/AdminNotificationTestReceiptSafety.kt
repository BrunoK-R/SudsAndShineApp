package com.sudsmobile.feature.profile

import com.sudsmobile.data.admin.AdminNotificationTestReceipt

internal fun AdminNotificationTestReceipt.isCurrentAdminSelfTest(requestedUid: String): Boolean {
    val normalizedRequestedUid = requestedUid.trim()
    val recipientMatches = normalizedRequestedUid.isNotBlank() && recipientUid.trim() == normalizedRequestedUid
    val scope = targetScope.trim().lowercase()
    return recipientMatches && scope == "self" && testOnly
}

internal fun AdminNotificationTestReceipt.toSelfTestQueuedMessage(kindLabel: String): String {
    val baseMessage = "$kindLabel em fila apenas para o administrador atual."
    if (campaignId.isBlank()) return baseMessage

    val sendStateLabel = sendState.trim().ifBlank { "draft_only" }
    val sendBlockedReasonLabel = sendBlockedReason.trim().ifBlank { "campaign-send-not-implemented" }
    val consentLabel = if (marketingConsentRequired) {
        " Público requer opt-in marketing."
    } else {
        ""
    }
    return "$baseMessage Envio real bloqueado ($sendStateLabel): $sendBlockedReasonLabel.$consentLabel"
}

internal const val UnsafeAdminNotificationTestReceiptMessage =
    "O backend não confirmou que o teste ficou limitado ao administrador atual."
