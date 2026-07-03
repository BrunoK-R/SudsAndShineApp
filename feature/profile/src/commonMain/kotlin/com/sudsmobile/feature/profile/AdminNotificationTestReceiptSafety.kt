package com.sudsmobile.feature.profile

import com.sudsmobile.data.admin.AdminNotificationTestReceipt

internal fun AdminNotificationTestReceipt.isCurrentAdminSelfTest(requestedUid: String): Boolean {
    val normalizedRequestedUid = requestedUid.trim()
    val recipientMatches = normalizedRequestedUid.isNotBlank() && recipientUid.trim() == normalizedRequestedUid
    val scope = targetScope.trim().lowercase()
    return recipientMatches && scope == "self" && testOnly
}

internal fun AdminNotificationTestReceipt.toSelfTestQueuedMessage(kindLabel: String): String {
    return when {
        sentCount > 0 || deliveryState.trim().equals("sent", ignoreCase = true) ->
            "$kindLabel enviado apenas para o administrador atual."
        tokenCount == 0 || deliveryState.trim().equals("no_recipients", ignoreCase = true) ->
            "Ative este dispositivo antes de enviar testes de notificação."
        failedCount > 0 || invalidatedCount > 0 || deliveryState.trim().equals("failed", ignoreCase = true) ->
            "Não foi possível entregar o teste a este dispositivo."
        else -> "$kindLabel em fila apenas para o administrador atual."
    }
}

internal const val UnsafeAdminNotificationTestReceiptMessage =
    "O backend não confirmou que o teste ficou limitado ao administrador atual."
