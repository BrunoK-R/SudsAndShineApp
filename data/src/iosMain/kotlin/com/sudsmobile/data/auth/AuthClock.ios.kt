package com.sudsmobile.data.auth

import kotlinx.cinterop.ExperimentalForeignApi
import platform.posix.time

@OptIn(ExperimentalForeignApi::class)
internal actual fun currentEpochSeconds(): Long = time(null)
