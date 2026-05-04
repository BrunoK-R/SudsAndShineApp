package org.sudsmobile.app

import android.app.Application

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initializeAndroidApp(isDebugBuild = BuildConfig.DEBUG)
    }
}
