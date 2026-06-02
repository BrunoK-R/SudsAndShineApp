package com.sudsmobile.data.admin

import com.sudsmobile.data.booking.BookingReservationExtra

data class AdminRole(
    val uid: String,
    val email: String,
    val role: String,
) {
    val isAdmin: Boolean
        get() = role == "admin"
}

data class AdminBookingRequest(
    val id: String,
    val reservationCode: String,
    val customerName: String,
    val customerEmail: String,
    val customerPhone: String,
    val serviceId: String,
    val serviceName: String,
    val slotStartIso: String,
    val slotEndIso: String,
    val status: String,
    val paymentStatus: String,
    val vehicleType: String,
    val vehicleLabel: String,
    val priceCents: Int?,
    val extras: List<BookingReservationExtra>,
    val notes: String,
    val createdAtIso: String,
    val pendingExpiresAtIso: String?,
    val loyaltyRewardApplied: Boolean,
)

data class AdminBookingDecisionRequest(
    val reservationId: String,
    val rejectionReason: String = "",
)

data class AdminBookingDecisionReceipt(
    val reservationId: String,
    val reservationCode: String,
    val status: String,
)

data class AdminServiceCatalogMutationRequest(
    val serviceId: String = "",
    val name: String,
    val description: String = "",
    val durationMinutes: Int,
    val passengerPriceCents: Int,
    val suvPriceCents: Int,
    val iconKey: String = "car",
    val popular: Boolean = false,
    val active: Boolean = true,
    val sortOrder: Int = 999,
)

data class AdminServiceCatalogItem(
    val id: String,
    val name: String,
    val description: String,
    val durationMinutes: Int,
    val passengerPriceCents: Int,
    val suvPriceCents: Int,
    val iconKey: String,
    val popular: Boolean,
    val active: Boolean,
    val sortOrder: Int,
    val createdAtIso: String = "",
    val updatedAtIso: String = "",
    val archivedAtIso: String = "",
    val createdByUid: String = "",
    val updatedByUid: String = "",
    val archivedByUid: String = "",
)

data class AdminServiceCatalogConfig(
    val services: List<AdminServiceCatalogItem>,
)

data class AdminServiceExtraMutationRequest(
    val extraId: String = "",
    val name: String,
    val description: String = "",
    val priceCents: Int,
    val iconKey: String = "auto_awesome",
    val eligibleServiceIds: List<String> = emptyList(),
    val active: Boolean = true,
    val sortOrder: Int = 999,
)

data class AdminServiceExtraItem(
    val id: String,
    val name: String,
    val description: String,
    val priceCents: Int,
    val iconKey: String,
    val eligibleServiceIds: List<String>,
    val active: Boolean,
    val sortOrder: Int,
    val createdAtIso: String = "",
    val updatedAtIso: String = "",
    val archivedAtIso: String = "",
    val createdByUid: String = "",
    val updatedByUid: String = "",
    val archivedByUid: String = "",
)

data class AdminServiceExtrasConfig(
    val extras: List<AdminServiceExtraItem>,
)

data class AdminServiceCatalogArchiveRequest(
    val serviceId: String,
)

data class AdminServiceExtraArchiveRequest(
    val extraId: String,
)

data class AdminServiceCatalogMutationReceipt(
    val serviceId: String,
    val status: String,
    val created: Boolean = false,
)

data class AdminServiceExtraMutationReceipt(
    val extraId: String,
    val status: String,
    val created: Boolean = false,
)

data class AdminBusinessInfoConfig(
    val phone: String,
    val email: String,
    val addressLine1: String,
    val addressLine2: String,
    val mapsUri: String,
    val whatsappUri: String,
    val openingHours: List<AdminBusinessOpeningHours>,
    val socialLinks: List<AdminBusinessSocialLink>,
    val source: String = "",
    val updatedAtIso: String = "",
    val updatedByUid: String = "",
)

data class AdminBusinessOpeningHours(
    val dayLabel: String,
    val hoursLabel: String,
    val closed: Boolean,
)

data class AdminBusinessSocialLink(
    val label: String,
    val uri: String,
)

data class AdminAvailabilityConfig(
    val defaultMaxBookingsPerSlot: Int,
    val openingHours: List<AdminBusinessOpeningHours>,
    val capacityOverrides: List<AdminCapacityOverrideItem> = emptyList(),
    val blockedSlots: List<AdminBlockedSlotItem> = emptyList(),
)

data class AdminBookingPolicyConfig(
    val pendingHoldMinutes: Int,
    val cancellationWindowMinutes: Int,
    val rescheduleWindowMinutes: Int,
    val paymentEligibilityCopy: String,
    val source: String = "",
    val updatedAtIso: String = "",
    val updatedByUid: String = "",
)

data class AdminLoyaltySettingsConfig(
    val stampsRequired: Int,
    val rewardType: String,
    val rewardValue: Int,
    val rewardDescription: String,
    val source: String = "",
    val updatedAtIso: String = "",
    val updatedByUid: String = "",
)

data class AdminNotificationTemplateConfig(
    val key: String,
    val label: String,
    val enabled: Boolean,
    val title: String,
    val body: String,
)

data class AdminNotificationSettingsConfig(
    val bookingStatusEnabled: Boolean,
    val appointmentReminderEnabled: Boolean,
    val loyaltyEnabled: Boolean,
    val adminPendingAlertEnabled: Boolean,
    val marketingEnabled: Boolean,
    val reminderLeadMinutes: Int,
    val quietHoursStart: String,
    val quietHoursEnd: String,
    val quietHoursTimeZone: String,
    val templates: List<AdminNotificationTemplateConfig>,
    val source: String = "",
    val updatedAtIso: String = "",
    val updatedByUid: String = "",
)

data class AdminAvailabilityUpdateRequest(
    val defaultMaxBookingsPerSlot: Int,
    val openingHours: List<AdminBusinessOpeningHours>,
)

data class AdminBookingPolicyUpdateRequest(
    val pendingHoldMinutes: Int,
    val cancellationWindowMinutes: Int,
    val rescheduleWindowMinutes: Int,
    val paymentEligibilityCopy: String,
)

data class AdminLoyaltySettingsUpdateRequest(
    val stampsRequired: Int,
    val rewardType: String,
    val rewardValue: Int,
    val rewardDescription: String,
)

data class AdminNotificationSettingsUpdateRequest(
    val bookingStatusEnabled: Boolean,
    val appointmentReminderEnabled: Boolean,
    val loyaltyEnabled: Boolean,
    val adminPendingAlertEnabled: Boolean,
    val marketingEnabled: Boolean,
    val reminderLeadMinutes: Int,
    val quietHoursStart: String,
    val quietHoursEnd: String,
    val quietHoursTimeZone: String,
    val templates: List<AdminNotificationTemplateConfig>,
)

data class AdminNotificationTestRequest(
    val templateKey: String = "",
    val campaignId: String = "",
)

data class AdminNotificationTestReceipt(
    val notificationId: String,
    val templateKey: String,
    val deliveryState: String,
    val recipientUid: String,
    val message: String,
    val campaignId: String = "",
)

data class AdminNotificationCampaignDraft(
    val campaignId: String,
    val title: String,
    val body: String,
    val targetAudience: String,
    val channels: List<String>,
    val marketingConsentRequired: Boolean,
    val status: String,
    val scheduledAtIso: String,
    val notes: String,
    val sendBlocked: Boolean,
    val sendBlockedReason: String,
    val createdAtIso: String = "",
    val updatedAtIso: String = "",
    val archivedAtIso: String = "",
    val createdByUid: String = "",
    val updatedByUid: String = "",
    val archivedByUid: String = "",
)

data class AdminNotificationCampaignDraftsConfig(
    val source: String,
    val campaigns: List<AdminNotificationCampaignDraft>,
)

data class AdminNotificationCampaignDraftMutationRequest(
    val campaignId: String = "",
    val title: String,
    val body: String,
    val targetAudience: String,
    val scheduledAtIso: String = "",
    val notes: String = "",
    val pushEnabled: Boolean = true,
)

data class AdminNotificationCampaignDraftArchiveRequest(
    val campaignId: String,
)

data class AdminNotificationCampaignDraftMutationReceipt(
    val campaignId: String,
    val status: String,
    val created: Boolean = false,
    val targetAudience: String = "",
    val sendBlocked: Boolean = true,
    val sendBlockedReason: String = "",
)

data class AdminCapacityOverrideItem(
    val date: String,
    val maxBookingsPerSlot: Int,
    val updatedAtIso: String = "",
    val updatedByUid: String = "",
)

data class AdminBlockedSlotItem(
    val blockedSlotId: String,
    val date: String,
    val slotStartIso: String,
    val slotEndIso: String,
    val reason: String,
    val updatedAtIso: String = "",
    val updatedByUid: String = "",
)

data class AdminCapacityOverrideUpsertRequest(
    val date: String,
    val maxBookingsPerSlot: Int,
)

data class AdminCapacityOverrideClearRequest(
    val date: String,
)

data class AdminCapacityOverrideMutationReceipt(
    val date: String,
    val status: String,
    val maxBookingsPerSlot: Int? = null,
)

data class AdminBlockedSlotUpsertRequest(
    val blockedSlotId: String = "",
    val date: String,
    val slotStartIso: String,
    val slotEndIso: String,
    val reason: String = "",
)

data class AdminBlockedSlotClearRequest(
    val blockedSlotId: String,
)

data class AdminBlockedSlotMutationReceipt(
    val blockedSlotId: String,
    val status: String,
    val date: String = "",
)

data class AdminBusinessInfoUpdateRequest(
    val phone: String,
    val email: String,
    val addressLine1: String,
    val addressLine2: String,
    val mapsUri: String,
    val whatsappUri: String,
    val openingHours: List<AdminBusinessOpeningHours>,
    val socialLinks: List<AdminBusinessSocialLink>,
)

sealed interface AdminRoleResult {
    data class Success(val role: AdminRole) : AdminRoleResult
    data class Failure(val error: AdminError) : AdminRoleResult
}

sealed interface AdminBookingRequestsResult {
    data class Success(val requests: List<AdminBookingRequest>) : AdminBookingRequestsResult
    data class Failure(val error: AdminError) : AdminBookingRequestsResult
}

sealed interface AdminBookingDecisionResult {
    data class Success(val receipt: AdminBookingDecisionReceipt) : AdminBookingDecisionResult
    data class Failure(val error: AdminError) : AdminBookingDecisionResult
}

sealed interface AdminServiceCatalogMutationResult {
    data class Success(val receipt: AdminServiceCatalogMutationReceipt) : AdminServiceCatalogMutationResult
    data class Failure(val error: AdminError) : AdminServiceCatalogMutationResult
}

sealed interface AdminServiceCatalogResult {
    data class Success(val config: AdminServiceCatalogConfig) : AdminServiceCatalogResult
    data class Failure(val error: AdminError) : AdminServiceCatalogResult
}

sealed interface AdminServiceExtrasResult {
    data class Success(val config: AdminServiceExtrasConfig) : AdminServiceExtrasResult
    data class Failure(val error: AdminError) : AdminServiceExtrasResult
}

sealed interface AdminServiceExtraMutationResult {
    data class Success(val receipt: AdminServiceExtraMutationReceipt) : AdminServiceExtraMutationResult
    data class Failure(val error: AdminError) : AdminServiceExtraMutationResult
}

sealed interface AdminBusinessInfoResult {
    data class Success(val config: AdminBusinessInfoConfig) : AdminBusinessInfoResult
    data class Failure(val error: AdminError) : AdminBusinessInfoResult
}

sealed interface AdminAvailabilityResult {
    data class Success(val config: AdminAvailabilityConfig) : AdminAvailabilityResult
    data class Failure(val error: AdminError) : AdminAvailabilityResult
}

sealed interface AdminBookingPolicyResult {
    data class Success(val config: AdminBookingPolicyConfig) : AdminBookingPolicyResult
    data class Failure(val error: AdminError) : AdminBookingPolicyResult
}

sealed interface AdminLoyaltySettingsResult {
    data class Success(val config: AdminLoyaltySettingsConfig) : AdminLoyaltySettingsResult
    data class Failure(val error: AdminError) : AdminLoyaltySettingsResult
}

sealed interface AdminNotificationSettingsResult {
    data class Success(val config: AdminNotificationSettingsConfig) : AdminNotificationSettingsResult
    data class Failure(val error: AdminError) : AdminNotificationSettingsResult
}

sealed interface AdminNotificationTestResult {
    data class Success(val receipt: AdminNotificationTestReceipt) : AdminNotificationTestResult
    data class Failure(val error: AdminError) : AdminNotificationTestResult
}

sealed interface AdminNotificationCampaignDraftsResult {
    data class Success(val config: AdminNotificationCampaignDraftsConfig) : AdminNotificationCampaignDraftsResult
    data class Failure(val error: AdminError) : AdminNotificationCampaignDraftsResult
}

sealed interface AdminNotificationCampaignDraftMutationResult {
    data class Success(
        val receipt: AdminNotificationCampaignDraftMutationReceipt,
    ) : AdminNotificationCampaignDraftMutationResult

    data class Failure(val error: AdminError) : AdminNotificationCampaignDraftMutationResult
}

sealed interface AdminCapacityOverrideMutationResult {
    data class Success(val receipt: AdminCapacityOverrideMutationReceipt) : AdminCapacityOverrideMutationResult
    data class Failure(val error: AdminError) : AdminCapacityOverrideMutationResult
}

sealed interface AdminBlockedSlotMutationResult {
    data class Success(val receipt: AdminBlockedSlotMutationReceipt) : AdminBlockedSlotMutationResult
    data class Failure(val error: AdminError) : AdminBlockedSlotMutationResult
}

sealed interface AdminError {
    val message: String

    data class Validation(override val message: String) : AdminError
    data class Permission(override val message: String) : AdminError
    data class Unauthenticated(override val message: String) : AdminError
    data class NotFound(override val message: String) : AdminError
    data class Conflict(override val message: String) : AdminError
    data class Unavailable(override val message: String) : AdminError
    data class Backend(override val message: String) : AdminError
}

interface AdminRepository {
    suspend fun syncMyRole(): AdminRoleResult
    suspend fun getPendingBookingRequests(): AdminBookingRequestsResult
    suspend fun getCompletableBookingRequests(): AdminBookingRequestsResult =
        AdminBookingRequestsResult.Failure(AdminError.Backend("Completable reservations are not implemented."))
    suspend fun getBusinessInfoConfiguration(): AdminBusinessInfoResult
    suspend fun getAvailabilityConfiguration(): AdminAvailabilityResult
    suspend fun getBookingPolicyConfiguration(): AdminBookingPolicyResult =
        AdminBookingPolicyResult.Failure(AdminError.Backend("Booking policy configuration is not implemented."))
    suspend fun getLoyaltySettingsConfiguration(): AdminLoyaltySettingsResult =
        AdminLoyaltySettingsResult.Failure(AdminError.Backend("Loyalty settings are not implemented."))
    suspend fun getNotificationSettingsConfiguration(): AdminNotificationSettingsResult =
        AdminNotificationSettingsResult.Failure(AdminError.Backend("Notification settings are not implemented."))
    suspend fun getServiceCatalogConfiguration(): AdminServiceCatalogResult
    suspend fun getServiceExtrasConfiguration(): AdminServiceExtrasResult
    suspend fun updateBusinessInfoConfiguration(
        request: AdminBusinessInfoUpdateRequest,
    ): AdminBusinessInfoResult

    suspend fun updateBookingPolicyConfiguration(
        request: AdminBookingPolicyUpdateRequest,
    ): AdminBookingPolicyResult =
        AdminBookingPolicyResult.Failure(AdminError.Backend("Booking policy configuration is not implemented."))

    suspend fun updateLoyaltySettingsConfiguration(
        request: AdminLoyaltySettingsUpdateRequest,
    ): AdminLoyaltySettingsResult =
        AdminLoyaltySettingsResult.Failure(AdminError.Backend("Loyalty settings are not implemented."))

    suspend fun updateNotificationSettingsConfiguration(
        request: AdminNotificationSettingsUpdateRequest,
    ): AdminNotificationSettingsResult =
        AdminNotificationSettingsResult.Failure(AdminError.Backend("Notification settings are not implemented."))

    suspend fun sendNotificationTestToSelf(
        request: AdminNotificationTestRequest,
    ): AdminNotificationTestResult =
        AdminNotificationTestResult.Failure(AdminError.Backend("Notification test send is not implemented."))

    suspend fun getNotificationCampaignDrafts(): AdminNotificationCampaignDraftsResult =
        AdminNotificationCampaignDraftsResult.Failure(
            AdminError.Backend("Notification campaign drafts are not implemented."),
        )

    suspend fun upsertNotificationCampaignDraft(
        request: AdminNotificationCampaignDraftMutationRequest,
    ): AdminNotificationCampaignDraftMutationResult =
        AdminNotificationCampaignDraftMutationResult.Failure(
            AdminError.Backend("Notification campaign drafts are not implemented."),
        )

    suspend fun archiveNotificationCampaignDraft(
        request: AdminNotificationCampaignDraftArchiveRequest,
    ): AdminNotificationCampaignDraftMutationResult =
        AdminNotificationCampaignDraftMutationResult.Failure(
            AdminError.Backend("Notification campaign drafts are not implemented."),
        )

    suspend fun updateAvailabilityConfiguration(
        request: AdminAvailabilityUpdateRequest,
    ): AdminAvailabilityResult

    suspend fun upsertCapacityOverride(
        request: AdminCapacityOverrideUpsertRequest,
    ): AdminCapacityOverrideMutationResult

    suspend fun clearCapacityOverride(
        request: AdminCapacityOverrideClearRequest,
    ): AdminCapacityOverrideMutationResult

    suspend fun upsertBlockedSlot(
        request: AdminBlockedSlotUpsertRequest,
    ): AdminBlockedSlotMutationResult =
        AdminBlockedSlotMutationResult.Failure(AdminError.Backend("Blocked slot configuration is not implemented."))

    suspend fun clearBlockedSlot(
        request: AdminBlockedSlotClearRequest,
    ): AdminBlockedSlotMutationResult =
        AdminBlockedSlotMutationResult.Failure(AdminError.Backend("Blocked slot configuration is not implemented."))

    suspend fun acceptBookingRequest(request: AdminBookingDecisionRequest): AdminBookingDecisionResult
    suspend fun rejectBookingRequest(request: AdminBookingDecisionRequest): AdminBookingDecisionResult
    suspend fun completeBookingRequest(request: AdminBookingDecisionRequest): AdminBookingDecisionResult =
        AdminBookingDecisionResult.Failure(AdminError.Backend("Reservation completion is not implemented."))
    suspend fun upsertServiceCatalogItem(
        request: AdminServiceCatalogMutationRequest,
    ): AdminServiceCatalogMutationResult

    suspend fun archiveServiceCatalogItem(
        request: AdminServiceCatalogArchiveRequest,
    ): AdminServiceCatalogMutationResult

    suspend fun upsertServiceExtra(
        request: AdminServiceExtraMutationRequest,
    ): AdminServiceExtraMutationResult

    suspend fun archiveServiceExtra(
        request: AdminServiceExtraArchiveRequest,
    ): AdminServiceExtraMutationResult
}
