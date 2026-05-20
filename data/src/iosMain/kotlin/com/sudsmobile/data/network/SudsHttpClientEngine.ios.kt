package com.sudsmobile.data.network

import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.engine.darwin.Darwin

internal actual fun sudsHttpClientEngine(): HttpClientEngineFactory<*> = Darwin
