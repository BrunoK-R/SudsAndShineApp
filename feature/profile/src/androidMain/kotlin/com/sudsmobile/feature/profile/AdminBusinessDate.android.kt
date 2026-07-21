package com.sudsmobile.feature.profile

import java.time.LocalDate
import java.time.ZoneId

internal actual fun currentAdminBusinessDateKey(): String {
    return LocalDate.now(ZoneId.of(AdminBusinessTimeZone)).toString()
}

private const val AdminBusinessTimeZone = "Europe/Lisbon"
