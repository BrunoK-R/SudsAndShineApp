package org.sudsmobile.app.notifications

import android.content.Intent
import android.os.Bundle

object AndroidNotificationIntentRouter {
    fun routeFromIntent(intent: Intent?): String? {
        val extras = intent?.extras ?: return null
        val payload = extras.toPushNotificationPayload()
        if (payload.isEmpty()) return null
        return PushNotificationRouting.routeForPayload(payload)
    }

    private fun Bundle.toPushNotificationPayload(): Map<String, String> {
        return PushNotificationDataKeys.All.mapNotNull { key ->
            getString(key)?.trim()?.takeIf { it.isNotBlank() }?.let { value -> key to value }
        }.toMap()
    }
}
