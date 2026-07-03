package com.sudsmobile.feature.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal expect fun PlatformGoogleSignInButton(
    enabled: Boolean,
    onIdToken: (String) -> Unit,
    onError: (String) -> Unit,
)

internal expect fun isGoogleSignInAvailable(): Boolean

@Composable
internal expect fun GoogleSignInLogo(enabled: Boolean)

@Composable
internal fun GoogleAuthButton(
    enabled: Boolean,
    onClick: () -> Unit,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(
            width = 1.dp,
            color = GoogleButtonBorder,
        ),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = GoogleButtonContainer,
            contentColor = GoogleButtonContent,
            disabledContainerColor = GoogleButtonContainer.copy(alpha = 0.72f),
            disabledContentColor = GoogleButtonContent.copy(alpha = 0.38f),
        ),
        contentPadding = PaddingValues(horizontal = 12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            GoogleSignInLogo(enabled = enabled)
            Spacer(Modifier.width(12.dp))
            Text(
                text = "Continuar com Google",
                modifier = Modifier.weight(1f),
                color = Color.Unspecified,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
            Spacer(Modifier.width(32.dp))
        }
    }
}

private val GoogleButtonContainer = Color(0xFFFFFFFF)
private val GoogleButtonBorder = Color(0xFF747775)
private val GoogleButtonContent = Color(0xFF1F1F1F)
