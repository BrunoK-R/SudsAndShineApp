package com.sudsmobile.feature.profile

import platform.Foundation.NSTimeZone
import platform.Foundation.knownTimeZoneNames

internal actual fun adminTimeZoneIds(): List<String> = NSTimeZone.knownTimeZoneNames.map { it.toString() }
