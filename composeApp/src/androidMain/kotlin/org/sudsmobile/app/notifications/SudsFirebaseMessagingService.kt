package org.sudsmobile.app.notifications

import android.Manifest
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.sudsmobile.data.auth.AuthRepository
import com.sudsmobile.data.notification.AndroidNotificationDeviceRegistrar
import com.sudsmobile.data.notification.NotificationDeviceRegistrar
import com.sudsmobile.data.notification.NotificationDeviceRegistrationRequestResult
import com.sudsmobile.data.notification.NotificationRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.koin.core.context.GlobalContext
import org.sudsmobile.app.MainActivity
import org.sudsmobile.app.R

class SudsFirebaseMessagingService : FirebaseMessagingService() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onMessageReceived(message: RemoteMessage) {
        showForegroundNotification(message)
    }

    override fun onRegistered(installationId: String) {
        serviceScope.launch {
            registerInstallationId(installationId)
        }
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun showForegroundNotification(message: RemoteMessage) {
        if (!canPostNotifications()) return

        val title = message.notification?.title
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?: message.data["title"]?.trim()?.takeIf { it.isNotBlank() }
            ?: getString(R.string.app_name)
        val body = message.notification?.body
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?: message.data["body"]?.trim()?.takeIf { it.isNotBlank() }
            ?: return

        AndroidNotificationChannels.ensureCreated(this)

        val pendingIntent = PendingIntent.getActivity(
            this,
            notificationRequestCode(message),
            notificationOpenIntent(message.data),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, PushNotificationContract.ChannelId)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }

        val notification = builder
            .setSmallIcon(R.drawable.ic_suds_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(Notification.BigTextStyle().bigText(body))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setShowWhen(true)
            .setCategory(Notification.CATEGORY_STATUS)
            .build()

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(notificationId(message), notification)
    }

    private suspend fun registerInstallationId(installationId: String) {
        val koin = runCatching { GlobalContext.get() }.getOrNull() ?: return
        val authRepository = runCatching { koin.get<AuthRepository>() }.getOrNull() ?: return
        val session = authRepository.currentSession() ?: return
        val registrar = (
            runCatching { koin.get<NotificationDeviceRegistrar>() }.getOrNull()
                as? AndroidNotificationDeviceRegistrar
            ) ?: return
        val notificationRepository = runCatching { koin.get<NotificationRepository>() }.getOrNull() ?: return

        when (val requestResult = registrar.buildRegistrationRequestForInstallationId(installationId)) {
            is NotificationDeviceRegistrationRequestResult.Success -> {
                when (
                    val result = notificationRepository.registerNotificationToken(requestResult.request)
                ) {
                    is com.sudsmobile.data.notification.NotificationTokenRegistrationResult.Success ->
                        registrar.markRegistered(session.user.uid, result.tokenId)
                    is com.sudsmobile.data.notification.NotificationTokenRegistrationResult.Failure -> Unit
                }
            }
            is NotificationDeviceRegistrationRequestResult.PermissionRequired,
            is NotificationDeviceRegistrationRequestResult.Unsupported,
            is NotificationDeviceRegistrationRequestResult.Failure -> Unit
        }
    }

    private fun canPostNotifications(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    }

    private fun notificationOpenIntent(data: Map<String, String>): Intent {
        return Intent(this, MainActivity::class.java)
            .setAction(PushNotificationContract.ClickAction)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            .apply {
                data.forEach { (key, value) -> putExtra(key, value) }
            }
    }

    private fun notificationRequestCode(message: RemoteMessage): Int {
        val requestCode = notificationStableKey(message).hashCode() and Int.MAX_VALUE
        return if (requestCode == 0) 1 else requestCode
    }

    private fun notificationId(message: RemoteMessage): Int {
        val id = notificationStableKey(message).hashCode() and Int.MAX_VALUE
        return if (id == 0) 1 else id
    }

    private fun notificationStableKey(message: RemoteMessage): String {
        return message.data[PushNotificationDataKeys.DedupeKey]
            ?: message.messageId
            ?: "${System.currentTimeMillis()}"
    }
}
