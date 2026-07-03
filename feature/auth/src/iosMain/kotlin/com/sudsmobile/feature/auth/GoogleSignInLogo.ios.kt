package com.sudsmobile.feature.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.painterResource
import sudsandshine.feature.auth.generated.resources.Res
import sudsandshine.feature.auth.generated.resources.google_g_logo

@Composable
internal actual fun GoogleSignInLogo(enabled: Boolean) {
    Image(
        painter = painterResource(Res.drawable.google_g_logo),
        contentDescription = null,
        modifier = Modifier
            .size(20.dp)
            .alpha(if (enabled) 1f else 0.38f),
    )
}
