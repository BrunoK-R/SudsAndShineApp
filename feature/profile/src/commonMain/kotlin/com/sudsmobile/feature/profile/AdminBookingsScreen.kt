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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.NotificationsActive
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
import androidx.compose.material3.Tab
import androidx.compose.material3.ScrollableTabRow
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
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
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
    onOpenNotificationPreferences: () -> Unit = {},
) {
    val viewModel: AdminBookingsViewModel = koinViewModel()
    val notificationPreferencesViewModel: NotificationPreferencesViewModel = koinViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val decisionState by viewModel.decisionState.collectAsStateWithLifecycle()
    val sessionState by viewModel.sessionState.collectAsStateWithLifecycle()
    val notificationSessionState by notificationPreferencesViewModel.sessionState.collectAsStateWithLifecycle()
    val notificationDeviceState by notificationPreferencesViewModel.deviceState.collectAsStateWithLifecycle()
    val bookingRevision by viewModel.bookingRevision.collectAsStateWithLifecycle()
    var rejectingReservationId by rememberSaveable { mutableStateOf<String?>(null) }
    var rejectionReason by rememberSaveable { mutableStateOf("") }
    val notificationPermissionController = rememberNotificationPermissionRequestController(
        onPermissionResult = notificationPreferencesViewModel::handlePermissionResult,
    )

    LaunchedEffect(sessionState, bookingRevision) {
        viewModel.refreshForSession()
    }

    LaunchedEffect(notificationSessionState) {
        notificationPreferencesViewModel.refreshDeviceForSession()
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
        notificationDeviceState = notificationDeviceState,
        rejectingReservationId = rejectingReservationId,
        rejectionReason = rejectionReason,
        onBack = onBack,
        onRetry = { viewModel.loadRequests() },
        onRequestSignIn = onRequestSignIn,
        onActivateNotifications = {
            if (notificationPermissionController.shouldRequestPostNotifications) {
                notificationPermissionController.requestPostNotifications()
            } else {
                notificationPreferencesViewModel.registerCurrentDevice()
            }
        },
        onOpenNotificationPreferences = onOpenNotificationPreferences,
        onDismissDecision = viewModel::clearDecisionState,
        onAccept = viewModel::acceptRequest,
        onComplete = viewModel::completeRequest,
        onMarkPaid = viewModel::markRequestPaid,
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
    notificationDeviceState: NotificationDeviceUiState,
    rejectingReservationId: String?,
    rejectionReason: String,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onRequestSignIn: () -> Unit,
    onActivateNotifications: () -> Unit,
    onOpenNotificationPreferences: () -> Unit,
    onDismissDecision: () -> Unit,
    onAccept: (String) -> Unit,
    onComplete: (String) -> Unit,
    onMarkPaid: (String) -> Unit,
    onStartReject: (String) -> Unit,
    onCancelReject: () -> Unit,
    onRejectionReasonChange: (String) -> Unit,
    onConfirmReject: (String) -> Unit,
) {
    var selectedTabIndex by rememberSaveable { mutableStateOf(0) }
    val tabs = listOf(
        AdminBookingsTab.Pending,
        AdminBookingsTab.Active,
        AdminBookingsTab.AwaitingPayment,
        AdminBookingsTab.Paid,
        AdminBookingsTab.Closed,
        AdminBookingsTab.All,
    )
    val selectedTab = tabs[selectedTabIndex.coerceIn(0, tabs.lastIndex)]
    val loadedState = uiState as? AdminBookingsUiState.Loaded

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        AdminBookingsHeader(
            businessDateLabel = loadedState?.businessDateLabel,
            onBack = onBack,
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = (-16).dp)
                .weight(1f),
            contentPadding = PaddingValues(
                start = 24.dp,
                end = 24.dp,
                bottom = contentPadding.calculateBottomPadding() + 24.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "decision-banner", contentType = "status") {
                AdminDecisionBanner(
                    decisionState = decisionState,
                    onDismiss = onDismissDecision,
                )
            }
            if (uiState is AdminBookingsUiState.Loaded || uiState is AdminBookingsUiState.Empty) {
                item(key = "notification-prompt", contentType = "status") {
                    AdminNotificationDevicePromptCard(
                        deviceState = notificationDeviceState,
                        onActivate = onActivateNotifications,
                        onOpenPreferences = onOpenNotificationPreferences,
                    )
                }
            }

            when (uiState) {
                AdminBookingsUiState.Idle,
                AdminBookingsUiState.Loading -> item(key = "loading", contentType = "status") {
                    AdminBookingsStatusCard(
                        title = "A carregar marcações",
                        body = "",
                        icon = Icons.Filled.CalendarMonth,
                        loading = true,
                    )
                }

                AdminBookingsUiState.Unauthenticated -> item(key = "unauthenticated", contentType = "status") {
                    AdminBookingsStatusCard(
                        title = "Sessão necessária",
                        body = "Entre com uma conta de administrador para gerir a operação.",
                        icon = Icons.Filled.Lock,
                        actionLabel = "Entrar ou criar conta",
                        onAction = onRequestSignIn,
                    )
                }

                AdminBookingsUiState.NotAdmin -> item(key = "not-admin", contentType = "status") {
                    AdminBookingsStatusCard(
                        title = "Acesso reservado",
                        body = "Esta área só está disponível para utilizadores administradores.",
                        icon = Icons.Filled.Security,
                    )
                }

                AdminBookingsUiState.Empty -> item(key = "empty", contentType = "status") {
                    AdminBookingsStatusCard(
                        title = "Sem marcações",
                        body = "Ainda não existem marcações registadas.",
                        icon = Icons.Filled.CheckCircle,
                        actionLabel = "Atualizar",
                        onAction = onRetry,
                    )
                }

                is AdminBookingsUiState.Error -> item(key = "error", contentType = "status") {
                    AdminBookingsStatusCard(
                        title = "Não foi possível carregar",
                        body = uiState.message,
                        icon = Icons.Filled.ErrorOutline,
                        actionLabel = if (uiState.retryable) "Tentar novamente" else null,
                        onAction = if (uiState.retryable) onRetry else null,
                    )
                }

                is AdminBookingsUiState.Loaded -> {
                    item(key = "summary", contentType = "summary") {
                        AdminBookingsCountCard(
                            pendingCount = uiState.pendingRequests.size,
                            activeCount = uiState.activeRequests.size,
                            awaitingPaymentCount = uiState.awaitingPaymentRequests.size,
                            paidCount = uiState.paidCount,
                            onRetry = onRetry,
                        )
                    }
                    item(key = "tabs", contentType = "tabs") {
                        AdminBookingsTabs(
                            selectedTab = selectedTab,
                            pendingCount = uiState.pendingRequests.size,
                            activeCount = uiState.activeRequests.size,
                            awaitingPaymentCount = uiState.awaitingPaymentRequests.size,
                            paidCount = uiState.paidRequests.size,
                            closedCount = uiState.closedRequests.size,
                            allCount = uiState.allRequests.size,
                            onSelect = { tab -> selectedTabIndex = tabs.indexOf(tab) },
                        )
                    }
                    when (selectedTab) {
                        AdminBookingsTab.Pending -> {
                            if (uiState.pendingRequests.isEmpty()) {
                                item(key = "pending-empty", contentType = "status") {
                                    AdminBookingsStatusCard(
                                        title = "Sem decisões pendentes",
                                        body = "Não há pedidos de marcação a aguardar validação.",
                                        icon = Icons.Filled.CheckCircle,
                                    )
                                }
                            } else {
                                items(
                                    items = uiState.pendingRequests,
                                    key = { "pending-${it.id}" },
                                    contentType = { "pending-booking" },
                                ) { request ->
                                    AdminPendingBookingCard(
                                        request = request,
                                        decisionState = decisionState,
                                        rejectingReservationId = rejectingReservationId,
                                        rejectionReason = rejectionReason,
                                        onAccept = onAccept,
                                        onStartReject = onStartReject,
                                        onCancelReject = onCancelReject,
                                        onRejectionReasonChange = onRejectionReasonChange,
                                        onConfirmReject = onConfirmReject,
                                    )
                                }
                            }
                        }

                        AdminBookingsTab.Active -> {
                            if (uiState.activeRequests.isEmpty()) {
                                item(key = "active-empty", contentType = "status") {
                                    AdminBookingsStatusCard(
                                        title = "Sem marcações confirmadas",
                                        body = "Não há trabalhos por marcar como realizados.",
                                        icon = Icons.Filled.CheckCircle,
                                    )
                                }
                            } else {
                                items(
                                    items = uiState.activeRequests,
                                    key = { "active-${it.id}" },
                                    contentType = { "accepted-booking" },
                                ) { request ->
                                    AdminAcceptedBookingCard(
                                        request = request,
                                        decisionState = decisionState,
                                        onComplete = onComplete,
                                        onMarkPaid = onMarkPaid,
                                    )
                                }
                            }
                        }

                        AdminBookingsTab.AwaitingPayment -> {
                            if (uiState.awaitingPaymentRequests.isEmpty()) {
                                item(key = "payment-empty", contentType = "status") {
                                    AdminBookingsStatusCard(
                                        title = "Sem pagamentos pendentes",
                                        body = "Não há trabalhos realizados por marcar como pagos.",
                                        icon = Icons.Filled.CheckCircle,
                                    )
                                }
                            } else {
                                items(
                                    items = uiState.awaitingPaymentRequests,
                                    key = { "payment-${it.id}" },
                                    contentType = { "payment-booking" },
                                ) { request ->
                                    AdminAcceptedBookingCard(
                                        request = request,
                                        decisionState = decisionState,
                                        onComplete = onComplete,
                                        onMarkPaid = onMarkPaid,
                                    )
                                }
                            }
                        }

                        AdminBookingsTab.Paid -> {
                            AdminReadOnlyBookingList(
                                requests = uiState.paidRequests,
                                emptyTitle = "Sem marcações pagas",
                                itemKeyPrefix = "paid",
                                decisionState = decisionState,
                                onComplete = onComplete,
                                onMarkPaid = onMarkPaid,
                            )
                        }

                        AdminBookingsTab.Closed -> {
                            AdminReadOnlyBookingList(
                                requests = uiState.closedRequests,
                                emptyTitle = "Sem marcações recusadas ou canceladas",
                                itemKeyPrefix = "closed",
                                decisionState = decisionState,
                                onComplete = onComplete,
                                onMarkPaid = onMarkPaid,
                            )
                        }

                        AdminBookingsTab.All -> {
                            items(
                                items = uiState.allRequests,
                                key = { "all-${it.id}" },
                                contentType = { "all-booking" },
                            ) { request ->
                                AdminAcceptedBookingCard(
                                    request = request,
                                    decisionState = decisionState,
                                    onComplete = onComplete,
                                    onMarkPaid = onMarkPaid,
                                    actionsEnabled = false,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun LazyListScope.AdminReadOnlyBookingList(
    requests: List<AdminBookingRequestUi>,
    emptyTitle: String,
    itemKeyPrefix: String,
    decisionState: AdminBookingDecisionUiState,
    onComplete: (String) -> Unit,
    onMarkPaid: (String) -> Unit,
) {
    if (requests.isEmpty()) {
        item(key = "$itemKeyPrefix-empty", contentType = "status") {
            AdminBookingsStatusCard(
                title = emptyTitle,
                body = "",
                icon = Icons.Filled.CheckCircle,
            )
        }
    } else {
        items(
            items = requests,
            key = { "$itemKeyPrefix-${it.id}" },
            contentType = { "$itemKeyPrefix-booking" },
        ) { request ->
            AdminAcceptedBookingCard(
                request = request,
                decisionState = decisionState,
                onComplete = onComplete,
                onMarkPaid = onMarkPaid,
                actionsEnabled = false,
            )
        }
    }
}

private enum class AdminBookingsTab(val label: String) {
    Pending("Pedidos"),
    Active("Ativas"),
    AwaitingPayment("A pagar"),
    Paid("Pagas"),
    Closed("Fechadas"),
    All("Todas"),
}

@Composable
private fun AdminBookingsTabs(
    selectedTab: AdminBookingsTab,
    pendingCount: Int,
    activeCount: Int,
    awaitingPaymentCount: Int,
    paidCount: Int,
    closedCount: Int,
    allCount: Int,
    onSelect: (AdminBookingsTab) -> Unit,
) {
    val tabs = listOf(
        AdminBookingsTab.Pending,
        AdminBookingsTab.Active,
        AdminBookingsTab.AwaitingPayment,
        AdminBookingsTab.Paid,
        AdminBookingsTab.Closed,
        AdminBookingsTab.All,
    )
    ScrollableTabRow(
        selectedTabIndex = tabs.indexOf(selectedTab),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp)),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        contentColor = MaterialTheme.colorScheme.tertiary,
        edgePadding = 0.dp,
    ) {
        tabs.forEach { tab ->
            val count = when (tab) {
                AdminBookingsTab.Pending -> pendingCount
                AdminBookingsTab.Active -> activeCount
                AdminBookingsTab.AwaitingPayment -> awaitingPaymentCount
                AdminBookingsTab.Paid -> paidCount
                AdminBookingsTab.Closed -> closedCount
                AdminBookingsTab.All -> allCount
            }
            Tab(
                selected = selectedTab == tab,
                onClick = { onSelect(tab) },
                text = {
                    Text(
                        text = "${tab.label} ($count)",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                    )
                },
            )
        }
    }
}

@Composable
private fun AdminPendingBookingCard(
    request: AdminBookingRequestUi,
    decisionState: AdminBookingDecisionUiState,
    rejectingReservationId: String?,
    rejectionReason: String,
    onAccept: (String) -> Unit,
    onStartReject: (String) -> Unit,
    onCancelReject: () -> Unit,
    onRejectionReasonChange: (String) -> Unit,
    onConfirmReject: (String) -> Unit,
) {
    AdminBookingRequestCard(
        request = request,
        decisionState = decisionState,
        rejecting = rejectingReservationId == request.id,
        rejectionReason = rejectionReason,
        onAccept = { onAccept(request.id) },
        onComplete = {},
        onMarkPaid = {},
        onStartReject = { onStartReject(request.id) },
        onCancelReject = onCancelReject,
        onRejectionReasonChange = onRejectionReasonChange,
        onConfirmReject = { onConfirmReject(request.id) },
    )
}

@Composable
private fun AdminAcceptedBookingCard(
    request: AdminBookingRequestUi,
    decisionState: AdminBookingDecisionUiState,
    onComplete: (String) -> Unit,
    onMarkPaid: (String) -> Unit,
    actionsEnabled: Boolean = true,
) {
    AdminBookingRequestCard(
        request = request,
        decisionState = decisionState,
        rejecting = false,
        rejectionReason = "",
        acceptedOnly = true,
        actionsEnabled = actionsEnabled,
        onAccept = {},
        onComplete = { onComplete(request.id) },
        onMarkPaid = { onMarkPaid(request.id) },
        onStartReject = {},
        onCancelReject = {},
        onRejectionReasonChange = { _ -> },
        onConfirmReject = {},
    )
}

@Composable
private fun AdminBookingsHeader(
    businessDateLabel: String?,
    onBack: () -> Unit,
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

        Text(
            text = "Marcações",
            modifier = Modifier.semantics { heading() },
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.inverseOnSurface,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = businessDateLabel ?: "Todas as marcações",
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
                AdminBookingDecisionAction.Complete -> "A marcar trabalho realizado."
                AdminBookingDecisionAction.MarkPaid -> "A marcar pagamento."
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
private fun AdminNotificationDevicePromptCard(
    deviceState: NotificationDeviceUiState,
    onActivate: () -> Unit,
    onOpenPreferences: () -> Unit,
) {
    val registeredTokenId = when (deviceState) {
        is NotificationDeviceUiState.Ready -> deviceState.registeredTokenId
        is NotificationDeviceUiState.Success -> deviceState.registeredTokenId
        else -> null
    }
    if (registeredTokenId != null) return
    if (
        deviceState == NotificationDeviceUiState.Checking ||
        deviceState == NotificationDeviceUiState.Unauthenticated ||
        deviceState is NotificationDeviceUiState.Removing ||
        deviceState is NotificationDeviceUiState.Unsupported
    ) {
        return
    }

    val registering = deviceState == NotificationDeviceUiState.Registering
    val title = when (deviceState) {
        NotificationDeviceUiState.Registering -> "A ativar notificações"
        is NotificationDeviceUiState.PermissionRequired -> "Ativar pedidos em tempo real"
        is NotificationDeviceUiState.Error -> "Notificações por ativar"
        else -> "Receber pedidos em tempo real"
    }
    val body = when (deviceState) {
        is NotificationDeviceUiState.PermissionRequired -> deviceState.message
        is NotificationDeviceUiState.Error -> deviceState.message
        NotificationDeviceUiState.Registering ->
            "Estamos a registar este dispositivo para receber novos pedidos de lavagem."
        else -> "Ative este dispositivo para receber novos pedidos de lavagem assim que forem criados."
    }
    val primaryActionLabel = when (deviceState) {
        is NotificationDeviceUiState.PermissionRequired -> "Permitir"
        is NotificationDeviceUiState.Error -> "Tentar novamente"
        else -> "Ativar"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(18.dp),
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Surface(
                modifier = Modifier.size(44.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.12f),
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (registering) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.NotificationsActive,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.76f),
                )
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Button(
                        onClick = onActivate,
                        enabled = !registering,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.onTertiaryContainer,
                            contentColor = MaterialTheme.colorScheme.tertiaryContainer,
                        ),
                    ) {
                        Text(primaryActionLabel)
                    }
                    TextButton(
                        onClick = onOpenPreferences,
                        enabled = !registering,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                        ),
                    ) {
                        Text(
                            text = "Preferências",
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
private fun AdminBookingsCountCard(
    pendingCount: Int,
    activeCount: Int,
    awaitingPaymentCount: Int,
    paidCount: Int,
    onRetry: () -> Unit,
) {
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
                        text = activeCount.toString(),
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
                    text = "Marcações",
                    modifier = Modifier.semantics { heading() },
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onTertiary,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "$pendingCount pedidos · $activeCount confirmadas",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onTertiary.copy(alpha = 0.76f),
                )
                Text(
                    text = "$awaitingPaymentCount a pagar · $paidCount pagas",
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
                    contentDescription = "Atualizar marcações",
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

@Composable
private fun AdminBookingRequestCard(
    request: AdminBookingRequestUi,
    decisionState: AdminBookingDecisionUiState,
    rejecting: Boolean,
    rejectionReason: String,
    acceptedOnly: Boolean = false,
    actionsEnabled: Boolean = true,
    onAccept: () -> Unit,
    onComplete: () -> Unit,
    onMarkPaid: () -> Unit,
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
                    modifier = Modifier.semantics { heading() },
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
                    if (acceptedOnly && request.statusLabel in setOf("Confirmada", "Em curso")) {
                        AdminTimingPill(timing = request.timing)
                    }
                    AdminPill(text = request.statusLabel)
                    if (request.statusDetail.isNotBlank()) {
                        AdminPill(text = request.statusDetail)
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            AdminField(label = "Cliente", value = request.customerContactLabel())
            AdminField(label = "Serviço", value = request.service)
            if (request.durationLabels.isNotEmpty()) {
                AdminField(label = "Tempos", value = request.durationLabels.joinToString(separator = "\n"))
            }
            AdminField(label = "Veículo", value = request.vehicle)
            AdminField(label = "Preço", value = request.price)
            AdminField(label = "Pagamento", value = request.paymentStatus)
            AdminField(label = "Criado", value = request.createdAt)
            if (request.auditLabels.isNotEmpty()) {
                AdminField(label = "Auditoria", value = request.auditLabels.joinToString(separator = "\n"))
            }
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

            if (acceptedOnly) {
                val action = when {
                    request.canMarkPaid -> AdminBookingDecisionAction.MarkPaid
                    request.canComplete -> AdminBookingDecisionAction.Complete
                    else -> null
                }
                if (actionsEnabled && action != null) {
                    Button(
                        onClick = when (action) {
                            AdminBookingDecisionAction.MarkPaid -> onMarkPaid
                            else -> onComplete
                        },
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
                        if (cardBusy && activeDecision?.action == action) {
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
                            text = when (action) {
                                AdminBookingDecisionAction.MarkPaid -> "Marcar como pago"
                                else -> "Trabalho realizado"
                            },
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                        )
                    }
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
private fun AdminTimingPill(timing: AdminBookingTiming) {
    val label = when (timing) {
        AdminBookingTiming.InProgress -> "A decorrer agora"
        AdminBookingTiming.Overdue -> "Em atraso"
        AdminBookingTiming.Today -> "Hoje"
        AdminBookingTiming.Upcoming -> "Próxima"
        AdminBookingTiming.NeedsReview -> "Horário a confirmar"
    }
    val containerColor = when (timing) {
        AdminBookingTiming.InProgress -> MaterialTheme.colorScheme.tertiaryContainer
        AdminBookingTiming.Overdue -> MaterialTheme.colorScheme.errorContainer
        AdminBookingTiming.Today -> MaterialTheme.colorScheme.primaryContainer
        AdminBookingTiming.Upcoming -> MaterialTheme.colorScheme.secondaryContainer
        AdminBookingTiming.NeedsReview -> MaterialTheme.colorScheme.surfaceContainerHigh
    }
    val contentColor = when (timing) {
        AdminBookingTiming.InProgress -> MaterialTheme.colorScheme.onTertiaryContainer
        AdminBookingTiming.Overdue -> MaterialTheme.colorScheme.onErrorContainer
        AdminBookingTiming.Today -> MaterialTheme.colorScheme.onPrimaryContainer
        AdminBookingTiming.Upcoming -> MaterialTheme.colorScheme.onSecondaryContainer
        AdminBookingTiming.NeedsReview -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        shape = RoundedCornerShape(50),
        color = containerColor,
        contentColor = contentColor,
    ) {
        Text(
            text = label,
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
