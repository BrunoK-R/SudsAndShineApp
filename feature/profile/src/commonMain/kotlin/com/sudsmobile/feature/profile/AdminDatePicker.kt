package com.sudsmobile.feature.profile

private const val MillisecondsPerDay = 86_400_000L
private val AdminDateIdRegex = Regex("^(\\d{4})-(\\d{2})-(\\d{2})$")

internal fun String.toAdminDatePickerUtcMillisOrNull(): Long? {
    val match = AdminDateIdRegex.matchEntire(trim()) ?: return null
    val year = match.groupValues[1].toIntOrNull() ?: return null
    val month = match.groupValues[2].toIntOrNull() ?: return null
    val day = match.groupValues[3].toIntOrNull() ?: return null
    if (year !in 1900..2100 || !isValidAdminDate(year, month, day)) return null
    return adminDaysFromCivil(year, month, day) * MillisecondsPerDay
}

internal fun Long.toAdminDateId(): String {
    val daysSinceEpoch = adminFloorDiv(this, MillisecondsPerDay)
    val date = adminCivilFromDays(daysSinceEpoch)
    return buildString(10) {
        append(date.year.toString().padStart(4, '0'))
        append('-')
        append(date.month.toString().padStart(2, '0'))
        append('-')
        append(date.day.toString().padStart(2, '0'))
    }
}

internal fun String.toAdminDateDisplayLabel(): String? {
    val match = AdminDateIdRegex.matchEntire(trim()) ?: return null
    val year = match.groupValues[1].toIntOrNull() ?: return null
    val month = match.groupValues[2].toIntOrNull() ?: return null
    val day = match.groupValues[3].toIntOrNull() ?: return null
    if (!isValidAdminDate(year, month, day)) return null
    return "${day.toString().padStart(2, '0')}/${month.toString().padStart(2, '0')}/$year"
}

private fun isValidAdminDate(year: Int, month: Int, day: Int): Boolean {
    if (year !in 1..9999 || month !in 1..12) return false
    val daysInMonth = when (month) {
        2 -> if (year % 400 == 0 || (year % 4 == 0 && year % 100 != 0)) 29 else 28
        4, 6, 9, 11 -> 30
        else -> 31
    }
    return day in 1..daysInMonth
}

private fun adminDaysFromCivil(year: Int, month: Int, day: Int): Long {
    val adjustedYear = year - if (month <= 2) 1 else 0
    val era = adjustedYear / 400
    val yearOfEra = adjustedYear - era * 400
    val adjustedMonth = month + if (month > 2) -3 else 9
    val dayOfYear = (153 * adjustedMonth + 2) / 5 + day - 1
    val dayOfEra = yearOfEra * 365 + yearOfEra / 4 - yearOfEra / 100 + dayOfYear
    return era.toLong() * 146_097L + dayOfEra - 719_468L
}

private fun adminCivilFromDays(daysSinceEpoch: Long): AdminCivilDate {
    val shiftedDays = daysSinceEpoch + 719_468L
    val era = adminFloorDiv(shiftedDays, 146_097L)
    val dayOfEra = shiftedDays - era * 146_097L
    val yearOfEra = (dayOfEra - dayOfEra / 1_460L + dayOfEra / 36_524L - dayOfEra / 146_096L) / 365L
    var year = yearOfEra + era * 400L
    val dayOfYear = dayOfEra - (365L * yearOfEra + yearOfEra / 4L - yearOfEra / 100L)
    val monthPrime = (5L * dayOfYear + 2L) / 153L
    val day = dayOfYear - (153L * monthPrime + 2L) / 5L + 1L
    val month = monthPrime + if (monthPrime < 10L) 3L else -9L
    year += if (month <= 2L) 1L else 0L
    return AdminCivilDate(year.toInt(), month.toInt(), day.toInt())
}

private fun adminFloorDiv(value: Long, divisor: Long): Long {
    val quotient = value / divisor
    val remainder = value % divisor
    return if (remainder != 0L && value < 0L) quotient - 1L else quotient
}

private data class AdminCivilDate(
    val year: Int,
    val month: Int,
    val day: Int,
)
