package org.sudsmobile.app

import android.app.Application
import com.sudsmobile.di.configureAndroidPlatform

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        configureAndroidPlatform(this)
        initializeAndroidApp(
            isDebugBuild = BuildConfig.DEBUG,
            useFirebaseEmulators = BuildConfig.USE_FIREBASE_EMULATORS,
        )
    }
}
