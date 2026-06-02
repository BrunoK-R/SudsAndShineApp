package com.sudsmobile.feature.profile

import com.sudsmobile.data.admin.AdminNotificationTestReceipt

internal fun AdminNotificationTestReceipt.isCurrentAdminSelfTest(requestedUid: String): Boolean {
    val normalizedRequestedUid = requestedUid.trim()
    val recipientMatches = normalizedRequestedUid.isNotBlank() && recipientUid.trim() == normalizedRequestedUid
    val scope = targetScope.trim().lowercase()
    return recipientMatches && scope == "self" && testOnly
}

internal fun AdminNotificationTestReceipt.toSelfTestQueuedMessage(kindLabel: String): String {
    return "$kindLabel em fila apenas para o administrador atual."
}

internal const val UnsafeAdminNotificationTestReceiptMessage =
    "O backend não confirmou que o teste ficou limitado ao administrador atual."
