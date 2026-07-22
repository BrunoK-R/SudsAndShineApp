package com.sudsmobile.data.entitlement

data class ServiceEntitlement(
    val id: String,
    val code: String,
    val kind: String,
    val name: String,
    val status: String,
    val totalUses: Int,
    val usedUses: Int,
    val remainingUses: Int,
    val eligibleServiceIds: List<String>,
    val eligibleServiceNames: List<String>,
    val validFromIso: String,
    val validUntilIso: String,
    val amountPaidCents: Int,
    val purchaseMode: String,
    val onlinePurchaseAvailable: Boolean,
    val createdAtIso: String,
    val updatedAtIso: String,
    val lastUsedAtIso: String,
    val lastReservationCode: String,
)

data class ServiceEntitlementList(
    val entitlements: List<ServiceEntitlement>,
    val purchaseMode: String,
    val onlinePurchaseAvailable: Boolean,
)

data class AdminEntitlementCustomer(
    val uid: String,
    val email: String,
    val displayName: String,
)

data class AdminServiceEntitlementList(
    val customer: AdminEntitlementCustomer,
    val entitlements: List<ServiceEntitlement>,
    val purchaseMode: String,
    val onlinePurchaseAvailable: Boolean,
)

data class IssueServiceEntitlementRequest(
    val operationId: String,
    val customerEmail: String,
    val kind: String,
    val name: String,
    val totalUses: Int,
    val validDays: Int,
    val amountPaidCents: Int,
    val eligibleServiceIds: List<String>,
    val staffNote: String,
)

data class AdjustServiceEntitlementUsageRequest(
    val operationId: String,
    val customerEmail: String,
    val entitlementId: String,
    val deltaUses: Int,
    val reservationCode: String,
    val staffNote: String,
)

data class RevokeServiceEntitlementRequest(
    val operationId: String,
    val customerEmail: String,
    val entitlementId: String,
    val reason: String,
)

sealed interface ServiceEntitlementListResult {
    data class Success(val value: ServiceEntitlementList) : ServiceEntitlementListResult
    data class Failure(val error: ServiceEntitlementError) : ServiceEntitlementListResult
}

sealed interface AdminServiceEntitlementListResult {
    data class Success(val value: AdminServiceEntitlementList) : AdminServiceEntitlementListResult
    data class Failure(val error: ServiceEntitlementError) : AdminServiceEntitlementListResult
}

sealed interface ServiceEntitlementMutationResult {
    data class Success(val entitlement: ServiceEntitlement) : ServiceEntitlementMutationResult
    data class Failure(val error: ServiceEntitlementError) : ServiceEntitlementMutationResult
}

sealed interface ServiceEntitlementError {
    val message: String

    data class Validation(override val message: String) : ServiceEntitlementError
    data class Permission(override val message: String) : ServiceEntitlementError
    data class Unauthenticated(override val message: String) : ServiceEntitlementError
    data class NotFound(override val message: String) : ServiceEntitlementError
    data class NotEligible(override val message: String) : ServiceEntitlementError
    data class Unavailable(override val message: String) : ServiceEntitlementError
    data class Backend(override val message: String) : ServiceEntitlementError
}

interface ServiceEntitlementRepository {
    suspend fun getMyEntitlements(): ServiceEntitlementListResult
    suspend fun getAdminEntitlements(customerEmail: String): AdminServiceEntitlementListResult
    suspend fun issueEntitlement(request: IssueServiceEntitlementRequest): ServiceEntitlementMutationResult
    suspend fun adjustUsage(request: AdjustServiceEntitlementUsageRequest): ServiceEntitlementMutationResult
    suspend fun revokeEntitlement(request: RevokeServiceEntitlementRequest): ServiceEntitlementMutationResult
}

interface ServiceEntitlementFunctionsApi {
    suspend fun getMyEntitlements(idToken: String): ServiceEntitlementListResult
    suspend fun getAdminEntitlements(customerEmail: String, idToken: String): AdminServiceEntitlementListResult
    suspend fun issueEntitlement(
        request: IssueServiceEntitlementRequest,
        idToken: String,
    ): ServiceEntitlementMutationResult
    suspend fun adjustUsage(
        request: AdjustServiceEntitlementUsageRequest,
        idToken: String,
    ): ServiceEntitlementMutationResult
    suspend fun revokeEntitlement(
        request: RevokeServiceEntitlementRequest,
        idToken: String,
    ): ServiceEntitlementMutationResult
}
