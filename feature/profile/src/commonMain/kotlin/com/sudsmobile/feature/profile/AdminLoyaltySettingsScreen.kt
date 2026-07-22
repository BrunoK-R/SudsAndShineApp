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
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
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
import com.sudsmobile.data.admin.AdminLoyaltyReport
import com.sudsmobile.data.admin.AdminLoyaltyReportEvent
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AdminLoyaltySettingsScreen(
    contentPadding: PaddingValues,
    onBack: () -> Unit,
    onRequestSignIn: () -> Unit = {},
) {
    val viewModel: AdminLoyaltySettingsViewModel = koinViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val saveState by viewModel.saveState.collectAsStateWithLifecycle()
    val reportState by viewModel.reportState.collectAsStateWithLifecycle()
    val sessionState by viewModel.sessionState.collectAsStateWithLifecycle()
    val entitlementViewModel: AdminServiceEntitlementsViewModel = koinViewModel()
    val entitlementState by entitlementViewModel.uiState.collectAsStateWithLifecycle()
    val entitlementActionState by entitlementViewModel.actionState.collectAsStateWithLifecycle()

    LaunchedEffect(sessionState) {
        viewModel.refreshForSession()
        entitlementViewModel.refreshForSession()
    }

    AdminLoyaltySettingsScreenContent(
        contentPadding = contentPadding,
        uiState = uiState,
        saveState = saveState,
        reportState = reportState,
        entitlementState = entitlementState,
        entitlementActionState = entitlementActionState,
        onBack = onBack,
        onRequestSignIn = onRequestSignIn,
        onRetry = { viewModel.loadConfiguration() },
        onFormChange = viewModel::updateForm,
        onSave = viewModel::save,
        onDismissSaveState = viewModel::clearSaveState,
        onRetryEntitlements = { entitlementViewModel.refreshForSession(force = true) },
        onEntitlementFormChange = entitlementViewModel::updateForm,
        onFindEntitlementCustomer = entitlementViewModel::findCustomer,
        onIssueEntitlement = entitlementViewModel::issue,
        onAdjustEntitlementUsage = entitlementViewModel::adjustUsage,
        onRequestEntitlementRevoke = entitlementViewModel::requestRevoke,
        onCancelEntitlementRevoke = entitlementViewModel::cancelRevoke,
        onConfirmEntitlementRevoke = entitlementViewModel::confirmRevoke,
        onDismissEntitlementAction = entitlementViewModel::clearActionState,
    )
}

@Composable
private fun AdminLoyaltySettingsScreenContent(
    contentPadding: PaddingValues,
    uiState: AdminLoyaltySettingsUiState,
    saveState: AdminLoyaltySettingsSaveState,
    reportState: AdminLoyaltyReportUiState,
    entitlementState: AdminServiceEntitlementsUiState,
    entitlementActionState: AdminServiceEntitlementActionState,
    onBack: () -> Unit,
    onRequestSignIn: () -> Unit,
    onRetry: () -> Unit,
    onFormChange: (AdminLoyaltySettingsForm) -> Unit,
    onSave: () -> Unit,
    onDismissSaveState: () -> Unit,
    onRetryEntitlements: () -> Unit,
    onEntitlementFormChange: (AdminServiceEntitlementForm) -> Unit,
    onFindEntitlementCustomer: () -> Unit,
    onIssueEntitlement: () -> Unit,
    onAdjustEntitlementUsage: (String, Int) -> Unit,
    onRequestEntitlementRevoke: (String) -> Unit,
    onCancelEntitlementRevoke: () -> Unit,
    onConfirmEntitlementRevoke: () -> Unit,
    onDismissEntitlementAction: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(bottom = contentPadding.calculateBottomPadding() + 24.dp),
    ) {
        AdminLoyaltySettingsHeader(onBack = onBack)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = (-16).dp)
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AdminLoyaltySettingsSaveBanner(
                saveState = saveState,
                onDismiss = onDismissSaveState,
            )

            when (uiState) {
                AdminLoyaltySettingsUiState.Idle,
                AdminLoyaltySettingsUiState.Loading -> AdminLoyaltySettingsStatusCard(
                    title = "A carregar fidelização",
                    body = "Estamos a consultar as regras do programa.",
                    icon = Icons.Filled.CardGiftcard,
                    loading = true,
                )

                AdminLoyaltySettingsUiState.Unauthenticated -> AdminLoyaltySettingsStatusCard(
                    title = "Sessão necessária",
                    body = "Entre com uma conta de administrador para editar a fidelização.",
                    icon = Icons.Filled.Lock,
                    actionLabel = "Entrar ou criar conta",
                    onAction = onRequestSignIn,
                )

                AdminLoyaltySettingsUiState.NotAdmin -> AdminLoyaltySettingsStatusCard(
                    title = "Acesso reservado",
                    body = "A fidelização só pode ser alterada por administradores.",
                    icon = Icons.Filled.Security,
                )

                is AdminLoyaltySettingsUiState.Error -> AdminLoyaltySettingsStatusCard(
                    title = "Não foi possível carregar",
                    body = uiState.message,
                    icon = Icons.Filled.ErrorOutline,
                    actionLabel = if (uiState.retryable) "Tentar novamente" else null,
                    onAction = if (uiState.retryable) onRetry else null,
                )

                is AdminLoyaltySettingsUiState.Loaded -> {
                    AdminLoyaltyReportSection(
                        state = reportState,
                        onRetry = onRetry,
                    )
                    AdminLoyaltySettingsFormCard(
                        form = uiState.form,
                        saving = saveState == AdminLoyaltySettingsSaveState.Saving,
                        onFormChange = onFormChange,
                        onSave = onSave,
                    )
                    AdminServiceEntitlementsSection(
                        uiState = entitlementState,
                        actionState = entitlementActionState,
                        onRetry = onRetryEntitlements,
                        onFormChange = onEntitlementFormChange,
                        onFindCustomer = onFindEntitlementCustomer,
                        onIssue = onIssueEntitlement,
                        onAdjustUsage = onAdjustEntitlementUsage,
                        onRequestRevoke = onRequestEntitlementRevoke,
                        onCancelRevoke = onCancelEntitlementRevoke,
                        onConfirmRevoke = onConfirmEntitlementRevoke,
                        onDismissAction = onDismissEntitlementAction,
                    )
                }
            }
        }
    }
}

@Composable
private fun AdminLoyaltySettingsHeader(onBack: () -> Unit) {
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
                text = "Fidelização",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.inverseOnSurface,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "Desempenho, auditoria e regras do programa",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = 0.68f),
            )
        }
    }
}

@Composable
private fun AdminLoyaltySettingsSaveBanner(
    saveState: AdminLoyaltySettingsSaveState,
    onDismiss: () -> Unit,
) {
    when (saveState) {
        AdminLoyaltySettingsSaveState.Idle,
        AdminLoyaltySettingsSaveState.Saving -> Unit
        is AdminLoyaltySettingsSaveState.Success -> AdminLoyaltySettingsStatusCard(
            title = "Fidelização guardada",
            body = saveState.message,
            icon = Icons.Filled.CheckCircle,
            actionLabel = "Fechar",
            onAction = onDismiss,
        )
        is AdminLoyaltySettingsSaveState.Error -> AdminLoyaltySettingsStatusCard(
            title = "Não foi possível guardar",
            body = saveState.message,
            icon = Icons.Filled.ErrorOutline,
            actionLabel = "Fechar",
            onAction = onDismiss,
        )
    }
}

@Composable
private fun AdminLoyaltySettingsStatusCard(
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
            Surface(
                modifier = Modifier.size(44.dp),
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.34f),
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
                        Icon(imageVector = icon, contentDescription = null)
                    }
                }
            }
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
}

@Composable
private fun AdminLoyaltyReportSection(
    state: AdminLoyaltyReportUiState,
    onRetry: () -> Unit,
) {
    when (state) {
        AdminLoyaltyReportUiState.Idle,
        AdminLoyaltyReportUiState.Loading -> AdminLoyaltySettingsStatusCard(
            title = "A calcular o programa",
            body = "Estamos a reunir lavagens elegíveis e utilização de recompensas.",
            icon = Icons.Filled.CardGiftcard,
            loading = true,
        )
        AdminLoyaltyReportUiState.Unauthenticated -> Unit
        AdminLoyaltyReportUiState.NotAdmin -> Unit
        is AdminLoyaltyReportUiState.Error -> AdminLoyaltySettingsStatusCard(
            title = "Relatório indisponível",
            body = state.message,
            icon = Icons.Filled.ErrorOutline,
            actionLabel = if (state.retryable) "Tentar novamente" else null,
            onAction = if (state.retryable) onRetry else null,
        )
        is AdminLoyaltyReportUiState.Loaded -> AdminLoyaltyReportCard(state.report)
    }
}

@Composable
private fun AdminLoyaltyReportCard(report: AdminLoyaltyReport) {
    val summary = report.summary
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        text = "Saúde da fidelização",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = summary.loyaltyReportScopeLabel(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                ) {
                    Icon(
                        imageVector = Icons.Filled.CardGiftcard,
                        contentDescription = null,
                        modifier = Modifier.padding(10.dp).size(22.dp),
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    AdminLoyaltyMetric(
                        value = summary.totalStamps.toString(),
                        label = "Selos no total",
                        modifier = Modifier.weight(1f),
                    )
                    AdminLoyaltyMetric(
                        value = summary.bonusStamps.toString(),
                        label = "Selos de indicação",
                        modifier = Modifier.weight(1f),
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    AdminLoyaltyMetric(
                        value = summary.qualifyingWashes.toString(),
                        label = "Lavagens elegíveis",
                        modifier = Modifier.weight(1f),
                    )
                    AdminLoyaltyMetric(
                        value = summary.rewardsEarned.toString(),
                        label = "Prémios ganhos",
                        modifier = Modifier.weight(1f),
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    AdminLoyaltyMetric(
                        value = summary.rewardsRedeemed.toString(),
                        label = "Prémios usados",
                        modifier = Modifier.weight(1f),
                    )
                    AdminLoyaltyMetric(
                        value = summary.estimatedAvailableRewards.toString(),
                        label = "Por utilizar",
                        modifier = Modifier.weight(1f),
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    AdminLoyaltyMetric(
                        value = summary.rewardsReserved.toString(),
                        label = "Em marcações",
                        modifier = Modifier.weight(1f),
                    )
                    AdminLoyaltyMetric(
                        value = summary.activeCustomers.toString(),
                        label = "Clientes ativos",
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            if (report.events.isEmpty()) {
                Text(
                    text = "Ainda não existem movimentos de fidelização.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Text(
                    text = "Movimentos recentes",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                )
                report.events.take(15).forEachIndexed { index, event ->
                    AdminLoyaltyReportEventRow(event)
                    if (index < report.events.take(15).lastIndex) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                        )
                    }
                }
            }

            if (summary.truncated) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(17.dp),
                    )
                    Text(
                        text = summary.loyaltyReportTruncationLabel(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun AdminLoyaltyMetric(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.tertiary,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun AdminLoyaltyReportEventRow(event: AdminLoyaltyReportEvent) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Text(
                text = event.loyaltyEventTitle(),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = event.occurredAtIso.toCompactLoyaltyReportDate(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = event.loyaltyEventDetail(),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (event.rewardCode.isNotBlank()) {
            Text(
                text = event.rewardCode,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.tertiary,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

private fun com.sudsmobile.data.admin.AdminLoyaltyReportSummary.loyaltyReportScopeLabel(): String {
    val period = listOf(periodStartIso.toCompactLoyaltyReportDate(), periodEndIso.toCompactLoyaltyReportDate())
        .filter { it.isNotBlank() }
        .distinct()
        .joinToString(" – ")
    val adjustments = adjustmentsScanned.takeIf { it > 0 }?.let { " · $it ajustes" }.orEmpty()
    val scan = "$reservationsScanned marcações analisadas$adjustments"
    return if (period.isBlank()) scan else "$scan · $period"
}

private fun com.sudsmobile.data.admin.AdminLoyaltyReportSummary.loyaltyReportTruncationLabel(): String {
    val adjustments = adjustmentsScanned.takeIf { it > 0 }?.let { " e $it ajustes" }.orEmpty()
    return "O cálculo usa as $reservationsScanned marcações mais recentes$adjustments."
}

private fun AdminLoyaltyReportEvent.loyaltyEventTitle(): String = when (kind) {
    "stamp_adjustment" -> "Selo de indicação"
    "reward_earned" -> "Recompensa conquistada"
    "reward_redeemed" -> "Recompensa utilizada"
    "reward_reserved" -> "Recompensa reservada"
    "reward_released" -> "Recompensa devolvida"
    else -> "Selo atribuído"
}

private fun AdminLoyaltyReportEvent.loyaltyEventDetail(): String {
    val customer = customerName.ifBlank { customerEmail.ifBlank { "Cliente" } }
    val booking = reservationCode.takeIf { it.isNotBlank() }?.let { " · $it" }.orEmpty()
    val progress = if (kind in setOf("stamp_granted", "stamp_adjustment") && stampPosition > 0 && stampsRequired > 0) {
        " · selo $stampPosition/$stampsRequired"
    } else {
        ""
    }
    return "$customer · $serviceName$booking$progress"
}

private fun String.toCompactLoyaltyReportDate(): String {
    val value = trim()
    if (value.length < 10) return value
    val date = value.take(10)
    val time = value.substringAfter("T", missingDelimiterValue = "").take(5)
    return if (time.length == 5) "$date $time" else date
}

@Composable
private fun AdminLoyaltySettingsFormCard(
    form: AdminLoyaltySettingsForm,
    saving: Boolean,
    onFormChange: (AdminLoyaltySettingsForm) -> Unit,
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
            AdminLoyaltySettingsAuditLabel(label = form.updatedAuditLabel)
            AdminLoyaltySettingsTextField(
                value = form.stampsRequired,
                onValueChange = { onFormChange(form.copy(stampsRequired = it)) },
                label = "Lavagens necessárias",
                supportingText = "Quantidade de selos antes de poder emitir recompensa.",
                keyboardType = KeyboardType.Number,
            )
            AdminLoyaltyRewardTypeSelector(
                selectedType = form.rewardType,
                onTypeSelected = { selectedType ->
                    val defaultValue = when (selectedType.normalizeRewardType()) {
                        "discount_percent" -> "15"
                        "discount_amount" -> "1500"
                        else -> "1"
                    }
                    onFormChange(form.copy(rewardType = selectedType, rewardValue = defaultValue))
                },
            )
            AdminLoyaltySettingsTextField(
                value = form.rewardValue,
                onValueChange = { onFormChange(form.copy(rewardValue = it)) },
                label = "Valor da recompensa",
                supportingText = form.rewardType.rewardValueSupportingText(),
                keyboardType = KeyboardType.Number,
            )
            AdminLoyaltySettingsTextField(
                value = form.rewardDescription,
                onValueChange = { onFormChange(form.copy(rewardDescription = it)) },
                label = "Descrição visível",
                supportingText = "Resumo usado quando a recompensa é emitida.",
                minLines = 2,
            )

            Button(
                onClick = onSave,
                enabled = !saving,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
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
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Guardar fidelização",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

@Composable
private fun AdminLoyaltySettingsAuditLabel(label: String) {
    if (label.isBlank()) return

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Filled.CheckCircle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier.size(18.dp),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun AdminLoyaltyRewardTypeSelector(
    selectedType: String,
    onTypeSelected: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Tipo de recompensa",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            LoyaltyRewardTypeOptions.forEach { option ->
                val selected = selectedType.normalizeRewardType() == option
                val buttonModifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                if (selected) {
                    Button(
                        onClick = { onTypeSelected(option) },
                        modifier = buttonModifier,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.tertiary,
                            contentColor = MaterialTheme.colorScheme.onTertiary,
                        ),
                    ) {
                        Text(
                            text = option.rewardTypeShortLabel(),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                } else {
                    OutlinedButton(
                        onClick = { onTypeSelected(option) },
                        modifier = buttonModifier,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                    ) {
                        Text(
                            text = option.rewardTypeShortLabel(),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminLoyaltySettingsTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    supportingText: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    minLines: Int = 1,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
            )
        },
        supportingText = {
            Text(
                text = supportingText,
                style = MaterialTheme.typography.bodySmall,
            )
        },
        minLines = minLines,
        shape = RoundedCornerShape(14.dp),
        textStyle = MaterialTheme.typography.bodyMedium,
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = keyboardType),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.tertiary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
            focusedLabelColor = MaterialTheme.colorScheme.tertiary,
            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
            cursorColor = MaterialTheme.colorScheme.tertiary,
        ),
    )
}

private val LoyaltyRewardTypeOptions = listOf("free_wash", "discount_amount", "discount_percent")

private fun String.rewardTypeShortLabel(): String {
    return when (normalizeRewardType()) {
        "discount_percent" -> "%"
        "discount_amount" -> "€"
        else -> "Livre"
    }
}

private fun String.rewardValueSupportingText(): String {
    return when (normalizeRewardType()) {
        "discount_percent" -> "Percentagem entre 1 e 100."
        "discount_amount" -> "Valor em cêntimos, por exemplo 1500 para 15,00 €."
        else -> "Número de lavagens oferecidas por recompensa."
    }
}
