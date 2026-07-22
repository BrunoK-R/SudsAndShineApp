package org.sudsmobile.app.notifications

import com.sudsmobile.navigation.Routes
import kotlin.test.Test
import kotlin.test.assertEquals

class PushNotificationRoutingTest {
    @Test
    fun reviewPromptWithReservationRoutesToRating() {
        val route = PushNotificationRouting.routeForPayload(
            mapOf(
                PushNotificationDataKeys.TemplateKey to "review_prompt",
                PushNotificationDataKeys.ReservationId to "reservation-1",
            ),
        )

        assertEquals(Routes.rating("reservation-1"), route)
    }

    @Test
    fun malformedReviewPromptRoutesHome() {
        val route = PushNotificationRouting.routeForPayload(
            mapOf(
                PushNotificationDataKeys.TemplateKey to "review_prompt",
                PushNotificationDataKeys.ReservationId to "bad/reservation",
            ),
        )

        assertEquals(Routes.Home, route)
    }

    @Test
    fun adminPendingBookingRoutesToAdminBookings() {
        val route = PushNotificationRouting.routeForPayload(
            mapOf(PushNotificationDataKeys.TemplateKey to "admin_pending_booking"),
        )

        assertEquals(Routes.AdminBookings, route)
    }

    @Test
    fun loyaltyRewardRoutesToLoyalty() {
        val route = PushNotificationRouting.routeForPayload(
            mapOf(PushNotificationDataKeys.Type to "loyalty_reward"),
        )

        assertEquals(Routes.Loyalty, route)
    }

    @Test
    fun waitlistAvailabilityRoutesToBookingFlow() {
        val route = PushNotificationRouting.routeForPayload(
            mapOf(PushNotificationDataKeys.TemplateKey to "waitlist_available"),
        )

        assertEquals(Routes.Products, route)
    }

    @Test
    fun campaignDraftSelfTestRoutesToCampaignDrafts() {
        val route = PushNotificationRouting.routeForPayload(
            mapOf(
                PushNotificationDataKeys.Type to "admin_test_notification",
                PushNotificationDataKeys.TemplateKey to "campaign_draft",
                PushNotificationDataKeys.TestOnly to "true",
            ),
        )

        assertEquals(Routes.AdminNotificationCampaignDrafts, route)
    }

    @Test
    fun campaignBroadcastRoutesHome() {
        val route = PushNotificationRouting.routeForPayload(
            mapOf(
                PushNotificationDataKeys.Type to "campaign_broadcast",
                PushNotificationDataKeys.TemplateKey to "campaign_draft",
                PushNotificationDataKeys.CampaignId to "summer-test",
            ),
        )

        assertEquals(Routes.Home, route)
    }

    @Test
    fun marketingCampaignUsesKnownExplicitRoute() {
        val route = PushNotificationRouting.routeForPayload(
            mapOf(
                PushNotificationDataKeys.Type to "marketing_campaign",
                PushNotificationDataKeys.CampaignId to "summer-test",
                PushNotificationDataKeys.Route to "profile",
            ),
        )

        assertEquals(Routes.Profile, route)
    }

    @Test
    fun marketingCampaignWithUnknownRouteFallsBackHome() {
        val route = PushNotificationRouting.routeForPayload(
            mapOf(
                PushNotificationDataKeys.Type to "marketing_campaign",
                PushNotificationDataKeys.CampaignId to "summer-test",
                PushNotificationDataKeys.Route to "../admin_bookings",
            ),
        )

        assertEquals(Routes.Home, route)
    }

    @Test
    fun bookingTransactionalTemplatesRouteToBookings() {
        val templates = listOf(
            "booking_request",
            "booking_accepted",
            "booking_rejected",
            "booking_in_progress",
            "booking_completed",
            "booking_expired",
            "booking_cancelled",
            "booking_rescheduled",
            "booking_reminder",
        )

        templates.forEach { template ->
            val route = PushNotificationRouting.routeForPayload(
                mapOf(PushNotificationDataKeys.TemplateKey to template),
            )

            assertEquals(Routes.Cart, route, "Expected $template to route to bookings")
        }
    }

    @Test
    fun unknownPayloadRoutesHome() {
        val route = PushNotificationRouting.routeForPayload(
            mapOf(PushNotificationDataKeys.TemplateKey to "unknown"),
        )

        assertEquals(Routes.Home, route)
    }
}
