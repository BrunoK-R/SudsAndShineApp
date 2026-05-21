package com.sudsmobile.di

import com.sudsmobile.data.dataModule
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.module.Module

fun initializeKoin(
    isDebugBuild: Boolean,
    useFirebaseEmulators: Boolean = false,
    additionalModules: List<Module> = emptyList(),
) {
    val modules = listOf(
        dataModule,
        platformModule,
    ) + additionalModules

    runCatching { stopKoin() }

    startKoin {
        properties(
            mapOf(
                "isDebugBuild" to isDebugBuild,
                "useFirebaseEmulators" to useFirebaseEmulators,
            ),
        )
        modules(modules)
    }
}

expect val platformModule: Module
