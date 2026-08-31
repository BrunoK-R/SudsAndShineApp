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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sudsmobile.shared.ui.SudsCustomerScreen
import com.sudsmobile.shared.ui.SudsSecondaryTopBar
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun NotificationPreferencesScreen(
    contentPadding: PaddingValues,
    onBack: () -> Unit,
    onRequestSignIn: () -> Unit,
) {
    val viewModel: NotificationPreferencesViewModel = koinViewModel()
    val adminAccessViewModel: AdminAccessViewModel = koinViewModel()
    val sessionState by viewModel.sessionState.collectAsStateWithLifecycle()
    val adminAccessState by adminAccessViewModel.uiState.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val saveState by viewModel.saveState.collectAsStateWithLifecycle()
    val deviceState by viewModel.deviceState.collectAsStateWithLifecycle()
    val permissionRequestController = rememberNotificationPermissionRequestController(
        onPermissionResult = viewModel::handlePermissionResult,
    )

    LaunchedEffect(sessionState) {
        viewModel.refreshForSession()
        adminAccessViewModel.refreshForSession()
    }

    NotificationPreferencesScreenContent(
        contentPadding = contentPadding,
        uiState = uiState,
        showAdminAlertPreference = adminAccessState is AdminAccessUiState.Admin,
        saveState = saveState,
        deviceState = deviceState,
        onBack = onBack,
        onRequestSignIn = onRequestSignIn,
        onRetry = viewModel::loadPreferences,
        onFormChange = viewModel::updateForm,
        onSave = viewModel::save,
        onEnableDevice = {
            if (permissionRequestController.shouldRequestPostNotifications) {
                permissionRequestController.requestPostNotifications()
            } else {
                viewModel.registerCurrentDevice()
            }
        },
        onRemoveDevice = viewModel::removeCurrentDevice,
        onDismissSaveState = viewModel::clearSaveState,
    )
}

@Composable
private fun NotificationPreferencesScreenContent(
    contentPadding: PaddingValues,
    uiState: NotificationPreferencesUiState,
    showAdminAlertPreference: Boolean,
    saveState: NotificationPreferencesSaveState,
    deviceState: NotificationDeviceUiState,
    onBack: () -> Unit,
    onRequestSignIn: () -> Unit,
    onRetry: () -> Unit,
    onFormChange: (NotificationPreferencesForm) -> Unit,
    onSave: () -> Unit,
    onEnableDevice: () -> Unit,
    onRemoveDevice: () -> Unit,
    onDismissSaveState: () -> Unit,
) {
    SudsCustomerScreen(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = contentPadding.calculateBottomPadding() + 24.dp),
        ) {
            NotificationPreferencesHeader(onBack = onBack)

            Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(top = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            NotificationPreferencesSaveBanner(
                saveState = saveState,
                onDismiss = onDismissSaveState,
                onRetry = onSave,
            )

            when (uiState) {
                NotificationPreferencesUiState.Idle,
                NotificationPreferencesUiState.Loading -> NotificationPreferencesStatusCard(
                    title = "A carregar notificações",
                    body = "Estamos a consultar as preferências da conta.",
                    icon = Icons.Filled.Notifications,
                    loading = true,
                )

                NotificationPreferencesUiState.Unauthenticated -> NotificationPreferencesStatusCard(
                    title = "Sessão necessária",
                    body = "Entre na sua conta para gerir notificações.",
                    icon = Icons.Filled.Lock,
                    actionLabel = "Iniciar sessão",
                    onAction = onRequestSignIn,
                )

                is NotificationPreferencesUiState.Error -> NotificationPreferencesStatusCard(
                    title = "Não foi possível carregar",
                    body = uiState.message,
                    icon = Icons.Filled.ErrorOutline,
                    actionLabel = if (uiState.retryable) "Tentar novamente" else null,
                    onAction = if (uiState.retryable) onRetry else null,
                )

                is NotificationPreferencesUiState.Loaded -> NotificationPreferencesFormCard(
                    form = uiState.form,
                    showAdminAlertPreference = showAdminAlertPreference,
                    deviceState = deviceState,
                    saving = saveState == NotificationPreferencesSaveState.Saving,
                    onFormChange = onFormChange,
                    onSave = onSave,
                    onEnableDevice = onEnableDevice,
                    onRemoveDevice = onRemoveDevice,
                )
            }
            }
        }
    }
}

@Composable
private fun NotificationPreferencesHeader(onBack: () -> Unit) {
    SudsSecondaryTopBar(
        title = "Notificações",
        eyebrow = "Preferências da conta",
        onBack = onBack,
    )
}

@Composable
private fun NotificationPreferencesSaveBanner(
    saveState: NotificationPreferencesSaveState,
    onDismiss: () -> Unit,
    onRetry: () -> Unit,
) {
    when (saveState) {
        NotificationPreferencesSaveState.Idle,
        NotificationPreferencesSaveState.Saving -> Unit
        is NotificationPreferencesSaveState.Success -> NotificationPreferencesStatusCard(
            title = "Notificações guardadas",
            body = saveState.message,
            icon = Icons.Filled.CheckCircle,
            actionLabel = "Fechar",
            onAction = onDismiss,
        )
        is NotificationPreferencesSaveState.Error -> NotificationPreferencesStatusCard(
            title = "Não foi possível guardar",
            body = saveState.message,
            icon = Icons.Filled.ErrorOutline,
            actionLabel = if (saveState.retryable) "Tentar novamente" else "Fechar",
            onAction = if (saveState.retryable) onRetry else onDismiss,
        )
    }
}

@Composable
private fun NotificationPreferencesStatusCard(
    title: String,
    body: String,
    icon: ImageVector,
    loading: Boolean = false,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.Top,
        ) {
            NotificationPreferencesIconContainer(icon = icon, loading = loading)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
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
                if (actionLabel != null && onAction != null) {
                    OutlinedButton(
                        onClick = onAction,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.tertiary,
                        ),
                    ) {
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
}

@Composable
private fun NotificationPreferencesFormCard(
    form: NotificationPreferencesForm,
    showAdminAlertPreference: Boolean,
    deviceState: NotificationDeviceUiState,
    saving: Boolean,
    onFormChange: (NotificationPreferencesForm) -> Unit,
    onSave: () -> Unit,
    onEnableDevice: () -> Unit,
    onRemoveDevice: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            NotificationDeviceEnrollmentCard(
                deviceState = deviceState,
                actionsEnabled = !saving,
                onEnableDevice = onEnableDevice,
                onRemoveDevice = onRemoveDevice,
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            NotificationPreferenceSwitchRow(
                title = "Atualizações de marcação",
                description = "Pedidos, confirmações, rejeições e expirações",
                checked = form.bookingStatusEnabled,
                enabled = !saving,
                onCheckedChange = { onFormChange(form.copy(bookingStatusEnabled = it)) },
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            NotificationPreferenceSwitchRow(
                title = "Lembretes de marcação",
                description = "Avisos antes da hora marcada",
                checked = form.appointmentReminderEnabled,
                enabled = !saving,
                onCheckedChange = { onFormChange(form.copy(appointmentReminderEnabled = it)) },
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            NotificationPreferenceSwitchRow(
                title = "Fidelização e recompensas",
                description = "Selos, prémios e benefícios",
                checked = form.loyaltyEnabled,
                enabled = !saving,
                onCheckedChange = { onFormChange(form.copy(loyaltyEnabled = it)) },
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            if (showAdminAlertPreference) {
                NotificationPreferenceSwitchRow(
                    title = "Alertas operacionais",
                    description = "Pedidos pendentes para administradores",
                    checked = form.adminPendingAlertEnabled,
                    enabled = !saving,
                    onCheckedChange = { onFormChange(form.copy(adminPendingAlertEnabled = it)) },
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
            NotificationPreferenceSwitchRow(
                title = "Marketing",
                description = "Ofertas e campanhas ocasionais",
                checked = form.marketingEnabled,
                enabled = !saving,
                onCheckedChange = { onFormChange(form.copy(marketingEnabled = it)) },
            )
            Button(
                onClick = onSave,
                enabled = !saving,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.tertiary,
                    contentColor = MaterialTheme.colorScheme.onTertiary,
                ),
            ) {
                if (saving) {
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
                        text = "Guardar preferências",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

@Composable
private fun NotificationDeviceEnrollmentCard(
    deviceState: NotificationDeviceUiState,
    actionsEnabled: Boolean,
    onEnableDevice: () -> Unit,
    onRemoveDevice: () -> Unit,
) {
    val isBusy = deviceState == NotificationDeviceUiState.Checking ||
        deviceState == NotificationDeviceUiState.Registering ||
        deviceState is NotificationDeviceUiState.Removing
    val registeredTokenId = when (deviceState) {
        is NotificationDeviceUiState.Ready -> deviceState.registeredTokenId
        is NotificationDeviceUiState.Success -> deviceState.registeredTokenId
        else -> null
    }
    val title = when {
        registeredTokenId != null -> "Este dispositivo está ativo"
        deviceState is NotificationDeviceUiState.PermissionRequired -> "Permissão necessária"
        deviceState is NotificationDeviceUiState.Unsupported -> "Indisponível neste dispositivo"
        deviceState is NotificationDeviceUiState.Error -> "Não foi possível ativar"
        else -> "Este dispositivo"
    }
    val body = when (deviceState) {
        NotificationDeviceUiState.Checking -> "A verificar o estado das notificações."
        NotificationDeviceUiState.Unauthenticated -> "Entre na sua conta para ativar este dispositivo."
        NotificationDeviceUiState.Registering -> "A registar este dispositivo para notificações."
        is NotificationDeviceUiState.Removing -> "A remover este dispositivo das notificações."
        is NotificationDeviceUiState.PermissionRequired -> deviceState.message
        is NotificationDeviceUiState.Unsupported -> deviceState.message
        is NotificationDeviceUiState.Error -> deviceState.message
        is NotificationDeviceUiState.Success -> deviceState.message
        is NotificationDeviceUiState.Ready -> {
            if (deviceState.registeredTokenId != null) {
                "Vai receber notificações permitidas pelas preferências abaixo."
            } else {
                "Ative para receber atualizações neste dispositivo."
            }
        }
    }
    val icon = when {
        registeredTokenId != null -> Icons.Filled.NotificationsActive
        deviceState is NotificationDeviceUiState.Unsupported -> Icons.Filled.NotificationsOff
        deviceState is NotificationDeviceUiState.Error -> Icons.Filled.ErrorOutline
        else -> Icons.Filled.Notifications
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.Top,
        ) {
            NotificationPreferencesIconContainer(icon = icon, loading = isBusy)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                NotificationDeviceActionRow(
                    deviceState = deviceState,
                    registeredTokenId = registeredTokenId,
                    actionsEnabled = actionsEnabled && !isBusy,
                    onEnableDevice = onEnableDevice,
                    onRemoveDevice = onRemoveDevice,
                )
            }
        }
    }
}

@Composable
private fun NotificationDeviceActionRow(
    deviceState: NotificationDeviceUiState,
    registeredTokenId: String?,
    actionsEnabled: Boolean,
    onEnableDevice: () -> Unit,
    onRemoveDevice: () -> Unit,
) {
    when {
        registeredTokenId != null -> OutlinedButton(
            onClick = onRemoveDevice,
            enabled = actionsEnabled,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
        ) {
            Text(
                text = "Remover deste dispositivo",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
            )
        }
        deviceState is NotificationDeviceUiState.Ready ||
            deviceState is NotificationDeviceUiState.PermissionRequired ||
            deviceState is NotificationDeviceUiState.Error -> Button(
                onClick = onEnableDevice,
                enabled = actionsEnabled,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.tertiary,
                    contentColor = MaterialTheme.colorScheme.onTertiary,
                ),
            ) {
                Text(
                    text = if (deviceState is NotificationDeviceUiState.Error && deviceState.retryable) {
                        "Tentar novamente"
                    } else {
                        "Ativar neste dispositivo"
                    },
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                )
            }
        else -> Unit
    }
}

@Composable
private fun NotificationPreferenceSwitchRow(
    title: String,
    description: String,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(
            checked = checked,
            enabled = enabled,
            onCheckedChange = if (enabled) onCheckedChange else null,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onTertiary,
                checkedTrackColor = MaterialTheme.colorScheme.tertiary,
                checkedBorderColor = MaterialTheme.colorScheme.tertiary,
                uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                uncheckedBorderColor = MaterialTheme.colorScheme.outline,
                disabledUncheckedThumbColor = MaterialTheme.colorScheme.outlineVariant,
                disabledUncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainer,
                disabledUncheckedBorderColor = MaterialTheme.colorScheme.outlineVariant,
            ),
        )
    }
}

@Composable
private fun NotificationPreferencesIconContainer(
    icon: ImageVector,
    loading: Boolean,
) {
    Surface(
        modifier = Modifier.size(44.dp),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    color = MaterialTheme.colorScheme.secondary,
                    strokeWidth = 2.dp,
                )
            } else {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
    }
}
