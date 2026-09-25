package com.sudsmobile.feature.profile

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AdminDatePickerTest {
    @Test
    fun convertsIsoDatesToPickerMillisAndBack() {
        listOf(
            "1970-01-01",
            "2000-02-29",
            "2026-09-25",
            "2100-12-31",
        ).forEach { date ->
            val millis = date.toAdminDatePickerUtcMillisOrNull()
            assertEquals(date, millis?.toAdminDateId())
        }
    }

    @Test
    fun rejectsImpossibleDates() {
        assertNull("2026-02-29".toAdminDatePickerUtcMillisOrNull())
        assertNull("2026-13-01".toAdminDatePickerUtcMillisOrNull())
        assertNull("2101-01-01".toAdminDatePickerUtcMillisOrNull())
        assertNull("25/09/2026".toAdminDatePickerUtcMillisOrNull())
    }

    @Test
    fun formatsSelectedDateForPortugueseDisplay() {
        assertEquals("25/09/2026", "2026-09-25".toAdminDateDisplayLabel())
    }
}
