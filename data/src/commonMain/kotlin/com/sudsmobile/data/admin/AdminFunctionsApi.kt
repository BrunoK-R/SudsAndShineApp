package com.sudsmobile.data.admin

interface AdminFunctionsApi {
    suspend fun syncMyRole(idToken: String): AdminRoleResult
    suspend fun getPendingBookingRequests(idToken: String): AdminBookingRequestsResult
    suspend fun getCompletableBookingRequests(idToken: String): AdminBookingRequestsResult =
        AdminBookingRequestsResult.Failure(AdminError.Backend("Completable reservations are not implemented."))
    suspend fun acceptBookingRequest(
        request: AdminBookingDecisionRequest,
        idToken: String,
    ): AdminBookingDecisionResult

    suspend fun rejectBookingRequest(
        request: AdminBookingDecisionRequest,
        idToken: String,
    ): AdminBookingDecisionResult

    suspend fun completeBookingRequest(
        request: AdminBookingDecisionRequest,
        idToken: String,
    ): AdminBookingDecisionResult =
        AdminBookingDecisionResult.Failure(AdminError.Backend("Reservation completion is not implemented."))

    suspend fun getBusinessInfoConfiguration(idToken: String): AdminBusinessInfoResult

    suspend fun getAvailabilityConfiguration(idToken: String): AdminAvailabilityResult

    suspend fun getBookingPolicyConfiguration(idToken: String): AdminBookingPolicyResult =
        AdminBookingPolicyResult.Failure(AdminError.Backend("Booking policy configuration is not implemented."))

    suspend fun getLoyaltySettingsConfiguration(idToken: String): AdminLoyaltySettingsResult =
        AdminLoyaltySettingsResult.Failure(AdminError.Backend("Loyalty settings are not implemented."))

    suspend fun getNotificationSettingsConfiguration(idToken: String): AdminNotificationSettingsResult =
        AdminNotificationSettingsResult.Failure(AdminError.Backend("Notification settings are not implemented."))

    suspend fun getServiceCatalogConfiguration(idToken: String): AdminServiceCatalogResult

    suspend fun getServiceExtrasConfiguration(idToken: String): AdminServiceExtrasResult

    suspend fun updateBusinessInfoConfiguration(
        request: AdminBusinessInfoUpdateRequest,
        idToken: String,
    ): AdminBusinessInfoResult

    suspend fun updateAvailabilityConfiguration(
        request: AdminAvailabilityUpdateRequest,
        idToken: String,
    ): AdminAvailabilityResult

    suspend fun updateBookingPolicyConfiguration(
        request: AdminBookingPolicyUpdateRequest,
        idToken: String,
    ): AdminBookingPolicyResult =
        AdminBookingPolicyResult.Failure(AdminError.Backend("Booking policy configuration is not implemented."))

    suspend fun updateLoyaltySettingsConfiguration(
        request: AdminLoyaltySettingsUpdateRequest,
        idToken: String,
    ): AdminLoyaltySettingsResult =
        AdminLoyaltySettingsResult.Failure(AdminError.Backend("Loyalty settings are not implemented."))

    suspend fun updateNotificationSettingsConfiguration(
        request: AdminNotificationSettingsUpdateRequest,
        idToken: String,
    ): AdminNotificationSettingsResult =
        AdminNotificationSettingsResult.Failure(AdminError.Backend("Notification settings are not implemented."))

    suspend fun sendNotificationTestToSelf(
        request: AdminNotificationTestRequest,
        idToken: String,
    ): AdminNotificationTestResult =
        AdminNotificationTestResult.Failure(AdminError.Backend("Notification test send is not implemented."))

    suspend fun getNotificationCampaignDrafts(idToken: String): AdminNotificationCampaignDraftsResult =
        AdminNotificationCampaignDraftsResult.Failure(
            AdminError.Backend("Notification campaign drafts are not implemented."),
        )

    suspend fun upsertNotificationCampaignDraft(
        request: AdminNotificationCampaignDraftMutationRequest,
        idToken: String,
    ): AdminNotificationCampaignDraftMutationResult =
        AdminNotificationCampaignDraftMutationResult.Failure(
            AdminError.Backend("Notification campaign drafts are not implemented."),
        )

    suspend fun archiveNotificationCampaignDraft(
        request: AdminNotificationCampaignDraftArchiveRequest,
        idToken: String,
    ): AdminNotificationCampaignDraftMutationResult =
        AdminNotificationCampaignDraftMutationResult.Failure(
            AdminError.Backend("Notification campaign drafts are not implemented."),
        )

    suspend fun upsertCapacityOverride(
        request: AdminCapacityOverrideUpsertRequest,
        idToken: String,
    ): AdminCapacityOverrideMutationResult

    suspend fun clearCapacityOverride(
        request: AdminCapacityOverrideClearRequest,
        idToken: String,
    ): AdminCapacityOverrideMutationResult

    suspend fun upsertBlockedSlot(
        request: AdminBlockedSlotUpsertRequest,
        idToken: String,
    ): AdminBlockedSlotMutationResult =
        AdminBlockedSlotMutationResult.Failure(AdminError.Backend("Blocked slot configuration is not implemented."))

    suspend fun clearBlockedSlot(
        request: AdminBlockedSlotClearRequest,
        idToken: String,
    ): AdminBlockedSlotMutationResult =
        AdminBlockedSlotMutationResult.Failure(AdminError.Backend("Blocked slot configuration is not implemented."))

    suspend fun upsertServiceCatalogItem(
        request: AdminServiceCatalogMutationRequest,
        idToken: String,
    ): AdminServiceCatalogMutationResult

    suspend fun archiveServiceCatalogItem(
        request: AdminServiceCatalogArchiveRequest,
        idToken: String,
    ): AdminServiceCatalogMutationResult

    suspend fun upsertServiceExtra(
        request: AdminServiceExtraMutationRequest,
        idToken: String,
    ): AdminServiceExtraMutationResult

    suspend fun archiveServiceExtra(
        request: AdminServiceExtraArchiveRequest,
        idToken: String,
    ): AdminServiceExtraMutationResult
}
