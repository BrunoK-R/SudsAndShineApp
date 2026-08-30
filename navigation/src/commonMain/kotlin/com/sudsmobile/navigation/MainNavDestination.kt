package com.sudsmobile.navigation

import androidx.compose.ui.graphics.vector.ImageVector

data class MainNavDestination(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val compactLabel: String = label,
)
