package com.sudsmobile.feature.cart

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Euro
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Recommend
import androidx.compose.material.icons.filled.SentimentSatisfied
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sudsmobile.data.booking.BookingAvailabilityDay
import com.sudsmobile.data.booking.BookingAvailabilityMonth
import com.sudsmobile.data.booking.BookingAvailabilitySlot
import org.koin.compose.viewmodel.koinViewModel

private enum class BookingsTab(val label: String) {
    Upcoming("Próximas"),
    Completed("Concluídas"),
}

private data class RatingTag(
    val id: String,
    val label: String,
    val icon: ImageVector,
)

private val ratingTags = listOf(
    RatingTag("fast", "Rápido", Icons.Filled.FlashOn),
    RatingTag("quality", "Qualidade", Icons.Filled.Shield),
    RatingTag("friendly", "Simpático", Icons.Filled.SentimentSatisfied),
    RatingTag("recommend", "Recomendo", Icons.Filled.Recommend),
)

@Composable
fun CartScreen(
    contentPadding: PaddingValues,
    onRateService: (String) -> Unit = {},
    onRequestSignIn: () -> Unit = {},
    onOpenPayment: (String) -> Unit = {},
) {
    val viewModel: CartBookingsViewModel = koinViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val businessInfoState by viewModel.businessInfoState.collectAsStateWithLifecycle()
    val cancellationState by viewModel.cancellationState.collectAsStateWithLifecycle()
    val rescheduleAvailabilityState by viewModel.rescheduleAvailabilityState.collectAsStateWithLifecycle()
    val rescheduleState by viewModel.rescheduleState.collectAsStateWithLifecycle()
    val sessionState by viewModel.sessionState.collectAsStateWithLifecycle()
    val bookingRevision by viewModel.bookingRevision.collectAsStateWithLifecycle()
    var selectedTabName by rememberSaveable { mutableStateOf(BookingsTab.Upcoming.name) }
    var pendingCancellationId by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingRescheduleId by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedRescheduleDateId by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedRescheduleTime by rememberSaveable { mutableStateOf<String?>(null) }
    var rescheduleAnchorDate by rememberSaveable { mutableStateOf<String?>(null) }
    var minimumRescheduleMonthAnchor by rememberSaveable { mutableStateOf<String?>(null) }
    val selectedTab = BookingsTab.valueOf(selectedTabName)

    LaunchedEffect(Unit) {
        viewModel.loadBusinessInfo()
    }

    LaunchedEffect(sessionState, bookingRevision) {
        viewModel.refreshForSession()
    }

    LaunchedEffect(cancellationState) {
        if (cancellationState is BookingCancellationUiState.Success) {
            pendingCancellationId = null
            viewModel.clearCancellationState()
        }
    }

    LaunchedEffect(rescheduleState) {
        if (rescheduleState is BookingRescheduleUiState.Success) {
            pendingRescheduleId = null
            selectedRescheduleDateId = null
            selectedRescheduleTime = null
            rescheduleAnchorDate = null
            minimumRescheduleMonthAnchor = null
            viewModel.clearRescheduleState()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(bottom = contentPadding.calculateBottomPadding() + 24.dp),
    ) {
        BookingsHeader()

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(top = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            BookingsContent(
                uiState = uiState,
                businessInfoState = businessInfoState,
                cancellationState = cancellationState,
                rescheduleAvailabilityState = rescheduleAvailabilityState,
                rescheduleState = rescheduleState,
                selectedTab = selectedTab,
                pendingCancellationId = pendingCancellationId,
                pendingRescheduleId = pendingRescheduleId,
                selectedRescheduleDateId = selectedRescheduleDateId,
                selectedRescheduleTime = selectedRescheduleTime,
                rescheduleAnchorDate = rescheduleAnchorDate,
                minimumRescheduleMonthAnchor = minimumRescheduleMonthAnchor,
                onTabSelected = { selectedTabName = it.name },
                onRetry = viewModel::loadBookings,
                onRetryBusinessInfo = { viewModel.loadBusinessInfo(force = true) },
                onRequestSignIn = onRequestSignIn,
                onOpenPayment = onOpenPayment,
                onRateService = onRateService,
                onRequestReschedule = { booking ->
                    viewModel.clearRescheduleState()
                    pendingCancellationId = null
                    pendingRescheduleId = booking.id
                    selectedRescheduleDateId = null
                    selectedRescheduleTime = null
                    rescheduleAnchorDate = booking.slotStartIso.substringBefore("T")
                    minimumRescheduleMonthAnchor = null
                    viewModel.loadRescheduleAvailability(
                        serviceDurationMinutes = booking.serviceDurationMinutes,
                        anchorDate = rescheduleAnchorDate,
                    )
                },
                onDismissReschedule = { reservationId ->
                    if (pendingRescheduleId == reservationId) {
                        pendingRescheduleId = null
                    }
                    selectedRescheduleDateId = null
                    selectedRescheduleTime = null
                    rescheduleAnchorDate = null
                    minimumRescheduleMonthAnchor = null
                    viewModel.clearRescheduleState()
                },
                onSelectRescheduleDate = { dateId ->
                    selectedRescheduleDateId = dateId
                    selectedRescheduleTime = null
                },
                onSelectRescheduleTime = { time ->
                    selectedRescheduleTime = time
                },
                onRescheduleMonthChanged = { booking, anchorDate ->
                    rescheduleAnchorDate = anchorDate
                    selectedRescheduleDateId = null
                    selectedRescheduleTime = null
                    viewModel.clearRescheduleState()
                    viewModel.loadRescheduleAvailability(
                        serviceDurationMinutes = booking.serviceDurationMinutes,
                        anchorDate = anchorDate,
                    )
                },
                onMinimumRescheduleMonthResolved = { anchorDate ->
                    if (minimumRescheduleMonthAnchor == null) {
                        minimumRescheduleMonthAnchor = anchorDate
                    }
                },
                onRetryRescheduleAvailability = { booking ->
                    viewModel.loadRescheduleAvailability(
                        serviceDurationMinutes = booking.serviceDurationMinutes,
                        anchorDate = rescheduleAnchorDate,
                    )
                },
                onConfirmReschedule = { booking ->
                    viewModel.rescheduleBooking(
                        BookingRescheduleDraft(
                            reservationId = booking.id,
                            dateId = selectedRescheduleDateId.orEmpty(),
                            time = selectedRescheduleTime.orEmpty(),
                            durationMinutes = booking.serviceDurationMinutes,
                        ),
                    )
                },
                onRequestCancellation = { reservationId ->
                    viewModel.clearCancellationState()
                    if (pendingRescheduleId == reservationId) {
                        pendingRescheduleId = null
                        viewModel.clearRescheduleState()
                    }
                    pendingCancellationId = reservationId
                },
                onDismissCancellation = { reservationId ->
                    if (pendingCancellationId == reservationId) {
                        pendingCancellationId = null
                    }
                    viewModel.clearCancellationState()
                },
                onConfirmCancellation = viewModel::cancelBooking,
            )
        }
    }
}

@Composable
private fun BookingsHeader() {
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
            .padding(top = 28.dp, bottom = 32.dp),
    ) {
        Text(
            text = "Marcações",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.inverseOnSurface,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "Gerir as suas marcações",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = 0.72f),
        )
    }
}

@Composable
private fun BookingsSegmentedTabs(
    selectedTab: BookingsTab,
    onTabSelected: (BookingsTab) -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        shape = RoundedCornerShape(14.dp),
        shadowElevation = 4.dp,
    ) {
        Row(
            modifier = Modifier.padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            BookingsTab.entries.forEach { tab ->
                val selected = selectedTab == tab
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (selected) {
                                MaterialTheme.colorScheme.tertiary
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerLowest
                            },
                        )
                        .clickable { onTabSelected(tab) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = tab.label,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (selected) {
                            MaterialTheme.colorScheme.onTertiary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    )
                }
            }
        }
    }
}

@Composable
private fun BookingsContent(
    uiState: CartBookingsUiState,
    businessInfoState: CartBusinessInfoUiState,
    cancellationState: BookingCancellationUiState,
    rescheduleAvailabilityState: BookingRescheduleAvailabilityUiState,
    rescheduleState: BookingRescheduleUiState,
    selectedTab: BookingsTab,
    pendingCancellationId: String?,
    pendingRescheduleId: String?,
    selectedRescheduleDateId: String?,
    selectedRescheduleTime: String?,
    rescheduleAnchorDate: String?,
    minimumRescheduleMonthAnchor: String?,
    onTabSelected: (BookingsTab) -> Unit,
    onRetry: () -> Unit,
    onRetryBusinessInfo: () -> Unit,
    onRequestSignIn: () -> Unit,
    onOpenPayment: (String) -> Unit,
    onRateService: (String) -> Unit,
    onRequestReschedule: (BookingSummaryUi) -> Unit,
    onDismissReschedule: (String) -> Unit,
    onSelectRescheduleDate: (String) -> Unit,
    onSelectRescheduleTime: (String) -> Unit,
    onRescheduleMonthChanged: (BookingSummaryUi, String) -> Unit,
    onMinimumRescheduleMonthResolved: (String?) -> Unit,
    onRetryRescheduleAvailability: (BookingSummaryUi) -> Unit,
    onConfirmReschedule: (BookingSummaryUi) -> Unit,
    onRequestCancellation: (String) -> Unit,
    onDismissCancellation: (String) -> Unit,
    onConfirmCancellation: (String) -> Unit,
) {
    when (uiState) {
        CartBookingsUiState.Idle,
        CartBookingsUiState.Loading -> BookingsStatusCard(
            title = "A carregar marcações",
            body = "Estamos a consultar as suas reservas em tempo real.",
            loading = true,
        )

        CartBookingsUiState.Unauthenticated -> BookingsStatusCard(
            title = "Sessão necessária",
            body = "Entre na sua conta para ver marcações associadas ao seu perfil.",
            actionLabel = "Entrar ou criar conta",
            onAction = onRequestSignIn,
        )

        CartBookingsUiState.Empty -> BookingsStatusCard(
            title = "Sem marcações",
            body = "As suas próximas marcações e lavagens concluídas aparecem aqui.",
            actionLabel = "Atualizar",
            onAction = onRetry,
        )

        is CartBookingsUiState.Error -> BookingsStatusCard(
            title = "Não foi possível carregar",
            body = uiState.message,
            actionLabel = if (uiState.retryable) "Tentar novamente" else null,
            onAction = if (uiState.retryable) onRetry else null,
        )

        is CartBookingsUiState.Loaded -> {
            val bookings = when (selectedTab) {
                BookingsTab.Upcoming -> uiState.upcoming
                BookingsTab.Completed -> uiState.completed
            }

            BookingsSegmentedTabs(
                selectedTab = selectedTab,
                onTabSelected = onTabSelected,
            )

            if (bookings.isEmpty()) {
                BookingsStatusCard(
                    title = if (selectedTab == BookingsTab.Upcoming) {
                        "Sem próximas marcações"
                    } else {
                        "Sem lavagens concluídas"
                    },
                    body = if (selectedTab == BookingsTab.Upcoming) {
                        "Quando marcar uma lavagem, ela aparece nesta lista."
                    } else {
                        "As lavagens finalizadas ficam guardadas neste histórico."
                    },
                    actionLabel = "Atualizar",
                    onAction = onRetry,
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    bookings.forEach { booking ->
                        BookingSummaryCard(
                            booking = booking,
                            showRatingAction = selectedTab == BookingsTab.Completed &&
                                booking.reviewable &&
                                !booking.reviewed,
                            showCancelAction = selectedTab == BookingsTab.Upcoming &&
                                booking.cancelable,
                            showRescheduleAction = selectedTab == BookingsTab.Upcoming &&
                                booking.cancelable,
                            businessInfoState = businessInfoState,
                            cancellationState = cancellationState.forReservation(booking.id),
                            cancellationConfirmationVisible = pendingCancellationId == booking.id,
                            rescheduleAvailabilityState = if (pendingRescheduleId == booking.id) {
                                rescheduleAvailabilityState
                            } else {
                                BookingRescheduleAvailabilityUiState.Idle
                            },
                            rescheduleState = rescheduleState.forReservation(booking.id),
                            rescheduleVisible = pendingRescheduleId == booking.id,
                            selectedRescheduleDateId = selectedRescheduleDateId,
                            selectedRescheduleTime = selectedRescheduleTime,
                            rescheduleAnchorDate = rescheduleAnchorDate,
                            minimumRescheduleMonthAnchor = minimumRescheduleMonthAnchor,
                            onRetryBusinessInfo = onRetryBusinessInfo,
                            onOpenPayment = { onOpenPayment(booking.id) },
                            onRateService = { onRateService(booking.id) },
                            onRequestReschedule = { onRequestReschedule(booking) },
                            onDismissReschedule = { onDismissReschedule(booking.id) },
                            onSelectRescheduleDate = onSelectRescheduleDate,
                            onSelectRescheduleTime = onSelectRescheduleTime,
                            onRescheduleMonthChanged = { anchorDate -> onRescheduleMonthChanged(booking, anchorDate) },
                            onMinimumRescheduleMonthResolved = onMinimumRescheduleMonthResolved,
                            onRetryRescheduleAvailability = { onRetryRescheduleAvailability(booking) },
                            onConfirmReschedule = { onConfirmReschedule(booking) },
                            onRequestCancellation = { onRequestCancellation(booking.id) },
                            onDismissCancellation = { onDismissCancellation(booking.id) },
                            onConfirmCancellation = { onConfirmCancellation(booking.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BookingsStatusCard(
    title: String,
    body: String,
    loading: Boolean = false,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
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
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Surface(
                    modifier = Modifier.size(42.dp),
                    color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.34f),
                    contentColor = MaterialTheme.colorScheme.tertiary,
                    shape = RoundedCornerShape(12.dp),
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
                                imageVector = Icons.Filled.CalendarMonth,
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
private fun BookingSummaryCard(
    booking: BookingSummaryUi,
    showRatingAction: Boolean,
    showCancelAction: Boolean,
    showRescheduleAction: Boolean,
    businessInfoState: CartBusinessInfoUiState,
    cancellationState: BookingCancellationUiState,
    cancellationConfirmationVisible: Boolean,
    rescheduleAvailabilityState: BookingRescheduleAvailabilityUiState,
    rescheduleState: BookingRescheduleUiState,
    rescheduleVisible: Boolean,
    selectedRescheduleDateId: String?,
    selectedRescheduleTime: String?,
    rescheduleAnchorDate: String?,
    minimumRescheduleMonthAnchor: String?,
    onRetryBusinessInfo: () -> Unit,
    onOpenPayment: () -> Unit,
    onRateService: () -> Unit,
    onRequestReschedule: () -> Unit,
    onDismissReschedule: () -> Unit,
    onSelectRescheduleDate: (String) -> Unit,
    onSelectRescheduleTime: (String) -> Unit,
    onRescheduleMonthChanged: (String) -> Unit,
    onMinimumRescheduleMonthResolved: (String?) -> Unit,
    onRetryRescheduleAvailability: () -> Unit,
    onConfirmReschedule: () -> Unit,
    onRequestCancellation: () -> Unit,
    onDismissCancellation: () -> Unit,
    onConfirmCancellation: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 5.dp),
        shape = RoundedCornerShape(18.dp),
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                BookingStatusBadge(status = booking.status)
                IconButton(
                    onClick = {},
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.MoreVert,
                        contentDescription = "Mais opções",
                        tint = MaterialTheme.colorScheme.outline,
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Surface(
                    modifier = Modifier.size(56.dp),
                    color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.34f),
                    contentColor = MaterialTheme.colorScheme.tertiary,
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = booking.icon,
                            contentDescription = null,
                            modifier = Modifier.size(28.dp),
                        )
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Text(
                        text = booking.service,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = booking.vehicle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Text(
                    text = booking.price,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.tertiary,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.End,
                )
            }

            Spacer(Modifier.height(16.dp))

            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                BookingDetailRow(Icons.Filled.CalendarMonth, booking.date)
                BookingDetailRow(Icons.Filled.AccessTime, booking.time)
                if (booking.showLocation) {
                    BookingLocationRows(
                        state = businessInfoState,
                        onRetry = onRetryBusinessInfo,
                    )
                }
            }

            if (booking.auditNotes.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                BookingAuditNotes(notes = booking.auditNotes)
            }

            if (booking.reviewed) {
                Spacer(Modifier.height(16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                ReviewedBookingRow(
                    rating = booking.reviewRating,
                    tags = booking.reviewTags,
                    comment = booking.reviewComment,
                    modifier = Modifier.padding(top = 14.dp),
                )
            } else if (showRatingAction) {
                Spacer(Modifier.height(16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Text(
                    text = "Avaliar Serviço",
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onRateService)
                        .padding(top = 14.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.tertiary,
                    fontWeight = FontWeight.Bold,
                )
            }

            if (booking.requiresPayment) {
                Spacer(Modifier.height(16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                PendingPaymentAction(
                    paymentLabel = booking.paymentLabel,
                    onOpenPayment = onOpenPayment,
                    modifier = Modifier.padding(top = 14.dp),
                )
            }

            if (showRescheduleAction) {
                Spacer(Modifier.height(16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                BookingRescheduleAction(
                    booking = booking,
                    availabilityState = rescheduleAvailabilityState,
                    rescheduleState = rescheduleState,
                    visible = rescheduleVisible,
                    selectedDateId = selectedRescheduleDateId,
                    selectedTime = selectedRescheduleTime,
                    anchorDate = rescheduleAnchorDate,
                    minimumMonthAnchor = minimumRescheduleMonthAnchor,
                    onRequestReschedule = onRequestReschedule,
                    onDismissReschedule = onDismissReschedule,
                    onDateSelected = onSelectRescheduleDate,
                    onTimeSelected = onSelectRescheduleTime,
                    onMonthChanged = onRescheduleMonthChanged,
                    onMinimumMonthResolved = onMinimumRescheduleMonthResolved,
                    onRetryAvailability = onRetryRescheduleAvailability,
                    onConfirmReschedule = onConfirmReschedule,
                    modifier = Modifier.padding(top = 14.dp),
                )
            }

            if (showCancelAction) {
                Spacer(Modifier.height(16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                BookingCancellationAction(
                    state = cancellationState,
                    confirmationVisible = cancellationConfirmationVisible,
                    onRequestCancellation = onRequestCancellation,
                    onDismissCancellation = onDismissCancellation,
                    onConfirmCancellation = onConfirmCancellation,
                    modifier = Modifier.padding(top = 14.dp),
                )
            }
        }
    }
}

@Composable
private fun BookingAuditNotes(
    notes: List<BookingAuditNoteUi>,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        notes.forEach { note ->
            val warning = note.tone == BookingAuditToneUi.Warning
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = if (warning) {
                    MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.44f)
                } else {
                    MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.28f)
                },
                contentColor = if (warning) {
                    MaterialTheme.colorScheme.onErrorContainer
                } else {
                    MaterialTheme.colorScheme.onTertiaryContainer
                },
                shape = RoundedCornerShape(14.dp),
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Info,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = if (warning) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.tertiary
                        },
                    )
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Text(
                            text = note.title,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = note.body,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PendingPaymentAction(
    paymentLabel: String,
    onOpenPayment: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(
                imageVector = Icons.Filled.Euro,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.tertiary,
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    text = paymentLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "Consulte o valor em aberto e as opções disponíveis.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        OutlinedButton(
            onClick = onOpenPayment,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.tertiary,
            ),
        ) {
            Text(
                text = "Ver pagamento",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun BookingLocationRows(
    state: CartBusinessInfoUiState,
    onRetry: () -> Unit,
) {
    val info = state.infoOrDefault()

    when (state) {
        CartBusinessInfoUiState.Idle,
        CartBusinessInfoUiState.Loading -> BookingDetailRow(
            icon = Icons.Filled.Place,
            text = "A carregar localização",
        )

        is CartBusinessInfoUiState.Loaded -> BookingDetailRow(
            icon = Icons.Filled.Place,
            text = info.singleLineAddress(),
        )

        is CartBusinessInfoUiState.Error -> {
            BookingDetailRow(
                icon = Icons.Filled.Place,
                text = info.singleLineAddress(),
            )
            Row(
                modifier = Modifier.padding(start = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Icon(
                    imageVector = Icons.Filled.Info,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.error,
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = state.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (state.retryable) {
                        OutlinedButton(
                            onClick = onRetry,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.tertiary,
                            ),
                        ) {
                            Text(
                                text = "Atualizar localização",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun CartBusinessInfoUi.singleLineAddress(): String {
    return listOf(addressLine1, addressLine2)
        .filter { it.isNotBlank() }
        .joinToString(separator = ", ")
}

@Composable
private fun BookingCancellationAction(
    state: BookingCancellationUiState,
    confirmationVisible: Boolean,
    onRequestCancellation: () -> Unit,
    onDismissCancellation: () -> Unit,
    onConfirmCancellation: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val loading = state is BookingCancellationUiState.Loading
    val errorState = state as? BookingCancellationUiState.Error
    val showConfirmAction = errorState?.retryable ?: true

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (confirmationVisible || errorState != null) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Icon(
                    imageVector = Icons.Filled.Info,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = if (errorState != null) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.tertiary
                    },
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = if (state is BookingCancellationUiState.Error) {
                            "Não foi possível cancelar"
                        } else {
                            "Cancelar esta marcação?"
                        },
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = if (state is BookingCancellationUiState.Error) {
                            state.message
                        } else {
                            "O horário fica disponível para outras marcações após confirmação."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                TextButton(
                    onClick = onDismissCancellation,
                    enabled = !loading,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        text = if (errorState != null && !errorState.retryable) "Fechar" else "Manter",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                    )
                }
                if (showConfirmAction) {
                    OutlinedButton(
                        onClick = onConfirmCancellation,
                        enabled = !loading,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error,
                        ),
                    ) {
                        if (loading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = MaterialTheme.colorScheme.error,
                                strokeWidth = 2.dp,
                            )
                            Spacer(Modifier.size(8.dp))
                        }
                        Text(
                            text = when {
                                loading -> "A cancelar"
                                errorState != null -> "Tentar novamente"
                                else -> "Cancelar"
                            },
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        } else {
            Text(
                text = "Cancelar marcação",
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onRequestCancellation),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

private fun BookingCancellationUiState.forReservation(reservationId: String): BookingCancellationUiState {
    return when (this) {
        BookingCancellationUiState.Idle -> BookingCancellationUiState.Idle
        is BookingCancellationUiState.Loading -> if (this.reservationId == reservationId) {
            this
        } else {
            BookingCancellationUiState.Idle
        }
        is BookingCancellationUiState.Success -> if (this.reservationId == reservationId) {
            this
        } else {
            BookingCancellationUiState.Idle
        }
        is BookingCancellationUiState.Error -> if (this.reservationId == reservationId) {
            this
        } else {
            BookingCancellationUiState.Idle
        }
    }
}

@Composable
private fun BookingRescheduleAction(
    booking: BookingSummaryUi,
    availabilityState: BookingRescheduleAvailabilityUiState,
    rescheduleState: BookingRescheduleUiState,
    visible: Boolean,
    selectedDateId: String?,
    selectedTime: String?,
    anchorDate: String?,
    minimumMonthAnchor: String?,
    onRequestReschedule: () -> Unit,
    onDismissReschedule: () -> Unit,
    onDateSelected: (String) -> Unit,
    onTimeSelected: (String) -> Unit,
    onMonthChanged: (String) -> Unit,
    onMinimumMonthResolved: (String?) -> Unit,
    onRetryAvailability: () -> Unit,
    onConfirmReschedule: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val loading = rescheduleState is BookingRescheduleUiState.Loading
    val errorState = rescheduleState as? BookingRescheduleUiState.Error
    val month = availabilityState.monthOrNull()
    val resolvedMonthAnchor = month?.monthAnchorDate()

    LaunchedEffect(resolvedMonthAnchor) {
        if (minimumMonthAnchor == null && resolvedMonthAnchor != null) {
            onMinimumMonthResolved(resolvedMonthAnchor)
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (!visible && errorState == null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onRequestReschedule),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Filled.Edit,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.tertiary,
                )
                Text(
                    text = "Remarcar marcação",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.tertiary,
                    fontWeight = FontWeight.Bold,
                )
            }
            return
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(
                imageVector = Icons.Filled.Edit,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.tertiary,
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    text = "Escolher novo horário",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "Atual: ${booking.date} às ${booking.time}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        RescheduleMonthControls(
            monthTitle = month?.monthTitle ?: anchorDate?.toMonthLabel().orEmpty().ifBlank { "Horários" },
            previousEnabled = !loading && minimumMonthAnchor != null &&
                resolvedMonthAnchor != null &&
                resolvedMonthAnchor > minimumMonthAnchor,
            nextEnabled = !loading,
            onPrevious = {
                val nextAnchor = shiftMonthAnchorDate(
                    anchorDate = resolvedMonthAnchor ?: anchorDate,
                    offsetMonths = -1,
                )
                if (nextAnchor != null) onMonthChanged(nextAnchor)
            },
            onNext = {
                val nextAnchor = shiftMonthAnchorDate(
                    anchorDate = resolvedMonthAnchor ?: anchorDate,
                    offsetMonths = 1,
                )
                if (nextAnchor != null) onMonthChanged(nextAnchor)
            },
        )

        when (availabilityState) {
            BookingRescheduleAvailabilityUiState.Idle,
            BookingRescheduleAvailabilityUiState.Loading -> RescheduleStatusRow(
                message = "A carregar horários disponíveis",
                loading = true,
            )

            is BookingRescheduleAvailabilityUiState.Error -> RescheduleStatusRow(
                message = availabilityState.message,
                isError = true,
                actionLabel = if (availabilityState.retryable) "Tentar novamente" else null,
                onAction = if (availabilityState.retryable) onRetryAvailability else null,
            )

            is BookingRescheduleAvailabilityUiState.Empty -> RescheduleStatusRow(
                message = "Sem horários disponíveis neste mês.",
            )

            is BookingRescheduleAvailabilityUiState.Loaded -> RescheduleAvailabilityPicker(
                month = availabilityState.month,
                selectedDateId = selectedDateId,
                selectedTime = selectedTime,
                onDateSelected = onDateSelected,
                onTimeSelected = onTimeSelected,
            )
        }

        if (errorState != null) {
            RescheduleStatusRow(
                message = errorState.message,
                isError = true,
                actionLabel = if (errorState.retryable) "Tentar novamente" else null,
                onAction = if (errorState.retryable) onConfirmReschedule else null,
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            TextButton(
                onClick = onDismissReschedule,
                enabled = !loading,
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = "Fechar",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                )
            }
            Button(
                onClick = onConfirmReschedule,
                enabled = !loading && selectedDateId != null && selectedTime != null,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.tertiary,
                    contentColor = MaterialTheme.colorScheme.onTertiary,
                ),
            ) {
                if (loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = MaterialTheme.colorScheme.onTertiary,
                        strokeWidth = 2.dp,
                    )
                    Spacer(Modifier.size(8.dp))
                }
                Text(
                    text = if (loading) "A remarcar" else "Confirmar",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun RescheduleMonthControls(
    monthTitle: String,
    previousEnabled: Boolean,
    nextEnabled: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = onPrevious,
            enabled = previousEnabled,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = "Mês anterior",
                tint = if (previousEnabled) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.42f)
                },
            )
        }
        Text(
            text = monthTitle,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        IconButton(
            onClick = onNext,
            enabled = nextEnabled,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Mês seguinte",
                tint = if (nextEnabled) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.42f)
                },
            )
        }
    }
}

@Composable
private fun RescheduleAvailabilityPicker(
    month: BookingAvailabilityMonth,
    selectedDateId: String?,
    selectedTime: String?,
    onDateSelected: (String) -> Unit,
    onTimeSelected: (String) -> Unit,
) {
    val firstAvailableDay = month.days.firstOrNull { it.available }
    val selectedDay = month.days.firstOrNull { it.id == selectedDateId }
        ?: firstAvailableDay

    LaunchedEffect(selectedDateId, firstAvailableDay?.id) {
        if (selectedDateId == null && firstAvailableDay != null) {
            onDateSelected(firstAvailableDay.id)
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        RescheduleDaySelector(
            days = month.days,
            selectedDateId = selectedDateId ?: firstAvailableDay?.id,
            onDateSelected = onDateSelected,
        )

        if (selectedDay == null) {
            RescheduleStatusRow(message = "Sem dias disponíveis neste mês.")
        } else {
            RescheduleSlotSelector(
                day = selectedDay,
                selectedTime = selectedTime,
                onTimeSelected = onTimeSelected,
            )
        }
    }
}

@Composable
private fun RescheduleDaySelector(
    days: List<BookingAvailabilityDay>,
    selectedDateId: String?,
    onDateSelected: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Data",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Bold,
        )
        days.chunked(4).forEach { rowDays ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                rowDays.forEach { day ->
                    RescheduleDayChip(
                        day = day,
                        selected = day.id == selectedDateId,
                        onClick = { onDateSelected(day.id) },
                        modifier = Modifier.weight(1f),
                    )
                }
                repeat(4 - rowDays.size) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun RescheduleDayChip(
    day: BookingAvailabilityDay,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val enabled = day.available
    val containerColor = when {
        selected -> MaterialTheme.colorScheme.tertiary
        enabled -> MaterialTheme.colorScheme.surfaceContainer
        else -> MaterialTheme.colorScheme.surfaceContainerLow
    }
    val contentColor = when {
        selected -> MaterialTheme.colorScheme.onTertiary
        enabled -> MaterialTheme.colorScheme.onSurface
        else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.54f)
    }

    Surface(
        modifier = modifier
            .height(58.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = containerColor,
        contentColor = contentColor,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = day.dayOfMonth.toString(),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Text(
                text = day.summaryLabel.ifBlank { day.dateLabel },
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun RescheduleSlotSelector(
    day: BookingAvailabilityDay,
    selectedTime: String?,
    onTimeSelected: (String) -> Unit,
) {
    val availableSlots = day.slots.filter { it.available }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Hora",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Bold,
        )
        if (availableSlots.isEmpty()) {
            RescheduleStatusRow(message = "Sem horários disponíveis neste dia.")
        } else {
            availableSlots.chunked(3).forEach { rowSlots ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    rowSlots.forEach { slot ->
                        RescheduleSlotChip(
                            slot = slot,
                            selected = slot.time == selectedTime,
                            onClick = { onTimeSelected(slot.time) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    repeat(3 - rowSlots.size) {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun RescheduleSlotChip(
    slot: BookingAvailabilitySlot,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = if (selected) {
            MaterialTheme.colorScheme.tertiary
        } else {
            MaterialTheme.colorScheme.surfaceContainer
        },
        contentColor = if (selected) {
            MaterialTheme.colorScheme.onTertiary
        } else {
            MaterialTheme.colorScheme.onSurface
        },
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = slot.time,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun RescheduleStatusRow(
    message: String,
    loading: Boolean = false,
    isError: Boolean = false,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                color = MaterialTheme.colorScheme.tertiary,
                strokeWidth = 2.dp,
            )
        } else {
            Icon(
                imageVector = if (isError) Icons.Filled.Info else Icons.Filled.CalendarMonth,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = if (isError) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.tertiary
                },
            )
        }
        Text(
            text = message,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodySmall,
            color = if (isError) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
        if (actionLabel != null && onAction != null) {
            TextButton(onClick = onAction) {
                Icon(
                    imageVector = Icons.Filled.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.size(6.dp))
                Text(
                    text = actionLabel,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

private fun BookingRescheduleUiState.forReservation(reservationId: String): BookingRescheduleUiState {
    return when (this) {
        BookingRescheduleUiState.Idle -> BookingRescheduleUiState.Idle
        is BookingRescheduleUiState.Loading -> if (this.reservationId == reservationId) {
            this
        } else {
            BookingRescheduleUiState.Idle
        }
        is BookingRescheduleUiState.Success -> if (this.reservationId == reservationId) {
            this
        } else {
            BookingRescheduleUiState.Idle
        }
        is BookingRescheduleUiState.Error -> if (this.reservationId == reservationId) {
            this
        } else {
            BookingRescheduleUiState.Idle
        }
    }
}

private fun BookingRescheduleAvailabilityUiState.monthOrNull(): BookingAvailabilityMonth? {
    return when (this) {
        BookingRescheduleAvailabilityUiState.Idle,
        BookingRescheduleAvailabilityUiState.Loading -> null
        is BookingRescheduleAvailabilityUiState.Loaded -> month
        is BookingRescheduleAvailabilityUiState.Empty -> month
        is BookingRescheduleAvailabilityUiState.Error -> null
    }
}

private fun BookingAvailabilityMonth.monthAnchorDate(): String? {
    return days.firstOrNull()?.id?.toMonthAnchorDateOrNull()
}

private fun String.toMonthAnchorDateOrNull(): String? {
    if (length < 7) return null
    val year = substring(0, 4).toIntOrNull() ?: return null
    val month = substring(5, 7).toIntOrNull() ?: return null
    if (year <= 0 || month !in 1..12) return null
    return year.toString().padStart(4, '0') + "-" + month.toString().padStart(2, '0') + "-01"
}

private fun String.toMonthLabel(): String? {
    val anchor = toMonthAnchorDateOrNull() ?: return null
    val year = anchor.substring(0, 4)
    val month = anchor.substring(5, 7).toIntOrNull()
        ?.let { rescheduleMonthNames.getOrNull(it - 1) }
        ?: return null
    return "$month $year"
}

private fun shiftMonthAnchorDate(anchorDate: String?, offsetMonths: Int): String? {
    val anchor = anchorDate?.toMonthAnchorDateOrNull() ?: return null
    val year = anchor.substring(0, 4).toIntOrNull() ?: return null
    val month = anchor.substring(5, 7).toIntOrNull() ?: return null
    val monthIndex = year * 12 + (month - 1) + offsetMonths
    if (monthIndex < 0) return null
    val nextYear = monthIndex / 12
    val nextMonth = monthIndex % 12 + 1
    return nextYear.toString().padStart(4, '0') + "-" + nextMonth.toString().padStart(2, '0') + "-01"
}

private val rescheduleMonthNames = listOf(
    "janeiro",
    "fevereiro",
    "março",
    "abril",
    "maio",
    "junho",
    "julho",
    "agosto",
    "setembro",
    "outubro",
    "novembro",
    "dezembro",
)

@Composable
private fun ReviewedBookingRow(
    rating: Int?,
    tags: List<String>,
    comment: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Serviço avaliado",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                (1..5).forEach { star ->
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = if (rating != null && star <= rating) {
                            MaterialTheme.colorScheme.tertiary
                        } else {
                            MaterialTheme.colorScheme.outlineVariant
                        },
                    )
                }
            }
        }
        if (tags.isNotEmpty()) {
            Text(
                text = tags.joinToString(separator = " • "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (comment.isNotBlank()) {
            Text(
                text = comment,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun RatingScreen(
    reservationId: String,
    contentPadding: PaddingValues,
    onBack: () -> Unit,
    onHome: () -> Unit,
    onRequestSignIn: () -> Unit = {},
) {
    val viewModel: RatingViewModel = koinViewModel()
    val targetState by viewModel.targetState.collectAsStateWithLifecycle()
    val submitState by viewModel.submitState.collectAsStateWithLifecycle()
    val sessionState by viewModel.sessionState.collectAsStateWithLifecycle()
    var rating by rememberSaveable { mutableStateOf(0) }
    var selectedTagIds by rememberSaveable { mutableStateOf(emptyList<String>()) }
    var comment by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(reservationId, sessionState) {
        viewModel.refreshTarget(reservationId)
    }

    if (submitState is RatingSubmitUiState.Success) {
        RatingSubmittedScreen(
            contentPadding = contentPadding,
            onHome = onHome,
        )
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = contentPadding.calculateBottomPadding() + if (rating > 0) 112.dp else 24.dp),
        ) {
            RatingHeader(onBack = onBack)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(top = 16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                when (val target = targetState) {
                    RatingTargetUiState.Idle,
                    RatingTargetUiState.Loading -> BookingsStatusCard(
                        title = "A carregar marcação",
                        body = "Estamos a validar a lavagem concluída para avaliação.",
                        loading = true,
                    )
                    RatingTargetUiState.Unauthenticated -> BookingsStatusCard(
                        title = "Sessão necessária",
                        body = "Entre na sua conta para avaliar esta lavagem.",
                        actionLabel = "Entrar",
                        onAction = onRequestSignIn,
                    )
                    RatingTargetUiState.NotFound -> BookingsStatusCard(
                        title = "Marcação indisponível",
                        body = "Esta lavagem já não está disponível para avaliação.",
                        actionLabel = "Voltar",
                        onAction = onBack,
                    )
                    RatingTargetUiState.AlreadyReviewed -> BookingsStatusCard(
                        title = "Avaliação já registada",
                        body = "Esta lavagem já tem uma avaliação guardada no histórico.",
                        actionLabel = "Voltar",
                        onAction = onBack,
                    )
                    is RatingTargetUiState.Error -> BookingsStatusCard(
                        title = "Não foi possível carregar",
                        body = target.message,
                        actionLabel = if (target.retryable) "Tentar novamente" else null,
                        onAction = if (target.retryable) {
                            { viewModel.loadTarget(reservationId) }
                        } else {
                            null
                        },
                    )
                    is RatingTargetUiState.Loaded -> {
                        CompletedServiceCard(target.target)
                        RatingStarsCard(
                            rating = rating,
                            onRatingSelected = {
                                rating = it
                                viewModel.clearSubmitError()
                            },
                        )

                        if (rating > 0) {
                            RatingTagsCard(
                                selectedTagIds = selectedTagIds,
                                onTagToggled = { tagId ->
                                    selectedTagIds = if (tagId in selectedTagIds) {
                                        selectedTagIds - tagId
                                    } else {
                                        selectedTagIds + tagId
                                    }
                                    viewModel.clearSubmitError()
                                },
                            )
                            RatingCommentCard(
                                comment = comment,
                                onCommentChange = {
                                    comment = it.take(1000)
                                    viewModel.clearSubmitError()
                                },
                            )
                        }

                        RatingSubmitMessageCard(
                            submitState = submitState,
                            onRetry = {
                                viewModel.submitReview(
                                    reservationId = target.target.reservationId,
                                    rating = rating,
                                    tags = selectedTagIds.toRatingLabels(),
                                    comment = comment,
                                )
                            },
                            onRequestSignIn = onRequestSignIn,
                        )
                    }
                }
            }
        }

        val loadedTarget = (targetState as? RatingTargetUiState.Loaded)?.target
        if (rating > 0 && loadedTarget != null) {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceContainerLowest,
                shadowElevation = 6.dp,
            ) {
                Column {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Button(
                        onClick = {
                            viewModel.submitReview(
                                reservationId = loadedTarget.reservationId,
                                rating = rating,
                                tags = selectedTagIds.toRatingLabels(),
                                comment = comment,
                            )
                        },
                        enabled = submitState !is RatingSubmitUiState.Loading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                            .padding(top = 16.dp, bottom = contentPadding.calculateBottomPadding() + 16.dp)
                            .height(56.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.tertiary,
                            contentColor = MaterialTheme.colorScheme.onTertiary,
                        ),
                    ) {
                        if (submitState is RatingSubmitUiState.Loading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onTertiary,
                                strokeWidth = 2.dp,
                            )
                            Spacer(Modifier.size(10.dp))
                        }
                        Text(
                            text = if (submitState is RatingSubmitUiState.Loading) {
                                "A enviar..."
                            } else {
                                "Enviar Avaliação"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }
}

private fun List<String>.toRatingLabels(): List<String> {
    return ratingTags
        .filter { it.id in this }
        .map { it.label }
}

@Composable
private fun RatingHeader(onBack: () -> Unit) {
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
            .padding(top = 8.dp, bottom = 28.dp),
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .clickable(onClick = onBack)
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiaryContainer,
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = "Voltar",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.tertiaryContainer,
            )
        }

        Spacer(Modifier.height(24.dp))

        Text(
            text = "Avaliar Serviço",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.inverseOnSurface,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Como foi a sua experiência?",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = 0.72f),
        )
    }
}

@Composable
private fun CompletedServiceCard(target: RatingTargetUi) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        shape = RoundedCornerShape(18.dp),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = "Serviço Realizado",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = target.service,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = target.date,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = target.vehicle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun RatingSubmitMessageCard(
    submitState: RatingSubmitUiState,
    onRetry: () -> Unit,
    onRequestSignIn: () -> Unit,
) {
    when (submitState) {
        RatingSubmitUiState.Idle,
        RatingSubmitUiState.Success -> Unit

        RatingSubmitUiState.Loading -> Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f),
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            shape = RoundedCornerShape(18.dp),
        ) {
            Row(
                modifier = Modifier.padding(18.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    color = MaterialTheme.colorScheme.secondary,
                    strokeWidth = 2.dp,
                )
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "A enviar avaliação",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "Estamos a guardar o feedback associado à sua marcação.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }

        is RatingSubmitUiState.ValidationError -> RatingSubmitErrorCard(
            message = submitState.message,
            actionLabel = null,
            onAction = null,
        )

        is RatingSubmitUiState.Error -> RatingSubmitErrorCard(
            message = submitState.message,
            actionLabel = when {
                submitState.requiresSignIn -> "Entrar"
                submitState.retryable -> "Tentar novamente"
                else -> null
            },
            onAction = when {
                submitState.requiresSignIn -> onRequestSignIn
                submitState.retryable -> onRetry
                else -> null
            },
        )
    }
}

@Composable
private fun RatingSubmitErrorCard(
    message: String,
    actionLabel: String?,
    onAction: (() -> Unit)?,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(18.dp),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Icon(
                    imageVector = Icons.Filled.Info,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                )
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Não foi possível enviar",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }

            if (actionLabel != null && onAction != null) {
                OutlinedButton(
                    onClick = onAction,
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.36f),
                    ),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    ),
                ) {
                    Text(actionLabel, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
private fun RatingStarsCard(
    rating: Int,
    onRatingSelected: (Int) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        shape = RoundedCornerShape(18.dp),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Classifique o Serviço",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(22.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                (1..5).forEach { star ->
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = "$star estrelas",
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .clickable { onRatingSelected(star) }
                            .padding(2.dp),
                        tint = if (star <= rating) {
                            MaterialTheme.colorScheme.tertiary
                        } else {
                            MaterialTheme.colorScheme.outlineVariant
                        },
                    )
                }
            }
            if (rating > 0) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = ratingLabel(rating),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun RatingTagsCard(
    selectedTagIds: List<String>,
    onTagToggled: (String) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        shape = RoundedCornerShape(18.dp),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "O que destacaria?",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
            )
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ratingTags.chunked(2).forEach { rowTags ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        rowTags.forEach { tag ->
                            RatingTagButton(
                                tag = tag,
                                selected = tag.id in selectedTagIds,
                                onClick = { onTagToggled(tag.id) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RatingTagButton(
    tag: RatingTag,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .height(92.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = if (selected) {
            MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.30f)
        } else {
            MaterialTheme.colorScheme.surfaceContainerLow
        },
        border = androidx.compose.foundation.BorderStroke(
            width = 2.dp,
            color = if (selected) {
                MaterialTheme.colorScheme.tertiary
            } else {
                MaterialTheme.colorScheme.outlineVariant
            },
        ),
        contentColor = if (selected) {
            MaterialTheme.colorScheme.tertiary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = tag.icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = tag.label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun RatingCommentCard(
    comment: String,
    onCommentChange: (String) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        shape = RoundedCornerShape(18.dp),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "Comentário (Opcional)",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
            )
            OutlinedTextField(
                value = comment,
                onValueChange = onCommentChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(128.dp),
                placeholder = {
                    Text(
                        text = "Partilhe mais detalhes sobre a sua experiência...",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                },
                minLines = 4,
                shape = RoundedCornerShape(14.dp),
                textStyle = MaterialTheme.typography.bodyMedium,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.tertiary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            )
        }
    }
}

@Composable
private fun RatingSubmittedScreen(
    contentPadding: PaddingValues,
    onHome: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding()
            .padding(horizontal = 24.dp)
            .padding(top = 48.dp, bottom = contentPadding.calculateBottomPadding() + 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Surface(
            modifier = Modifier.size(112.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.tertiary,
            contentColor = MaterialTheme.colorScheme.onTertiary,
            shadowElevation = 10.dp,
        ) {
            Icon(
                imageVector = Icons.Filled.Star,
                contentDescription = null,
                modifier = Modifier.padding(24.dp),
            )
        }
        Spacer(Modifier.height(32.dp))
        Text(
            text = "Obrigado!",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = "A sua avaliação ajuda-nos a melhorar o serviço",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(32.dp))
        Button(
            onClick = onHome,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.tertiary,
                contentColor = MaterialTheme.colorScheme.onTertiary,
            ),
        ) {
            Text(
                text = "Voltar ao Início",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

private fun ratingLabel(rating: Int): String = when (rating) {
    5 -> "Excelente!"
    4 -> "Muito Bom!"
    3 -> "Bom"
    2 -> "Pode Melhorar"
    else -> "Insatisfeito"
}

@Composable
private fun BookingStatusBadge(status: BookingStatusUi) {
    val containerColor = when (status) {
        BookingStatusUi.Pending -> MaterialTheme.colorScheme.secondaryContainer
        BookingStatusUi.Confirmed -> MaterialTheme.colorScheme.tertiaryContainer
        BookingStatusUi.InProgress -> MaterialTheme.colorScheme.primaryContainer
        BookingStatusUi.Completed -> MaterialTheme.colorScheme.primaryContainer
        BookingStatusUi.Cancelled -> MaterialTheme.colorScheme.errorContainer
        BookingStatusUi.Unknown -> MaterialTheme.colorScheme.surfaceContainerHigh
    }
    val contentColor = when (status) {
        BookingStatusUi.Pending -> MaterialTheme.colorScheme.onSecondaryContainer
        BookingStatusUi.Confirmed -> MaterialTheme.colorScheme.onTertiaryContainer
        BookingStatusUi.InProgress -> MaterialTheme.colorScheme.onPrimaryContainer
        BookingStatusUi.Completed -> MaterialTheme.colorScheme.onPrimaryContainer
        BookingStatusUi.Cancelled -> MaterialTheme.colorScheme.onErrorContainer
        BookingStatusUi.Unknown -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val icon = when (status) {
        BookingStatusUi.Pending,
        BookingStatusUi.Confirmed,
        BookingStatusUi.InProgress,
        BookingStatusUi.Completed -> Icons.Filled.CheckCircle
        BookingStatusUi.Cancelled,
        BookingStatusUi.Unknown -> Icons.Filled.RadioButtonUnchecked
    }

    Surface(
        color = containerColor,
        contentColor = contentColor,
        shape = CircleShape,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
            )
            Text(
                text = status.label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun BookingDetailRow(
    icon: ImageVector,
    text: String,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier.size(18.dp),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
