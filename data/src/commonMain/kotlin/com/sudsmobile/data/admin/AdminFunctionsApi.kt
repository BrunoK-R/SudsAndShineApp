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
}
