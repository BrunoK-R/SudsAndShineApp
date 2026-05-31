package com.sudsmobile.feature.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
internal expect fun PlatformGoogleSignInButton(
    enabled: Boolean,
    onIdToken: (String) -> Unit,
    onError: (String) -> Unit,
)

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
            color = MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = 0.24f),
        ),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = MaterialTheme.colorScheme.inverseOnSurface,
            contentColor = MaterialTheme.colorScheme.inverseSurface,
            disabledContainerColor = MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = 0.44f),
            disabledContentColor = MaterialTheme.colorScheme.inverseSurface.copy(alpha = 0.56f),
        ),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.size(24.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.inverseSurface.copy(alpha = 0.08f),
                contentColor = MaterialTheme.colorScheme.inverseSurface,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "G",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            Text(
                text = "Continuar com Google",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}
