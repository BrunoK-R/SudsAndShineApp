package com.sudsmobile.feature.cart

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

internal actual fun formatBookingAuditTimestamp(isoTimestamp: String): String? {
    return runCatching {
        val instant = Instant.parse(isoTimestamp.trim())
        BookingAuditTimestampFormatter.format(instant.atZone(BookingAuditTimeZone))
    }.getOrNull()
}

private val BookingAuditTimeZone = ZoneId.of("Europe/Lisbon")
private val BookingAuditTimestampFormatter = DateTimeFormatter.ofPattern(
    "d 'de' MMMM, uuuu 'às' HH:mm",
    Locale.forLanguageTag("pt-PT"),
)
