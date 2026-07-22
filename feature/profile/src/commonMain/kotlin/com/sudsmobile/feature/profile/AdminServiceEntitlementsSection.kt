package com.sudsmobile.feature.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CardMembership
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import com.sudsmobile.data.entitlement.ServiceEntitlement

@Composable
internal fun AdminServiceEntitlementsSection(
    uiState: AdminServiceEntitlementsUiState,
    actionState: AdminServiceEntitlementActionState,
    onRetry: () -> Unit,
    onFormChange: (AdminServiceEntitlementForm) -> Unit,
    onFindCustomer: () -> Unit,
    onIssue: () -> Unit,
    onAdjustUsage: (String, Int) -> Unit,
    onRequestRevoke: (String) -> Unit,
    onCancelRevoke: () -> Unit,
    onConfirmRevoke: () -> Unit,
    onDismissAction: () -> Unit,
) {
    when (uiState) {
        AdminServiceEntitlementsUiState.Idle,
        AdminServiceEntitlementsUiState.Loading -> AdminEntitlementStatusCard(
            title = "A carregar planos",
            body = "Estamos a preparar os planos vendidos ao balcão.",
            loading = true,
        )
        AdminServiceEntitlementsUiState.Unauthenticated -> AdminEntitlementStatusCard(
            title = "Sessão necessária",
            body = "Entre como administrador para gerir planos.",
        )
        is AdminServiceEntitlementsUiState.Error -> AdminEntitlementStatusCard(
            title = "Planos indisponíveis",
            body = uiState.message,
            actionLabel = if (uiState.retryable) "Tentar novamente" else null,
            onAction = if (uiState.retryable) onRetry else null,
        )
        is AdminServiceEntitlementsUiState.Loaded -> {
            AdminServiceEntitlementsLoadedCard(
                state = uiState,
                actionState = actionState,
                onFormChange = onFormChange,
                onFindCustomer = onFindCustomer,
                onIssue = onIssue,
                onAdjustUsage = onAdjustUsage,
                onRequestRevoke = onRequestRevoke,
                onDismissAction = onDismissAction,
            )
            if (uiState.pendingRevocationId != null) {
                AlertDialog(
                    onDismissRequest = onCancelRevoke,
                    title = { Text("Revogar este plano?") },
                    text = {
                        Text(
                            "O saldo deixa de poder ser utilizado. O plano e o histórico não são apagados. " +
                                "A nota de operação será guardada como motivo.",
                        )
                    },
                    confirmButton = {
                        Button(onClick = onConfirmRevoke) { Text("Revogar plano") }
                    },
                    dismissButton = {
                        TextButton(onClick = onCancelRevoke) { Text("Cancelar") }
                    },
                )
            }
        }
    }
}

@Composable
private fun AdminServiceEntitlementsLoadedCard(
    state: AdminServiceEntitlementsUiState.Loaded,
    actionState: AdminServiceEntitlementActionState,
    onFormChange: (AdminServiceEntitlementForm) -> Unit,
    onFindCustomer: () -> Unit,
    onIssue: () -> Unit,
    onAdjustUsage: (String, Int) -> Unit,
    onRequestRevoke: (String) -> Unit,
    onDismissAction: () -> Unit,
) {
    val working = actionState == AdminServiceEntitlementActionState.Working
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 5.dp),
        shape = RoundedCornerShape(18.dp),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.CardMembership, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                Text("Planos e pacotes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Text(
                "Venda e pagamento são registados no balcão. Não existe compra online nem renovação automática. " +
                    "Cada emissão, utilização, correção e revogação fica auditada.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            AdminEntitlementActionBanner(actionState, onDismissAction)

            OutlinedTextField(
                value = state.form.customerEmail,
                onValueChange = { onFormChange(state.form.copy(customerEmail = it)) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !working,
                singleLine = true,
                label = { Text("Email da conta do cliente") },
            )
            Button(
                onClick = onFindCustomer,
                modifier = Modifier.fillMaxWidth(),
                enabled = !working && state.form.customerEmail.isNotBlank(),
            ) {
                if (working) {
                    CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                } else {
                    Icon(Icons.Filled.Search, contentDescription = null)
                    Text("Consultar conta", modifier = Modifier.padding(start = 8.dp))
                }
            }

            state.customer?.let { customer ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(customer.displayName.ifBlank { "Cliente registado" }, fontWeight = FontWeight.Bold)
                        Text(customer.email, style = MaterialTheme.typography.bodySmall)
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Text("Emitir novo plano", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = state.form.kind == "package",
                        onClick = { onFormChange(state.form.copy(kind = "package")) },
                        label = { Text("Pacote") },
                        enabled = !working,
                    )
                    FilterChip(
                        selected = state.form.kind == "membership",
                        onClick = { onFormChange(state.form.copy(kind = "membership")) },
                        label = { Text("Plano") },
                        enabled = !working,
                    )
                }
                OutlinedTextField(
                    value = state.form.name,
                    onValueChange = { onFormChange(state.form.copy(name = it)) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !working,
                    singleLine = true,
                    label = { Text("Nome comercial") },
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    AdminEntitlementNumberField(
                        value = state.form.totalUses,
                        label = "Utilizações",
                        enabled = !working,
                        modifier = Modifier.weight(1f),
                        onValueChange = { onFormChange(state.form.copy(totalUses = it)) },
                    )
                    AdminEntitlementNumberField(
                        value = state.form.validDays,
                        label = "Validade (dias)",
                        enabled = !working,
                        modifier = Modifier.weight(1f),
                        onValueChange = { onFormChange(state.form.copy(validDays = it)) },
                    )
                }
                OutlinedTextField(
                    value = state.form.amountPaidEuros,
                    onValueChange = { onFormChange(state.form.copy(amountPaidEuros = it)) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !working,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    label = { Text("Valor pago no balcão (€)") },
                    supportingText = { Text("Use 0 se for oferta ou migração manual.") },
                )
                Text("Serviços incluídos", style = MaterialTheme.typography.labelLarge)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    state.services.forEach { service ->
                        FilterChip(
                            selected = service.id in state.form.selectedServiceIds,
                            onClick = {
                                val selected = state.form.selectedServiceIds.toMutableSet()
                                if (!selected.add(service.id)) selected.remove(service.id)
                                onFormChange(state.form.copy(selectedServiceIds = selected))
                            },
                            label = { Text(service.name) },
                            enabled = !working,
                        )
                    }
                }
                OutlinedTextField(
                    value = state.form.issueNote,
                    onValueChange = { onFormChange(state.form.copy(issueNote = it)) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !working,
                    label = { Text("Nota de emissão") },
                    minLines = 2,
                )
                Button(
                    onClick = onIssue,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !working && state.form.selectedServiceIds.isNotEmpty(),
                ) {
                    Text("Emitir e associar à conta")
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Text("Registar utilização", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = state.form.usageReservationCode,
                    onValueChange = { onFormChange(state.form.copy(usageReservationCode = it.uppercase())) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !working,
                    singleLine = true,
                    label = { Text("Código da marcação (opcional)") },
                )
                OutlinedTextField(
                    value = state.form.usageNote,
                    onValueChange = { onFormChange(state.form.copy(usageNote = it)) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !working,
                    label = { Text("Nota de operação") },
                    minLines = 2,
                )

                if (state.entitlements.isEmpty()) {
                    Text(
                        "Esta conta ainda não tem planos.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    state.entitlements.forEachIndexed { index, entitlement ->
                        AdminServiceEntitlementRow(
                            entitlement = entitlement,
                            working = working,
                            onUse = { onAdjustUsage(entitlement.id, 1) },
                            onCorrect = { onAdjustUsage(entitlement.id, -1) },
                            onRevoke = { onRequestRevoke(entitlement.id) },
                        )
                        if (index < state.entitlements.lastIndex) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminServiceEntitlementRow(
    entitlement: ServiceEntitlement,
    working: Boolean,
    onUse: () -> Unit,
    onCorrect: () -> Unit,
    onRevoke: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column(modifier = Modifier.weight(1f)) {
                Text(entitlement.name, fontWeight = FontWeight.Bold)
                Text(
                    "${entitlement.remainingUses}/${entitlement.totalUses} disponíveis · ${entitlement.status.toAdminStatusLabel()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    "Validade: ${entitlement.validUntilIso.take(10)} · ${entitlement.code}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Button(
            onClick = onUse,
            modifier = Modifier.fillMaxWidth(),
            enabled = !working && entitlement.status == "active" && entitlement.remainingUses > 0,
        ) {
            Text("Registar 1 utilização")
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = onCorrect,
                modifier = Modifier.weight(1f),
                enabled = !working && entitlement.usedUses > 0,
            ) {
                Text("Corrigir -1")
            }
            OutlinedButton(
                onClick = onRevoke,
                modifier = Modifier.weight(1f),
                enabled = !working && entitlement.status != "revoked",
            ) {
                Text("Revogar")
            }
        }
    }
}

@Composable
private fun AdminEntitlementNumberField(
    value: String,
    label: String,
    enabled: Boolean,
    modifier: Modifier,
    onValueChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = { onValueChange(it.filter(Char::isDigit)) },
        modifier = modifier,
        enabled = enabled,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        label = { Text(label) },
    )
}

@Composable
private fun AdminEntitlementActionBanner(
    state: AdminServiceEntitlementActionState,
    onDismiss: () -> Unit,
) {
    when (state) {
        AdminServiceEntitlementActionState.Idle,
        AdminServiceEntitlementActionState.Working -> Unit
        is AdminServiceEntitlementActionState.Success -> Surface(
            color = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
            shape = RoundedCornerShape(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.CheckCircle, contentDescription = null)
                Text(state.message, modifier = Modifier.weight(1f).padding(horizontal = 8.dp))
                TextButton(onClick = onDismiss) { Text("Fechar") }
            }
        }
        is AdminServiceEntitlementActionState.Error -> Surface(
            color = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
            shape = RoundedCornerShape(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.ErrorOutline, contentDescription = null)
                Text(state.message, modifier = Modifier.weight(1f).padding(horizontal = 8.dp))
                TextButton(onClick = onDismiss) { Text("Fechar") }
            }
        }
    }
}

@Composable
private fun AdminEntitlementStatusCard(
    title: String,
    body: String,
    loading: Boolean = false,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        shape = RoundedCornerShape(18.dp),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (loading) CircularProgressIndicator()
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (actionLabel != null && onAction != null) {
                OutlinedButton(onClick = onAction) { Text(actionLabel) }
            }
        }
    }
}

private fun String.toAdminStatusLabel(): String = when (this) {
    "active" -> "ativo"
    "scheduled" -> "agendado"
    "exhausted" -> "esgotado"
    "expired" -> "expirado"
    "revoked" -> "revogado"
    else -> "indisponível"
}
