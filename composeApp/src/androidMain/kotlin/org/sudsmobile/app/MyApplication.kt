package org.sudsmobile.app

import android.app.Application
import com.sudsmobile.di.configureAndroidPlatform
import org.sudsmobile.app.notifications.AndroidNotificationChannels

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        configureAndroidPlatform(this)
        AndroidNotificationChannels.ensureCreated(this)
        initializeAndroidApp(
            isDebugBuild = BuildConfig.DEBUG,
            useFirebaseEmulators = BuildConfig.USE_FIREBASE_EMULATORS,
        )
    }
}
