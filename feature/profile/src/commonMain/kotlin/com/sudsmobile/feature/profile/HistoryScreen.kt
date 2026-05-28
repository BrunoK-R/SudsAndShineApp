package com.sudsmobile.feature.profile

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
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun HistoryScreen(
    contentPadding: PaddingValues,
    onBack: () -> Unit,
    onRequestSignIn: () -> Unit = {},
    onRateService: (String) -> Unit = {},
    onBookAgain: (String) -> Unit = {},
) {
    val viewModel: ProfileHistoryViewModel = koinViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val sessionState by viewModel.sessionState.collectAsStateWithLifecycle()
    val bookingRevision by viewModel.bookingRevision.collectAsStateWithLifecycle()

    LaunchedEffect(sessionState, bookingRevision) {
        viewModel.refreshForSession()
    }

    HistoryScreenContent(
        contentPadding = contentPadding,
        uiState = uiState,
        onBack = onBack,
        onRetry = viewModel::loadHistory,
        onRequestSignIn = onRequestSignIn,
        onRateService = onRateService,
        onBookAgain = onBookAgain,
    )
}

@Composable
private fun HistoryScreenContent(
    contentPadding: PaddingValues,
    uiState: ProfileHistoryUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onRequestSignIn: () -> Unit,
    onRateService: (String) -> Unit,
    onBookAgain: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(bottom = contentPadding.calculateBottomPadding() + 24.dp),
    ) {
        HistoryHeader(onBack = onBack)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = (-16).dp)
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            when (uiState) {
                ProfileHistoryUiState.Idle,
                ProfileHistoryUiState.Loading -> HistoryStatusCard(
                    title = "A carregar histórico",
                    body = "Estamos a consultar as lavagens associadas à sua conta.",
                    loading = true,
                )

                ProfileHistoryUiState.Unauthenticated -> HistoryStatusCard(
                    title = "Sessão necessária",
                    body = "Entre na sua conta para ver o histórico de lavagens.",
                    icon = HistoryStatusIcon.Locked,
                    actionLabel = "Entrar ou criar conta",
                    onAction = onRequestSignIn,
                )

                ProfileHistoryUiState.Empty -> HistoryStatusCard(
                    title = "Sem lavagens concluídas",
                    body = "Quando uma lavagem for concluída, o detalhe aparece aqui.",
                    icon = HistoryStatusIcon.Empty,
                    actionLabel = "Atualizar",
                    onAction = onRetry,
                )

                is ProfileHistoryUiState.Error -> HistoryStatusCard(
                    title = "Não foi possível carregar",
                    body = uiState.message,
                    icon = HistoryStatusIcon.Error,
                    actionLabel = if (uiState.retryable) "Tentar novamente" else null,
                    onAction = if (uiState.retryable) onRetry else null,
                )

                is ProfileHistoryUiState.Loaded -> {
                    HistorySummaryCard(summary = uiState.summary)

                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        uiState.items.forEach { item ->
                            HistoryItemCard(
                                item = item,
                                onRateService = { onRateService(item.id) },
                                onBookAgain = item.rebookServiceId?.let { serviceId ->
                                    { onBookAgain(serviceId) }
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryHeader(onBack: () -> Unit) {
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
            text = "Histórico de Lavagens",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.inverseOnSurface,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "Todas as suas lavagens anteriores",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = 0.72f),
        )
    }
}

@Composable
private fun HistorySummaryCard(summary: ProfileHistorySummaryUi) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiary),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        shape = RoundedCornerShape(18.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            HistorySummaryStat(
                label = "Total de Lavagens",
                value = summary.washCount,
                modifier = Modifier.weight(1f),
            )
            HistorySummaryStat(
                label = "Total Investido",
                value = summary.totalSpent,
                modifier = Modifier.weight(1f),
                alignEnd = true,
            )
        }
    }
}

@Composable
private fun HistorySummaryStat(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    alignEnd: Boolean = false,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = if (alignEnd) Alignment.End else Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onTertiary.copy(alpha = 0.78f),
            textAlign = if (alignEnd) TextAlign.End else TextAlign.Start,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onTertiary,
            fontWeight = FontWeight.Bold,
            textAlign = if (alignEnd) TextAlign.End else TextAlign.Start,
        )
    }
}

@Composable
private fun HistoryItemCard(
    item: ProfileHistoryItemUi,
    onRateService: () -> Unit,
    onBookAgain: (() -> Unit)?,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 5.dp),
        shape = RoundedCornerShape(18.dp),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                HistoryStatusBadge(status = item.status)
                Text(
                    text = item.price,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.tertiary,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.End,
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = item.service,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = item.vehicle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.CalendarMonth,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.tertiary,
                )
                Text(
                    text = item.date,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            HistoryBookingDetails(item = item)

            if (item.auditNotes.isNotEmpty()) {
                HistoryAuditNotes(notes = item.auditNotes)
            }

            if (item.reviewed) {
                HistoryReviewSummary(
                    rating = item.reviewRating,
                    tags = item.reviewTags,
                    comment = item.reviewComment,
                )
            } else if (item.reviewable) {
                HistoryReviewAction(onRateService = onRateService)
            }

            if (onBookAgain != null) {
                HistoryBookAgainAction(onBookAgain = onBookAgain)
            }
        }
    }
}

@Composable
private fun HistoryBookingDetails(item: ProfileHistoryItemUi) {
    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        HistoryDetailRow(label = "Referência", value = item.reference)
        HistoryDetailRow(label = "Hora", value = item.time)
        HistoryDetailRow(label = "Pagamento", value = item.paymentStatus)

        if (item.extras.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Text(
                    text = "Extras",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold,
                )
                item.extras.forEach { extra ->
                    HistoryDetailRow(label = extra.name, value = extra.price)
                }
            }
        }
    }
}

@Composable
private fun HistoryDetailRow(
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            modifier = Modifier.weight(1.1f),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.End,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun HistoryReviewSummary(
    rating: Int?,
    tags: List<String>,
    comment: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Avaliação enviada",
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
private fun HistoryReviewAction(onRateService: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(
                imageVector = Icons.Filled.Star,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.tertiary,
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    text = "Avaliação pendente",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "Partilhe como correu esta lavagem concluída.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        OutlinedButton(
            onClick = onRateService,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.tertiary,
            ),
        ) {
            Icon(
                imageVector = Icons.Filled.Star,
                contentDescription = null,
                modifier = Modifier.size(17.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Avaliar serviço",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun HistoryBookAgainAction(onBookAgain: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(
                imageVector = Icons.Filled.CalendarMonth,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.tertiary,
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    text = "Reservar novamente",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "Usar o mesmo serviço como ponto de partida.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        OutlinedButton(
            onClick = onBookAgain,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.tertiary,
            ),
        ) {
            Icon(
                imageVector = Icons.Filled.CalendarMonth,
                contentDescription = null,
                modifier = Modifier.size(17.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Marcar outra lavagem",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun HistoryAuditNotes(notes: List<ProfileHistoryAuditNoteUi>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        notes.forEach { note ->
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.28f),
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
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
                        tint = MaterialTheme.colorScheme.tertiary,
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
private fun HistoryStatusBadge(status: ProfileHistoryStatusUi) {
    val containerColor = when (status) {
        ProfileHistoryStatusUi.Completed -> MaterialTheme.colorScheme.primaryContainer
        ProfileHistoryStatusUi.Cancelled -> MaterialTheme.colorScheme.errorContainer
        ProfileHistoryStatusUi.Past -> MaterialTheme.colorScheme.secondaryContainer
    }
    val contentColor = when (status) {
        ProfileHistoryStatusUi.Completed -> MaterialTheme.colorScheme.onPrimaryContainer
        ProfileHistoryStatusUi.Cancelled -> MaterialTheme.colorScheme.onErrorContainer
        ProfileHistoryStatusUi.Past -> MaterialTheme.colorScheme.onSecondaryContainer
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
                imageVector = Icons.Filled.CheckCircle,
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

private enum class HistoryStatusIcon {
    Empty,
    Error,
    Locked,
}

@Composable
private fun HistoryStatusCard(
    title: String,
    body: String,
    loading: Boolean = false,
    icon: HistoryStatusIcon = HistoryStatusIcon.Empty,
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
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.tertiary,
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Icon(
                                imageVector = when (icon) {
                                    HistoryStatusIcon.Empty -> Icons.Filled.History
                                    HistoryStatusIcon.Error -> Icons.Filled.ErrorOutline
                                    HistoryStatusIcon.Locked -> Icons.Filled.Lock
                                },
                                contentDescription = null,
                                modifier = Modifier.size(22.dp),
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
                if (icon == HistoryStatusIcon.Locked) {
                    Button(
                        onClick = onAction,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp),
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
                } else {
                    OutlinedButton(
                        onClick = onAction,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp),
                        shape = RoundedCornerShape(12.dp),
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
