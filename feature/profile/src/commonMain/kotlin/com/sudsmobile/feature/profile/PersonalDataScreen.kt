package com.sudsmobile.feature.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sudsmobile.data.auth.AuthSessionState
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun PersonalDataScreen(
    contentPadding: PaddingValues,
    onBack: () -> Unit,
    onRequestSignIn: () -> Unit,
) {
    val viewModel: PersonalDataViewModel = koinViewModel()
    val sessionState by viewModel.sessionState.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val saveState by viewModel.saveState.collectAsStateWithLifecycle()

    LaunchedEffect(sessionState) {
        viewModel.refreshForSession()
    }

    PersonalDataScreenContent(
        contentPadding = contentPadding,
        sessionState = sessionState,
        uiState = uiState,
        saveState = saveState,
        onBack = onBack,
        onRequestSignIn = onRequestSignIn,
        onRetry = viewModel::loadProfile,
        onSave = viewModel::saveProfile,
        onClearSaveState = viewModel::clearSaveState,
    )
}

@Composable
private fun PersonalDataScreenContent(
    contentPadding: PaddingValues,
    sessionState: AuthSessionState,
    uiState: PersonalDataUiState,
    saveState: PersonalDataSaveUiState,
    onBack: () -> Unit,
    onRequestSignIn: () -> Unit,
    onRetry: () -> Unit,
    onSave: (PersonalDataFormUi) -> Unit,
    onClearSaveState: () -> Unit,
) {
    var displayName by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var phoneNumber by rememberSaveable { mutableStateOf("") }
    var marketingOptIn by rememberSaveable { mutableStateOf(false) }
    var appointmentReminderOptIn by rememberSaveable { mutableStateOf(false) }
    val isSaving = saveState is PersonalDataSaveUiState.Saving

    LaunchedEffect(uiState) {
        val loaded = uiState as? PersonalDataUiState.Loaded ?: return@LaunchedEffect
        displayName = loaded.form.displayName
        email = loaded.form.email
        phoneNumber = loaded.form.phoneNumber
        marketingOptIn = loaded.form.marketingOptIn
        appointmentReminderOptIn = loaded.form.appointmentReminderOptIn
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(bottom = contentPadding.calculateBottomPadding() + 24.dp),
    ) {
        PersonalDataHeader(onBack = onBack)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .offset(y = (-16).dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            when (uiState) {
                PersonalDataUiState.Idle,
                PersonalDataUiState.Loading -> PersonalDataStatusCard(
                    title = "A carregar dados pessoais",
                    body = "Estamos a validar a sessão e a obter o seu perfil.",
                    icon = PersonalDataStatusIcon.Loading,
                )

                PersonalDataUiState.Unauthenticated -> PersonalDataStatusCard(
                    title = "Sessão necessária",
                    body = "Entre na sua conta para consultar e atualizar os dados pessoais.",
                    icon = PersonalDataStatusIcon.Locked,
                    actionLabel = "Iniciar sessão",
                    onAction = onRequestSignIn,
                )

                is PersonalDataUiState.Error -> PersonalDataStatusCard(
                    title = "Não foi possível carregar",
                    body = uiState.message,
                    icon = PersonalDataStatusIcon.Error,
                    actionLabel = if (uiState.retryable) "Tentar novamente" else null,
                    onAction = onRetry,
                )

                is PersonalDataUiState.Loaded -> {
                    PersonalDataFormCard(
                        displayName = displayName,
                        email = email,
                        phoneNumber = phoneNumber,
                        marketingOptIn = marketingOptIn,
                        enabled = !isSaving,
                        onDisplayNameChange = {
                            displayName = it
                            onClearSaveState()
                        },
                        onPhoneNumberChange = {
                            phoneNumber = it
                            onClearSaveState()
                        },
                        onMarketingOptInChange = {
                            marketingOptIn = it
                            onClearSaveState()
                        },
                    )
                    PersonalDataSaveStatus(saveState = saveState)
                    Button(
                        onClick = {
                            onSave(
                                PersonalDataFormUi(
                                    displayName = displayName,
                                    email = email,
                                    phoneNumber = phoneNumber,
                                    marketingOptIn = marketingOptIn,
                                    appointmentReminderOptIn = appointmentReminderOptIn,
                                ),
                            )
                        },
                        enabled = !isSaving,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.tertiary,
                            contentColor = MaterialTheme.colorScheme.onTertiary,
                        ),
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onTertiary,
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Filled.Save,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Guardar alterações",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }

            if (sessionState == AuthSessionState.Restoring) {
                Text(
                    text = "A sessão ainda está a ser restaurada.",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun PersonalDataHeader(onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.inverseSurface,
                        MaterialTheme.colorScheme.secondary,
                    ),
                ),
            )
            .safeDrawingPadding()
            .padding(horizontal = 24.dp)
            .padding(top = 8.dp, bottom = 34.dp),
    ) {
        OutlinedButton(
            onClick = onBack,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.42f)),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.tertiaryContainer,
            ),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text("Voltar", style = MaterialTheme.typography.labelLarge)
        }

        Spacer(Modifier.height(24.dp))
        Text(
            text = "Dados Pessoais",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.inverseOnSurface,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Perfil associado à sua conta Suds & Shine",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = 0.72f),
        )
    }
}

@Composable
private fun PersonalDataFormCard(
    displayName: String,
    email: String,
    phoneNumber: String,
    marketingOptIn: Boolean,
    enabled: Boolean,
    onDisplayNameChange: (String) -> Unit,
    onPhoneNumberChange: (String) -> Unit,
    onMarketingOptInChange: (Boolean) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        shape = RoundedCornerShape(18.dp),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            PersonalDataField(
                label = "Nome completo",
                value = displayName,
                placeholder = "João Silva",
                icon = Icons.Filled.Person,
                enabled = enabled,
                onValueChange = onDisplayNameChange,
            )
            PersonalDataField(
                label = "Email",
                value = email,
                placeholder = "seuemail@exemplo.com",
                icon = Icons.Filled.Mail,
                enabled = false,
                readOnly = true,
                keyboardType = KeyboardType.Email,
                onValueChange = {},
            )
            PersonalDataField(
                label = "Telemóvel",
                value = phoneNumber,
                placeholder = "913 005 855",
                icon = Icons.Filled.Phone,
                enabled = enabled,
                keyboardType = KeyboardType.Phone,
                imeAction = ImeAction.Done,
                onValueChange = onPhoneNumberChange,
            )
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(18.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = enabled) { onMarketingOptInChange(!marketingOptIn) }
                .padding(18.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Checkbox(
                checked = marketingOptIn,
                onCheckedChange = if (enabled) onMarketingOptInChange else null,
                colors = CheckboxDefaults.colors(
                    checkedColor = MaterialTheme.colorScheme.tertiary,
                    checkmarkColor = MaterialTheme.colorScheme.onTertiary,
                    uncheckedColor = MaterialTheme.colorScheme.outline,
                ),
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Receber novidades e campanhas",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "Pode alterar esta preferência a qualquer momento.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun PersonalDataField(
    label: String,
    value: String,
    placeholder: String,
    icon: ImageVector,
    enabled: Boolean,
    onValueChange: (String) -> Unit,
    readOnly: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        enabled = enabled,
        readOnly = readOnly,
        label = {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
            )
        },
        placeholder = {
            Text(
                text = placeholder,
                style = MaterialTheme.typography.bodyMedium,
            )
        },
        leadingIcon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
            )
        },
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        textStyle = MaterialTheme.typography.bodyMedium,
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboardType,
            imeAction = imeAction,
        ),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.tertiary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
            disabledBorderColor = MaterialTheme.colorScheme.outlineVariant,
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            focusedLabelColor = MaterialTheme.colorScheme.onSurface,
            unfocusedLabelColor = MaterialTheme.colorScheme.onSurface,
            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
            focusedTextColor = MaterialTheme.colorScheme.onSurface,
            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
            disabledTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
            focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
            unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
            disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
    )
}

@Composable
private fun PersonalDataSaveStatus(saveState: PersonalDataSaveUiState) {
    when (saveState) {
        PersonalDataSaveUiState.Idle -> Unit
        PersonalDataSaveUiState.Saving -> PersonalDataInlineStatus(
            icon = PersonalDataStatusIcon.Loading,
            title = "A guardar alterações",
            body = "Estamos a atualizar os dados associados à sua conta.",
            error = false,
        )

        is PersonalDataSaveUiState.Saved -> PersonalDataInlineStatus(
            icon = PersonalDataStatusIcon.Success,
            title = "Dados atualizados",
            body = saveState.message,
            error = false,
        )

        is PersonalDataSaveUiState.ValidationError -> PersonalDataInlineStatus(
            icon = PersonalDataStatusIcon.Error,
            title = "Revise os dados",
            body = saveState.message,
            error = true,
        )

        is PersonalDataSaveUiState.Error -> PersonalDataInlineStatus(
            icon = PersonalDataStatusIcon.Error,
            title = "Não foi possível guardar",
            body = saveState.message,
            error = true,
        )
    }
}

private enum class PersonalDataStatusIcon {
    Loading,
    Locked,
    Error,
    Success,
}

@Composable
private fun PersonalDataInlineStatus(
    icon: PersonalDataStatusIcon,
    title: String,
    body: String,
    error: Boolean,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = if (error) {
            MaterialTheme.colorScheme.errorContainer
        } else {
            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f)
        },
        contentColor = if (error) {
            MaterialTheme.colorScheme.onErrorContainer
        } else {
            MaterialTheme.colorScheme.onSecondaryContainer
        },
    ) {
        PersonalDataStatusRow(
            icon = icon,
            title = title,
            body = body,
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Composable
private fun PersonalDataStatusCard(
    title: String,
    body: String,
    icon: PersonalDataStatusIcon,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        shape = RoundedCornerShape(18.dp),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            PersonalDataStatusRow(
                icon = icon,
                title = title,
                body = body,
            )
            if (actionLabel != null && onAction != null) {
                OutlinedButton(
                    onClick = onAction,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.tertiary,
                    ),
                ) {
                    Icon(
                        imageVector = if (icon == PersonalDataStatusIcon.Locked) {
                            Icons.Filled.Lock
                        } else {
                            Icons.Filled.Refresh
                        },
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = actionLabel,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

@Composable
private fun PersonalDataStatusRow(
    icon: PersonalDataStatusIcon,
    title: String,
    body: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Surface(
            modifier = Modifier.size(44.dp),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.30f),
            contentColor = MaterialTheme.colorScheme.tertiary,
        ) {
            Box(contentAlignment = Alignment.Center) {
                when (icon) {
                    PersonalDataStatusIcon.Loading -> CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = MaterialTheme.colorScheme.tertiary,
                        strokeWidth = 2.dp,
                    )
                    PersonalDataStatusIcon.Locked -> Icon(Icons.Filled.Lock, null)
                    PersonalDataStatusIcon.Error -> Icon(Icons.Filled.ErrorOutline, null)
                    PersonalDataStatusIcon.Success -> Icon(Icons.Filled.CheckCircle, null)
                }
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = body,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
