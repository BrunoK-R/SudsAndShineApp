package com.sudsmobile.feature.products

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.WaterDrop

internal fun bookingPixelReferenceCatalog(): ProductCatalogUiState.Loaded =
    ProductCatalogUiState.Loaded(
        services = listOf(
            ProductServiceUi(
                id = "standard",
                name = "Lavagem Standard",
                description = "Exterior e interior",
                durationMinutes = 30,
                durationLabel = "30 min",
                passengerPriceCents = 2_500,
                suvPriceCents = 3_000,
                passengerPrice = "25,00€",
                suvPrice = "30,00€",
                icon = Icons.Filled.DirectionsCar,
                popular = false,
            ),
            ProductServiceUi(
                id = "premium",
                name = "Lavagem Premium",
                description = "Acabamento premium",
                durationMinutes = 45,
                durationLabel = "45 min",
                passengerPriceCents = 3_200,
                suvPriceCents = 3_700,
                passengerPrice = "32,00€",
                suvPrice = "37,00€",
                icon = Icons.Filled.AutoAwesome,
                popular = true,
            ),
            ProductServiceUi(
                id = "exterior",
                name = "Lavagem Exterior",
                description = "Exterior cuidado",
                durationMinutes = 20,
                durationLabel = "20 min",
                passengerPriceCents = 1_600,
                suvPriceCents = 2_000,
                passengerPrice = "16,00€",
                suvPrice = "20,00€",
                icon = Icons.Filled.WaterDrop,
                popular = false,
            ),
        ),
    )
