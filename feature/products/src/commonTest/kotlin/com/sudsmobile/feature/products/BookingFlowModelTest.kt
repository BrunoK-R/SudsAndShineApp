package com.sudsmobile.feature.products

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class BookingFlowModelTest {
    @Test
    fun mapsInputStepsToFourSegmentProgress() {
        assertEquals(0, bookingProgressIndex(BookingStep.Service))
        assertEquals(1, bookingProgressIndex(BookingStep.Vehicle))
        assertEquals(2, bookingProgressIndex(BookingStep.DateTime))
        assertEquals(3, bookingProgressIndex(BookingStep.Contact))
        assertNull(bookingProgressIndex(BookingStep.Confirmation))
        assertNull(bookingProgressIndex(BookingStep.Success))
    }

    @Test
    fun resolvesForwardBackwardAndStationaryTransitions() {
        assertEquals(
            BookingStepDirection.Forward,
            bookingStepDirection(BookingStep.Service, BookingStep.Vehicle),
        )
        assertEquals(
            BookingStepDirection.Forward,
            bookingStepDirection(BookingStep.Vehicle, BookingStep.Confirmation),
        )
        assertEquals(
            BookingStepDirection.Backward,
            bookingStepDirection(BookingStep.Confirmation, BookingStep.DateTime),
        )
        assertEquals(
            BookingStepDirection.None,
            bookingStepDirection(BookingStep.Contact, BookingStep.Contact),
        )
    }

    @Test
    fun preservesContinueRulesAcrossEveryStep() {
        fun enabled(
            step: BookingStep,
            service: Boolean = false,
            vehicleSelection: Boolean = false,
            resolvedVehicle: Boolean = false,
            date: Boolean = false,
            time: Boolean = false,
            contact: Boolean = false,
            loading: Boolean = false,
        ) = isBookingContinueEnabled(
            step = step,
            hasService = service,
            hasVehicleSelection = vehicleSelection,
            hasResolvedVehicle = resolvedVehicle,
            hasDate = date,
            hasTime = time,
            contactFormValid = contact,
            submissionLoading = loading,
        )

        assertEquals(false, enabled(BookingStep.Service))
        assertEquals(true, enabled(BookingStep.Service, service = true))
        assertEquals(false, enabled(BookingStep.Vehicle))
        assertEquals(true, enabled(BookingStep.Vehicle, vehicleSelection = true))
        assertEquals(false, enabled(BookingStep.DateTime, date = true))
        assertEquals(true, enabled(BookingStep.DateTime, date = true, time = true))
        assertEquals(false, enabled(BookingStep.Contact))
        assertEquals(true, enabled(BookingStep.Contact, contact = true))

        val validConfirmation = enabled(
            step = BookingStep.Confirmation,
            service = true,
            vehicleSelection = true,
            resolvedVehicle = true,
            date = true,
            time = true,
            contact = true,
        )
        assertEquals(true, validConfirmation)
        assertEquals(
            false,
            enabled(
                step = BookingStep.Confirmation,
                service = true,
                vehicleSelection = true,
                resolvedVehicle = true,
                date = true,
                time = true,
                contact = true,
                loading = true,
            ),
        )
        assertEquals(false, enabled(BookingStep.Success, service = true, contact = true))
    }

    @Test
    fun keepsStartingPriceUntilVehicleIsKnownThenIncludesExtras() {
        assertEquals(
            "A partir de 25,00€",
            bookingSelectionPriceLabel(
                passengerPriceLabel = "25,00€",
                passengerPriceCents = 2_500,
                suvPriceCents = 3_000,
                vehicleType = null,
                extrasPriceCents = 500,
            ),
        )
        assertEquals(
            "30,00€",
            bookingSelectionPriceLabel(
                passengerPriceLabel = "25,00€",
                passengerPriceCents = 2_500,
                suvPriceCents = 3_000,
                vehicleType = "passenger",
                extrasPriceCents = 500,
            ),
        )
        assertEquals(
            "35,00€",
            bookingSelectionPriceLabel(
                passengerPriceLabel = "25,00€",
                passengerPriceCents = 2_500,
                suvPriceCents = 3_000,
                vehicleType = "suv",
                extrasPriceCents = 500,
            ),
        )
    }
}
