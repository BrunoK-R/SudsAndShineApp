package com.sudsmobile.feature.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AdminAvailabilityScreen(
    contentPadding: PaddingValues,
    onBack: () -> Unit,
    onRequestSignIn: () -> Unit = {},
) {
    val viewModel: AdminAvailabilityViewModel = koinViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val saveState by viewModel.saveState.collectAsStateWithLifecycle()
    val sessionState by viewModel.sessionState.collectAsStateWithLifecycle()

    LaunchedEffect(sessionState) {
        viewModel.refreshForSession()
    }

    AdminAvailabilityScreenContent(
        contentPadding = contentPadding,
        uiState = uiState,
        saveState = saveState,
        onBack = onBack,
        onRequestSignIn = onRequestSignIn,
        onRetry = { viewModel.loadConfiguration() },
        onFormChange = viewModel::updateForm,
        onSave = viewModel::save,
        onSaveCapacityOverride = viewModel::saveCapacityOverride,
        onClearCapacityOverride = viewModel::clearCapacityOverride,
        onSaveBlockedSlot = viewModel::saveBlockedSlot,
        onClearBlockedSlot = viewModel::clearBlockedSlot,
        onDismissSaveState = viewModel::clearSaveState,
    )
}

@Composable
private fun AdminAvailabilityScreenContent(
    contentPadding: PaddingValues,
    uiState: AdminAvailabilityUiState,
    saveState: AdminAvailabilitySaveState,
    onBack: () -> Unit,
    onRequestSignIn: () -> Unit,
    onRetry: () -> Unit,
    onFormChange: (AdminAvailabilityForm) -> Unit,
    onSave: () -> Unit,
    onSaveCapacityOverride: () -> Unit,
    onClearCapacityOverride: (String) -> Unit,
    onSaveBlockedSlot: () -> Unit,
    onClearBlockedSlot: (String) -> Unit,
    onDismissSaveState: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(bottom = contentPadding.calculateBottomPadding() + 24.dp),
    ) {
        AdminAvailabilityHeader(onBack = onBack)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = (-16).dp)
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AdminAvailabilitySaveBanner(
                saveState = saveState,
                onDismiss = onDismissSaveState,
            )

            when (uiState) {
                AdminAvailabilityUiState.Idle,
                AdminAvailabilityUiState.Loading -> AdminAvailabilityStatusCard(
                    title = "A carregar disponibilidade",
                    body = "Estamos a consultar os horários e a capacidade.",
                    icon = Icons.Filled.Build,
                    loading = true,
                )

                AdminAvailabilityUiState.Unauthenticated -> AdminAvailabilityStatusCard(
                    title = "Sessão necessária",
                    body = "Entre com uma conta de administrador para editar a disponibilidade.",
                    icon = Icons.Filled.Lock,
                    actionLabel = "Entrar ou criar conta",
                    onAction = onRequestSignIn,
                )

                AdminAvailabilityUiState.NotAdmin -> AdminAvailabilityStatusCard(
                    title = "Acesso reservado",
                    body = "A disponibilidade só pode ser alterada por administradores.",
                    icon = Icons.Filled.Security,
                )

                is AdminAvailabilityUiState.Error -> AdminAvailabilityStatusCard(
                    title = "Não foi possível carregar",
                    body = uiState.message,
                    icon = Icons.Filled.ErrorOutline,
                    actionLabel = if (uiState.retryable) "Tentar novamente" else null,
                    onAction = if (uiState.retryable) onRetry else null,
                )

                is AdminAvailabilityUiState.Loaded -> AdminAvailabilityFormCard(
                    form = uiState.form,
                    saving = saveState == AdminAvailabilitySaveState.Saving,
                    onFormChange = onFormChange,
                    onSave = onSave,
                    onSaveCapacityOverride = onSaveCapacityOverride,
                    onClearCapacityOverride = onClearCapacityOverride,
                    onSaveBlockedSlot = onSaveBlockedSlot,
                    onClearBlockedSlot = onClearBlockedSlot,
                )
            }
        }
    }
}

@Composable
private fun AdminAvailabilityHeader(onBack: () -> Unit) {
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
                text = "Disponibilidade",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.inverseOnSurface,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "Horários e capacidade de marcação",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = 0.68f),
            )
        }
    }
}

@Composable
private fun AdminAvailabilitySaveBanner(
    saveState: AdminAvailabilitySaveState,
    onDismiss: () -> Unit,
) {
    when (saveState) {
        AdminAvailabilitySaveState.Idle,
        AdminAvailabilitySaveState.Saving -> Unit
        is AdminAvailabilitySaveState.Success -> AdminAvailabilityStatusCard(
            title = "Disponibilidade guardada",
            body = saveState.message,
            icon = Icons.Filled.CheckCircle,
            actionLabel = "Fechar",
            onAction = onDismiss,
        )
        is AdminAvailabilitySaveState.Error -> AdminAvailabilityStatusCard(
            title = "Não foi possível guardar",
            body = saveState.message,
            icon = Icons.Filled.ErrorOutline,
            actionLabel = "Fechar",
            onAction = onDismiss,
        )
    }
}

@Composable
private fun AdminAvailabilityStatusCard(
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
private fun AdminAvailabilityFormCard(
    form: AdminAvailabilityForm,
    saving: Boolean,
    onFormChange: (AdminAvailabilityForm) -> Unit,
    onSave: () -> Unit,
    onSaveCapacityOverride: () -> Unit,
    onClearCapacityOverride: (String) -> Unit,
    onSaveBlockedSlot: () -> Unit,
    onClearBlockedSlot: (String) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            AdminAvailabilitySectionHeader(
                icon = Icons.Filled.Build,
                title = "Regras de marcação",
                body = "Capacidade, intervalo e dias abertos.",
            )

            AdminAvailabilityStepper(
                label = "Capacidade por horário",
                value = form.defaultCapacityValue(),
                range = 1..20,
                enabled = !saving,
                valueText = { value -> "$value por horário" },
                onValueChange = { value ->
                    onFormChange(form.copy(defaultMaxBookingsPerSlot = value.toString()))
                },
            )

            AdminIntervalSelector(
                selectedInterval = form.defaultIntervalValue(),
                enabled = !saving,
                onIntervalSelected = { interval ->
                    onFormChange(form.copy(defaultSlotIntervalMinutes = interval.toString()))
                },
            )

            AdminWeeklyAvailabilityEditor(
                days = form.weeklyHours,
                enabled = !saving,
                onDayChange = { updatedDay ->
                    val updatedDays = form.weeklyHours.map { day ->
                        if (day.dayLabel == updatedDay.dayLabel) updatedDay else day
                    }
                    onFormChange(form.withWeeklyHours(updatedDays))
                },
            )

            Button(
                onClick = onSave,
                enabled = !saving,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.inverseSurface,
                    contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                ),
            ) {
                if (saving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = MaterialTheme.colorScheme.inverseOnSurface,
                        strokeWidth = 2.dp,
                    )
                    Spacer(Modifier.width(8.dp))
                } else {
                    Icon(
                        imageVector = Icons.Filled.Save,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Text(
                    text = if (saving) "A guardar" else "Guardar disponibilidade",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                )
            }

            AdminDateExceptionEditor(
                form = form,
                saving = saving,
                configuredDates = form.configuredAvailabilityDates(),
                onFormChange = onFormChange,
                onSaveCapacityOverride = onSaveCapacityOverride,
            )

            if (form.capacityOverrides.isEmpty()) {
                Text(
                    text = "Sem exceções de capacidade configuradas.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    form.capacityOverrides.forEach { override ->
                        AdminCapacityOverrideRow(
                            override = override,
                            saving = saving,
                            onClear = onClearCapacityOverride,
                        )
                    }
                }
            }

            AdminBlockedSlotEditor(
                form = form,
                saving = saving,
                configuredDates = form.configuredAvailabilityDates(),
                onFormChange = onFormChange,
                onSaveBlockedSlot = onSaveBlockedSlot,
            )

            if (form.blockedSlots.isEmpty()) {
                Text(
                    text = "Sem bloqueios de horário configurados.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    form.blockedSlots.forEach { blockedSlot ->
                        AdminBlockedSlotRow(
                            blockedSlot = blockedSlot,
                            saving = saving,
                            onClear = onClearBlockedSlot,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminAvailabilitySectionHeader(
    icon: ImageVector,
    title: String,
    body: String? = null,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Surface(
            modifier = Modifier.size(40.dp),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.28f),
            contentColor = MaterialTheme.colorScheme.tertiary,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
            )
            if (body != null) {
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun AdminAvailabilityStepper(
    label: String,
    value: Int,
    range: IntRange,
    enabled: Boolean,
    valueText: (Int) -> String,
    onValueChange: (Int) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = valueText(value),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                )
            }
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { onValueChange((value - 1).coerceIn(range)) },
                        enabled = enabled && value > range.first,
                        modifier = Modifier.size(42.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Remove,
                            contentDescription = "Diminuir",
                            tint = if (enabled && value > range.first) {
                                MaterialTheme.colorScheme.tertiary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                            },
                        )
                    }
                    Text(
                        text = value.toString(),
                        modifier = Modifier.width(34.dp),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                    )
                    IconButton(
                        onClick = { onValueChange((value + 1).coerceIn(range)) },
                        enabled = enabled && value < range.last,
                        modifier = Modifier.size(42.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = "Aumentar",
                            tint = if (enabled && value < range.last) {
                                MaterialTheme.colorScheme.tertiary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                            },
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AdminIntervalSelector(
    selectedInterval: Int,
    enabled: Boolean,
    onIntervalSelected: (Int) -> Unit,
) {
    val intervals = (listOf(15, 20, 30, 45, 60) + selectedInterval)
        .filter { it in 5..240 }
        .distinct()
        .sorted()
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Intervalo entre horários",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Bold,
        )
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            intervals.forEach { interval ->
                AdminChoiceChip(
                    label = "${interval} min",
                    selected = selectedInterval == interval,
                    enabled = enabled,
                    onClick = { onIntervalSelected(interval) },
                )
            }
        }
    }
}

@Composable
private fun AdminWeeklyAvailabilityEditor(
    days: List<AdminOpeningHoursDayForm>,
    enabled: Boolean,
    onDayChange: (AdminOpeningHoursDayForm) -> Unit,
) {
    var selectedDayLabel by rememberSaveable { mutableStateOf(days.firstOrNull()?.dayLabel.orEmpty()) }
    LaunchedEffect(days) {
        if (days.none { it.dayLabel == selectedDayLabel }) {
            selectedDayLabel = days.firstOrNull()?.dayLabel.orEmpty()
        }
    }
    val selectedDay = days.firstOrNull { it.dayLabel == selectedDayLabel } ?: days.firstOrNull()

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Disponibilidade semanal",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
        )

        AdminWeeklyAvailabilityTabs(
            days = days,
            selectedDayLabel = selectedDay?.dayLabel.orEmpty(),
            enabled = enabled,
            onDaySelected = { selectedDayLabel = it },
        )

        if (selectedDay != null) {
            AdminWeeklyAvailabilityPanel(
                day = selectedDay,
                enabled = enabled,
                onDayChange = onDayChange,
            )
        }
    }
}

@Composable
private fun AdminWeeklyAvailabilityTabs(
    days: List<AdminOpeningHoursDayForm>,
    selectedDayLabel: String,
    enabled: Boolean,
    onDaySelected: (String) -> Unit,
) {
    val selectedIndex = days.indexOfFirst { it.dayLabel == selectedDayLabel }.coerceAtLeast(0)
    TabRow(
        selectedTabIndex = selectedIndex,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp)),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        contentColor = MaterialTheme.colorScheme.tertiary,
    ) {
        days.forEach { day ->
            Tab(
                selected = selectedDayLabel == day.dayLabel,
                enabled = enabled,
                onClick = { onDaySelected(day.dayLabel) },
                text = {
                    Text(
                        text = day.dayTabLabel(),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            selectedDayLabel == day.dayLabel -> MaterialTheme.colorScheme.tertiary
                            day.enabled -> MaterialTheme.colorScheme.onSurface
                            else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.58f)
                        },
                    )
                },
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AdminWeeklyAvailabilityPanel(
    day: AdminOpeningHoursDayForm,
    enabled: Boolean,
    onDayChange: (AdminOpeningHoursDayForm) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = day.dayLabel,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = if (day.enabled) day.hoursLabel else "Encerrado",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = day.enabled,
                    enabled = enabled,
                    onCheckedChange = { checked -> onDayChange(day.copy(enabled = checked)) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.onTertiary,
                        checkedTrackColor = MaterialTheme.colorScheme.tertiary,
                    ),
                )
            }

            if (day.enabled) {
                AdminTimeChoiceRow(
                    label = "Abre",
                    options = adminWeeklyStartTimeChoices.withSelectedTime(day.startTime),
                    selectedTime = day.startTime,
                    enabled = enabled,
                    onTimeSelected = { selectedStart ->
                        val selectedStartMinutes = selectedStart.adminMinutesSinceMidnightOrNull()
                        val currentEndMinutes = day.endTime.adminMinutesSinceMidnightOrNull()
                        val adjustedEnd = if (
                            selectedStartMinutes != null &&
                            currentEndMinutes != null &&
                            currentEndMinutes <= selectedStartMinutes
                        ) {
                            adminWeeklyEndTimeChoices
                                .withSelectedTime(day.endTime)
                                .firstOrNull { endTime ->
                                    (endTime.adminMinutesSinceMidnightOrNull() ?: 0) > selectedStartMinutes
                                } ?: day.endTime
                        } else {
                            day.endTime
                        }
                        onDayChange(day.copy(startTime = selectedStart, endTime = adjustedEnd))
                    },
                )
                AdminTimeChoiceRow(
                    label = "Fecha",
                    options = adminWeeklyEndTimeChoices
                        .withSelectedTime(day.endTime)
                        .filter { endTime ->
                            val endMinutes = endTime.adminMinutesSinceMidnightOrNull()
                            val startMinutes = day.startTime.adminMinutesSinceMidnightOrNull()
                            endMinutes != null && startMinutes != null && endMinutes > startMinutes
                        }
                        .ifEmpty { adminWeeklyEndTimeChoices.withSelectedTime(day.endTime) },
                    selectedTime = day.endTime,
                    enabled = enabled,
                    onTimeSelected = { selectedEnd -> onDayChange(day.copy(endTime = selectedEnd)) },
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AdminTimeChoiceRow(
    label: String,
    options: List<String>,
    selectedTime: String,
    enabled: Boolean,
    onTimeSelected: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Bold,
        )
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            options.forEach { option ->
                AdminChoiceChip(
                    label = option,
                    selected = selectedTime == option,
                    enabled = enabled,
                    onClick = { onTimeSelected(option) },
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AdminDateExceptionEditor(
    form: AdminAvailabilityForm,
    saving: Boolean,
    configuredDates: List<String>,
    onFormChange: (AdminAvailabilityForm) -> Unit,
    onSaveCapacityOverride: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        AdminAvailabilitySectionHeader(
            icon = Icons.Filled.CalendarMonth,
            title = "Exceções por data",
            body = "Capacidade especial por dia.",
        )
        AdminAvailabilityTextField(
            value = form.overrideDate,
            onValueChange = { onFormChange(form.copy(overrideDate = it)) },
            label = "Data da exceção (AAAA-MM-DD)",
            enabled = !saving,
            singleLine = true,
            keyboardType = KeyboardType.Number,
        )
        if (configuredDates.isNotEmpty()) {
            AdminDateShortcutChips(
                dates = configuredDates,
                selectedDate = form.overrideDate,
                enabled = !saving,
                onDateSelected = { date -> onFormChange(form.copy(overrideDate = date)) },
            )
        }
        AdminAvailabilityStepper(
            label = "Capacidade nessa data",
            value = form.overrideCapacityValue(),
            range = 0..20,
            enabled = !saving,
            valueText = { value -> if (value == 0) "Fechado" else "$value por horário" },
            onValueChange = { value ->
                onFormChange(form.copy(overrideMaxBookingsPerSlot = value.toString()))
            },
        )
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            AdminChoiceChip(
                label = "Fechado",
                selected = form.overrideCapacityValue() == 0,
                enabled = !saving,
                onClick = { onFormChange(form.copy(overrideMaxBookingsPerSlot = "0")) },
            )
            AdminChoiceChip(
                label = "Capacidade normal",
                selected = form.overrideCapacityValue() == form.defaultCapacityValue(),
                enabled = !saving,
                onClick = {
                    onFormChange(
                        form.copy(overrideMaxBookingsPerSlot = form.defaultCapacityValue().toString()),
                    )
                },
            )
        }
        Button(
            onClick = onSaveCapacityOverride,
            enabled = !saving,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.tertiary,
                contentColor = MaterialTheme.colorScheme.onTertiary,
            ),
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Guardar exceção",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun AdminBlockedSlotEditor(
    form: AdminAvailabilityForm,
    saving: Boolean,
    configuredDates: List<String>,
    onFormChange: (AdminAvailabilityForm) -> Unit,
    onSaveBlockedSlot: () -> Unit,
) {
    val startOptions = form.blockStartTimeOptions()
    val endOptions = form.blockEndTimeOptions()
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        AdminAvailabilitySectionHeader(
            icon = Icons.Filled.AccessTime,
            title = "Bloqueios de horário",
            body = "Janelas indisponíveis para marcação.",
        )
        AdminAvailabilityTextField(
            value = form.blockedDate,
            onValueChange = { onFormChange(form.copy(blockedDate = it)) },
            label = "Data do bloqueio (AAAA-MM-DD)",
            enabled = !saving,
            singleLine = true,
            keyboardType = KeyboardType.Number,
        )
        if (configuredDates.isNotEmpty()) {
            AdminDateShortcutChips(
                dates = configuredDates,
                selectedDate = form.blockedDate,
                enabled = !saving,
                onDateSelected = { date -> onFormChange(form.copy(blockedDate = date)) },
            )
        }
        AdminTimeChoiceRow(
            label = "Início",
            options = startOptions,
            selectedTime = form.blockedStartTime,
            enabled = !saving,
            onTimeSelected = { selectedStart ->
                val nextEnd = form.nextBlockEndTime(selectedStart)
                onFormChange(
                    form.copy(
                        blockedStartTime = selectedStart,
                        blockedEndTime = nextEnd,
                    ),
                )
            },
        )
        AdminTimeChoiceRow(
            label = "Fim",
            options = endOptions,
            selectedTime = form.blockedEndTime,
            enabled = !saving,
            onTimeSelected = { selectedEnd -> onFormChange(form.copy(blockedEndTime = selectedEnd)) },
        )
        AdminAvailabilityTextField(
            value = form.blockedReason,
            onValueChange = { onFormChange(form.copy(blockedReason = it)) },
            label = "Motivo",
            enabled = !saving,
            singleLine = true,
        )
        Button(
            onClick = onSaveBlockedSlot,
            enabled = !saving,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.tertiary,
                contentColor = MaterialTheme.colorScheme.onTertiary,
            ),
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Guardar bloqueio",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AdminDateShortcutChips(
    dates: List<String>,
    selectedDate: String,
    enabled: Boolean,
    onDateSelected: (String) -> Unit,
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        dates.forEach { date ->
            AdminChoiceChip(
                label = date,
                selected = selectedDate == date,
                enabled = enabled,
                onClick = { onDateSelected(date) },
            )
        }
    }
}

@Composable
private fun AdminChoiceChip(
    label: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .height(42.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = if (selected) {
            MaterialTheme.colorScheme.tertiary
        } else {
            MaterialTheme.colorScheme.surfaceContainerLow
        },
        contentColor = if (selected) {
            MaterialTheme.colorScheme.onTertiary
        } else {
            MaterialTheme.colorScheme.onSurface
        },
        border = BorderStroke(
            width = 1.dp,
            color = if (selected) {
                MaterialTheme.colorScheme.tertiary
            } else {
                MaterialTheme.colorScheme.outlineVariant
            },
        ),
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 14.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = if (enabled) {
                    if (selected) MaterialTheme.colorScheme.onTertiary else MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.48f)
                },
            )
        }
    }
}

@Composable
private fun AdminCapacityOverrideRow(
    override: AdminCapacityOverrideUi,
    saving: Boolean,
    onClear: (String) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = override.date,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = override.capacityLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (override.updatedAuditLabel.isNotBlank()) {
                    Text(
                        text = override.updatedAuditLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            OutlinedButton(
                onClick = { onClear(override.date) },
                enabled = !saving,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error,
                ),
            ) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "Limpar",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun AdminBlockedSlotRow(
    blockedSlot: AdminBlockedSlotUi,
    saving: Boolean,
    onClear: (String) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = blockedSlot.date,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = blockedSlot.timeLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (blockedSlot.reason.isNotBlank()) {
                    Text(
                        text = blockedSlot.reason,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (blockedSlot.updatedAuditLabel.isNotBlank()) {
                    Text(
                        text = blockedSlot.updatedAuditLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            OutlinedButton(
                onClick = { onClear(blockedSlot.blockedSlotId) },
                enabled = !saving,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error,
                ),
            ) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "Limpar",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun AdminAvailabilityTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    singleLine: Boolean = false,
    minLines: Int = 1,
    keyboardType: KeyboardType = KeyboardType.Text,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        enabled = enabled,
        modifier = modifier.fillMaxWidth(),
        singleLine = singleLine,
        minLines = minLines,
        label = { Text(label) },
        textStyle = MaterialTheme.typography.bodyMedium,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.tertiary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
            focusedLabelColor = MaterialTheme.colorScheme.tertiary,
            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
            focusedTextColor = MaterialTheme.colorScheme.onSurface,
            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
        ),
    )
}

private val adminWeeklyStartTimeChoices = listOf(
    "07:00",
    "08:00",
    "09:00",
    "10:00",
    "11:00",
    "12:00",
    "13:00",
    "14:00",
)

private val adminWeeklyEndTimeChoices = listOf(
    "12:00",
    "13:00",
    "14:00",
    "15:00",
    "16:00",
    "17:00",
    "18:00",
    "19:00",
    "20:00",
    "21:00",
)

private fun AdminAvailabilityForm.defaultCapacityValue(): Int {
    return defaultMaxBookingsPerSlot.trim().toIntOrNull()?.coerceIn(1, 20) ?: 1
}

private fun AdminAvailabilityForm.defaultIntervalValue(): Int {
    return defaultSlotIntervalMinutes.trim().toIntOrNull()?.coerceIn(5, 240) ?: 30
}

private fun AdminAvailabilityForm.overrideCapacityValue(): Int {
    return overrideMaxBookingsPerSlot.trim().toIntOrNull()?.coerceIn(0, 20) ?: defaultCapacityValue()
}

private fun AdminAvailabilityForm.configuredAvailabilityDates(): List<String> {
    return (capacityOverrides.map { it.date } + blockedSlots.map { it.date })
        .filter { it.isNotBlank() }
        .distinct()
        .take(8)
}

private fun AdminAvailabilityForm.blockStartTimeOptions(): List<String> {
    return blockTimeOptions().dropLast(1).withSelectedTime(blockedStartTime)
}

private fun AdminAvailabilityForm.blockEndTimeOptions(): List<String> {
    val startMinutes = blockedStartTime.adminMinutesSinceMidnightOrNull()
    val options = blockTimeOptions()
        .filter { option ->
            val optionMinutes = option.adminMinutesSinceMidnightOrNull()
            startMinutes == null || optionMinutes != null && optionMinutes > startMinutes
        }
        .withSelectedTime(blockedEndTime)
    return options.ifEmpty {
        listOf(blockedEndTime.ifBlank { "10:00" }).withSelectedTime("10:00")
    }
}

private fun AdminAvailabilityForm.nextBlockEndTime(startTime: String): String {
    val startMinutes = startTime.adminMinutesSinceMidnightOrNull()
    val currentEndMinutes = blockedEndTime.adminMinutesSinceMidnightOrNull()
    if (startMinutes == null || currentEndMinutes == null || currentEndMinutes > startMinutes) {
        return blockedEndTime
    }
    return copy(blockedStartTime = startTime)
        .blockEndTimeOptions()
        .firstOrNull { option ->
            val optionMinutes = option.adminMinutesSinceMidnightOrNull()
            optionMinutes != null && optionMinutes > startMinutes
        }
        ?: blockedEndTime
}

private fun AdminAvailabilityForm.blockTimeOptions(): List<String> {
    val interval = defaultIntervalValue()
    val openDays = weeklyHours.filter { it.enabled }
    val earliestStart = openDays.mapNotNull { it.startTime.adminMinutesSinceMidnightOrNull() }.minOrNull() ?: 9 * 60
    val latestEnd = openDays.mapNotNull { it.endTime.adminMinutesSinceMidnightOrNull() }.maxOrNull() ?: 19 * 60
    if (latestEnd <= earliestStart) return listOf("09:00", "10:00")

    val options = mutableListOf<String>()
    var minutes = earliestStart
    while (minutes <= latestEnd) {
        options += minutes.toAdminTimeLabel()
        minutes += interval
    }
    if (options.lastOrNull() != latestEnd.toAdminTimeLabel()) {
        options += latestEnd.toAdminTimeLabel()
    }
    return options.distinct()
}

private fun List<String>.withSelectedTime(time: String): List<String> {
    return (this + time)
        .filter { it.adminMinutesSinceMidnightOrNull() != null }
        .distinct()
        .sortedBy { it.adminMinutesSinceMidnightOrNull() ?: Int.MAX_VALUE }
}

private fun String.adminMinutesSinceMidnightOrNull(): Int? {
    val value = trim()
    if (!Regex("^\\d{2}:\\d{2}$").matches(value)) return null
    val hour = value.substring(0, 2).toIntOrNull() ?: return null
    val minute = value.substring(3, 5).toIntOrNull() ?: return null
    if (hour !in 0..23 || minute !in 0..59) return null
    return hour * 60 + minute
}

private fun Int.toAdminTimeLabel(): String {
    val hour = this / 60
    val minute = this % 60
    return "${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}"
}

private fun AdminOpeningHoursDayForm.dayTabLabel(): String {
    return dayLabel.take(3)
}
