package com.sudsmobile.data.auth

internal actual fun currentEpochSeconds(): Long = System.currentTimeMillis() / 1_000L
