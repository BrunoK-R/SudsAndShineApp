package org.sudsmobile.app

import com.sudsmobile.data.notification.IosNotificationRegistrationBridge
import com.sudsmobile.data.notification.NotificationDeviceSync
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.mp.KoinPlatform
import org.sudsmobile.app.notifications.PushNotificationRouting
import androidx.compose.runtime.mutableStateOf

internal val iosPendingNotificationRoute = mutableStateOf<String?>(null)
private val notificationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

fun configureIosNotificationRegistration(handler: (((String) -> Unit, (String) -> Unit) -> Unit)?) {
    IosNotificationRegistrationBridge.configure(handler)
}

fun refreshIosNotificationRegistration() {
    notificationScope.launch { KoinPlatform.getKoin().get<NotificationDeviceSync>().refresh() }
}

fun openIosPushNotification(data: Map<String, String>) {
    iosPendingNotificationRoute.value = PushNotificationRouting.routeForPayload(data)
}
