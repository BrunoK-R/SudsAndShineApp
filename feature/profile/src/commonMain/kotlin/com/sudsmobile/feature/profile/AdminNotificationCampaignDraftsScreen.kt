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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AdminNotificationCampaignDraftsScreen(
    contentPadding: PaddingValues,
    onBack: () -> Unit,
    onRequestSignIn: () -> Unit = {},
) {
    val viewModel: AdminNotificationCampaignDraftsViewModel = koinViewModel()
    val notificationPreferencesViewModel: NotificationPreferencesViewModel = koinViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val mutationState by viewModel.mutationState.collectAsStateWithLifecycle()
    val sessionState by viewModel.sessionState.collectAsStateWithLifecycle()
    val deviceState by notificationPreferencesViewModel.deviceState.collectAsStateWithLifecycle()
    val permissionRequestController = rememberNotificationPermissionRequestController(
        onPermissionResult = notificationPreferencesViewModel::handlePermissionResult,
    )

    LaunchedEffect(sessionState) {
        viewModel.refreshForSession()
        notificationPreferencesViewModel.refreshForSession()
    }

    AdminNotificationCampaignDraftsScreenContent(
        contentPadding = contentPadding,
        uiState = uiState,
        mutationState = mutationState,
        deviceState = deviceState,
        onBack = onBack,
        onRequestSignIn = onRequestSignIn,
        onRetry = { viewModel.loadDrafts(force = true) },
        onStartCreate = viewModel::startCreate,
        onEdit = viewModel::editDraft,
        onArchive = viewModel::archive,
        onSendTest = viewModel::sendTest,
        onBroadcast = viewModel::broadcast,
        onEnableDevice = {
            if (permissionRequestController.shouldRequestPostNotifications) {
                permissionRequestController.requestPostNotifications()
            } else {
                notificationPreferencesViewModel.registerCurrentDevice()
            }
        },
        onFormChange = viewModel::updateForm,
        onCancelEdit = viewModel::cancelEdit,
        onSave = viewModel::save,
        onDismissMutationState = viewModel::clearMutationState,
    )
}

@Composable
private fun AdminNotificationCampaignDraftsScreenContent(
    contentPadding: PaddingValues,
    uiState: AdminNotificationCampaignDraftsUiState,
    mutationState: AdminNotificationCampaignDraftMutationState,
    deviceState: NotificationDeviceUiState,
    onBack: () -> Unit,
    onRequestSignIn: () -> Unit,
    onRetry: () -> Unit,
    onStartCreate: () -> Unit,
    onEdit: (String) -> Unit,
    onArchive: (String) -> Unit,
    onSendTest: (String) -> Unit,
    onBroadcast: (String) -> Unit,
    onEnableDevice: () -> Unit,
    onFormChange: (AdminNotificationCampaignDraftForm) -> Unit,
    onCancelEdit: () -> Unit,
    onSave: () -> Unit,
    onDismissMutationState: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(bottom = contentPadding.calculateBottomPadding() + 24.dp),
    ) {
        AdminNotificationCampaignDraftsHeader(onBack = onBack)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = (-16).dp)
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AdminNotificationDevicePrompt(
                uiState = uiState,
                deviceState = deviceState,
                onEnableDevice = onEnableDevice,
            )
            AdminNotificationCampaignDraftMutationBanner(
                mutationState = mutationState,
                onDismiss = onDismissMutationState,
            )

            when (uiState) {
                AdminNotificationCampaignDraftsUiState.Idle,
                AdminNotificationCampaignDraftsUiState.Loading -> AdminNotificationCampaignDraftStatusCard(
                    title = "A carregar notificações",
                    body = "Estamos a consultar as mensagens guardadas.",
                    icon = Icons.Filled.Notifications,
                    loading = true,
                )

                AdminNotificationCampaignDraftsUiState.Unauthenticated -> AdminNotificationCampaignDraftStatusCard(
                    title = "Sessão necessária",
                    body = "Entre com uma conta de administrador para enviar notificações.",
                    icon = Icons.Filled.Lock,
                    actionLabel = "Entrar ou criar conta",
                    onAction = onRequestSignIn,
                )

                AdminNotificationCampaignDraftsUiState.NotAdmin -> AdminNotificationCampaignDraftStatusCard(
                    title = "Acesso reservado",
                    body = "Só administradores podem enviar notificações.",
                    icon = Icons.Filled.Security,
                )

                AdminNotificationCampaignDraftsUiState.Empty -> AdminNotificationCampaignDraftStatusCard(
                    title = "Sem notificações",
                    body = "Crie uma notificação com título e mensagem.",
                    icon = Icons.Filled.Notifications,
                    actionLabel = "Nova notificação",
                    onAction = onStartCreate,
                )

                is AdminNotificationCampaignDraftsUiState.Error -> AdminNotificationCampaignDraftStatusCard(
                    title = "Não foi possível carregar",
                    body = uiState.message,
                    icon = Icons.Filled.ErrorOutline,
                    actionLabel = if (uiState.retryable) "Tentar novamente" else null,
                    onAction = if (uiState.retryable) onRetry else null,
                )

                is AdminNotificationCampaignDraftsUiState.Loaded -> AdminNotificationCampaignDraftsLoadedContent(
                    state = uiState,
                    mutationState = mutationState,
                    onStartCreate = onStartCreate,
                    onEdit = onEdit,
                    onArchive = onArchive,
                    onSendTest = onSendTest,
                    onBroadcast = onBroadcast,
                    onFormChange = onFormChange,
                    onCancelEdit = onCancelEdit,
                    onSave = onSave,
                )
            }
        }
    }
}

@Composable
private fun AdminNotificationDevicePrompt(
    uiState: AdminNotificationCampaignDraftsUiState,
    deviceState: NotificationDeviceUiState,
    onEnableDevice: () -> Unit,
) {
    val adminToolsAvailable = uiState is AdminNotificationCampaignDraftsUiState.Loaded ||
        uiState is AdminNotificationCampaignDraftsUiState.Empty
    if (!adminToolsAvailable) return

    val registeredTokenId = when (deviceState) {
        is NotificationDeviceUiState.Ready -> deviceState.registeredTokenId
        is NotificationDeviceUiState.Success -> deviceState.registeredTokenId
        else -> null
    }
    if (registeredTokenId != null) return

    when (deviceState) {
        NotificationDeviceUiState.Checking,
        NotificationDeviceUiState.Registering -> AdminNotificationCampaignDraftStatusCard(
            title = "A preparar notificações",
            body = "Estamos a verificar este dispositivo.",
            icon = Icons.Filled.Notifications,
            loading = true,
        )
        is NotificationDeviceUiState.Removing,
        NotificationDeviceUiState.Unauthenticated -> Unit
        is NotificationDeviceUiState.Unsupported -> AdminNotificationCampaignDraftStatusCard(
            title = "Notificações indisponíveis",
            body = deviceState.message,
            icon = Icons.Filled.ErrorOutline,
        )
        is NotificationDeviceUiState.PermissionRequired -> AdminNotificationCampaignDraftStatusCard(
            title = "Ativar notificações",
            body = deviceState.message,
            icon = Icons.Filled.Notifications,
            actionLabel = "Ativar",
            onAction = onEnableDevice,
        )
        is NotificationDeviceUiState.Error -> AdminNotificationCampaignDraftStatusCard(
            title = "Notificações não registadas",
            body = deviceState.message,
            icon = Icons.Filled.ErrorOutline,
            actionLabel = if (deviceState.retryable) "Tentar novamente" else null,
            onAction = if (deviceState.retryable) onEnableDevice else null,
        )
        is NotificationDeviceUiState.Ready,
        is NotificationDeviceUiState.Success -> AdminNotificationCampaignDraftStatusCard(
            title = "Ativar notificações",
            body = "Registe este dispositivo para receber testes e alertas administrativos.",
            icon = Icons.Filled.Notifications,
            actionLabel = "Ativar",
            onAction = onEnableDevice,
        )
    }
}

@Composable
private fun AdminNotificationCampaignDraftsHeader(onBack: () -> Unit) {
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
                text = "Escreva a mensagem e envie para os clientes.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = 0.68f),
            )
        }
    }
}

@Composable
private fun AdminNotificationCampaignDraftsLoadedContent(
    state: AdminNotificationCampaignDraftsUiState.Loaded,
    mutationState: AdminNotificationCampaignDraftMutationState,
    onStartCreate: () -> Unit,
    onEdit: (String) -> Unit,
    onArchive: (String) -> Unit,
    onSendTest: (String) -> Unit,
    onBroadcast: (String) -> Unit,
    onFormChange: (AdminNotificationCampaignDraftForm) -> Unit,
    onCancelEdit: () -> Unit,
    onSave: () -> Unit,
) {
    val saving = mutationState == AdminNotificationCampaignDraftMutationState.Saving
    var pendingBroadcastId by rememberSaveable { mutableStateOf<String?>(null) }
    val pendingBroadcastDraft = state.drafts.firstOrNull { it.campaignId == pendingBroadcastId }

    pendingBroadcastDraft?.let { draft ->
        AlertDialog(
            onDismissRequest = { pendingBroadcastId = null },
            icon = {
                Icon(
                    imageVector = Icons.Filled.Security,
                    contentDescription = null,
                )
            },
            title = { Text("Enviar notificação?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Esta mensagem será enviada para: ${draft.targetAudienceLabel}.")
                    Text("Confirme o título e a mensagem. O envio não pode ser anulado.")
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingBroadcastId = null
                        onBroadcast(draft.campaignId)
                    },
                ) {
                    Text("Confirmar envio")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingBroadcastId = null }) {
                    Text("Cancelar")
                }
            },
        )
    }

    state.form?.let { form ->
        AdminNotificationCampaignDraftFormCard(
            form = form,
            saving = saving,
            onFormChange = onFormChange,
            onCancel = onCancelEdit,
            onSave = onSave,
        )
    }

    OutlinedButton(
        onClick = onStartCreate,
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.tertiary),
    ) {
        Icon(
            imageVector = Icons.Filled.Add,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = "Nova notificação",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
        )
    }

    state.drafts.forEach { draft ->
        AdminNotificationCampaignDraftCard(
            draft = draft,
            archiving = mutationState == AdminNotificationCampaignDraftMutationState.Archiving(draft.campaignId),
            testing = mutationState == AdminNotificationCampaignDraftMutationState.Testing(draft.campaignId),
            broadcasting = mutationState == AdminNotificationCampaignDraftMutationState.Broadcasting(draft.campaignId),
            onEdit = { onEdit(draft.campaignId) },
            onArchive = { onArchive(draft.campaignId) },
            onSendTest = { onSendTest(draft.campaignId) },
            onBroadcast = { pendingBroadcastId = draft.campaignId },
        )
    }
}

@Composable
private fun AdminNotificationCampaignDraftFormCard(
    form: AdminNotificationCampaignDraftForm,
    saving: Boolean,
    onFormChange: (AdminNotificationCampaignDraftForm) -> Unit,
    onCancel: () -> Unit,
    onSave: () -> Unit,
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
            Text(
                text = if (form.isEditingExisting) "Editar notificação" else "Nova notificação",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
            )
            AdminNotificationCampaignDraftTextField(
                value = form.title,
                onValueChange = { onFormChange(form.copy(title = it)) },
                label = "Título",
                enabled = !saving,
            )
            AdminNotificationCampaignDraftTextField(
                value = form.body,
                onValueChange = { onFormChange(form.copy(body = it)) },
                label = "Mensagem",
                enabled = !saving,
                minLines = 3,
            )
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.55f),
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = "Público: " + if (form.targetAudience == "test_users") {
                            "Utilizadores de teste"
                        } else {
                            "Clientes com opt-in marketing"
                        },
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = if (form.targetAudience == "test_users") {
                            "O envio de teste fica limitado à conta administrativa atual."
                        } else {
                            "Só serão considerados dispositivos ativos de clientes que aceitaram marketing."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = onCancel,
                    enabled = !saving,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                ) {
                    Text("Cancelar", style = MaterialTheme.typography.labelLarge)
                }
                Button(
                    onClick = onSave,
                    enabled = !saving,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.tertiary,
                        contentColor = MaterialTheme.colorScheme.onTertiary,
                    ),
                ) {
                    if (saving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
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
                        text = if (saving) "A guardar" else "Guardar",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

@Composable
private fun AdminNotificationCampaignDraftCard(
    draft: AdminNotificationCampaignDraftUi,
    archiving: Boolean,
    testing: Boolean,
    broadcasting: Boolean,
    onEdit: () -> Unit,
    onArchive: () -> Unit,
    onSendTest: () -> Unit,
    onBroadcast: () -> Unit,
) {
    val archived = draft.status == "archived"
    val sent = draft.status == "sent"
    val busy = archiving || testing || broadcasting
    val canMutate = !busy && !archived && !sent
    val canBroadcast = canMutate && !draft.deliveryLocked && !draft.sendBlocked
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
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top,
            ) {
                AdminNotificationCampaignDraftIcon(icon = Icons.Filled.Notifications)
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = draft.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                CampaignStatusPill(text = draft.statusLabel)
            }
            Text(
                text = draft.body,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.45f),
            ) {
                Text(
                    text = "Público: ${draft.targetAudienceLabel}",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            if (draft.deliveryLocked || draft.sendBlocked || sent) {
                CampaignDeliveryLockPanel(draft = draft)
            }
            OutlinedButton(
                onClick = onSendTest,
                enabled = canMutate,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = MaterialTheme.shapes.medium,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.secondary),
            ) {
                if (testing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = MaterialTheme.colorScheme.secondary,
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
                    text = if (testing) "A enviar teste" else "Enviar teste",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                )
            }
            Button(
                onClick = onBroadcast,
                enabled = canBroadcast,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.tertiary,
                    contentColor = MaterialTheme.colorScheme.onTertiary,
                ),
            ) {
                if (broadcasting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = MaterialTheme.colorScheme.onTertiary,
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
                    text = when {
                        broadcasting -> "A enviar"
                        draft.targetAudience == "test_users" -> "Enviar campanha de teste"
                        else -> "Enviar para clientes"
                    },
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = onEdit,
                    enabled = canMutate,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.tertiary),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Editar", style = MaterialTheme.typography.labelLarge)
                }
                OutlinedButton(
                    onClick = onArchive,
                    enabled = canMutate,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) {
                    if (archiving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = MaterialTheme.colorScheme.error,
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.Archive,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (archiving) "A arquivar" else "Arquivar",
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }
    }
}

@Composable
private fun CampaignDeliveryLockPanel(draft: AdminNotificationCampaignDraftUi) {
    val locked = draft.deliveryLocked || draft.sendBlocked
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = if (locked) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = if (locked) {
            MaterialTheme.colorScheme.onErrorContainer
        } else {
            MaterialTheme.colorScheme.onTertiaryContainer
        },
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(
                imageVector = if (locked) Icons.Filled.Lock else Icons.Filled.CheckCircle,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
            )
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = draft.sendStateLabel,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                )
                if (draft.sendBlockedReason.isNotBlank()) {
                    Text(
                        text = draft.sendBlockedReason,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}

@Composable
private fun CampaignStatusPill(
    text: String,
    emphasized: Boolean = false,
) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = if (emphasized) {
            MaterialTheme.colorScheme.errorContainer
        } else {
            MaterialTheme.colorScheme.tertiaryContainer
        },
        contentColor = if (emphasized) {
            MaterialTheme.colorScheme.onErrorContainer
        } else {
            MaterialTheme.colorScheme.onTertiaryContainer
        },
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun AdminNotificationCampaignDraftMutationBanner(
    mutationState: AdminNotificationCampaignDraftMutationState,
    onDismiss: () -> Unit,
) {
    when (mutationState) {
        AdminNotificationCampaignDraftMutationState.Idle,
        AdminNotificationCampaignDraftMutationState.Saving,
        is AdminNotificationCampaignDraftMutationState.Archiving,
        is AdminNotificationCampaignDraftMutationState.Testing,
        is AdminNotificationCampaignDraftMutationState.Broadcasting -> Unit
        is AdminNotificationCampaignDraftMutationState.Success -> AdminNotificationCampaignDraftStatusCard(
            title = "Notificações atualizadas",
            body = mutationState.message,
            icon = Icons.Filled.CheckCircle,
            actionLabel = "Fechar",
            onAction = onDismiss,
        )
        is AdminNotificationCampaignDraftMutationState.Error -> AdminNotificationCampaignDraftStatusCard(
            title = "Não foi possível concluir",
            body = mutationState.message,
            icon = Icons.Filled.ErrorOutline,
            actionLabel = "Fechar",
            onAction = onDismiss,
        )
    }
}

@Composable
private fun AdminNotificationCampaignDraftStatusCard(
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
            AdminNotificationCampaignDraftIcon(icon = icon)
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
private fun AdminNotificationCampaignDraftTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    enabled: Boolean,
    minLines: Int = 1,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        minLines = minLines,
        shape = RoundedCornerShape(14.dp),
        textStyle = MaterialTheme.typography.bodyMedium,
    )
}

@Composable
private fun AdminNotificationCampaignDraftIcon(icon: ImageVector) {
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
