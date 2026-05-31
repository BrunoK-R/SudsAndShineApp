package com.sudsmobile.data.admin

interface AdminFunctionsApi {
    suspend fun syncMyRole(idToken: String): AdminRoleResult
    suspend fun getPendingBookingRequests(idToken: String): AdminBookingRequestsResult
    suspend fun acceptBookingRequest(
        request: AdminBookingDecisionRequest,
        idToken: String,
    ): AdminBookingDecisionResult

    suspend fun rejectBookingRequest(
        request: AdminBookingDecisionRequest,
        idToken: String,
    ): AdminBookingDecisionResult

    suspend fun getBusinessInfoConfiguration(idToken: String): AdminBusinessInfoResult

    suspend fun getAvailabilityConfiguration(idToken: String): AdminAvailabilityResult

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

    suspend fun upsertCapacityOverride(
        request: AdminCapacityOverrideUpsertRequest,
        idToken: String,
    ): AdminCapacityOverrideMutationResult

    suspend fun clearCapacityOverride(
        request: AdminCapacityOverrideClearRequest,
        idToken: String,
    ): AdminCapacityOverrideMutationResult

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
