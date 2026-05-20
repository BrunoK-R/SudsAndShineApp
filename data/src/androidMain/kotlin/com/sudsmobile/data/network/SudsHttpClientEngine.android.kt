package com.sudsmobile.data.network

import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.engine.android.Android

internal actual fun sudsHttpClientEngine(): HttpClientEngineFactory<*> = Android
