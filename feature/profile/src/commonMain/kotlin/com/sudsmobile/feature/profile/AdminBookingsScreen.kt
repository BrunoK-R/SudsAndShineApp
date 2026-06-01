package com.sudsmobile.feature.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
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
fun AdminBookingsScreen(
    contentPadding: PaddingValues,
    onBack: () -> Unit,
    onRequestSignIn: () -> Unit = {},
) {
    val viewModel: AdminBookingsViewModel = koinViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val decisionState by viewModel.decisionState.collectAsStateWithLifecycle()
    val sessionState by viewModel.sessionState.collectAsStateWithLifecycle()
    val bookingRevision by viewModel.bookingRevision.collectAsStateWithLifecycle()
    var rejectingReservationId by rememberSaveable { mutableStateOf<String?>(null) }
    var rejectionReason by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(sessionState, bookingRevision) {
        viewModel.refreshForSession()
    }

    LaunchedEffect(decisionState) {
        if (decisionState is AdminBookingDecisionUiState.Success) {
            rejectingReservationId = null
            rejectionReason = ""
        }
    }

    AdminBookingsScreenContent(
        contentPadding = contentPadding,
        uiState = uiState,
        decisionState = decisionState,
        rejectingReservationId = rejectingReservationId,
        rejectionReason = rejectionReason,
        onBack = onBack,
        onRetry = { viewModel.loadRequests() },
        onRequestSignIn = onRequestSignIn,
        onDismissDecision = viewModel::clearDecisionState,
        onAccept = viewModel::acceptRequest,
        onComplete = viewModel::completeRequest,
        onStartReject = { reservationId ->
            viewModel.clearDecisionState()
            rejectingReservationId = reservationId
            rejectionReason = ""
        },
        onCancelReject = {
            rejectingReservationId = null
            rejectionReason = ""
        },
        onRejectionReasonChange = { rejectionReason = it.take(MaxAdminRejectionReasonLength) },
        onConfirmReject = { reservationId ->
            viewModel.rejectRequest(reservationId, rejectionReason)
        },
    )
}

@Composable
private fun AdminBookingsScreenContent(
    contentPadding: PaddingValues,
    uiState: AdminBookingsUiState,
    decisionState: AdminBookingDecisionUiState,
    rejectingReservationId: String?,
    rejectionReason: String,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onRequestSignIn: () -> Unit,
    onDismissDecision: () -> Unit,
    onAccept: (String) -> Unit,
    onComplete: (String) -> Unit,
    onStartReject: (String) -> Unit,
    onCancelReject: () -> Unit,
    onRejectionReasonChange: (String) -> Unit,
    onConfirmReject: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(bottom = contentPadding.calculateBottomPadding() + 24.dp),
    ) {
        AdminBookingsHeader(onBack = onBack)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = (-16).dp)
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AdminDecisionBanner(
                decisionState = decisionState,
                onDismiss = onDismissDecision,
            )

            when (uiState) {
                AdminBookingsUiState.Idle,
                AdminBookingsUiState.Loading -> AdminBookingsStatusCard(
                    title = "A carregar pedidos",
                    body = "Estamos a consultar as marcações que aguardam validação.",
                    icon = Icons.Filled.CalendarMonth,
                    loading = true,
                )

                AdminBookingsUiState.Unauthenticated -> AdminBookingsStatusCard(
                    title = "Sessão necessária",
                    body = "Entre com uma conta de administrador para gerir pedidos.",
                    icon = Icons.Filled.Lock,
                    actionLabel = "Entrar ou criar conta",
                    onAction = onRequestSignIn,
                )

                AdminBookingsUiState.NotAdmin -> AdminBookingsStatusCard(
                    title = "Acesso reservado",
                    body = "Esta área só está disponível para utilizadores administradores.",
                    icon = Icons.Filled.Security,
                )

                AdminBookingsUiState.Empty -> AdminBookingsStatusCard(
                    title = "Sem marcações para ação",
                    body = "Não existem pedidos pendentes nem serviços prontos a concluir neste momento.",
                    icon = Icons.Filled.CheckCircle,
                    actionLabel = "Atualizar",
                    onAction = onRetry,
                )

                is AdminBookingsUiState.Error -> AdminBookingsStatusCard(
                    title = "Não foi possível carregar",
                    body = uiState.message,
                    icon = Icons.Filled.ErrorOutline,
                    actionLabel = if (uiState.retryable) "Tentar novamente" else null,
                    onAction = if (uiState.retryable) onRetry else null,
                )

                is AdminBookingsUiState.Loaded -> {
                    AdminBookingsCountCard(
                        pendingCount = uiState.pendingRequests.size,
                        completableCount = uiState.completableRequests.size,
                        onRetry = onRetry,
                    )
                    if (uiState.pendingRequests.isNotEmpty()) {
                        AdminBookingsSectionTitle("Pedidos pendentes")
                        uiState.pendingRequests.forEach { request ->
                            AdminBookingRequestCard(
                                request = request,
                                decisionState = decisionState,
                                rejecting = rejectingReservationId == request.id,
                                rejectionReason = rejectionReason,
                                onAccept = { onAccept(request.id) },
                                onComplete = { onComplete(request.id) },
                                onStartReject = { onStartReject(request.id) },
                                onCancelReject = onCancelReject,
                                onRejectionReasonChange = onRejectionReasonChange,
                                onConfirmReject = { onConfirmReject(request.id) },
                            )
                        }
                    }
                    if (uiState.completableRequests.isNotEmpty()) {
                        AdminBookingsSectionTitle("Prontas a concluir")
                        uiState.completableRequests.forEach { request ->
                            AdminBookingRequestCard(
                                request = request,
                                decisionState = decisionState,
                                rejecting = false,
                                rejectionReason = "",
                                completeOnly = true,
                                onAccept = { onAccept(request.id) },
                                onComplete = { onComplete(request.id) },
                                onStartReject = { onStartReject(request.id) },
                                onCancelReject = onCancelReject,
                                onRejectionReasonChange = onRejectionReasonChange,
                                onConfirmReject = { onConfirmReject(request.id) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminBookingsHeader(onBack: () -> Unit) {
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

        Text(
            text = "Gestão de marcações",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.inverseOnSurface,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "Validação e conclusão operacional",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = 0.72f),
        )
    }
}

@Composable
private fun AdminDecisionBanner(
    decisionState: AdminBookingDecisionUiState,
    onDismiss: () -> Unit,
) {
    when (decisionState) {
        AdminBookingDecisionUiState.Idle -> Unit
        is AdminBookingDecisionUiState.Loading -> AdminInlineStatus(
            message = when (decisionState.action) {
                AdminBookingDecisionAction.Accept -> "A aceitar marcação."
                AdminBookingDecisionAction.Reject -> "A rejeitar marcação."
                AdminBookingDecisionAction.Complete -> "A concluir marcação."
            },
            loading = true,
        )
        is AdminBookingDecisionUiState.Success -> AdminInlineStatus(
            message = decisionState.message,
            icon = Icons.Filled.CheckCircle,
            onDismiss = onDismiss,
        )
        is AdminBookingDecisionUiState.Error -> AdminInlineStatus(
            message = decisionState.message,
            icon = Icons.Filled.ErrorOutline,
            error = true,
            onDismiss = onDismiss,
        )
    }
}

@Composable
private fun AdminBookingsCountCard(
    pendingCount: Int,
    completableCount: Int,
    onRetry: () -> Unit,
) {
    val totalCount = pendingCount + completableCount
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiary),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        shape = RoundedCornerShape(18.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.size(44.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.onTertiary.copy(alpha = 0.12f),
                contentColor = MaterialTheme.colorScheme.onTertiary,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = totalCount.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = "Marcações administrativas",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onTertiary,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "$pendingCount pendentes · $completableCount prontas a concluir",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onTertiary.copy(alpha = 0.76f),
                )
            }
            TextButton(
                onClick = onRetry,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.onTertiary,
                ),
            ) {
                Icon(
                    imageVector = Icons.Filled.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

@Composable
private fun AdminBookingsSectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurface,
        fontWeight = FontWeight.Bold,
    )
}

@Composable
private fun AdminBookingRequestCard(
    request: AdminBookingRequestUi,
    decisionState: AdminBookingDecisionUiState,
    rejecting: Boolean,
    rejectionReason: String,
    completeOnly: Boolean = false,
    onAccept: () -> Unit,
    onComplete: () -> Unit,
    onStartReject: () -> Unit,
    onCancelReject: () -> Unit,
    onRejectionReasonChange: (String) -> Unit,
    onConfirmReject: () -> Unit,
) {
    val activeDecision = decisionState as? AdminBookingDecisionUiState.Loading
    val decisionInProgress = activeDecision != null
    val cardBusy = activeDecision?.reservationId == request.id

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        shape = RoundedCornerShape(18.dp),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = request.reference,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${request.date} · ${request.time}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    AdminPill(text = request.statusLabel)
                    if (request.statusDetail.isNotBlank()) {
                        AdminPill(text = request.statusDetail)
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            AdminField(label = "Cliente", value = request.customerContactLabel())
            AdminField(label = "Serviço", value = request.service)
            AdminField(label = "Veículo", value = request.vehicle)
            AdminField(label = "Preço", value = request.price)
            AdminField(label = "Pagamento", value = request.paymentStatus)
            AdminField(label = "Criado", value = request.createdAt)
            if (request.extras.isNotEmpty()) {
                AdminField(
                    label = "Extras",
                    value = request.extras.joinToString(separator = "\n") { "${it.name} · ${it.price}" },
                )
            }
            if (request.notes.isNotBlank()) {
                AdminField(label = "Notas", value = request.notes)
            }
            if (request.loyaltyRewardApplied) {
                AdminInlineStatus(
                    message = "Recompensa de fidelização reservada para este pedido.",
                    icon = Icons.Filled.MarkEmailRead,
                )
            }

            if (completeOnly) {
                Button(
                    onClick = onComplete,
                    enabled = !decisionInProgress,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.tertiary,
                        contentColor = MaterialTheme.colorScheme.onTertiary,
                    ),
                ) {
                    if (cardBusy && activeDecision?.action == AdminBookingDecisionAction.Complete) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = MaterialTheme.colorScheme.onTertiary,
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "Concluir",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                    )
                }
            } else if (rejecting) {
                AdminRejectEditor(
                    rejectionReason = rejectionReason,
                    enabled = !decisionInProgress,
                    onRejectionReasonChange = onRejectionReasonChange,
                    onCancel = onCancelReject,
                    onConfirm = onConfirmReject,
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    OutlinedButton(
                        onClick = onStartReject,
                        enabled = !decisionInProgress,
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error,
                        ),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ErrorOutline,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Rejeitar",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Button(
                        onClick = onAccept,
                        enabled = !decisionInProgress,
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.tertiary,
                            contentColor = MaterialTheme.colorScheme.onTertiary,
                        ),
                    ) {
                        if (cardBusy && activeDecision?.action == AdminBookingDecisionAction.Accept) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = MaterialTheme.colorScheme.onTertiary,
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Aceitar",
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
private fun AdminRejectEditor(
    rejectionReason: String,
    enabled: Boolean,
    onRejectionReasonChange: (String) -> Unit,
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        OutlinedTextField(
            value = rejectionReason,
            onValueChange = onRejectionReasonChange,
            enabled = enabled,
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
            maxLines = 3,
            label = { Text("Motivo opcional") },
            supportingText = {
                Text("${rejectionReason.length}/$MaxAdminRejectionReasonLength")
            },
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.tertiary,
                focusedLabelColor = MaterialTheme.colorScheme.tertiary,
            ),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            OutlinedButton(
                onClick = onCancel,
                enabled = enabled,
                modifier = Modifier
                    .weight(1f)
                    .height(46.dp),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text(
                    text = "Cancelar",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                )
            }
            Button(
                onClick = onConfirm,
                enabled = enabled,
                modifier = Modifier
                    .weight(1f)
                    .height(46.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError,
                ),
            ) {
                Icon(
                    imageVector = Icons.Filled.ErrorOutline,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "Rejeitar",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun AdminBookingsStatusCard(
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
                    modifier = Modifier.size(44.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.36f),
                    contentColor = MaterialTheme.colorScheme.tertiary,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (loading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.tertiary,
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp),
                            )
                        }
                    }
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp),
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
private fun AdminField(
    label: String,
    value: String,
) {
    if (value.isBlank()) return

    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun AdminPill(text: String) {
    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.62f),
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun AdminInlineStatus(
    message: String,
    icon: ImageVector = Icons.Filled.MarkEmailRead,
    loading: Boolean = false,
    error: Boolean = false,
    onDismiss: (() -> Unit)? = null,
) {
    val containerColor = if (error) {
        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.82f)
    } else {
        MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.28f)
    }
    val contentColor = if (error) {
        MaterialTheme.colorScheme.onErrorContainer
    } else {
        MaterialTheme.colorScheme.onTertiaryContainer
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = containerColor,
        contentColor = contentColor,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = contentColor,
                    strokeWidth = 2.dp,
                )
            } else {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = contentColor,
                )
            }
            Text(
                text = message,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.labelMedium,
                color = contentColor,
            )
            if (onDismiss != null) {
                TextButton(
                    onClick = onDismiss,
                    colors = ButtonDefaults.textButtonColors(contentColor = contentColor),
                ) {
                    Text(
                        text = "Ok",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

private fun AdminBookingRequestUi.customerContactLabel(): String {
    return listOf(customerName, customerEmail, customerPhone)
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .joinToString(separator = "\n")
}

private const val MaxAdminRejectionReasonLength = 500
