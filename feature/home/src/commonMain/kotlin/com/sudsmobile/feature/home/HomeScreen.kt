package com.sudsmobile.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    contentPadding: PaddingValues,
    onBookService: () -> Unit = {},
    onViewServices: () -> Unit = {},
    onViewBookings: () -> Unit = {},
    onOpenRewards: () -> Unit = {},
    onOpenProfile: () -> Unit = {},
    onRequestSignIn: () -> Unit = {},
) {
    val viewModel: HomeViewModel = koinViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val sessionState by viewModel.sessionState.collectAsStateWithLifecycle()
    val bookingRevision by viewModel.bookingRevision.collectAsStateWithLifecycle()

    LaunchedEffect(sessionState, bookingRevision) {
        viewModel.refreshForSession()
    }

    HomeScreenContent(
        contentPadding = contentPadding,
        uiState = uiState,
        onBookService = onBookService,
        onViewServices = onViewServices,
        onViewBookings = onViewBookings,
        onOpenRewards = onOpenRewards,
        onOpenProfile = onOpenProfile,
        onRequestSignIn = onRequestSignIn,
        onRetry = viewModel::retry,
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HomeScreenContent(
    contentPadding: PaddingValues,
    uiState: HomeUiState,
    onBookService: () -> Unit,
    onViewServices: () -> Unit,
    onViewBookings: () -> Unit,
    onOpenRewards: () -> Unit,
    onOpenProfile: () -> Unit,
    onRequestSignIn: () -> Unit,
    onRetry: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(bottom = contentPadding.calculateBottomPadding()),
    ) {
        HomeHeader(
            identity = uiState.identityOrDefault(),
            onBookService = onBookService,
            onOpenProfile = onOpenProfile,
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(top = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            HomeBookingSummary(
                uiState = uiState,
                onBookService = onBookService,
                onViewBookings = onViewBookings,
                onRequestSignIn = onRequestSignIn,
                onRetry = onRetry,
            )
            HomeLoyaltyCard(
                uiState = uiState,
                onOpenRewards = onOpenRewards,
                onRequestSignIn = onRequestSignIn,
            )
            FeaturedServices(
                services = uiState.featuredServicesOrEmpty(),
                warningMessage = uiState.warningMessageOrNull(),
                warningRetryable = uiState.warningRetryableOrFalse(),
                onBookService = onBookService,
                onViewServices = onViewServices,
                onRetry = onRetry,
            )
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                maxItemsInEachRow = 3,
            ) {
                StatCard(Icons.AutoMirrored.Filled.TrendingUp, "500+", "Carros", Modifier.weight(1f))
                StatCard(Icons.Filled.Star, "4.9", "Avaliação", Modifier.weight(1f))
                StatCard(Icons.Filled.EmojiEvents, "3+", "Anos", Modifier.weight(1f))
            }
            BenefitsCard()
        }
    }
}

@Composable
private fun HomeHeader(
    identity: HomeIdentityUi,
    onBookService: () -> Unit,
    onOpenProfile: () -> Unit,
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
            .padding(horizontal = 24.dp, vertical = 28.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = identity.greeting,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.inverseOnSurface,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = identity.subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = 0.72f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(16.dp))
            Surface(
                modifier = Modifier
                    .size(48.dp)
                    .clickable(onClick = onOpenProfile),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = 0.10f),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = Brush.linearGradient(
                        listOf(
                            MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = 0.24f),
                            MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = 0.08f),
                        ),
                    ),
                ),
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = identity.initials,
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = onBookService,
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
            ),
            contentPadding = PaddingValues(horizontal = 24.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row {
                    Icon(Icons.Filled.CalendarMonth, contentDescription = null)
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = "Marcar Agora",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Icon(Icons.Filled.ChevronRight, contentDescription = null)
            }
        }
    }
}

@Composable
private fun HomeBookingSummary(
    uiState: HomeUiState,
    onBookService: () -> Unit,
    onViewBookings: () -> Unit,
    onRequestSignIn: () -> Unit,
    onRetry: () -> Unit,
) {
    when (uiState) {
        HomeUiState.Idle,
        HomeUiState.Loading -> HomeStatusCard(
            title = "A carregar marcações",
            body = "Estamos a consultar os seus dados em tempo real.",
            loading = true,
        )

        is HomeUiState.Unauthenticated -> HomeStatusCard(
            title = "Acompanhe as suas marcações",
            body = "Entre na conta para ver a próxima lavagem e guardar o histórico.",
            actionLabel = "Entrar ou criar conta",
            onAction = onRequestSignIn,
        )

        is HomeUiState.Empty -> HomeStatusCard(
            title = "Sem próxima marcação",
            body = "Escolha um serviço e reserve um horário disponível.",
            actionLabel = "Marcar agora",
            onAction = onBookService,
        )

        is HomeUiState.Error -> HomeStatusCard(
            title = "Não foi possível carregar",
            body = uiState.message,
            actionLabel = if (uiState.retryable) "Tentar novamente" else null,
            onAction = if (uiState.retryable) onRetry else null,
        )

        is HomeUiState.Loaded -> {
            val booking = uiState.nextBooking
            if (booking == null) {
                HomeStatusCard(
                    title = "Sem próxima marcação",
                    body = "O histórico está atualizado. Reserve a próxima lavagem quando quiser.",
                    actionLabel = "Marcar agora",
                    onAction = onBookService,
                )
            } else {
                UpcomingBookingCard(
                    booking = booking,
                    onViewBookings = onViewBookings,
                )
            }
        }
    }
}

@Composable
private fun UpcomingBookingCard(
    booking: HomeBookingUi,
    onViewBookings: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        shape = RoundedCornerShape(18.dp),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    "Próxima Marcação",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                )
                Surface(
                    color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.28f),
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                    shape = CircleShape,
                ) {
                    Text(
                        booking.statusLabel,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            DetailRow(booking.icon, booking.service, true)
            DetailRow(Icons.Filled.CalendarMonth, booking.date)
            DetailRow(Icons.Filled.AccessTime, booking.time)
            DetailRow(Icons.Filled.DirectionsCar, booking.vehicle)
            DetailRow(Icons.Filled.Star, booking.price, true)
            OutlinedButton(
                onClick = onViewBookings,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary,
                ),
            ) {
                Text("Ver Detalhes")
            }
        }
    }
}

@Composable
private fun HomeStatusCard(
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
                    color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.30f),
                    contentColor = MaterialTheme.colorScheme.tertiary,
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
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
                        style = MaterialTheme.typography.titleSmall,
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
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary,
                    ),
                ) {
                    Text(actionLabel)
                }
            }
        }
    }
}

@Composable
private fun DetailRow(icon: ImageVector, text: String, highlighted: Boolean = false) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (highlighted) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.outline,
            modifier = Modifier.size(20.dp),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun HomeLoyaltyCard(
    uiState: HomeUiState,
    onOpenRewards: () -> Unit,
    onRequestSignIn: () -> Unit,
) {
    when (uiState) {
        HomeUiState.Idle,
        HomeUiState.Loading -> LoyaltyCard(
            title = "Programa de Fidelização",
            progressLabel = "A calcular lavagens",
            remainingLabel = "",
            progress = 0f,
            body = "O progresso será atualizado assim que a sessão estiver pronta.",
            onClick = onOpenRewards,
        )

        is HomeUiState.Unauthenticated -> LoyaltyCard(
            title = "Programa de Fidelização",
            progressLabel = "Entre para acumular lavagens",
            remainingLabel = "Conta necessária",
            progress = 0f,
            body = "As lavagens concluídas contam automaticamente para a próxima oferta.",
            onClick = onRequestSignIn,
        )

        is HomeUiState.Empty -> LoyaltyCard(
            loyalty = uiState.loyalty,
            onOpenRewards = onOpenRewards,
        )

        is HomeUiState.Loaded -> LoyaltyCard(
            loyalty = uiState.loyalty,
            onOpenRewards = onOpenRewards,
        )

        is HomeUiState.Error -> LoyaltyCard(
            title = "Programa de Fidelização",
            progressLabel = "Progresso indisponível",
            remainingLabel = "",
            progress = 0f,
            body = "Tente novamente para recuperar o histórico da sua conta.",
            onClick = onOpenRewards,
        )
    }
}

@Composable
private fun LoyaltyCard(
    loyalty: HomeLoyaltyUi,
    onOpenRewards: () -> Unit,
) {
    LoyaltyCard(
        title = "Programa de Fidelização",
        progressLabel = "${loyalty.completedWashes} de ${loyalty.targetWashes} lavagens",
        remainingLabel = "${loyalty.remainingWashes} restantes",
        progress = loyalty.progress,
        body = "Mais ${loyalty.remainingWashes} lavagens para ganhar 1 lavagem grátis!",
        onClick = onOpenRewards,
    )
}

@Composable
private fun LoyaltyCard(
    title: String,
    progressLabel: String,
    remainingLabel: String,
    progress: Float,
    body: String,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.inverseSurface),
        shape = RoundedCornerShape(18.dp),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.inverseOnSurface,
                    fontWeight = FontWeight.Bold,
                )
                Icon(
                    Icons.Filled.EmojiEvents,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiaryContainer,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    progressLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = 0.76f),
                )
                Text(
                    remainingLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    fontWeight = FontWeight.Bold,
                )
            }
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape),
                color = MaterialTheme.colorScheme.tertiaryContainer,
                trackColor = MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = 0.12f),
            )
            Text(
                body,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = 0.70f),
            )
        }
    }
}

@Composable
private fun FeaturedServices(
    services: List<HomeFeaturedServiceUi>,
    warningMessage: String?,
    warningRetryable: Boolean,
    onBookService: () -> Unit,
    onViewServices: () -> Unit,
    onRetry: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                "Serviços em Destaque",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
            )
            Text(
                "Ver Todos",
                modifier = Modifier.clickable(onClick = onViewServices),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.tertiary,
                fontWeight = FontWeight.Bold,
            )
        }
        if (services.isEmpty()) {
            HomeStatusCard(
                title = "Serviços indisponíveis",
                body = warningMessage ?: "Não foi possível carregar os serviços em destaque.",
                actionLabel = if (warningRetryable) "Tentar novamente" else null,
                onAction = if (warningRetryable) onRetry else null,
            )
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                services.forEach { service ->
                    ServiceCard(
                        icon = service.icon,
                        title = service.name,
                        price = service.price,
                        duration = service.duration,
                        modifier = Modifier.weight(1f),
                        onClick = onBookService,
                    )
                }
                if (services.size == 1) {
                    Spacer(Modifier.weight(1f))
                }
            }
            if (warningMessage != null) {
                Text(
                    text = warningMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ServiceCard(
    icon: ImageVector,
    title: String,
    price: String,
    duration: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    Card(
        modifier = modifier
            .aspectRatio(1.05f)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            IconBadge(icon)
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    price,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.tertiary,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    duration,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun StatCard(icon: ImageVector, value: String, label: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            IconBadge(icon, small = true)
            Text(
                value,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
            )
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun BenefitsCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(
                "Por que escolher-nos?",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
            )
            BenefitRow(Icons.Filled.Shield, "Acabamento Premium", "Produtos de qualidade superior")
            BenefitRow(Icons.Filled.AutoAwesome, "Serviço Rápido", "Eficiente e pontual")
        }
    }
}

@Composable
private fun BenefitRow(icon: ImageVector, title: String, body: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        IconBadge(icon, small = true)
        Column {
            Text(
                title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
            )
            Text(
                body,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun IconBadge(icon: ImageVector, small: Boolean = false) {
    Surface(
        modifier = Modifier.size(if (small) 40.dp else 48.dp),
        shape = RoundedCornerShape(if (small) 12.dp else 14.dp),
        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.30f),
        contentColor = MaterialTheme.colorScheme.tertiary,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.padding(if (small) 10.dp else 12.dp),
        )
    }
}
