package com.sudsmobile.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person

val mainDestinations = listOf(
    MainNavDestination(Routes.Home, "Início", Icons.Filled.Home),
    MainNavDestination(Routes.Products, "Marcar", Icons.Filled.CalendarMonth),
    MainNavDestination(Routes.Cart, "Marcações", Icons.Filled.History),
    MainNavDestination(Routes.Loyalty, "Recompensas", Icons.Filled.CardGiftcard),
    MainNavDestination(Routes.Profile, "Perfil", Icons.Filled.Person),
)
