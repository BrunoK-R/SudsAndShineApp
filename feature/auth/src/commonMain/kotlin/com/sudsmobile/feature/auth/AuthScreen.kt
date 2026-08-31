package com.sudsmobile.feature.auth

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sudsmobile.shared.theme.SudsColors
import com.sudsmobile.shared.ui.SudsBrandMark
import com.sudsmobile.shared.ui.SudsCustomerScreen
import com.sudsmobile.shared.ui.SudsPrimaryButton
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
            onEmailSignIn = viewModel::signIn,
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
    SudsCustomerScreen(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(top = 32.dp, bottom = 32.dp),
            content = content,
        )
    }
}

@Composable
private fun ColumnScope.ProviderSignInContent(
    isLoading: Boolean,
    errorMessage: String?,
    onEmailSignIn: (String, String) -> Unit,
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
    EmailSignInForm(
        isLoading = isLoading,
        onSignIn = onEmailSignIn,
    )
    Spacer(Modifier.height(22.dp))
    Text(
        text = "OU",
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        color = SudsColors.onBrandMuted,
        style = MaterialTheme.typography.labelSmall,
        textAlign = TextAlign.Center,
        fontWeight = FontWeight.Bold,
    )
    Spacer(Modifier.height(18.dp))
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
            contentDescription = "Suds & Shine",
        )
    }
}

@Composable
private fun EmailSignInForm(
    isLoading: Boolean,
    onSignIn: (String, String) -> Unit,
) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    val canSubmit = !isLoading && email.isNotBlank() && password.isNotBlank()
    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = SudsColors.onBrand,
        unfocusedTextColor = SudsColors.onBrand,
        focusedBorderColor = SudsColors.cyan,
        unfocusedBorderColor = SudsColors.glassBorder,
        focusedLabelColor = SudsColors.cyanMuted,
        unfocusedLabelColor = SudsColors.onBrandMuted,
        focusedLeadingIconColor = SudsColors.cyan,
        unfocusedLeadingIconColor = SudsColors.onBrandMuted,
        cursorColor = SudsColors.cyan,
    )

    OutlinedTextField(
        value = email,
        onValueChange = { email = it.take(254) },
        modifier = Modifier.fillMaxWidth(),
        enabled = !isLoading,
        singleLine = true,
        label = { Text("Email") },
        leadingIcon = {
            Icon(Icons.Filled.Email, contentDescription = null)
        },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Email,
            imeAction = ImeAction.Next,
        ),
        shape = RoundedCornerShape(14.dp),
        colors = fieldColors,
    )
    Spacer(Modifier.height(12.dp))
    OutlinedTextField(
        value = password,
        onValueChange = { password = it.take(128) },
        modifier = Modifier.fillMaxWidth(),
        enabled = !isLoading,
        singleLine = true,
        label = { Text("Palavra-passe") },
        leadingIcon = {
            Icon(Icons.Filled.Lock, contentDescription = null)
        },
        trailingIcon = {
            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                Icon(
                    imageVector = if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                    contentDescription = if (passwordVisible) "Ocultar palavra-passe" else "Mostrar palavra-passe",
                    tint = SudsColors.onBrandMuted,
                )
            }
        },
        visualTransformation = if (passwordVisible) {
            VisualTransformation.None
        } else {
            PasswordVisualTransformation()
        },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Password,
            imeAction = ImeAction.Done,
        ),
        shape = RoundedCornerShape(14.dp),
        colors = fieldColors,
    )
    Spacer(Modifier.height(18.dp))
    SudsPrimaryButton(
        label = if (isLoading) "A entrar..." else "Entrar",
        onClick = { onSignIn(email.trim(), password) },
        enabled = canSubmit,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
    )
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
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
    )
}
