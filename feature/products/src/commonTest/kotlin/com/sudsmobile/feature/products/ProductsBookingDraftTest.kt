package com.sudsmobile.feature.products

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class ProductsBookingDraftTest {
    @Test
    fun mapsPassengerDraftToBackendRequestWindow() {
        val request = validDraft().toCreateRequest()

        assertNotNull(request)
        assertEquals("2026-05-20T09:30:00.000Z", request.slotStartIso)
        assertEquals("2026-05-20T10:15:00.000Z", request.slotEndIso)
        assertEquals("passageiros", request.vehicleType)
    }

    @Test
    fun mapsSavedVehicleMetadataToBackendRequest() {
        val request = validDraft()
            .copy(
                vehicleType = "suv",
                userVehicleId = "vehicle-1",
                vehicleLabel = "BMW 320d",
            )
            .toCreateRequest()

        assertNotNull(request)
        assertEquals("suv", request.vehicleType)
        assertEquals("vehicle-1", request.userVehicleId)
        assertEquals("BMW 320d", request.vehicleLabel)
    }

    @Test
    fun mapsLoyaltyRewardCodeToBackendRequest() {
        val request = validDraft()
            .copy(loyaltyRewardCode = "SS-FREE-UID1-0001")
            .toCreateRequest()

        assertNotNull(request)
        assertEquals("SS-FREE-UID1-0001", request.loyaltyRewardCode)
    }

    @Test
    fun mapsSelectedExtrasToBackendRequest() {
        val request = validDraft()
            .copy(extraIds = listOf("wax", "vacuum"))
            .toCreateRequest()

        assertNotNull(request)
        assertEquals(listOf("wax", "vacuum"), request.extraIds)
    }

    @Test
    fun invalidTimeDoesNotCreateBackendRequest() {
        val request = validDraft().copy(time = "25:90").toCreateRequest()

        assertNull(request)
    }

    @Test
    fun invalidCalendarDateDoesNotCreateBackendRequest() {
        val request = validDraft().copy(dateId = "2026-02-29").toCreateRequest()

        assertNull(request)
    }

    @Test
    fun leapDayCreatesBackendRequestForLeapYear() {
        val request = validDraft().copy(dateId = "2028-02-29").toCreateRequest()

        assertNotNull(request)
        assertEquals("2028-02-29T09:30:00.000Z", request.slotStartIso)
    }

    @Test
    fun serviceDurationCrossingMidnightDoesNotCreateBackendRequest() {
        val request = validDraft()
            .copy(
                time = "23:30",
                serviceDurationMinutes = 45,
            )
            .toCreateRequest()

        assertNull(request)
    }
}

private fun validDraft(): ProductsBookingDraft = ProductsBookingDraft(
    customerName = "Bruno Ribeiro",
    customerEmail = "bruno@example.com",
    customerPhone = "+351913005855",
    serviceId = "premium",
    serviceName = "Lavagem Premium",
    dateId = "2026-05-20",
    time = "09:30",
    serviceDurationMinutes = 45,
    vehicleType = "passenger",
    gdprConsent = true,
    notes = "",
)
