package com.sudsmobile.feature.profile

import java.time.ZoneId

internal actual fun adminTimeZoneIds(): List<String> = ZoneId.getAvailableZoneIds().toList()
