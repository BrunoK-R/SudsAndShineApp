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
    fun invalidTimeDoesNotCreateBackendRequest() {
        val request = validDraft().copy(time = "25:90").toCreateRequest()

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
