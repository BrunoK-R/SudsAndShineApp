package com.sudsmobile.feature.products

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.Weekend
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sudsmobile.data.catalog.ServiceCatalog
import com.sudsmobile.data.catalog.ServiceCatalogError
import com.sudsmobile.data.catalog.ServiceCatalogExtra
import com.sudsmobile.data.catalog.ServiceCatalogRepository
import com.sudsmobile.data.catalog.ServiceCatalogResult
import com.sudsmobile.data.catalog.ServiceCatalogService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ProductServiceUi(
    val id: String,
    val name: String,
    val description: String,
    val durationMinutes: Int,
    val durationLabel: String,
    val passengerPriceCents: Int,
    val suvPriceCents: Int,
    val passengerPrice: String,
    val suvPrice: String,
    val icon: ImageVector,
    val popular: Boolean,
    val dryingDurationMinutes: Int = 0,
)

data class ProductExtraUi(
    val id: String,
    val name: String,
    val description: String,
    val priceCents: Int,
    val price: String,
    val passengerPriceCents: Int = priceCents,
    val suvPriceCents: Int = passengerPriceCents,
    val additionalDurationMinutes: Int = 0,
    val quantityEnabled: Boolean = false,
    val maxQuantity: Int = 1,
    val icon: ImageVector,
    val eligibleServiceIds: List<String> = emptyList(),
    val dryingDurationMinutes: Int = 0,
)

sealed interface ProductCatalogUiState {
    data object Idle : ProductCatalogUiState
    data object Loading : ProductCatalogUiState
    data class Loaded(
        val services: List<ProductServiceUi>,
        val extras: List<ProductExtraUi> = emptyList(),
    ) : ProductCatalogUiState
    data object Empty : ProductCatalogUiState
    data class Error(val message: String, val retryable: Boolean) : ProductCatalogUiState
}

class ProductsCatalogViewModel(
    private val serviceCatalogRepository: ServiceCatalogRepository,
) : ViewModel() {
    private val _catalogState = MutableStateFlow<ProductCatalogUiState>(ProductCatalogUiState.Idle)
    val catalogState: StateFlow<ProductCatalogUiState> = _catalogState.asStateFlow()

    fun loadCatalog() {
        if (_catalogState.value is ProductCatalogUiState.Loading) return

        viewModelScope.launch {
            _catalogState.value = ProductCatalogUiState.Loading
            _catalogState.value = when (val result = serviceCatalogRepository.getServiceCatalog()) {
                is ServiceCatalogResult.Success -> result.catalog.toUiState()
                is ServiceCatalogResult.Failure -> result.error.toUiState()
            }
        }
    }
}

private fun ServiceCatalog.toUiState(): ProductCatalogUiState {
    val mappedServices = services
        .mapNotNull { it.toUiModelOrNull() }
    val mappedExtras = extras
        .mapNotNull { it.toUiModelOrNull() }

    return if (mappedServices.isEmpty()) {
        ProductCatalogUiState.Empty
    } else {
        ProductCatalogUiState.Loaded(
            services = mappedServices,
            extras = mappedExtras,
        )
    }
}

private fun ServiceCatalogService.toUiModelOrNull(): ProductServiceUi? {
    if (id.isBlank() || name.isBlank() || durationMinutes <= 0) return null

    return ProductServiceUi(
        id = id,
        name = name,
        description = description,
        durationMinutes = durationMinutes,
        dryingDurationMinutes = dryingDurationMinutes.coerceIn(0, 10_080),
        durationLabel = "$durationMinutes min",
        passengerPriceCents = passengerPriceCents,
        suvPriceCents = suvPriceCents,
        passengerPrice = passengerPriceCents.toEuroLabel(),
        suvPrice = suvPriceCents.toEuroLabel(),
        icon = iconKey.toServiceIcon(),
        popular = popular,
    )
}

private fun ServiceCatalogExtra.toUiModelOrNull(): ProductExtraUi? {
    if (id.isBlank() || name.isBlank()) return null

    return ProductExtraUi(
        id = id,
        name = name,
        description = description,
        priceCents = priceCents.coerceAtLeast(0),
        price = priceCents.coerceAtLeast(0).toEuroLabel(),
        passengerPriceCents = passengerPriceCents.coerceAtLeast(0),
        suvPriceCents = suvPriceCents.coerceAtLeast(0),
        additionalDurationMinutes = additionalDurationMinutes.coerceIn(0, 480),
        dryingDurationMinutes = dryingDurationMinutes.coerceIn(0, 10_080),
        quantityEnabled = quantityEnabled,
        maxQuantity = if (quantityEnabled) maxQuantity.coerceIn(1, 20) else 1,
        icon = iconKey.toServiceIcon(),
        eligibleServiceIds = eligibleServiceIds,
    )
}

private fun ServiceCatalogError.toUiState(): ProductCatalogUiState.Error {
    val retryable = this is ServiceCatalogError.Unavailable ||
        this is ServiceCatalogError.Backend
    return ProductCatalogUiState.Error(message = message, retryable = retryable)
}

internal fun Int.toEuroLabel(): String {
    val euros = this / 100
    val remainder = this % 100
    return "$euros,${remainder.toString().padStart(2, '0')}€"
}

private fun String.toServiceIcon(): ImageVector = when (lowercase()) {
    "air", "vacuum", "odor" -> Icons.Filled.Air
    "circle", "tires", "tyre" -> Icons.Filled.Circle
    "shield", "wax" -> Icons.Filled.Shield
    "sparkles", "auto_awesome", "premium" -> Icons.Filled.AutoAwesome
    "water", "water_drop", "droplets", "exterior" -> Icons.Filled.WaterDrop
    "sofa", "weekend", "interior" -> Icons.Filled.Weekend
    else -> Icons.Filled.DirectionsCar
}
