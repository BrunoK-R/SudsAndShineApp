package org.sudsmobile.app.notifications

import com.sudsmobile.navigation.Routes

object PushNotificationContract {
    const val ChannelId = "suds_notifications"
    const val ClickAction = "org.sudsmobile.app.NOTIFICATION_OPEN"
}

object PushNotificationDataKeys {
    const val Type = "type"
    const val TemplateKey = "templateKey"
    const val CampaignId = "campaignId"
    const val ReservationId = "reservationId"
    const val ReservationCode = "reservationCode"
    const val RedemptionId = "redemptionId"
    const val TargetScope = "targetScope"
    const val TestOnly = "testOnly"
    const val DedupeKey = "dedupeKey"
    const val Source = "source"
    const val Route = "route"

    val All: Set<String> = setOf(
        Type,
        TemplateKey,
        CampaignId,
        ReservationId,
        ReservationCode,
        RedemptionId,
        TargetScope,
        TestOnly,
        DedupeKey,
        Source,
        Route,
    )
}

object PushNotificationRouting {
    private val bookingStatusTemplateKeys = setOf(
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

    fun routeForPayload(data: Map<String, String>): String {
        val type = data[PushNotificationDataKeys.Type].cleanPayloadValue(80).lowercase()
        val templateKey = data[PushNotificationDataKeys.TemplateKey].cleanPayloadValue(80).lowercase()
        val explicitRoute = data[PushNotificationDataKeys.Route].cleanPayloadValue(80).lowercase()

        return when {
            templateKey == "review_prompt" -> data[PushNotificationDataKeys.ReservationId]
                .cleanRouteArgument()
                ?.let(Routes::rating)
                ?: Routes.Home

            templateKey == "admin_pending_booking" || type == "admin_pending_booking" ->
                Routes.AdminBookings

            templateKey == "loyalty_reward" || type == "loyalty_reward" ->
                Routes.Loyalty

            templateKey == "campaign_draft" && data.isCampaignAdminSelfTest() ->
                Routes.AdminNotificationCampaignDrafts
            templateKey == "campaign_draft" ->
                Routes.Home

            type == "marketing_campaign" || type == "campaign_broadcast" ->
                explicitRoute.toKnownNotificationRoute() ?: Routes.Home

            templateKey in bookingStatusTemplateKeys ||
                type == "booking_status" ||
                type == "booking_reminder" ->
                Routes.Cart

            else -> Routes.Home
        }
    }

    private fun String.toKnownNotificationRoute(): String? {
        return when (this) {
            Routes.Home -> Routes.Home
            Routes.Cart -> Routes.Cart
            Routes.Loyalty -> Routes.Loyalty
            Routes.Profile -> Routes.Profile
            else -> null
        }
    }

    private fun Map<String, String>.isCampaignAdminSelfTest(): Boolean {
        return this[PushNotificationDataKeys.TestOnly]?.trim()?.equals("true", ignoreCase = true) == true ||
            this[PushNotificationDataKeys.TargetScope]?.trim()?.equals("self", ignoreCase = true) == true
    }

    private fun String?.cleanPayloadValue(maxLength: Int): String {
        return this
            ?.trim()
            ?.replace(Regex("\\s+"), " ")
            ?.take(maxLength)
            .orEmpty()
    }

    private fun String?.cleanRouteArgument(): String? {
        val value = cleanPayloadValue(160)
        if (value.isBlank()) return null
        if (value.any { it == '/' || it == '?' || it == '#' || it.isISOControl() }) return null
        return value
    }
}
