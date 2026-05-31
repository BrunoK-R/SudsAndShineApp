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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Lock
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
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AdminServiceCatalogScreen(
    contentPadding: PaddingValues,
    onBack: () -> Unit,
    onRequestSignIn: () -> Unit = {},
) {
    val viewModel: AdminServiceCatalogViewModel = koinViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val mutationState by viewModel.mutationState.collectAsStateWithLifecycle()
    val sessionState by viewModel.sessionState.collectAsStateWithLifecycle()

    LaunchedEffect(sessionState) {
        viewModel.refreshForSession()
    }

    AdminServiceCatalogScreenContent(
        contentPadding = contentPadding,
        uiState = uiState,
        mutationState = mutationState,
        onBack = onBack,
        onRequestSignIn = onRequestSignIn,
        onRetry = { viewModel.loadCatalog(force = true) },
        onStartCreate = viewModel::startCreate,
        onEdit = viewModel::editService,
        onArchive = viewModel::archive,
        onFormChange = viewModel::updateForm,
        onCancelEdit = viewModel::cancelEdit,
        onSave = viewModel::save,
        onDismissMutationState = viewModel::clearMutationState,
    )
}

@Composable
private fun AdminServiceCatalogScreenContent(
    contentPadding: PaddingValues,
    uiState: AdminServiceCatalogUiState,
    mutationState: AdminServiceCatalogMutationState,
    onBack: () -> Unit,
    onRequestSignIn: () -> Unit,
    onRetry: () -> Unit,
    onStartCreate: () -> Unit,
    onEdit: (String) -> Unit,
    onArchive: (String) -> Unit,
    onFormChange: (AdminServiceCatalogForm) -> Unit,
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
        AdminServiceCatalogHeader(onBack = onBack)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = (-16).dp)
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AdminServiceCatalogMutationBanner(
                mutationState = mutationState,
                onDismiss = onDismissMutationState,
            )

            when (uiState) {
                AdminServiceCatalogUiState.Idle,
                AdminServiceCatalogUiState.Loading -> AdminServiceCatalogStatusCard(
                    title = "A carregar catálogo",
                    body = "Estamos a consultar os serviços configurados.",
                    icon = Icons.Filled.Build,
                    loading = true,
                )

                AdminServiceCatalogUiState.Unauthenticated -> AdminServiceCatalogStatusCard(
                    title = "Sessão necessária",
                    body = "Entre com uma conta de administrador para gerir os serviços.",
                    icon = Icons.Filled.Lock,
                    actionLabel = "Entrar ou criar conta",
                    onAction = onRequestSignIn,
                )

                AdminServiceCatalogUiState.NotAdmin -> AdminServiceCatalogStatusCard(
                    title = "Acesso reservado",
                    body = "O catálogo só pode ser alterado por administradores.",
                    icon = Icons.Filled.Security,
                )

                AdminServiceCatalogUiState.Empty -> AdminServiceCatalogStatusCard(
                    title = "Sem serviços configurados",
                    body = "Crie o primeiro serviço para disponibilizar marcações.",
                    icon = Icons.Filled.Build,
                    actionLabel = "Criar serviço",
                    onAction = onStartCreate,
                )

                is AdminServiceCatalogUiState.Error -> AdminServiceCatalogStatusCard(
                    title = "Não foi possível carregar",
                    body = uiState.message,
                    icon = Icons.Filled.ErrorOutline,
                    actionLabel = if (uiState.retryable) "Tentar novamente" else null,
                    onAction = if (uiState.retryable) onRetry else null,
                )

                is AdminServiceCatalogUiState.Loaded -> AdminServiceCatalogLoadedContent(
                    state = uiState,
                    mutationState = mutationState,
                    onStartCreate = onStartCreate,
                    onEdit = onEdit,
                    onArchive = onArchive,
                    onFormChange = onFormChange,
                    onCancelEdit = onCancelEdit,
                    onSave = onSave,
                )
            }
        }
    }
}

@Composable
private fun AdminServiceCatalogHeader(onBack: () -> Unit) {
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
                text = "Catálogo de serviços",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.inverseOnSurface,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "Preços, duração e visibilidade das lavagens",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = 0.68f),
            )
        }
    }
}

@Composable
private fun AdminServiceCatalogLoadedContent(
    state: AdminServiceCatalogUiState.Loaded,
    mutationState: AdminServiceCatalogMutationState,
    onStartCreate: () -> Unit,
    onEdit: (String) -> Unit,
    onArchive: (String) -> Unit,
    onFormChange: (AdminServiceCatalogForm) -> Unit,
    onCancelEdit: () -> Unit,
    onSave: () -> Unit,
) {
    val saving = mutationState == AdminServiceCatalogMutationState.Saving
    state.form?.let { form ->
        AdminServiceCatalogFormCard(
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
            text = "Novo serviço",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
        )
    }

    state.services.forEach { service ->
        AdminServiceCatalogServiceCard(
            service = service,
            archiving = mutationState == AdminServiceCatalogMutationState.Archiving(service.id),
            onEdit = { onEdit(service.id) },
            onArchive = { onArchive(service.id) },
        )
    }
}

@Composable
private fun AdminServiceCatalogMutationBanner(
    mutationState: AdminServiceCatalogMutationState,
    onDismiss: () -> Unit,
) {
    when (mutationState) {
        AdminServiceCatalogMutationState.Idle,
        AdminServiceCatalogMutationState.Saving,
        is AdminServiceCatalogMutationState.Archiving -> Unit
        is AdminServiceCatalogMutationState.Success -> AdminServiceCatalogStatusCard(
            title = "Catálogo atualizado",
            body = mutationState.message,
            icon = Icons.Filled.CheckCircle,
            actionLabel = "Fechar",
            onAction = onDismiss,
        )
        is AdminServiceCatalogMutationState.Error -> AdminServiceCatalogStatusCard(
            title = "Não foi possível guardar",
            body = mutationState.message,
            icon = Icons.Filled.ErrorOutline,
            actionLabel = "Fechar",
            onAction = onDismiss,
        )
    }
}

@Composable
private fun AdminServiceCatalogStatusCard(
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
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Surface(
                    modifier = Modifier.size(42.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.28f),
                    contentColor = MaterialTheme.colorScheme.tertiary,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (loading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = MaterialTheme.colorScheme.tertiary,
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
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
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
                }
            }

            if (actionLabel != null && onAction != null) {
                OutlinedButton(
                    onClick = onAction,
                    modifier = Modifier.fillMaxWidth(),
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

@Composable
private fun AdminServiceCatalogServiceCard(
    service: AdminServiceCatalogServiceUi,
    archiving: Boolean,
    onEdit: () -> Unit,
    onArchive: () -> Unit,
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
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Surface(
                    modifier = Modifier.size(42.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = if (service.active) {
                        MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.28f)
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerHigh
                    },
                    contentColor = if (service.active) {
                        MaterialTheme.colorScheme.tertiary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.Build,
                            contentDescription = null,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = service.name,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        AdminServiceStatusPill(active = service.active)
                    }
                    Text(
                        text = service.id,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (service.description.isNotBlank()) {
                        Text(
                            text = service.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                AdminServiceCatalogMetric(
                    value = service.durationLabel,
                    label = "Duração",
                    modifier = Modifier.weight(1f),
                )
                AdminServiceCatalogMetric(
                    value = service.passengerPriceLabel,
                    label = "Ligeiro",
                    modifier = Modifier.weight(1f),
                )
                AdminServiceCatalogMetric(
                    value = service.suvPriceLabel,
                    label = "SUV",
                    modifier = Modifier.weight(1f),
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedButton(
                    onClick = onEdit,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.tertiary),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "Editar",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                    )
                }
                OutlinedButton(
                    onClick = onArchive,
                    enabled = service.active && !archiving,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
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
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Arquivar",
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
private fun AdminServiceCatalogFormCard(
    form: AdminServiceCatalogForm,
    saving: Boolean,
    onFormChange: (AdminServiceCatalogForm) -> Unit,
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
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = if (form.isEditingExisting) "Editar serviço" else "Novo serviço",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
            )
            AdminServiceTextField(
                value = form.serviceId,
                onValueChange = { onFormChange(form.copy(serviceId = it.take(80))) },
                label = "Identificador",
                enabled = !form.isEditingExisting,
                singleLine = true,
            )
            AdminServiceTextField(
                value = form.name,
                onValueChange = { onFormChange(form.copy(name = it.take(120))) },
                label = "Nome",
                singleLine = true,
            )
            AdminServiceTextField(
                value = form.description,
                onValueChange = { onFormChange(form.copy(description = it.take(1000))) },
                label = "Descrição",
                minLines = 2,
                maxLines = 4,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AdminServiceTextField(
                    value = form.durationMinutes,
                    onValueChange = { onFormChange(form.copy(durationMinutes = it.take(3))) },
                    label = "Minutos",
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                )
                AdminServiceTextField(
                    value = form.sortOrder,
                    onValueChange = { onFormChange(form.copy(sortOrder = it.take(4))) },
                    label = "Ordem",
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AdminServiceTextField(
                    value = form.passengerPrice,
                    onValueChange = { onFormChange(form.copy(passengerPrice = it.take(12))) },
                    label = "Ligeiro (€)",
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                )
                AdminServiceTextField(
                    value = form.suvPrice,
                    onValueChange = { onFormChange(form.copy(suvPrice = it.take(12))) },
                    label = "SUV (€)",
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                )
            }
            AdminServiceTextField(
                value = form.iconKey,
                onValueChange = { onFormChange(form.copy(iconKey = it.take(40))) },
                label = "Ícone",
                singleLine = true,
            )
            AdminServiceSwitchRow(
                label = "Disponível para clientes",
                checked = form.active,
                onCheckedChange = { onFormChange(form.copy(active = it)) },
            )
            AdminServiceSwitchRow(
                label = "Destacar como popular",
                checked = form.popular,
                onCheckedChange = { onFormChange(form.copy(popular = it)) },
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                ) {
                    Text(
                        text = "Cancelar",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                    )
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
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Guardar",
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
private fun AdminServiceTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    singleLine: Boolean = false,
    minLines: Int = 1,
    maxLines: Int = 1,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        enabled = enabled,
        label = { Text(label) },
        singleLine = singleLine,
        minLines = minLines,
        maxLines = maxLines,
        textStyle = MaterialTheme.typography.bodyMedium,
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.tertiary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
            focusedLabelColor = MaterialTheme.colorScheme.tertiary,
        ),
    )
}

@Composable
private fun AdminServiceSwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onTertiary,
                checkedTrackColor = MaterialTheme.colorScheme.tertiary,
            ),
        )
    }
}

@Composable
private fun AdminServiceStatusPill(active: Boolean) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = if (active) {
            MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.42f)
        } else {
            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.60f)
        },
        contentColor = if (active) {
            MaterialTheme.colorScheme.onTertiaryContainer
        } else {
            MaterialTheme.colorScheme.onErrorContainer
        },
    ) {
        Text(
            text = if (active) "Ativo" else "Inativo",
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun AdminServiceCatalogMetric(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
