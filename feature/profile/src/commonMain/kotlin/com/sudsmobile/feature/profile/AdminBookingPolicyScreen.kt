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
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
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
import com.sudsmobile.shared.ui.SudsAdminHeader
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AdminBookingPolicyScreen(
    contentPadding: PaddingValues,
    onBack: () -> Unit,
    onRequestSignIn: () -> Unit = {},
) {
    val viewModel: AdminBookingPolicyViewModel = koinViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val saveState by viewModel.saveState.collectAsStateWithLifecycle()
    val sessionState by viewModel.sessionState.collectAsStateWithLifecycle()

    LaunchedEffect(sessionState) {
        viewModel.refreshForSession()
    }

    AdminBookingPolicyScreenContent(
        contentPadding = contentPadding,
        uiState = uiState,
        saveState = saveState,
        onBack = onBack,
        onRequestSignIn = onRequestSignIn,
        onRetry = { viewModel.loadConfiguration() },
        onFormChange = viewModel::updateForm,
        onSave = viewModel::save,
        onDismissSaveState = viewModel::clearSaveState,
    )
}

@Composable
private fun AdminBookingPolicyScreenContent(
    contentPadding: PaddingValues,
    uiState: AdminBookingPolicyUiState,
    saveState: AdminBookingPolicySaveState,
    onBack: () -> Unit,
    onRequestSignIn: () -> Unit,
    onRetry: () -> Unit,
    onFormChange: (AdminBookingPolicyForm) -> Unit,
    onSave: () -> Unit,
    onDismissSaveState: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        AdminBookingPolicyHeader(onBack = onBack)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(bottom = contentPadding.calculateBottomPadding() + 24.dp)
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AdminBookingPolicySaveBanner(
                saveState = saveState,
                onDismiss = onDismissSaveState,
            )

            when (uiState) {
                AdminBookingPolicyUiState.Idle,
                AdminBookingPolicyUiState.Loading -> AdminBookingPolicyStatusCard(
                    title = "A carregar política",
                    body = "Estamos a consultar as regras de marcação.",
                    icon = Icons.Filled.CalendarMonth,
                    loading = true,
                )

                AdminBookingPolicyUiState.Unauthenticated -> AdminBookingPolicyStatusCard(
                    title = "Sessão necessária",
                    body = "Entre com uma conta de administrador para editar a política.",
                    icon = Icons.Filled.Lock,
                    actionLabel = "Entrar ou criar conta",
                    onAction = onRequestSignIn,
                )

                AdminBookingPolicyUiState.NotAdmin -> AdminBookingPolicyStatusCard(
                    title = "Acesso reservado",
                    body = "A política de marcações só pode ser alterada por administradores.",
                    icon = Icons.Filled.Security,
                )

                is AdminBookingPolicyUiState.Error -> AdminBookingPolicyStatusCard(
                    title = "Não foi possível carregar",
                    body = uiState.message,
                    icon = Icons.Filled.ErrorOutline,
                    actionLabel = if (uiState.retryable) "Tentar novamente" else null,
                    onAction = if (uiState.retryable) onRetry else null,
                )

                is AdminBookingPolicyUiState.Loaded -> AdminBookingPolicyFormCard(
                    form = uiState.form,
                    saving = saveState == AdminBookingPolicySaveState.Saving,
                    onFormChange = onFormChange,
                    onSave = onSave,
                )
            }
        }
    }
}

@Composable
private fun AdminBookingPolicyHeader(onBack: () -> Unit) {
    SudsAdminHeader(
        title = "Política de marcações",
        subtitle = "Prazos, retenção e mensagem de pagamento",
        onBack = onBack,
    )
}

@Composable
private fun AdminBookingPolicySaveBanner(
    saveState: AdminBookingPolicySaveState,
    onDismiss: () -> Unit,
) {
    when (saveState) {
        AdminBookingPolicySaveState.Idle,
        AdminBookingPolicySaveState.Saving -> Unit
        is AdminBookingPolicySaveState.Success -> AdminBookingPolicyStatusCard(
            title = "Política guardada",
            body = saveState.message,
            icon = Icons.Filled.CheckCircle,
            actionLabel = "Fechar",
            onAction = onDismiss,
        )
        is AdminBookingPolicySaveState.Error -> AdminBookingPolicyStatusCard(
            title = "Não foi possível guardar",
            body = saveState.message,
            icon = Icons.Filled.ErrorOutline,
            actionLabel = "Fechar",
            onAction = onDismiss,
        )
    }
}

@Composable
private fun AdminBookingPolicyStatusCard(
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
private fun AdminBookingPolicyFormCard(
    form: AdminBookingPolicyForm,
    saving: Boolean,
    onFormChange: (AdminBookingPolicyForm) -> Unit,
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
            AdminBookingPolicyAuditLabel(label = form.updatedAuditLabel)
            AdminDurationLookup(
                value = form.pendingHoldMinutes,
                onValueChange = { onFormChange(form.copy(pendingHoldMinutes = it)) },
                label = "Reserva pendente ativa por",
                enabled = !saving,
                minutes = listOf(15, 30, 60, 120, 180, 240, 720, 1440),
            )
            AdminDurationLookup(
                value = form.cancellationWindowMinutes,
                onValueChange = { onFormChange(form.copy(cancellationWindowMinutes = it)) },
                label = "Antecedência para cancelar",
                enabled = !saving,
                minutes = listOf(0, 30, 60, 120, 240, 720, 1440, 2880),
            )
            AdminDurationLookup(
                value = form.rescheduleWindowMinutes,
                onValueChange = { onFormChange(form.copy(rescheduleWindowMinutes = it)) },
                label = "Antecedência para remarcar",
                enabled = !saving,
                minutes = listOf(0, 30, 60, 120, 240, 720, 1440, 2880),
            )
            AdminBookingPolicyTextField(
                value = form.paymentEligibilityCopy,
                onValueChange = { onFormChange(form.copy(paymentEligibilityCopy = it)) },
                label = "Mensagem de pagamento",
                supportingText = "Texto mostrado quando o pagamento ainda depende de validação.",
                minLines = 3,
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
                        text = "Guardar política",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

@Composable
private fun AdminBookingPolicyAuditLabel(label: String) {
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
private fun AdminBookingPolicyTextField(
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
