package com.sudsmobile.feature.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AuthScreen(
    onLoginSuccess: () -> Unit,
    onLoginCancelled: () -> Unit,
) {
    val viewModel: AuthViewModel = koinViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isLoading = uiState is AuthUiState.Loading
    val errorMessage = (uiState as? AuthUiState.Error)?.message

    LaunchedEffect(uiState) {
        if (uiState is AuthUiState.Authenticated) {
            viewModel.clearTransientState()
            onLoginSuccess()
        }
    }

    AuthShell {
        ProviderSignInContent(
            isLoading = isLoading,
            errorMessage = errorMessage,
            onGoogleIdToken = viewModel::signInWithGoogleIdToken,
            onGoogleError = viewModel::showGoogleSignInError,
            onGuest = onLoginCancelled,
        )
    }
}

@Composable
private fun AuthShell(
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.inverseSurface,
                        MaterialTheme.colorScheme.secondary,
                    ),
                ),
            )
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .padding(top = 32.dp, bottom = 32.dp),
        content = content,
    )
}

@Composable
private fun ColumnScope.ProviderSignInContent(
    isLoading: Boolean,
    errorMessage: String?,
    onGoogleIdToken: (String) -> Unit,
    onGoogleError: (String) -> Unit,
    onGuest: () -> Unit,
) {
    BrandMark(modifier = Modifier.align(Alignment.CenterHorizontally))
    Spacer(Modifier.height(48.dp))
    AuthHeader(
        title = "Bem-vindo",
        subtitle = "Entre com a sua conta para continuar",
    )
    Spacer(Modifier.height(28.dp))
    AuthErrorMessage(errorMessage)
    ProviderSignInOptions(
        isLoading = isLoading,
        onGoogleIdToken = onGoogleIdToken,
        onGoogleError = onGoogleError,
    )
    Spacer(Modifier.height(14.dp))
    GuestButton(onClick = onGuest, enabled = !isLoading)
}

@Composable
private fun ProviderSignInOptions(
    isLoading: Boolean,
    onGoogleIdToken: (String) -> Unit,
    onGoogleError: (String) -> Unit,
) {
    if (isGoogleSignInAvailable()) {
        PlatformGoogleSignInButton(
            enabled = !isLoading,
            onIdToken = onGoogleIdToken,
            onError = onGoogleError,
        )
    }
}

@Composable
private fun BrandMark(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.size(88.dp),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = 0.10f),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = 0.20f),
        ),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Filled.AutoAwesome,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.tertiaryContainer,
            )
        }
    }
}

@Composable
private fun AuthHeader(
    title: String,
    subtitle: String,
) {
    Text(
        text = title,
        style = MaterialTheme.typography.headlineMedium,
        color = MaterialTheme.colorScheme.inverseOnSurface,
        fontWeight = FontWeight.Bold,
    )
    Spacer(Modifier.height(8.dp))
    Text(
        text = subtitle,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = 0.66f),
    )
}

@Composable
private fun AuthErrorMessage(message: String?) {
    if (message == null) return

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 18.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.92f),
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun GuestButton(
    onClick: () -> Unit,
    enabled: Boolean,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.inverseOnSurface,
            contentColor = MaterialTheme.colorScheme.inverseSurface,
        ),
    ) {
        Text(
            text = "Continuar como Convidado",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )
    }
}
