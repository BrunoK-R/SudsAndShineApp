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

    suspend fun updateBusinessInfoConfiguration(
        request: AdminBusinessInfoUpdateRequest,
        idToken: String,
    ): AdminBusinessInfoResult

    suspend fun upsertServiceCatalogItem(
        request: AdminServiceCatalogMutationRequest,
        idToken: String,
    ): AdminServiceCatalogMutationResult

    suspend fun archiveServiceCatalogItem(
        request: AdminServiceCatalogArchiveRequest,
        idToken: String,
    ): AdminServiceCatalogMutationResult
}
