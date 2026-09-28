package com.sudsmobile.data.catalog

data class ServiceCatalog(
    val services: List<ServiceCatalogService>,
    val extras: List<ServiceCatalogExtra> = emptyList(),
)

data class ServiceCatalogService(
    val id: String,
    val name: String,
    val description: String,
    val durationMinutes: Int,
    val passengerPriceCents: Int,
    val suvPriceCents: Int,
    val iconKey: String,
    val popular: Boolean,
    val dryingDurationMinutes: Int = 0,
    val pickupPolicy: String = "",
)

data class ServiceCatalogExtra(
    val id: String,
    val name: String,
    val description: String,
    val priceCents: Int,
    val passengerPriceCents: Int = priceCents,
    val suvPriceCents: Int = passengerPriceCents,
    val additionalDurationMinutes: Int = 0,
    val quantityEnabled: Boolean = false,
    val maxQuantity: Int = 1,
    val iconKey: String,
    val eligibleServiceIds: List<String> = emptyList(),
    val dryingDurationMinutes: Int = 0,
    val pickupPolicy: String = "",
)

sealed interface ServiceCatalogResult {
    data class Success(val catalog: ServiceCatalog) : ServiceCatalogResult
    data class Failure(val error: ServiceCatalogError) : ServiceCatalogResult
}

sealed interface ServiceCatalogError {
    val message: String

    data class Permission(override val message: String) : ServiceCatalogError
    data class Unauthenticated(override val message: String) : ServiceCatalogError
    data class Unavailable(override val message: String) : ServiceCatalogError
    data class Backend(override val message: String) : ServiceCatalogError
}

interface ServiceCatalogRepository {
    suspend fun getServiceCatalog(): ServiceCatalogResult
}
