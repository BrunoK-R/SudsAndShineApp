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
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AdminNotificationSettingsScreen(
    contentPadding: PaddingValues,
    onBack: () -> Unit,
    onRequestSignIn: () -> Unit = {},
) {
    val viewModel: AdminNotificationSettingsViewModel = koinViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val saveState by viewModel.saveState.collectAsStateWithLifecycle()
    val testState by viewModel.testState.collectAsStateWithLifecycle()
    val sessionState by viewModel.sessionState.collectAsStateWithLifecycle()

    LaunchedEffect(sessionState) {
        viewModel.refreshForSession()
    }

    AdminNotificationSettingsScreenContent(
        contentPadding = contentPadding,
        uiState = uiState,
        saveState = saveState,
        testState = testState,
        onBack = onBack,
        onRequestSignIn = onRequestSignIn,
        onRetry = { viewModel.loadConfiguration() },
        onFormChange = viewModel::updateForm,
        onTemplateChange = viewModel::updateTemplate,
        onSave = viewModel::save,
        onSendTest = viewModel::sendTest,
        onDismissSaveState = viewModel::clearSaveState,
        onDismissTestState = viewModel::clearTestState,
    )
}

@Composable
private fun AdminNotificationSettingsScreenContent(
    contentPadding: PaddingValues,
    uiState: AdminNotificationSettingsUiState,
    saveState: AdminNotificationSettingsSaveState,
    testState: AdminNotificationTestState,
    onBack: () -> Unit,
    onRequestSignIn: () -> Unit,
    onRetry: () -> Unit,
    onFormChange: (AdminNotificationSettingsForm) -> Unit,
    onTemplateChange: (AdminNotificationTemplateForm) -> Unit,
    onSave: () -> Unit,
    onSendTest: (String) -> Unit,
    onDismissSaveState: () -> Unit,
    onDismissTestState: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(bottom = contentPadding.calculateBottomPadding() + 24.dp),
    ) {
        AdminNotificationSettingsHeader(onBack = onBack)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = (-16).dp)
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AdminNotificationSettingsSaveBanner(
                saveState = saveState,
                onDismiss = onDismissSaveState,
            )
            AdminNotificationSettingsTestBanner(
                testState = testState,
                onDismiss = onDismissTestState,
            )

            when (uiState) {
                AdminNotificationSettingsUiState.Idle,
                AdminNotificationSettingsUiState.Loading -> AdminNotificationSettingsStatusCard(
                    title = "A carregar notificações",
                    body = "Estamos a consultar modelos e canais ativos.",
                    icon = Icons.Filled.Notifications,
                    loading = true,
                )

                AdminNotificationSettingsUiState.Unauthenticated -> AdminNotificationSettingsStatusCard(
                    title = "Sessão necessária",
                    body = "Entre com uma conta de administrador para editar notificações.",
                    icon = Icons.Filled.Lock,
                    actionLabel = "Entrar ou criar conta",
                    onAction = onRequestSignIn,
                )

                AdminNotificationSettingsUiState.NotAdmin -> AdminNotificationSettingsStatusCard(
                    title = "Acesso reservado",
                    body = "As notificações só podem ser alteradas por administradores.",
                    icon = Icons.Filled.Security,
                )

                is AdminNotificationSettingsUiState.Error -> AdminNotificationSettingsStatusCard(
                    title = "Não foi possível carregar",
                    body = uiState.message,
                    icon = Icons.Filled.ErrorOutline,
                    actionLabel = if (uiState.retryable) "Tentar novamente" else null,
                    onAction = if (uiState.retryable) onRetry else null,
                )

                is AdminNotificationSettingsUiState.Loaded -> AdminNotificationSettingsFormCard(
                    form = uiState.form,
                    saving = saveState == AdminNotificationSettingsSaveState.Saving,
                    sendingTemplateKey = (testState as? AdminNotificationTestState.Sending)?.templateKey,
                    onFormChange = onFormChange,
                    onTemplateChange = onTemplateChange,
                    onSave = onSave,
                    onSendTest = onSendTest,
                )
            }
        }
    }
}

@Composable
private fun AdminNotificationSettingsHeader(onBack: () -> Unit) {
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
            .padding(top = 18.dp, bottom = 38.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .clickable(onClick = onBack)
                .padding(vertical = 8.dp, horizontal = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiaryContainer,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Voltar",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.tertiaryContainer,
                fontWeight = FontWeight.Bold,
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "Notificações",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.inverseOnSurface,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "Canais, silêncio e modelos de ciclo de marcação",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = 0.68f),
            )
        }
    }
}

@Composable
private fun AdminNotificationSettingsSaveBanner(
    saveState: AdminNotificationSettingsSaveState,
    onDismiss: () -> Unit,
) {
    when (saveState) {
        AdminNotificationSettingsSaveState.Idle,
        AdminNotificationSettingsSaveState.Saving -> Unit
        is AdminNotificationSettingsSaveState.Success -> AdminNotificationSettingsStatusCard(
            title = "Notificações guardadas",
            body = saveState.message,
            icon = Icons.Filled.CheckCircle,
            actionLabel = "Fechar",
            onAction = onDismiss,
        )
        is AdminNotificationSettingsSaveState.Error -> AdminNotificationSettingsStatusCard(
            title = "Não foi possível guardar",
            body = saveState.message,
            icon = Icons.Filled.ErrorOutline,
            actionLabel = "Fechar",
            onAction = onDismiss,
        )
    }
}

@Composable
private fun AdminNotificationSettingsTestBanner(
    testState: AdminNotificationTestState,
    onDismiss: () -> Unit,
) {
    when (testState) {
        AdminNotificationTestState.Idle,
        is AdminNotificationTestState.Sending -> Unit
        is AdminNotificationTestState.Success -> AdminNotificationSettingsStatusCard(
            title = "Teste em fila",
            body = "${testState.templateLabel}: ${testState.message}",
            icon = Icons.Filled.CheckCircle,
            actionLabel = "Fechar",
            onAction = onDismiss,
        )
        is AdminNotificationTestState.Error -> AdminNotificationSettingsStatusCard(
            title = "Teste não enviado",
            body = "${testState.templateLabel}: ${testState.message}",
            icon = Icons.Filled.ErrorOutline,
            actionLabel = "Fechar",
            onAction = onDismiss,
        )
    }
}

@Composable
private fun AdminNotificationSettingsStatusCard(
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
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            AdminNotificationIconContainer(icon = icon)
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
                if (loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = MaterialTheme.colorScheme.tertiary,
                        strokeWidth = 2.dp,
                    )
                }
                if (actionLabel != null && onAction != null) {
                    OutlinedButton(
                        onClick = onAction,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.tertiary,
                        ),
                    ) {
                        Text(actionLabel, style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminNotificationSettingsFormCard(
    form: AdminNotificationSettingsForm,
    saving: Boolean,
    sendingTemplateKey: String?,
    onFormChange: (AdminNotificationSettingsForm) -> Unit,
    onTemplateChange: (AdminNotificationTemplateForm) -> Unit,
    onSave: () -> Unit,
    onSendTest: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                AdminNotificationSwitchRow(
                    title = "Estados da marcação",
                    body = "Pedidos, aceitações, rejeições e expiração",
                    checked = form.bookingStatusEnabled,
                    enabled = !saving,
                    onCheckedChange = { onFormChange(form.copy(bookingStatusEnabled = it)) },
                )
                AdminNotificationSwitchRow(
                    title = "Lembretes de marcação",
                    body = "Avisos antes do horário confirmado",
                    checked = form.appointmentReminderEnabled,
                    enabled = !saving,
                    onCheckedChange = { onFormChange(form.copy(appointmentReminderEnabled = it)) },
                )
                AdminNotificationSwitchRow(
                    title = "Fidelização",
                    body = "Selos, recompensas e benefícios",
                    checked = form.loyaltyEnabled,
                    enabled = !saving,
                    onCheckedChange = { onFormChange(form.copy(loyaltyEnabled = it)) },
                )
                AdminNotificationSwitchRow(
                    title = "Alertas admin",
                    body = "Avisar administradores sobre pedidos pendentes",
                    checked = form.adminPendingAlertEnabled,
                    enabled = !saving,
                    onCheckedChange = { onFormChange(form.copy(adminPendingAlertEnabled = it)) },
                )
                AdminNotificationSwitchRow(
                    title = "Marketing",
                    body = "Mantido desligado salvo opt-in explícito do cliente",
                    checked = form.marketingEnabled,
                    enabled = !saving,
                    onCheckedChange = { onFormChange(form.copy(marketingEnabled = it)) },
                )
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = "Agendamento",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                )
                AdminNotificationTextField(
                    value = form.reminderLeadMinutes,
                    onValueChange = { onFormChange(form.copy(reminderLeadMinutes = it)) },
                    label = "Antecedência do lembrete (minutos)",
                    enabled = !saving,
                    keyboardType = KeyboardType.Number,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    AdminNotificationTextField(
                        value = form.quietHoursStart,
                        onValueChange = { onFormChange(form.copy(quietHoursStart = it)) },
                        label = "Silêncio início",
                        enabled = !saving,
                        modifier = Modifier.weight(1f),
                    )
                    AdminNotificationTextField(
                        value = form.quietHoursEnd,
                        onValueChange = { onFormChange(form.copy(quietHoursEnd = it)) },
                        label = "Silêncio fim",
                        enabled = !saving,
                        modifier = Modifier.weight(1f),
                    )
                }
                AdminNotificationTextField(
                    value = form.quietHoursTimeZone,
                    onValueChange = { onFormChange(form.copy(quietHoursTimeZone = it)) },
                    label = "Fuso horário",
                    enabled = !saving,
                )
            }
        }

        form.templates.forEach { template ->
            AdminNotificationTemplateCard(
                template = template,
                enabled = !saving && sendingTemplateKey == null,
                sending = sendingTemplateKey == template.key,
                onTemplateChange = onTemplateChange,
                onSendTest = onSendTest,
            )
        }

        Button(
            onClick = onSave,
            enabled = !saving,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
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
                    modifier = Modifier.size(18.dp),
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = if (saving) "A guardar" else "Guardar notificações",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun AdminNotificationTemplateCard(
    template: AdminNotificationTemplateForm,
    enabled: Boolean,
    sending: Boolean,
    onTemplateChange: (AdminNotificationTemplateForm) -> Unit,
    onSendTest: (String) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AdminNotificationSwitchRow(
                title = template.label,
                body = template.key,
                checked = template.enabled,
                enabled = enabled,
                onCheckedChange = { onTemplateChange(template.copy(enabled = it)) },
            )
            AdminNotificationTextField(
                value = template.title,
                onValueChange = { onTemplateChange(template.copy(title = it)) },
                label = "Título",
                enabled = enabled,
            )
            AdminNotificationTextField(
                value = template.body,
                onValueChange = { onTemplateChange(template.copy(body = it)) },
                label = "Mensagem",
                enabled = enabled,
                minLines = 2,
            )
            OutlinedButton(
                onClick = { onSendTest(template.key) },
                enabled = enabled && !sending,
                modifier = Modifier.fillMaxWidth(),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.tertiary,
                ),
                shape = RoundedCornerShape(14.dp),
            ) {
                if (sending) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = MaterialTheme.colorScheme.tertiary,
                        strokeWidth = 2.dp,
                    )
                } else {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (sending) "A enviar teste" else "Enviar teste",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun AdminNotificationSwitchRow(
    title: String,
    body: String,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp),
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
        }
        Switch(
            checked = checked,
            enabled = enabled,
            onCheckedChange = onCheckedChange,
        )
    }
}

@Composable
private fun AdminNotificationTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    minLines: Int = 1,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        enabled = enabled,
        modifier = modifier.fillMaxWidth(),
        label = { Text(label) },
        minLines = minLines,
        shape = RoundedCornerShape(14.dp),
        textStyle = MaterialTheme.typography.bodyMedium,
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = keyboardType),
    )
}

@Composable
private fun AdminNotificationIconContainer(icon: ImageVector) {
    Box(
        modifier = Modifier
            .size(42.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.tertiaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onTertiaryContainer,
            modifier = Modifier.size(22.dp),
        )
    }
}
