package org.sudsmobile.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.sudsmobile.app.notifications.AndroidNotificationIntentRouter

class MainActivity : ComponentActivity() {
    private var pendingNotificationRoute by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        updatePendingNotificationRoute(intent)
        setContent {
            App(
                pendingNotificationRoute = pendingNotificationRoute,
                onNotificationRouteConsumed = { pendingNotificationRoute = null },
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        updatePendingNotificationRoute(intent)
    }

    private fun updatePendingNotificationRoute(intent: Intent?) {
        AndroidNotificationIntentRouter.routeFromIntent(intent)?.let { route ->
            pendingNotificationRoute = route
        }
    }
}
