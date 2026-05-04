package com.sudsmobile.navigation

import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScaffoldTopBar(title: String) {
    CenterAlignedTopAppBar(
        title = { Text(title) },
    )
}
