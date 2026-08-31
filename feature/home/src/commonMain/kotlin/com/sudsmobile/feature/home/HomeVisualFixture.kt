package com.sudsmobile.feature.home

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.WaterDrop
import com.sudsmobile.shared.loyalty.toLoyaltyProgress

internal fun homePixelReferenceState(): HomeUiState.Loaded = HomeUiState.Loaded(
    identity = HomeIdentityUi(
        greeting = "Olá, Bruno",
        subtitle = "Leiria",
        initials = "BR",
    ),
    nextBooking = HomeBookingUi(
        id = "visual-premium",
        service = "Lavagem Premium",
        date = "Ter, 1 de setembro",
        time = "10:30",
        location = "Suds & Shine - Leiria, Piso -1",
        vehicle = "BMW 320d",
        price = "32,00€",
        statusLabel = "Confirmado",
        icon = Icons.Filled.AutoAwesome,
    ),
    loyalty = 7.toLoyaltyProgress(),
    featuredServices = listOf(
        HomeFeaturedServiceUi(
            id = "exterior",
            name = "Exterior",
            price = "16,00€",
            duration = "20 min",
            icon = Icons.Filled.WaterDrop,
            popular = false,
        ),
        HomeFeaturedServiceUi(
            id = "standard",
            name = "Completa",
            price = "25,00€",
            duration = "30 min",
            icon = Icons.Filled.DirectionsCar,
            popular = false,
        ),
        HomeFeaturedServiceUi(
            id = "premium",
            name = "Detailing",
            price = "32,00€",
            duration = "45 min",
            icon = Icons.Filled.AutoAwesome,
            popular = true,
        ),
    ),
    stats = listOf(
        HomeStatUi("500+", "Carros Tratados", Icons.Filled.DirectionsCar),
        HomeStatUi("4.9", "Avaliação Média", Icons.Filled.AutoAwesome),
        HomeStatUi("3+", "Anos Experiência", Icons.Filled.Shield),
    ),
)
