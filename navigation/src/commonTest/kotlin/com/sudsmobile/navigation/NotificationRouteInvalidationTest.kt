package com.sudsmobile.navigation

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NotificationRouteInvalidationTest {
    @Test
    fun bookingLifecycleNotificationRoutesInvalidateBookingData() {
        assertTrue(Routes.Cart.invalidatesBookingsFromNotification())
        assertTrue(Routes.AdminBookings.invalidatesBookingsFromNotification())
        assertTrue(Routes.Loyalty.invalidatesBookingsFromNotification())
        assertTrue(Routes.History.invalidatesBookingsFromNotification())
        assertTrue(Routes.rating("reservation-1").invalidatesBookingsFromNotification())
    }

    @Test
    fun unrelatedNotificationRoutesDoNotInvalidateBookingData() {
        assertFalse(Routes.Home.invalidatesBookingsFromNotification())
        assertFalse(Routes.Profile.invalidatesBookingsFromNotification())
        assertFalse(Routes.AdminNotificationCampaignDrafts.invalidatesBookingsFromNotification())
        assertFalse("rating".invalidatesBookingsFromNotification())
    }
}
