package com.sudsmobile.data.entitlement

import kotlinx.serialization.Serializable

@Serializable
data class AdminCustomerLookup(
    val uid: String, val email: String, val displayName: String = "", val phoneNumber: String = "",
)

@Serializable
data class AdminBookingLookup(
    val id: String, val reservationCode: String, val serviceId: String = "", val serviceName: String = "",
    val slotStart: String = "", val status: String = "",
)

@Serializable
data class AdminPackageTemplate(
    val id: String = "", val name: String, val kind: String, val totalUses: Int, val validDays: Int,
    val eligibleServiceIds: List<String>,
)

@Serializable
data class AdminOperationLookups(
    val customers: List<AdminCustomerLookup> = emptyList(),
    val reservations: List<AdminBookingLookup> = emptyList(),
    val templates: List<AdminPackageTemplate> = emptyList(),
)

sealed interface AdminOperationLookupResult {
    data class Success(val value: AdminOperationLookups) : AdminOperationLookupResult
    data class Failure(val error: ServiceEntitlementError) : AdminOperationLookupResult
}

sealed interface AdminPackageTemplateResult {
    data class Success(val template: AdminPackageTemplate) : AdminPackageTemplateResult
    data class Failure(val error: ServiceEntitlementError) : AdminPackageTemplateResult
}
