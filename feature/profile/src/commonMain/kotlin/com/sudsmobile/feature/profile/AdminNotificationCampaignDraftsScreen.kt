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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val mutationState by viewModel.mutationState.collectAsStateWithLifecycle()
    val sessionState by viewModel.sessionState.collectAsStateWithLifecycle()

    LaunchedEffect(sessionState) {
        viewModel.refreshForSession()
    }

    AdminNotificationCampaignDraftsScreenContent(
        contentPadding = contentPadding,
        uiState = uiState,
        mutationState = mutationState,
        onBack = onBack,
        onRequestSignIn = onRequestSignIn,
        onRetry = { viewModel.loadDrafts(force = true) },
        onStartCreate = viewModel::startCreate,
        onEdit = viewModel::editDraft,
        onArchive = viewModel::archive,
        onSendTest = viewModel::sendTest,
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
    onBack: () -> Unit,
    onRequestSignIn: () -> Unit,
    onRetry: () -> Unit,
    onStartCreate: () -> Unit,
    onEdit: (String) -> Unit,
    onArchive: (String) -> Unit,
    onSendTest: (String) -> Unit,
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
            AdminNotificationCampaignDraftMutationBanner(
                mutationState = mutationState,
                onDismiss = onDismissMutationState,
            )

            when (uiState) {
                AdminNotificationCampaignDraftsUiState.Idle,
                AdminNotificationCampaignDraftsUiState.Loading -> AdminNotificationCampaignDraftStatusCard(
                    title = "A carregar campanhas",
                    body = "Estamos a consultar os rascunhos configurados.",
                    icon = Icons.Filled.Notifications,
                    loading = true,
                )

                AdminNotificationCampaignDraftsUiState.Unauthenticated -> AdminNotificationCampaignDraftStatusCard(
                    title = "Sessão necessária",
                    body = "Entre com uma conta de administrador para gerir campanhas.",
                    icon = Icons.Filled.Lock,
                    actionLabel = "Entrar ou criar conta",
                    onAction = onRequestSignIn,
                )

                AdminNotificationCampaignDraftsUiState.NotAdmin -> AdminNotificationCampaignDraftStatusCard(
                    title = "Acesso reservado",
                    body = "Campanhas só podem ser alteradas por administradores.",
                    icon = Icons.Filled.Security,
                )

                AdminNotificationCampaignDraftsUiState.Empty -> AdminNotificationCampaignDraftStatusCard(
                    title = "Sem rascunhos",
                    body = "Crie o primeiro rascunho push sem ativar envio.",
                    icon = Icons.Filled.Notifications,
                    actionLabel = "Novo rascunho",
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
                    onFormChange = onFormChange,
                    onCancelEdit = onCancelEdit,
                    onSave = onSave,
                )
            }
        }
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
                text = "Campanhas",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.inverseOnSurface,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "Rascunhos push com envio bloqueado",
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
    onFormChange: (AdminNotificationCampaignDraftForm) -> Unit,
    onCancelEdit: () -> Unit,
    onSave: () -> Unit,
) {
    val saving = mutationState == AdminNotificationCampaignDraftMutationState.Saving
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
            text = "Novo rascunho",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
        )
    }

    state.drafts.forEach { draft ->
        AdminNotificationCampaignDraftCard(
            draft = draft,
            archiving = mutationState == AdminNotificationCampaignDraftMutationState.Archiving(draft.campaignId),
            testing = mutationState == AdminNotificationCampaignDraftMutationState.Testing(draft.campaignId),
            onEdit = { onEdit(draft.campaignId) },
            onArchive = { onArchive(draft.campaignId) },
            onSendTest = { onSendTest(draft.campaignId) },
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
                text = if (form.isEditingExisting) "Editar rascunho" else "Novo rascunho",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
            )
            AdminNotificationCampaignDraftTextField(
                value = form.campaignId,
                onValueChange = { onFormChange(form.copy(campaignId = it)) },
                label = "ID do rascunho",
                enabled = !saving && !form.isEditingExisting,
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
            CampaignAudienceSelector(
                selectedAudience = form.targetAudience,
                enabled = !saving,
                onAudienceSelected = { onFormChange(form.copy(targetAudience = it)) },
            )
            AdminNotificationCampaignDraftTextField(
                value = form.scheduledAtIso,
                onValueChange = { onFormChange(form.copy(scheduledAtIso = it)) },
                label = "Agendamento ISO UTC",
                enabled = !saving,
            )
            AdminNotificationCampaignDraftTextField(
                value = form.notes,
                onValueChange = { onFormChange(form.copy(notes = it)) },
                label = "Notas internas",
                enabled = !saving,
                minLines = 2,
            )

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
private fun CampaignAudienceSelector(
    selectedAudience: String,
    enabled: Boolean,
    onAudienceSelected: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Público",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CampaignDraftAudienceKeys.forEach { audience ->
                val selected = selectedAudience == audience
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(enabled = enabled) { onAudienceSelected(audience) },
                    color = if (selected) {
                        MaterialTheme.colorScheme.tertiary
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerLow
                    },
                    contentColor = if (selected) {
                        MaterialTheme.colorScheme.onTertiary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text(
                        text = audience.toAudienceLabel(),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 12.dp),
                        style = MaterialTheme.typography.labelMedium,
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
    onEdit: () -> Unit,
    onArchive: () -> Unit,
    onSendTest: () -> Unit,
) {
    val archived = draft.status == "archived"
    val busy = archiving || testing
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
                    Text(
                        text = draft.campaignId,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
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
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CampaignMetric(label = "Público", value = draft.targetAudienceLabel, modifier = Modifier.weight(1f))
                CampaignMetric(label = "Agenda", value = draft.scheduledAtLabel, modifier = Modifier.weight(1f))
            }
            if (draft.notes.isNotBlank()) {
                Text(
                    text = draft.notes,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (
                draft.createdAuditLabel.isNotBlank() ||
                draft.updatedAuditLabel.isNotBlank() ||
                draft.archivedAuditLabel.isNotBlank()
            ) {
                CampaignAuditTrail(draft = draft)
            }
            CampaignStatusPill(
                text = if (draft.sendBlocked) "Envio bloqueado" else "Envio indisponível",
                emphasized = true,
            )
            OutlinedButton(
                onClick = onSendTest,
                enabled = !busy && !archived,
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
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = onEdit,
                    enabled = !busy && !archived,
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
                    enabled = !busy && !archived,
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
private fun CampaignAuditTrail(draft: AdminNotificationCampaignDraftUi) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = "Auditoria",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
            )
            if (draft.createdAuditLabel.isNotBlank()) {
                Text(text = draft.createdAuditLabel, style = MaterialTheme.typography.labelMedium)
            }
            if (draft.updatedAuditLabel.isNotBlank()) {
                Text(text = draft.updatedAuditLabel, style = MaterialTheme.typography.labelMedium)
            }
            if (draft.archivedAuditLabel.isNotBlank()) {
                Text(text = draft.archivedAuditLabel, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
private fun CampaignMetric(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
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
        is AdminNotificationCampaignDraftMutationState.Testing -> Unit
        is AdminNotificationCampaignDraftMutationState.Success -> AdminNotificationCampaignDraftStatusCard(
            title = "Campanhas atualizadas",
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

private fun String.toAudienceLabel(): String {
    return when (this) {
        "marketing_opt_in_users" -> "Marketing opt-in"
        else -> "Teste"
    }
}
