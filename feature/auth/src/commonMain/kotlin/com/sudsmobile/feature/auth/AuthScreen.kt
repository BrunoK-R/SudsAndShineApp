package com.sudsmobile.feature.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel

private enum class AuthMode {
    Login,
    Register,
    ForgotPassword,
    ResetSent,
}

@Composable
fun AuthScreen(
    onLoginSuccess: () -> Unit,
    onLoginCancelled: () -> Unit,
) {
    val viewModel: AuthViewModel = koinViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var mode by rememberSaveable { mutableStateOf(AuthMode.Login.name) }
    var showPassword by rememberSaveable { mutableStateOf(false) }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var name by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }
    var acceptsTerms by rememberSaveable { mutableStateOf(false) }
    val isLoading = uiState is AuthUiState.Loading
    val errorMessage = when (val state = uiState) {
        is AuthUiState.Error -> state.message
        is AuthUiState.RegistrationProfileError -> state.message
        else -> null
    }
    val profileSyncFailed = uiState is AuthUiState.RegistrationProfileError

    LaunchedEffect(uiState) {
        when (uiState) {
            is AuthUiState.Authenticated -> {
                viewModel.clearTransientState()
                onLoginSuccess()
            }
            AuthUiState.PasswordResetSent -> {
                mode = AuthMode.ResetSent.name
            }
            else -> Unit
        }
    }

    AuthShell {
        when (AuthMode.valueOf(mode)) {
            AuthMode.Login -> LoginContent(
                email = email,
                password = password,
                showPassword = showPassword,
                isLoading = isLoading,
                errorMessage = errorMessage,
                onEmailChange = { email = it },
                onPasswordChange = { password = it },
                onTogglePassword = { showPassword = !showPassword },
                onGoogleIdToken = viewModel::signInWithGoogleIdToken,
                onGoogleError = viewModel::showGoogleSignInError,
                onForgotPassword = {
                    viewModel.clearTransientState()
                    mode = AuthMode.ForgotPassword.name
                },
                onLogin = { viewModel.signIn(email, password) },
                onGuest = onLoginCancelled,
                onCreateAccount = {
                    viewModel.clearTransientState()
                    mode = AuthMode.Register.name
                },
            )

            AuthMode.Register -> RegisterContent(
                name = name,
                email = email,
                phone = phone,
                password = password,
                showPassword = showPassword,
                acceptsTerms = acceptsTerms,
                isLoading = isLoading,
                errorMessage = errorMessage,
                profileSyncFailed = profileSyncFailed,
                onNameChange = { name = it },
                onEmailChange = { email = it },
                onPhoneChange = { phone = it },
                onPasswordChange = { password = it },
                onTogglePassword = { showPassword = !showPassword },
                onAcceptsTermsChange = { acceptsTerms = it },
                onRegister = {
                    viewModel.register(
                        displayName = name,
                        email = email,
                        phoneNumber = phone,
                        password = password,
                        acceptsTerms = acceptsTerms,
                    )
                },
                onRetryProfileSave = viewModel::retryRegistrationProfileSave,
                onContinueAfterProfileError = viewModel::continueAfterRegistrationProfileError,
                onLogin = {
                    viewModel.clearTransientState()
                    mode = AuthMode.Login.name
                },
            )

            AuthMode.ForgotPassword -> ForgotPasswordContent(
                email = email,
                isLoading = isLoading,
                errorMessage = errorMessage,
                onEmailChange = { email = it },
                onBack = {
                    viewModel.clearTransientState()
                    mode = AuthMode.Login.name
                },
                onSubmit = { viewModel.sendPasswordReset(email) },
            )

            AuthMode.ResetSent -> ResetSentContent(
                email = email,
                onBackToLogin = {
                    viewModel.clearTransientState()
                    mode = AuthMode.Login.name
                },
            )
        }
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
private fun ColumnScope.LoginContent(
    email: String,
    password: String,
    showPassword: Boolean,
    isLoading: Boolean,
    errorMessage: String?,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onTogglePassword: () -> Unit,
    onGoogleIdToken: (String) -> Unit,
    onGoogleError: (String) -> Unit,
    onForgotPassword: () -> Unit,
    onLogin: () -> Unit,
    onGuest: () -> Unit,
    onCreateAccount: () -> Unit,
) {
    BrandMark(modifier = Modifier.align(Alignment.CenterHorizontally))
    Spacer(Modifier.height(48.dp))
    AuthHeader(
        title = "Bem-vindo",
        subtitle = "Entre na sua conta para continuar",
    )
    Spacer(Modifier.height(28.dp))
    AuthErrorMessage(errorMessage)
    PlatformGoogleSignInButton(
        enabled = !isLoading,
        onIdToken = onGoogleIdToken,
        onError = onGoogleError,
    )
    AuthDivider()

    AuthTextField(
        label = "Email",
        value = email,
        onValueChange = onEmailChange,
        placeholder = "seuemail@exemplo.com",
        icon = Icons.Filled.Mail,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Email,
            imeAction = ImeAction.Next,
        ),
    )
    Spacer(Modifier.height(18.dp))
    AuthTextField(
        label = "Palavra-passe",
        value = password,
        onValueChange = onPasswordChange,
        placeholder = "********",
        icon = Icons.Filled.Lock,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Password,
            imeAction = ImeAction.Done,
        ),
        visualTransformation = if (showPassword) {
            VisualTransformation.None
        } else {
            PasswordVisualTransformation()
        },
        trailingIcon = {
            IconButton(onClick = onTogglePassword) {
                Icon(
                    imageVector = if (showPassword) {
                        Icons.Filled.VisibilityOff
                    } else {
                        Icons.Filled.Visibility
                    },
                    contentDescription = if (showPassword) {
                        "Ocultar palavra-passe"
                    } else {
                        "Mostrar palavra-passe"
                    },
                    tint = MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = 0.64f),
                )
            }
        },
    )
    TextButton(
        onClick = onForgotPassword,
        modifier = Modifier.align(Alignment.Start),
        colors = ButtonDefaults.textButtonColors(
            contentColor = MaterialTheme.colorScheme.tertiaryContainer,
        ),
    ) {
        Text("Esqueceu a palavra-passe?")
    }
    Spacer(Modifier.height(8.dp))
    PrimaryAuthButton(
        text = "Entrar",
        onClick = onLogin,
        enabled = !isLoading,
        loading = isLoading,
    )
    Spacer(Modifier.height(14.dp))
    GuestButton(onClick = onGuest, enabled = !isLoading)
    Spacer(Modifier.height(22.dp))
    InlineAuthAction(
        prefix = "Não tem conta? ",
        action = "Criar Conta",
        onClick = onCreateAccount,
    )
}

@Composable
private fun ColumnScope.RegisterContent(
    name: String,
    email: String,
    phone: String,
    password: String,
    showPassword: Boolean,
    acceptsTerms: Boolean,
    isLoading: Boolean,
    errorMessage: String?,
    profileSyncFailed: Boolean,
    onNameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onPhoneChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onTogglePassword: () -> Unit,
    onAcceptsTermsChange: (Boolean) -> Unit,
    onRegister: () -> Unit,
    onRetryProfileSave: () -> Unit,
    onContinueAfterProfileError: () -> Unit,
    onLogin: () -> Unit,
) {
    BrandMark(modifier = Modifier.align(Alignment.CenterHorizontally))
    Spacer(Modifier.height(32.dp))
    AuthHeader(
        title = "Criar Conta",
        subtitle = "Registe-se para começar",
    )
    Spacer(Modifier.height(24.dp))
    AuthErrorMessage(errorMessage)

    AuthTextField(
        label = "Nome Completo",
        value = name,
        onValueChange = onNameChange,
        placeholder = "João Silva",
        icon = Icons.Filled.Person,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
    )
    Spacer(Modifier.height(14.dp))
    AuthTextField(
        label = "Email",
        value = email,
        onValueChange = onEmailChange,
        placeholder = "seuemail@exemplo.com",
        icon = Icons.Filled.Mail,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Email,
            imeAction = ImeAction.Next,
        ),
    )
    Spacer(Modifier.height(14.dp))
    AuthTextField(
        label = "Telemóvel",
        value = phone,
        onValueChange = onPhoneChange,
        placeholder = "913 005 855",
        icon = Icons.Filled.Phone,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Phone,
            imeAction = ImeAction.Next,
        ),
    )
    Spacer(Modifier.height(14.dp))
    AuthTextField(
        label = "Palavra-passe",
        value = password,
        onValueChange = onPasswordChange,
        placeholder = "********",
        icon = Icons.Filled.Lock,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Password,
            imeAction = ImeAction.Done,
        ),
        visualTransformation = if (showPassword) {
            VisualTransformation.None
        } else {
            PasswordVisualTransformation()
        },
        trailingIcon = {
            IconButton(onClick = onTogglePassword) {
                Icon(
                    imageVector = if (showPassword) {
                        Icons.Filled.VisibilityOff
                    } else {
                        Icons.Filled.Visibility
                    },
                    contentDescription = if (showPassword) {
                        "Ocultar palavra-passe"
                    } else {
                        "Mostrar palavra-passe"
                    },
                    tint = MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = 0.64f),
                )
            }
        },
    )
    TermsRow(
        checked = acceptsTerms,
        onCheckedChange = onAcceptsTermsChange,
    )
    Spacer(Modifier.height(12.dp))
    PrimaryAuthButton(
        text = if (profileSyncFailed) "Tentar guardar dados" else "Criar Conta",
        onClick = if (profileSyncFailed) onRetryProfileSave else onRegister,
        enabled = (profileSyncFailed || acceptsTerms) && !isLoading,
        loading = isLoading,
    )
    Spacer(Modifier.height(22.dp))
    if (profileSyncFailed) {
        TextButton(
            onClick = onContinueAfterProfileError,
            modifier = Modifier.align(Alignment.CenterHorizontally),
            colors = ButtonDefaults.textButtonColors(
                contentColor = MaterialTheme.colorScheme.tertiaryContainer,
            ),
        ) {
            Text(
                text = "Continuar e completar depois",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
            )
        }
    } else {
        InlineAuthAction(
            prefix = "Já tem conta? ",
            action = "Entrar",
            onClick = onLogin,
        )
    }
}

@Composable
private fun ColumnScope.ForgotPasswordContent(
    email: String,
    isLoading: Boolean,
    errorMessage: String?,
    onEmailChange: (String) -> Unit,
    onBack: () -> Unit,
    onSubmit: () -> Unit,
) {
    BackAction(onClick = onBack)
    Spacer(Modifier.height(30.dp))
    AuthHeader(
        title = "Recuperar Palavra-passe",
        subtitle = "Introduza o seu email para receber instruções",
    )
    Spacer(Modifier.height(30.dp))
    AuthErrorMessage(errorMessage)
    AuthTextField(
        label = "Email",
        value = email,
        onValueChange = onEmailChange,
        placeholder = "seuemail@exemplo.com",
        icon = Icons.Filled.Mail,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Email,
            imeAction = ImeAction.Done,
        ),
    )
    Spacer(Modifier.height(24.dp))
    PrimaryAuthButton(
        text = "Enviar Instruções",
        onClick = onSubmit,
        enabled = email.isNotBlank() && !isLoading,
        loading = isLoading,
    )
}

@Composable
private fun ColumnScope.ResetSentContent(
    email: String,
    onBackToLogin: () -> Unit,
) {
    Spacer(Modifier.height(96.dp))
    Surface(
        modifier = Modifier
            .align(Alignment.CenterHorizontally)
            .size(136.dp),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = 0.10f),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = 0.20f),
        ),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.tertiaryContainer,
            )
        }
    }
    Spacer(Modifier.height(28.dp))
    Text(
        text = "Email Enviado",
        modifier = Modifier.fillMaxWidth(),
        style = MaterialTheme.typography.headlineMedium,
        color = MaterialTheme.colorScheme.inverseOnSurface,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(14.dp))
    Text(
        text = "Enviámos instruções para recuperar a sua palavra-passe para $email",
        modifier = Modifier.fillMaxWidth(),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = 0.72f),
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(30.dp))
    PrimaryAuthButton(
        text = "Voltar ao Login",
        onClick = onBackToLogin,
    )
    Spacer(Modifier.height(96.dp))
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
private fun AuthTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailingIcon: @Composable (() -> Unit)? = null,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.inverseOnSurface,
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                color = MaterialTheme.colorScheme.inverseOnSurface,
            ),
            placeholder = {
                Text(
                    text = placeholder,
                    color = MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = 0.50f),
                    style = MaterialTheme.typography.bodyLarge,
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = 0.60f),
                )
            },
            trailingIcon = trailingIcon,
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            keyboardOptions = keyboardOptions,
            visualTransformation = visualTransformation,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = MaterialTheme.colorScheme.inverseOnSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.inverseOnSurface,
                cursorColor = MaterialTheme.colorScheme.tertiaryContainer,
                focusedContainerColor = MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = 0.10f),
                unfocusedContainerColor = MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = 0.10f),
                focusedBorderColor = MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = 0.36f),
                unfocusedBorderColor = MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = 0.20f),
            ),
        )
    }
}

@Composable
private fun PrimaryAuthButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    loading: Boolean = false,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
            disabledContainerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.44f),
            disabledContentColor = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.56f),
        ),
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = MaterialTheme.colorScheme.onTertiaryContainer,
                strokeWidth = 2.dp,
            )
        } else {
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
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
        )
    }
}

@Composable
private fun AuthDivider() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = 0.20f),
        )
        Text(
            text = "ou",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = 0.60f),
        )
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = 0.20f),
        )
    }
}

@Composable
private fun InlineAuthAction(
    prefix: String,
    action: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = prefix,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = 0.66f),
        )
        Text(
            text = action,
            modifier = Modifier.clickable(onClick = onClick),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.tertiaryContainer,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun TermsRow(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(
                checkedColor = MaterialTheme.colorScheme.tertiaryContainer,
                uncheckedColor = MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = 0.34f),
                checkmarkColor = MaterialTheme.colorScheme.onTertiaryContainer,
            ),
        )
        Text(
            text = "Aceito a Política de Privacidade e os Termos de Serviço",
            modifier = Modifier
                .weight(1f)
                .padding(top = 10.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = 0.68f),
        )
    }
}

@Composable
private fun BackAction(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.tertiaryContainer,
        )
        Text(
            text = "Voltar",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.tertiaryContainer,
        )
    }
}
