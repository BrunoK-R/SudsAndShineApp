package com.sudsmobile.feature.cart

import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSLocale
import platform.Foundation.NSTimeZone
import platform.Foundation.timeZoneWithName

internal actual fun formatBookingAuditTimestamp(isoTimestamp: String): String? {
    val date = parseBookingAuditTimestamp(isoTimestamp) ?: return null
    return NSDateFormatter().run {
        dateFormat = "d 'de' MMMM, yyyy 'às' HH:mm"
        locale = NSLocale(localeIdentifier = "pt_PT")
        timeZone = requireNotNull(NSTimeZone.timeZoneWithName("Europe/Lisbon"))
        stringFromDate(date)
    }
}

private fun parseBookingAuditTimestamp(isoTimestamp: String): NSDate? {
    val parser = NSDateFormatter().apply {
        locale = NSLocale(localeIdentifier = "en_US_POSIX")
    }
    val normalized = isoTimestamp.trim()
    for (format in BookingAuditInputFormats) {
        parser.dateFormat = format
        parser.dateFromString(normalized)?.let { return it }
    }
    return null
}

private val BookingAuditInputFormats = listOf(
    "yyyy-MM-dd'T'HH:mm:ss.SSSXXXXX",
    "yyyy-MM-dd'T'HH:mm:ssXXXXX",
)
