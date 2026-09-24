package com.sudsmobile.feature.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sudsmobile.shared.theme.SudsColors
import com.sudsmobile.shared.ui.SudsBrandMark
import com.sudsmobile.shared.ui.SudsCustomerScreen
import com.sudsmobile.shared.ui.SudsPrimaryButton
import com.sudsmobile.shared.ui.SudsSecondaryTopBar
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AuthScreen(
    onLoginSuccess: () -> Unit,
    onLoginCancelled: () -> Unit,
    onBack: () -> Unit,
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

    AuthShell(onBack = onBack) {
        ProviderSignInContent(
            isLoading = isLoading,
            errorMessage = errorMessage,
            onGoogleIdToken = viewModel::signInWithGoogleIdToken,
            onGoogleError = viewModel::showGoogleSignInError,
            onAppleStart = viewModel::beginAppleSignIn,
            onAppleCredential = viewModel::signInWithAppleIdToken,
            onAppleError = viewModel::showAppleSignInError,
            onAppleCancelled = viewModel::cancelAppleSignIn,
            onGuest = onLoginCancelled,
        )
    }
}

@Composable
private fun AuthShell(
    onBack: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    SudsCustomerScreen(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(bottom = 32.dp),
        ) {
            SudsSecondaryTopBar(
                title = "Iniciar sessão",
                onBack = onBack,
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(top = 20.dp),
                content = content,
            )
        }
    }
}

@Composable
private fun ColumnScope.ProviderSignInContent(
    isLoading: Boolean,
    errorMessage: String?,
    onGoogleIdToken: (String) -> Unit,
    onGoogleError: (String) -> Unit,
    onAppleStart: () -> Boolean,
    onAppleCredential: (String, String, String?) -> Unit,
    onAppleError: (String) -> Unit,
    onAppleCancelled: () -> Unit,
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
        onAppleStart = onAppleStart,
        onAppleCredential = onAppleCredential,
        onAppleError = onAppleError,
        onAppleCancelled = onAppleCancelled,
    )
    Spacer(Modifier.height(14.dp))
    GuestButton(onClick = onGuest, enabled = !isLoading)
}

@Composable
private fun ProviderSignInOptions(
    isLoading: Boolean,
    onGoogleIdToken: (String) -> Unit,
    onGoogleError: (String) -> Unit,
    onAppleStart: () -> Boolean,
    onAppleCredential: (String, String, String?) -> Unit,
    onAppleError: (String) -> Unit,
    onAppleCancelled: () -> Unit,
) {
    if (isGoogleSignInAvailable()) {
        PlatformGoogleSignInButton(
            enabled = !isLoading,
            onIdToken = onGoogleIdToken,
            onError = onGoogleError,
        )
    }
    if (isAppleSignInAvailable()) {
        if (isGoogleSignInAvailable()) Spacer(Modifier.height(12.dp))
        PlatformAppleSignInButton(
            enabled = !isLoading,
            onStart = onAppleStart,
            onCredential = onAppleCredential,
            onError = onAppleError,
            onCancelled = onAppleCancelled,
        )
    }
}

@Composable
private fun BrandMark(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.size(88.dp),
        shape = RoundedCornerShape(20.dp),
        color = SudsColors.glass,
        border = BorderStroke(
            width = 1.dp,
            color = SudsColors.glassBorder,
        ),
    ) {
        SudsBrandMark(
            modifier = Modifier
                .fillMaxSize()
                .padding(7.dp),
            contentDescription = "Suds e Shine",
        )
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
        color = SudsColors.onBrand,
        fontWeight = FontWeight.Bold,
    )
    Spacer(Modifier.height(8.dp))
    Text(
        text = subtitle,
        style = MaterialTheme.typography.bodyLarge,
        color = SudsColors.onBrandMuted,
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
    SudsPrimaryButton(
        label = "Continuar como convidado",
        labelTextStyle = signInButtonLabelStyle(),
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
    )
}
