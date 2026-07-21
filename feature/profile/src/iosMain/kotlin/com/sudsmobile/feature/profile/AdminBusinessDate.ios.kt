package com.sudsmobile.feature.profile

import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSLocale
import platform.Foundation.NSTimeZone
import platform.Foundation.timeZoneWithName

internal actual fun currentAdminBusinessDateKey(): String {
    return NSDateFormatter().run {
        dateFormat = "yyyy-MM-dd"
        locale = NSLocale(localeIdentifier = "en_US_POSIX")
        timeZone = requireNotNull(NSTimeZone.timeZoneWithName(AdminBusinessTimeZone))
        stringFromDate(NSDate())
    }
}

private const val AdminBusinessTimeZone = "Europe/Lisbon"
