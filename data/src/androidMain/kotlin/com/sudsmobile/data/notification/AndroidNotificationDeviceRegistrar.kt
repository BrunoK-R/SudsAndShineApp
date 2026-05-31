package com.sudsmobile.data.notification

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import com.google.firebase.messaging.FirebaseMessaging
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine

class AndroidNotificationDeviceRegistrar(
    context: Context,
    private val firebaseMessaging: FirebaseMessaging = FirebaseMessaging.getInstance(),
) : NotificationDeviceRegistrar {
    private val appContext = context.applicationContext
    private val preferences = appContext.getSharedPreferences(PreferencesName, Context.MODE_PRIVATE)

    override suspend fun currentState(): NotificationDeviceRegistrationState {
        return NotificationDeviceRegistrationState(
            permissionStatus = currentPermissionStatus(),
            registeredTokenId = preferences.getString(RegisteredTokenIdKey, null),
            platform = NotificationTokenPlatform.Android,
        )
    }

    override suspend fun buildRegistrationRequest(): NotificationDeviceRegistrationRequestResult {
        val permissionStatus = currentPermissionStatus()
        if (permissionStatus == NotificationDevicePermissionStatus.RequiresPermission) {
            return NotificationDeviceRegistrationRequestResult.PermissionRequired(
                "Autorize notificações para registar este dispositivo.",
            )
        }

        return try {
            val token = firebaseMessaging.awaitToken()
            NotificationDeviceRegistrationRequestResult.Success(
                NotificationTokenRegistrationRequest(
                    token = token,
                    platform = NotificationTokenPlatform.Android,
                    tokenId = currentTokenId(),
                    deviceLabel = deviceLabel(),
                    appVersion = appVersion(),
                ),
            )
        } catch (cause: CancellationException) {
            throw cause
        } catch (cause: Throwable) {
            NotificationDeviceRegistrationRequestResult.Failure(
                "Não foi possível obter o token de notificações deste dispositivo.",
            )
        }
    }

    override suspend fun markRegistered(tokenId: String) {
        preferences.edit().putString(RegisteredTokenIdKey, tokenId).apply()
    }

    override suspend fun markDeleted(tokenId: String) {
        if (preferences.getString(RegisteredTokenIdKey, null) == tokenId) {
            preferences.edit().remove(RegisteredTokenIdKey).apply()
        }
    }

    private fun currentPermissionStatus(): NotificationDevicePermissionStatus {
        return when {
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU -> NotificationDevicePermissionStatus.NotRequired
            appContext.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED -> NotificationDevicePermissionStatus.Granted
            else -> NotificationDevicePermissionStatus.RequiresPermission
        }
    }

    private fun currentTokenId(): String {
        val existing = preferences.getString(DeviceTokenIdKey, null)
        if (!existing.isNullOrBlank()) return existing

        val generated = "android-${UUID.randomUUID()}"
        preferences.edit().putString(DeviceTokenIdKey, generated).apply()
        return generated
    }

    private fun deviceLabel(): String {
        val manufacturer = Build.MANUFACTURER.orEmpty().trim()
        val model = Build.MODEL.orEmpty().trim()
        val label = when {
            manufacturer.isBlank() -> model
            model.startsWith(manufacturer, ignoreCase = true) -> model
            else -> "$manufacturer $model"
        }.trim()

        return label.ifBlank { "Android device" }.take(MaxDeviceLabelLength)
    }

    private fun appVersion(): String {
        val packageManager = appContext.packageManager
        val packageName = appContext.packageName
        val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            packageManager.getPackageInfo(packageName, 0)
        }
        return packageInfo.versionName?.take(MaxAppVersionLength).orEmpty()
    }
}

private suspend fun FirebaseMessaging.awaitToken(): String = suspendCancellableCoroutine { continuation ->
    token.addOnCompleteListener { task ->
        if (!continuation.isActive) return@addOnCompleteListener

        val result = if (task.isSuccessful) task.result else null
        when {
            task.isSuccessful && !result.isNullOrBlank() -> continuation.resume(result)
            task.exception != null -> continuation.resumeWithException(task.exception ?: IllegalStateException())
            else -> continuation.resumeWithException(IllegalStateException("Firebase Messaging token is empty."))
        }
    }
}

private const val PreferencesName = "suds_notification_device"
private const val DeviceTokenIdKey = "device_token_id"
private const val RegisteredTokenIdKey = "registered_token_id"
private const val MaxDeviceLabelLength = 120
private const val MaxAppVersionLength = 64
