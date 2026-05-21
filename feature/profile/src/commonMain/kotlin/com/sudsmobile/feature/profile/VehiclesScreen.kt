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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun VehiclesScreen(
    contentPadding: PaddingValues,
    onBack: () -> Unit,
    onRequestSignIn: () -> Unit = {},
) {
    val viewModel: VehiclesViewModel = koinViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val mutationState by viewModel.mutationState.collectAsStateWithLifecycle()
    val sessionState by viewModel.sessionState.collectAsStateWithLifecycle()
    var showEditor by remember { mutableStateOf(false) }
    var draft by remember { mutableStateOf(VehicleDraftUi()) }

    LaunchedEffect(sessionState) {
        viewModel.refreshForSession()
    }

    LaunchedEffect(mutationState) {
        if (mutationState is VehicleMutationUiState.Success && showEditor) {
            showEditor = false
            draft = VehicleDraftUi()
        }
    }

    VehiclesScreenContent(
        contentPadding = contentPadding,
        uiState = uiState,
        mutationState = mutationState,
        onBack = onBack,
        onRetry = viewModel::loadVehicles,
        onRequestSignIn = onRequestSignIn,
        onAddVehicle = {
            if (uiState is VehiclesUiState.Unauthenticated) {
                onRequestSignIn()
            } else {
                draft = VehicleDraftUi()
                viewModel.clearMutationState()
                showEditor = true
            }
        },
        onEditVehicle = { vehicle ->
            draft = vehicle.toDraft()
            viewModel.clearMutationState()
            showEditor = true
        },
        onDeleteVehicle = viewModel::deleteVehicle,
        onSetDefaultVehicle = viewModel::setDefaultVehicle,
        onDismissMutation = viewModel::clearMutationState,
    )

    if (showEditor) {
        AddVehicleDialog(
            draft = draft,
            mutationState = mutationState,
            onDraftChange = { draft = it },
            onDismiss = {
                showEditor = false
                viewModel.clearMutationState()
            },
            onSave = { viewModel.saveVehicle(draft) },
        )
    }
}

@Composable
private fun VehiclesScreenContent(
    contentPadding: PaddingValues,
    uiState: VehiclesUiState,
    mutationState: VehicleMutationUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onRequestSignIn: () -> Unit,
    onAddVehicle: () -> Unit,
    onEditVehicle: (VehicleUi) -> Unit,
    onDeleteVehicle: (String) -> Unit,
    onSetDefaultVehicle: (VehicleUi) -> Unit,
    onDismissMutation: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(bottom = contentPadding.calculateBottomPadding() + 24.dp),
    ) {
        VehiclesHeader(
            onBack = onBack,
            onAddVehicle = onAddVehicle,
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = (-16).dp)
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            VehiclesMutationBanner(
                mutationState = mutationState,
                onDismiss = onDismissMutation,
            )

            when (uiState) {
                VehiclesUiState.Idle,
                VehiclesUiState.Loading -> VehiclesStatusCard(
                    title = "A carregar veículos",
                    body = "Estamos a consultar os veículos associados à sua conta.",
                    icon = VehiclesStatusIcon.Loading,
                )

                VehiclesUiState.Unauthenticated -> VehiclesStatusCard(
                    title = "Sessão necessária",
                    body = "Entre na sua conta para guardar e gerir veículos.",
                    icon = VehiclesStatusIcon.Locked,
                    actionLabel = "Entrar ou criar conta",
                    onAction = onRequestSignIn,
                )

                VehiclesUiState.Empty -> EmptyVehiclesCard(onAddVehicle = onAddVehicle)

                is VehiclesUiState.Error -> VehiclesStatusCard(
                    title = "Não foi possível carregar",
                    body = uiState.message,
                    icon = VehiclesStatusIcon.Error,
                    actionLabel = if (uiState.retryable) "Tentar novamente" else null,
                    onAction = if (uiState.retryable) onRetry else null,
                )

                is VehiclesUiState.Loaded -> uiState.vehicles.forEach { vehicle ->
                    VehicleCard(
                        vehicle = vehicle,
                        actionsEnabled = mutationState !is VehicleMutationUiState.Loading,
                        onEdit = { onEditVehicle(vehicle) },
                        onDelete = { onDeleteVehicle(vehicle.id) },
                        onSetDefault = { onSetDefaultVehicle(vehicle) },
                    )
                }
            }
        }
    }
}

@Composable
private fun VehiclesHeader(
    onBack: () -> Unit,
    onAddVehicle: () -> Unit,
) {
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
            .padding(top = 24.dp, bottom = 32.dp),
    ) {
        TextButton(
            onClick = onBack,
            colors = ButtonDefaults.textButtonColors(
                contentColor = MaterialTheme.colorScheme.tertiaryContainer,
            ),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Voltar",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
            )
        }

        Spacer(Modifier.height(18.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = "Meus Veículos",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.inverseOnSurface,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "Gerir os seus veículos",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = 0.72f),
                )
            }

            Button(
                onClick = onAddVehicle,
                modifier = Modifier.size(52.dp),
                shape = RoundedCornerShape(26.dp),
                contentPadding = PaddingValues(0.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.tertiary,
                    contentColor = MaterialTheme.colorScheme.onTertiary,
                ),
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "Adicionar veículo",
                    modifier = Modifier.size(26.dp),
                )
            }
        }
    }
}

@Composable
private fun VehiclesMutationBanner(
    mutationState: VehicleMutationUiState,
    onDismiss: () -> Unit,
) {
    if (mutationState is VehicleMutationUiState.Idle) return

    val isLoading = mutationState is VehicleMutationUiState.Loading
    val isSuccess = mutationState is VehicleMutationUiState.Success
    val message = when (mutationState) {
        VehicleMutationUiState.Idle -> ""
        VehicleMutationUiState.Loading -> "A atualizar veículo..."
        is VehicleMutationUiState.Success -> mutationState.message
        is VehicleMutationUiState.ValidationError -> mutationState.message
        is VehicleMutationUiState.Error -> mutationState.message
    }
    val containerColor = when {
        isSuccess -> MaterialTheme.colorScheme.tertiaryContainer
        mutationState is VehicleMutationUiState.ValidationError ||
            mutationState is VehicleMutationUiState.Error -> MaterialTheme.colorScheme.errorContainer
        else -> MaterialTheme.colorScheme.surfaceContainerLowest
    }
    val contentColor = when {
        isSuccess -> MaterialTheme.colorScheme.onTertiaryContainer
        mutationState is VehicleMutationUiState.ValidationError ||
            mutationState is VehicleMutationUiState.Error -> MaterialTheme.colorScheme.onErrorContainer
        else -> MaterialTheme.colorScheme.onSurface
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = containerColor,
        contentColor = contentColor,
        tonalElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = contentColor,
                )
            } else {
                Icon(
                    imageVector = if (isSuccess) Icons.Filled.CheckCircle else Icons.Filled.ErrorOutline,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
            }
            Text(
                text = message,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
            )
            if (!isLoading) {
                TextButton(onClick = onDismiss) {
                    Text("OK", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@Composable
private fun VehicleCard(
    vehicle: VehicleUi,
    actionsEnabled: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onSetDefault: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            ProfileVehicleIcon()

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top,
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = "${vehicle.brand} ${vehicle.model}",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = vehicle.plate,
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        VehicleTypeBadge(type = vehicle.type)
                        if (vehicle.isDefault) {
                            DefaultVehicleBadge()
                        }
                    }
                }

                if (vehicle.color.isNotBlank()) {
                    Text(
                        text = "Cor: ${vehicle.color}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (!vehicle.isDefault) {
                        OutlinedVehicleAction(
                            icon = Icons.Filled.CheckCircle,
                            label = "Usar por defeito",
                            enabled = actionsEnabled,
                            contentColor = MaterialTheme.colorScheme.tertiary,
                            borderColor = MaterialTheme.colorScheme.tertiary,
                            onClick = onSetDefault,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedVehicleAction(
                            icon = Icons.Filled.Edit,
                            label = "Editar",
                            enabled = actionsEnabled,
                            contentColor = MaterialTheme.colorScheme.onSurface,
                            borderColor = MaterialTheme.colorScheme.outline,
                            onClick = onEdit,
                            modifier = Modifier.weight(1f),
                        )
                        OutlinedVehicleAction(
                            icon = Icons.Filled.Delete,
                            label = "Remover",
                            enabled = actionsEnabled,
                            contentColor = MaterialTheme.colorScheme.error,
                            borderColor = MaterialTheme.colorScheme.error,
                            onClick = onDelete,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DefaultVehicleBadge() {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = null,
                modifier = Modifier.size(13.dp),
            )
            Text(
                text = "Predefinido",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun ProfileVehicleIcon() {
    Surface(
        modifier = Modifier.size(64.dp),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.30f),
        contentColor = MaterialTheme.colorScheme.tertiary,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Filled.DirectionsCar,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
            )
        }
    }
}

@Composable
private fun VehicleTypeBadge(type: VehicleTypeUi) {
    val containerColor = when (type) {
        VehicleTypeUi.Passenger -> MaterialTheme.colorScheme.primaryContainer
        VehicleTypeUi.Suv -> MaterialTheme.colorScheme.secondaryContainer
    }
    val contentColor = when (type) {
        VehicleTypeUi.Passenger -> MaterialTheme.colorScheme.onPrimaryContainer
        VehicleTypeUi.Suv -> MaterialTheme.colorScheme.onSecondaryContainer
    }

    Surface(
        shape = RoundedCornerShape(999.dp),
        color = containerColor,
        contentColor = contentColor,
    ) {
        Text(
            text = type.label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun OutlinedVehicleAction(
    icon: ImageVector,
    label: String,
    enabled: Boolean,
    contentColor: androidx.compose.ui.graphics.Color,
    borderColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, borderColor),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = contentColor),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun EmptyVehiclesCard(onAddVehicle: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.DirectionsCar,
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                tint = MaterialTheme.colorScheme.outline,
            )
            Text(
                text = "Nenhum veículo registado",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "Adicione os seus veículos para facilitar futuras marcações",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Button(
                onClick = onAddVehicle,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.tertiary,
                    contentColor = MaterialTheme.colorScheme.onTertiary,
                ),
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Adicionar Veículo",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

private enum class VehiclesStatusIcon {
    Loading,
    Locked,
    Empty,
    Error,
}

@Composable
private fun VehiclesStatusCard(
    title: String,
    body: String,
    icon: VehiclesStatusIcon,
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
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Surface(
                modifier = Modifier.size(58.dp),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                contentColor = MaterialTheme.colorScheme.tertiary,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    when (icon) {
                        VehiclesStatusIcon.Loading -> CircularProgressIndicator(
                            modifier = Modifier.size(28.dp),
                            strokeWidth = 3.dp,
                            color = MaterialTheme.colorScheme.tertiary,
                        )
                        VehiclesStatusIcon.Locked -> Icon(Icons.Filled.Lock, null, modifier = Modifier.size(30.dp))
                        VehiclesStatusIcon.Empty -> Icon(
                            Icons.Filled.DirectionsCar,
                            null,
                            modifier = Modifier.size(30.dp),
                        )
                        VehiclesStatusIcon.Error -> Icon(
                            Icons.Filled.ErrorOutline,
                            null,
                            modifier = Modifier.size(30.dp),
                            tint = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Text(
                text = body,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            if (actionLabel != null && onAction != null) {
                Button(
                    onClick = onAction,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.tertiary,
                        contentColor = MaterialTheme.colorScheme.onTertiary,
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
private fun AddVehicleDialog(
    draft: VehicleDraftUi,
    mutationState: VehicleMutationUiState,
    onDraftChange: (VehicleDraftUi) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
) {
    val isEditing = draft.id != null
    val isSaving = mutationState is VehicleMutationUiState.Loading

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isEditing) "Editar Veículo" else "Adicionar Veículo",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                DialogMutationMessage(mutationState = mutationState)
                VehicleTextField(
                    value = draft.brand,
                    onValueChange = { onDraftChange(draft.copy(brand = it)) },
                    label = "Marca *",
                    placeholder = "BMW",
                )
                VehicleTextField(
                    value = draft.model,
                    onValueChange = { onDraftChange(draft.copy(model = it)) },
                    label = "Modelo *",
                    placeholder = "320d",
                )
                VehicleTextField(
                    value = draft.plate,
                    onValueChange = { onDraftChange(draft.copy(plate = it)) },
                    label = "Matrícula *",
                    placeholder = "AA-00-BB",
                )
                VehicleTextField(
                    value = draft.color,
                    onValueChange = { onDraftChange(draft.copy(color = it)) },
                    label = "Cor",
                    placeholder = "Preto",
                )
                VehicleTypeSelector(
                    selected = draft.type,
                    onSelect = { onDraftChange(draft.copy(type = it)) },
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                DefaultVehicleSwitch(
                    checked = draft.isDefault,
                    enabled = !isSaving,
                    onCheckedChange = { onDraftChange(draft.copy(isDefault = it)) },
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onSave,
                enabled = draft.canSubmit && !isSaving,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.tertiary,
                    contentColor = MaterialTheme.colorScheme.onTertiary,
                ),
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onTertiary,
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Text(
                    text = if (isSaving) "A guardar" else if (isEditing) "Guardar" else "Adicionar",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isSaving,
            ) {
                Text(
                    text = "Cancelar",
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
    )
}

@Composable
private fun DefaultVehicleSwitch(
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onCheckedChange(!checked) },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = "Veículo predefinido",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "Usar primeiro nas próximas marcações.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(
            checked = checked,
            enabled = enabled,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onTertiary,
                checkedTrackColor = MaterialTheme.colorScheme.tertiary,
                checkedBorderColor = MaterialTheme.colorScheme.tertiary,
                uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                uncheckedBorderColor = MaterialTheme.colorScheme.outline,
            ),
        )
    }
}

@Composable
private fun DialogMutationMessage(mutationState: VehicleMutationUiState) {
    val message = when (mutationState) {
        is VehicleMutationUiState.ValidationError -> mutationState.message
        is VehicleMutationUiState.Error -> mutationState.message
        else -> null
    } ?: return

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Filled.ErrorOutline,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun VehicleTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        placeholder = { Text(placeholder) },
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        textStyle = MaterialTheme.typography.bodyMedium,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.tertiary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            focusedLabelColor = MaterialTheme.colorScheme.onSurface,
            unfocusedLabelColor = MaterialTheme.colorScheme.onSurface,
            focusedTextColor = MaterialTheme.colorScheme.onSurface,
            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
            focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
            unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
    )
}

@Composable
private fun VehicleTypeSelector(
    selected: VehicleTypeUi,
    onSelect: (VehicleTypeUi) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Tipo *",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            VehicleTypeUi.values().forEach { type ->
                val isSelected = selected == type
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onSelect(type) },
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.tertiary
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerLow
                    },
                    contentColor = if (isSelected) {
                        MaterialTheme.colorScheme.onTertiary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    border = BorderStroke(
                        1.dp,
                        if (isSelected) {
                            MaterialTheme.colorScheme.tertiary
                        } else {
                            MaterialTheme.colorScheme.outlineVariant
                        },
                    ),
                ) {
                    Text(
                        text = type.label,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}
