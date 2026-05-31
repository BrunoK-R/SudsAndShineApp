package com.sudsmobile.data.booking

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BookingReservationStatusTest {
    @Test
    fun mapsWebsiteCompatibleReservationStatuses() {
        assertEquals(BookingReservationStatus.Pending, "novo".toBookingReservationStatus())
        assertEquals(BookingReservationStatus.Confirmed, "confirmado".toBookingReservationStatus())
        assertEquals(BookingReservationStatus.InProgress, "em_execucao".toBookingReservationStatus())
        assertEquals(BookingReservationStatus.Completed, "concluido".toBookingReservationStatus())
        assertEquals(BookingReservationStatus.Cancelled, "cancelado".toBookingReservationStatus())
        assertEquals(BookingReservationStatus.Rejected, "rejeitado".toBookingReservationStatus())
        assertEquals(BookingReservationStatus.Expired, "expirado".toBookingReservationStatus())
        assertEquals(BookingReservationStatus.Unknown, "waiting_for_payment".toBookingReservationStatus())
    }

    @Test
    fun mapsWebsiteCompatiblePaymentStatuses() {
        assertEquals(BookingPaymentStatus.Pending, "waiting_for_payment".toBookingPaymentStatus())
        assertEquals(BookingPaymentStatus.Pending, "awaiting-payment".toBookingPaymentStatus())
        assertEquals(BookingPaymentStatus.Paid, "pago".toBookingPaymentStatus())
        assertEquals(BookingPaymentStatus.CoveredByLoyalty, "covered_by_loyalty".toBookingPaymentStatus())
        assertEquals(BookingPaymentStatus.Failed, "declined".toBookingPaymentStatus())
        assertEquals(BookingPaymentStatus.Refunded, "refunded".toBookingPaymentStatus())
        assertEquals(BookingPaymentStatus.Unknown, "manual_review".toBookingPaymentStatus())
    }

    @Test
    fun exposesCancelableAndReviewableReservationPredicates() {
        assertTrue(reservation(status = "novo", upcoming = true).isCancelableReservation())
        assertTrue(reservation(status = "confirmado", upcoming = true).isCancelableReservation())
        assertFalse(reservation(status = "em_execucao", upcoming = true).isCancelableReservation())
        assertFalse(reservation(status = "cancelado", upcoming = true).isCancelableReservation())

        assertTrue(reservation(status = "concluido", upcoming = false).isReviewableReservation())
        assertTrue(reservation(status = "waiting_for_payment", upcoming = false).isCompletedReservation())
        assertFalse(reservation(status = "cancelado", upcoming = false).isReviewableReservation())
        assertFalse(reservation(status = "pending", upcoming = true).isReviewableReservation())
    }

    @Test
    fun exposesPaymentPredicateOnlyForConfirmedUpcomingPayableReservations() {
        assertTrue(
            reservation(status = "confirmed", upcoming = true, paymentStatus = "pending", priceCents = 3200)
                .requiresPayment(),
        )
        assertTrue(
            reservation(status = "confirmed", upcoming = true, paymentStatus = "failed", priceCents = 3200)
                .requiresPayment(),
        )
        assertFalse(
            reservation(status = "confirmed", upcoming = true, paymentStatus = "paid", priceCents = 3200)
                .requiresPayment(),
        )
        assertFalse(
            reservation(status = "pending", upcoming = true, paymentStatus = "covered_by_loyalty", priceCents = 0)
                .requiresPayment(),
        )
        assertFalse(
            reservation(status = "pending", upcoming = true, paymentStatus = "pending", priceCents = 3200)
                .requiresPayment(),
        )
        assertFalse(
            reservation(status = "cancelled", upcoming = true, paymentStatus = "pending", priceCents = 3200)
                .requiresPayment(),
        )
        assertFalse(
            reservation(status = "pending", upcoming = false, paymentStatus = "pending", priceCents = 3200)
                .requiresPayment(),
        )
    }
}

private fun reservation(
    status: String,
    upcoming: Boolean,
    paymentStatus: String = "",
    priceCents: Int? = 3200,
): BookingHistoryReservation = BookingHistoryReservation(
    id = "reservation-$status",
    reservationCode = "SS-$status",
    serviceId = "premium",
    serviceName = "Lavagem Premium",
    slotStartIso = "2026-05-22T10:00:00.000Z",
    slotEndIso = "2026-05-22T10:45:00.000Z",
    status = status,
    paymentStatus = paymentStatus,
    vehicleType = "passageiros",
    priceCents = priceCents,
    upcoming = upcoming,
)
