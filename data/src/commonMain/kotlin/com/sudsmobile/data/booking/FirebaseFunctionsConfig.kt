package com.sudsmobile.data.booking

data class FirebaseFunctionsConfig(
    val projectId: String,
    val region: String,
    val useEmulator: Boolean,
    val emulatorHost: String,
) {
    val createReservationUrl: String
        get() = if (useEmulator) {
            "http://$emulatorHost:5001/$projectId/$region/createReservation"
        } else {
            "https://$region-$projectId.cloudfunctions.net/createReservation"
        }

    val getAvailabilityUrl: String
        get() = if (useEmulator) {
            "http://$emulatorHost:5001/$projectId/$region/getAvailability"
        } else {
            "https://$region-$projectId.cloudfunctions.net/getAvailability"
        }

    val getMyWaitlistUrl: String
        get() = functionUrl("getMyWaitlist")

    val joinMyWaitlistUrl: String
        get() = functionUrl("joinMyWaitlist")

    val cancelMyWaitlistUrl: String
        get() = functionUrl("cancelMyWaitlist")

    val getServiceCatalogUrl: String
        get() = if (useEmulator) {
            "http://$emulatorHost:5001/$projectId/$region/getServiceCatalog"
        } else {
            "https://$region-$projectId.cloudfunctions.net/getServiceCatalog"
        }

    val getBusinessInfoUrl: String
        get() = functionUrl("getBusinessInfo")

    val getMyReservationsUrl: String
        get() = if (useEmulator) {
            "http://$emulatorHost:5001/$projectId/$region/getMyReservations"
        } else {
            "https://$region-$projectId.cloudfunctions.net/getMyReservations"
        }

    val submitReservationReviewUrl: String
        get() = functionUrl("submitReservationReview")

    val cancelMyReservationUrl: String
        get() = functionUrl("cancelMyReservation")

    val rescheduleMyReservationUrl: String
        get() = functionUrl("rescheduleMyReservation")

    val getMyLoyaltyUrl: String
        get() = functionUrl("getMyLoyalty")

    val redeemMyLoyaltyRewardUrl: String
        get() = functionUrl("redeemMyLoyaltyReward")

    val syncMyRoleUrl: String
        get() = functionUrl("syncMyRole")

    val getAdminPendingReservationsUrl: String
        get() = functionUrl("getAdminPendingReservations")

    val getAdminAcceptedReservationsUrl: String
        get() = functionUrl("getAdminAcceptedReservations")

    val getAdminCompletableReservationsUrl: String
        get() = functionUrl("getAdminCompletableReservations")

    val acceptReservationUrl: String
        get() = functionUrl("acceptReservation")

    val rejectReservationUrl: String
        get() = functionUrl("rejectReservation")

    val startReservationUrl: String
        get() = functionUrl("startReservation")

    val completeReservationUrl: String
        get() = functionUrl("completeReservation")

    val getAdminBusinessInfoUrl: String
        get() = functionUrl("getAdminBusinessInfo")

    val getAdminAvailabilityConfigurationUrl: String
        get() = functionUrl("getAdminAvailabilityConfiguration")

    val getAdminBookingPolicyUrl: String
        get() = functionUrl("getAdminBookingPolicy")

    val getAdminLoyaltySettingsUrl: String
        get() = functionUrl("getAdminLoyaltySettings")

    val getAdminLoyaltyReportUrl: String
        get() = functionUrl("getAdminLoyaltyReport")

    val getAdminNotificationSettingsUrl: String
        get() = functionUrl("getAdminNotificationSettings")

    val getAdminServiceCatalogUrl: String
        get() = functionUrl("getAdminServiceCatalog")

    val getAdminServiceExtrasUrl: String
        get() = functionUrl("getAdminServiceExtras")

    val updateBusinessInfoUrl: String
        get() = functionUrl("updateBusinessInfo")

    val updateAvailabilityConfigurationUrl: String
        get() = functionUrl("updateAvailabilityConfiguration")

    val updateBookingPolicyUrl: String
        get() = functionUrl("updateBookingPolicy")

    val updateLoyaltySettingsUrl: String
        get() = functionUrl("updateLoyaltySettings")

    val updateNotificationSettingsUrl: String
        get() = functionUrl("updateNotificationSettings")

    val sendAdminNotificationTestUrl: String
        get() = functionUrl("sendAdminNotificationTest")

    val getAdminNotificationCampaignDraftsUrl: String
        get() = functionUrl("getAdminNotificationCampaignDrafts")

    val upsertAdminNotificationCampaignDraftUrl: String
        get() = functionUrl("upsertAdminNotificationCampaignDraft")

    val archiveAdminNotificationCampaignDraftUrl: String
        get() = functionUrl("archiveAdminNotificationCampaignDraft")

    val broadcastAdminNotificationCampaignUrl: String
        get() = functionUrl("broadcastAdminNotificationCampaign")

    val upsertCapacityOverrideUrl: String
        get() = functionUrl("upsertCapacityOverride")

    val clearCapacityOverrideUrl: String
        get() = functionUrl("clearCapacityOverride")

    val upsertBlockedSlotUrl: String
        get() = functionUrl("upsertBlockedSlot")

    val clearBlockedSlotUrl: String
        get() = functionUrl("clearBlockedSlot")

    val upsertServiceCatalogItemUrl: String
        get() = functionUrl("upsertServiceCatalogItem")

    val archiveServiceCatalogItemUrl: String
        get() = functionUrl("archiveServiceCatalogItem")

    val upsertServiceExtraUrl: String
        get() = functionUrl("upsertServiceExtra")

    val archiveServiceExtraUrl: String
        get() = functionUrl("archiveServiceExtra")

    val getMyVehiclesUrl: String
        get() = functionUrl("getMyVehicles")

    val createVehicleUrl: String
        get() = functionUrl("createVehicle")

    val updateVehicleUrl: String
        get() = functionUrl("updateVehicle")

    val deleteVehicleUrl: String
        get() = functionUrl("deleteVehicle")

    val getMyProfileUrl: String
        get() = functionUrl("getMyProfile")

    val updateMyProfileUrl: String
        get() = functionUrl("updateMyProfile")

    val updateMyProfilePhotoUrl: String
        get() = functionUrl("updateMyProfilePhoto")

    val getMyNotificationPreferencesUrl: String
        get() = functionUrl("getMyNotificationPreferences")

    val updateMyNotificationPreferencesUrl: String
        get() = functionUrl("updateMyNotificationPreferences")

    val registerNotificationTokenUrl: String
        get() = functionUrl("registerNotificationToken")

    val deleteNotificationTokenUrl: String
        get() = functionUrl("deleteNotificationToken")

    private fun functionUrl(functionName: String): String {
        return if (useEmulator) {
            "http://$emulatorHost:5001/$projectId/$region/$functionName"
        } else {
            "https://$region-$projectId.cloudfunctions.net/$functionName"
        }
    }

    companion object {
        fun default(
            isDebugBuild: Boolean,
            platformName: String,
            useFirebaseEmulators: Boolean = false,
        ): FirebaseFunctionsConfig {
            val normalizedPlatform = platformName.lowercase()
            val emulatorHost = when {
                normalizedPlatform.contains("android") -> "10.0.2.2"
                else -> "127.0.0.1"
            }

            return FirebaseFunctionsConfig(
                projectId = "sudsandshine-bd3e2",
                region = "europe-west1",
                useEmulator = isDebugBuild && useFirebaseEmulators,
                emulatorHost = emulatorHost,
            )
        }
    }
}
