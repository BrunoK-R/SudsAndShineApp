package com.sudsmobile.feature.home

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sudsmobile.shared.theme.SudsColors
import com.sudsmobile.shared.theme.SudsShapes
import com.sudsmobile.shared.theme.SudsSpacing
import com.sudsmobile.shared.ui.SudsGlassCard
import com.sudsmobile.shared.ui.SudsPrimaryButton
import com.sudsmobile.shared.ui.SudsSectionHeader
import com.sudsmobile.shared.ui.SudsServiceArtwork
import com.sudsmobile.shared.ui.SudsStatus
import com.sudsmobile.shared.ui.SudsStatusCard
import com.sudsmobile.shared.ui.serviceArtworkStyleForKey

@Composable
internal fun HomeExpandedHeader(
    identity: HomeIdentityUi,
    artworkTranslationPx: Float,
    artworkAlpha: Float,
    collapseProgress: Float,
    onBookService: () -> Unit,
    onOpenProfile: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .heightIn(min = 280.dp)
            .padding(
                start = SudsSpacing.contentGutter,
                top = SudsSpacing.md,
                end = SudsSpacing.contentGutter,
                bottom = SudsSpacing.xl,
            )
            .then(
                if (collapseProgress >= 0.5f) Modifier.clearAndSetSemantics { }
                else Modifier,
            ),
        verticalArrangement = Arrangement.spacedBy(SudsSpacing.xl),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(SudsSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(SudsSpacing.xxs),
            ) {
                Text(
                    text = identity.greeting,
                    color = SudsColors.onBrand,
                    style = MaterialTheme.typography.headlineSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = identity.subtitle,
                    color = SudsColors.onBrandMuted,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            HomeAvatar(
                initials = identity.initials,
                onClick = onOpenProfile,
            )
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(SudsSpacing.lg),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(SudsSpacing.xs)) {
                Text(
                    text = "CUIDADO AUTOMÓVEL",
                    color = SudsColors.cyanMuted,
                    style = MaterialTheme.typography.labelMedium,
                )
                Text(
                    text = "Brilho que se nota. Cuidado que fica.",
                    color = SudsColors.onBrand,
                    style = MaterialTheme.typography.headlineMedium,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(SudsSpacing.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SudsPrimaryButton(
                    label = "Marcar agora",
                    onClick = onBookService,
                    modifier = Modifier
                        .weight(1f)
                        .widthIn(max = 216.dp)
                        .semantics { contentDescription = "Marcar lavagem" },
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Filled.CalendarMonth,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                        )
                    },
                )
                SudsServiceArtwork(
                    modifier = Modifier.graphicsLayer {
                        translationY = artworkTranslationPx
                        alpha = artworkAlpha
                    },
                    size = 88.dp,
                )
            }
        }
    }
}

@Composable
internal fun HomeCompactHeader(
    identity: HomeIdentityUi,
    collapseProgress: Float,
    reduceMotion: Boolean,
    onOpenProfile: () -> Unit,
) {
    val progress = collapseProgress.coerceIn(0f, 1f)
    val visibleProgress = if (reduceMotion) {
        if (progress >= 0.5f) 1f else 0f
    } else {
        progress
    }
    val interactive = progress >= 0.5f

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { alpha = visibleProgress }
            .background(SudsColors.ink)
            .statusBarsPadding()
            .heightIn(min = 64.dp)
            .padding(horizontal = SudsSpacing.contentGutter)
            .then(
                if (!interactive) Modifier.clearAndSetSemantics { }
                else Modifier,
            ),
        horizontalArrangement = Arrangement.spacedBy(SudsSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(SudsSpacing.xxs),
        ) {
            Text(
                text = "SUDS & SHINE",
                color = SudsColors.cyanMuted,
                style = MaterialTheme.typography.labelSmall,
            )
            Text(
                text = identity.greeting,
                color = SudsColors.onBrand,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        HomeAvatar(
            initials = identity.initials,
            onClick = onOpenProfile,
            enabled = interactive,
            compact = true,
        )
    }
}

@Composable
private fun HomeAvatar(
    initials: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    compact: Boolean = false,
) {
    Surface(
        modifier = Modifier
            .size(if (compact) SudsSpacing.minimumTouchTarget else 52.dp)
            .semantics {
                role = Role.Button
                contentDescription = "Abrir perfil"
            }
            .clickable(enabled = enabled, onClick = onClick),
        shape = CircleShape,
        color = SudsColors.glassStrong,
        border = androidx.compose.foundation.BorderStroke(
            width = SudsSpacing.hairline,
            color = SudsColors.glassBorder,
        ),
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = initials,
                color = SudsColors.champagne,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
internal fun HomeBookingSection(
    uiState: HomeUiState,
    onBookService: () -> Unit,
    onViewBookings: () -> Unit,
    onRequestSignIn: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (homeBookingPresentation(uiState)) {
        HomeBookingPresentation.Loading -> SudsStatusCard(
            title = "A preparar as suas marcações",
            modifier = modifier,
            message = "Só demora um instante.",
            action = {
                CircularProgressIndicator(
                    modifier = Modifier
                        .padding(top = SudsSpacing.sm)
                        .size(20.dp),
                    color = SudsColors.cyan,
                    strokeWidth = 2.dp,
                )
            },
        )

        HomeBookingPresentation.Guest -> SudsStatusCard(
            title = "As suas marcações num só lugar",
            modifier = modifier,
            message = "Entre para acompanhar serviços, viaturas e recompensas.",
            action = { HomeTextAction("Entrar na conta", onRequestSignIn) },
        )

        HomeBookingPresentation.Empty -> SudsStatusCard(
            title = "Pronto para voltar a brilhar?",
            modifier = modifier,
            message = "Escolha o serviço ideal e reserve em poucos passos.",
            action = { HomeTextAction("Marcar agora", onBookService) },
        )

        HomeBookingPresentation.Error -> {
            val error = uiState as HomeUiState.Error
            SudsStatusCard(
                title = "Não conseguimos atualizar as marcações",
                modifier = modifier,
                message = error.message,
                status = SudsStatus.Error,
                action = if (error.retryable) {
                    { HomeTextAction("Tentar novamente", onRetry) }
                } else {
                    null
                },
            )
        }

        HomeBookingPresentation.Upcoming -> UpcomingBookingHero(
            booking = (uiState as HomeUiState.Loaded).nextBooking!!,
            onViewBookings = onViewBookings,
            modifier = modifier,
        )
    }
}

@Composable
private fun UpcomingBookingHero(
    booking: HomeBookingUi,
    onViewBookings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SudsGlassCard(
        modifier = modifier.fillMaxWidth(),
        containerColor = SudsColors.glassStrong,
        contentPadding = PaddingValues(SudsSpacing.xl),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(SudsSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "PRÓXIMA MARCAÇÃO",
                    color = SudsColors.cyanMuted,
                    style = MaterialTheme.typography.labelMedium,
                )
                Text(
                    text = booking.service,
                    color = SudsColors.onBrand,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Surface(
                shape = SudsShapes.capsule,
                color = SudsColors.success.copy(alpha = 0.16f),
            ) {
                Text(
                    text = booking.statusLabel,
                    modifier = Modifier.padding(
                        horizontal = SudsSpacing.sm,
                        vertical = SudsSpacing.xs,
                    ),
                    color = SudsColors.success,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }

        Spacer(Modifier.height(SudsSpacing.lg))
        HomeBookingDetail(Icons.Filled.CalendarMonth, booking.date)
        HomeBookingDetail(Icons.Filled.AccessTime, booking.time)
        HomeBookingDetail(Icons.Filled.Place, booking.location)
        HomeBookingDetail(Icons.Filled.DirectionsCar, booking.vehicle)
        HomeBookingDetail(Icons.Filled.Star, booking.price, emphasized = true)
        Spacer(Modifier.height(SudsSpacing.sm))
        HomeTextAction(
            label = "Ver detalhes",
            onClick = onViewBookings,
            trailingIcon = true,
        )
    }
}

@Composable
private fun HomeBookingDetail(
    icon: ImageVector,
    label: String,
    emphasized: Boolean = false,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = SudsSpacing.xs),
        horizontalArrangement = Arrangement.spacedBy(SudsSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (emphasized) SudsColors.champagne else SudsColors.cyanMuted,
            modifier = Modifier.size(20.dp),
        )
        Text(
            text = label,
            color = if (emphasized) SudsColors.onBrand else SudsColors.onBrandMuted,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (emphasized) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}

@Composable
internal fun HomeLoyaltySection(
    uiState: HomeUiState,
    onOpenRewards: () -> Unit,
    onRequestSignIn: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (uiState) {
        HomeUiState.Idle,
        HomeUiState.Loading -> HomeLoyaltyBubble(
            current = 0,
            target = 10,
            title = "A contar os seus selos",
            body = "O progresso aparece assim que a sessão estiver pronta.",
            onClick = onOpenRewards,
            modifier = modifier,
        )

        is HomeUiState.Unauthenticated -> HomeLoyaltyBubble(
            current = 0,
            target = 10,
            title = "Cada lavagem aproxima a próxima",
            body = "Entre para acumular selos e desbloquear ofertas.",
            actionLabel = "Entrar e começar",
            onClick = onRequestSignIn,
            modifier = modifier,
        )

        is HomeUiState.Empty -> HomeLoyaltyBubble(
            loyalty = uiState.loyalty,
            onClick = onOpenRewards,
            modifier = modifier,
        )

        is HomeUiState.Loaded -> HomeLoyaltyBubble(
            loyalty = uiState.loyalty,
            onClick = onOpenRewards,
            modifier = modifier,
        )

        is HomeUiState.Error -> HomeLoyaltyBubble(
            current = 0,
            target = 10,
            title = "Selos temporariamente indisponíveis",
            body = "Pode continuar a explorar as suas recompensas.",
            actionLabel = "Ver recompensas",
            onClick = onOpenRewards,
            modifier = modifier,
        )
    }
}

@Composable
private fun HomeLoyaltyBubble(
    loyalty: HomeLoyaltyUi,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    HomeLoyaltyBubble(
        current = loyalty.currentWashes,
        target = loyalty.targetWashes,
        title = if (loyalty.rewardReady) "A sua oferta está pronta" else "O brilho também recompensa",
        body = if (loyalty.rewardReady) {
            "Tem uma lavagem grátis disponível para a próxima visita."
        } else {
            "Faltam ${loyalty.remainingWashes} selos para a próxima lavagem grátis."
        },
        actionLabel = if (loyalty.rewardReady) "Usar oferta" else "Ver recompensas",
        onClick = onClick,
        modifier = modifier,
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HomeLoyaltyBubble(
    current: Int,
    target: Int,
    title: String,
    body: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
) {
    val safeTarget = target.coerceAtLeast(1)
    val visibleTarget = safeTarget.coerceAtMost(10)
    val visibleCurrent = current.coerceIn(0, safeTarget)

    SudsGlassCard(
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                role = Role.Button
                progressBarRangeInfo = ProgressBarRangeInfo(
                    current = visibleCurrent.toFloat(),
                    range = 0f..safeTarget.toFloat(),
                    steps = (safeTarget - 1).coerceAtLeast(0),
                )
            }
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(SudsSpacing.md),
            verticalAlignment = Alignment.Top,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(SudsSpacing.xxs),
            ) {
                Text(
                    text = "CLUBE DE BRILHO",
                    color = SudsColors.champagne,
                    style = MaterialTheme.typography.labelMedium,
                )
                Text(
                    text = title,
                    color = SudsColors.onBrand,
                    style = MaterialTheme.typography.titleLarge,
                )
            }
            Icon(
                imageVector = Icons.Filled.EmojiEvents,
                contentDescription = null,
                tint = SudsColors.champagne,
                modifier = Modifier.size(28.dp),
            )
        }
        Spacer(Modifier.height(SudsSpacing.md))
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(SudsSpacing.sm),
            verticalArrangement = Arrangement.spacedBy(SudsSpacing.sm),
            maxItemsInEachRow = 5,
        ) {
            repeat(visibleTarget) { index ->
                val completed = index < visibleCurrent.coerceAtMost(visibleTarget)
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(
                            if (completed) SudsColors.champagne
                            else SudsColors.glassStrong,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    if (completed) {
                        Icon(
                            imageVector = Icons.Filled.AutoAwesome,
                            contentDescription = null,
                            tint = SudsColors.onAction,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(SudsSpacing.md))
        Text(
            text = body,
            color = SudsColors.onBrandMuted,
            style = MaterialTheme.typography.bodyMedium,
        )
        if (actionLabel != null) {
            HomeTextAction(actionLabel, onClick)
        }
    }
}

@Composable
internal fun HomeFeaturedServicesSection(
    services: List<HomeFeaturedServiceUi>,
    warningMessage: String?,
    warningRetryable: Boolean,
    onBookSelectedService: (String) -> Unit,
    onViewServices: () -> Unit,
    onRetry: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(SudsSpacing.md)) {
        SudsSectionHeader(
            title = "Escolha o seu cuidado",
            supportingText = "Serviços pensados para cada rotina",
            modifier = Modifier.padding(horizontal = SudsSpacing.contentGutter),
            action = {
                HomeTextAction(
                    label = "Ver todos",
                    onClick = onViewServices,
                    compact = true,
                )
            },
        )

        if (services.isEmpty()) {
            SudsStatusCard(
                title = "Serviços indisponíveis",
                modifier = Modifier.padding(horizontal = SudsSpacing.contentGutter),
                message = warningMessage ?: "Não foi possível carregar os serviços em destaque.",
                status = SudsStatus.Warning,
                action = if (warningRetryable) {
                    { HomeTextAction("Tentar novamente", onRetry) }
                } else {
                    null
                },
            )
        } else {
            LazyRow(
                contentPadding = PaddingValues(horizontal = SudsSpacing.contentGutter),
                horizontalArrangement = Arrangement.spacedBy(SudsSpacing.md),
            ) {
                items(
                    items = services,
                    key = { it.id },
                ) { service ->
                    HomeServiceCard(
                        service = service,
                        onClick = { onBookSelectedService(service.id) },
                    )
                }
            }
            if (warningMessage != null) {
                Text(
                    text = warningMessage,
                    modifier = Modifier.padding(horizontal = SudsSpacing.contentGutter),
                    color = SudsColors.warning,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun HomeServiceCard(
    service: HomeFeaturedServiceUi,
    onClick: () -> Unit,
) {
    SudsGlassCard(
        modifier = Modifier
            .width(238.dp)
            .heightIn(min = 286.dp)
            .semantics {
                role = Role.Button
                contentDescription = "${service.name}, a partir de ${service.price}, ${service.duration}"
            }
            .clickable(onClick = onClick),
        contentPadding = PaddingValues(SudsSpacing.md),
    ) {
        Box(Modifier.fillMaxWidth()) {
            SudsServiceArtwork(
                modifier = Modifier.align(Alignment.Center),
                style = serviceArtworkStyleForKey("${service.id} ${service.name}"),
                size = 118.dp,
            )
            if (service.popular) {
                Surface(
                    modifier = Modifier.align(Alignment.TopEnd),
                    shape = SudsShapes.capsule,
                    color = SudsColors.champagne,
                ) {
                    Text(
                        text = "POPULAR",
                        modifier = Modifier.padding(
                            horizontal = SudsSpacing.sm,
                            vertical = SudsSpacing.xxs,
                        ),
                        color = SudsColors.onAction,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
        }
        Spacer(Modifier.height(SudsSpacing.md))
        Text(
            text = service.name,
            color = SudsColors.onBrand,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(SudsSpacing.xs))
        Text(
            text = "A partir de ${service.price}",
            color = SudsColors.champagne,
            style = MaterialTheme.typography.labelLarge,
        )
        Text(
            text = service.duration,
            color = SudsColors.onBrandMuted,
            style = MaterialTheme.typography.bodySmall,
        )
        Spacer(Modifier.height(SudsSpacing.sm))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Escolher serviço",
                color = SudsColors.cyan,
                style = MaterialTheme.typography.labelLarge,
            )
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = SudsColors.cyan,
            )
        }
    }
}

@Composable
internal fun HomeStatsSection(
    stats: List<HomeStatUi>,
    warningMessage: String?,
    warningRetryable: Boolean,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(SudsSpacing.sm),
    ) {
        SudsGlassCard(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(
                horizontal = SudsSpacing.md,
                vertical = SudsSpacing.lg,
            ),
        ) {
            Text(
                text = "CUIDADO COM PROVA DADA",
                color = SudsColors.cyanMuted,
                style = MaterialTheme.typography.labelMedium,
            )
            Spacer(Modifier.height(SudsSpacing.md))
            stats.take(3).forEachIndexed { index, stat ->
                HomeStatRow(stat)
                if (index < stats.take(3).lastIndex) {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = SudsSpacing.sm),
                        color = SudsColors.glassBorder,
                    )
                }
            }
        }
        if (warningMessage != null) {
            Text(
                text = warningMessage,
                color = SudsColors.warning,
                style = MaterialTheme.typography.bodySmall,
            )
            if (warningRetryable) HomeTextAction("Atualizar dados", onRetry)
        }
    }
}

@Composable
private fun HomeStatRow(stat: HomeStatUi) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(SudsSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        HomeIconBadge(stat.icon)
        Text(
            text = stat.value,
            modifier = Modifier.widthIn(min = 58.dp),
            color = SudsColors.onBrand,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = stat.label,
            modifier = Modifier.weight(1f),
            color = SudsColors.onBrandMuted,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
internal fun HomeBenefitsSection(modifier: Modifier = Modifier) {
    SudsGlassCard(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(SudsSpacing.lg),
    ) {
        Text(
            text = "Mais do que uma lavagem",
            color = SudsColors.onBrand,
            style = MaterialTheme.typography.titleLarge,
        )
        Spacer(Modifier.height(SudsSpacing.md))
        HomeBenefitRow(
            icon = Icons.Filled.Shield,
            title = "Cuidado consistente",
            body = "Produtos selecionados e atenção em cada detalhe.",
        )
        Spacer(Modifier.height(SudsSpacing.md))
        HomeBenefitRow(
            icon = Icons.Filled.AutoAwesome,
            title = "Tempo bem aproveitado",
            body = "Uma experiência simples, da marcação ao acabamento.",
        )
    }
}

@Composable
private fun HomeBenefitRow(
    icon: ImageVector,
    title: String,
    body: String,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(SudsSpacing.md),
        verticalAlignment = Alignment.Top,
    ) {
        HomeIconBadge(icon)
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(SudsSpacing.xxs),
        ) {
            Text(
                text = title,
                color = SudsColors.onBrand,
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                text = body,
                color = SudsColors.onBrandMuted,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun HomeIconBadge(icon: ImageVector) {
    Surface(
        modifier = Modifier.size(SudsSpacing.minimumTouchTarget),
        shape = SudsShapes.control,
        color = SudsColors.cyan.copy(alpha = 0.12f),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.padding(SudsSpacing.sm),
            tint = SudsColors.cyan,
        )
    }
}

@Composable
private fun HomeTextAction(
    label: String,
    onClick: () -> Unit,
    trailingIcon: Boolean = false,
    compact: Boolean = false,
) {
    TextButton(
        onClick = onClick,
        modifier = Modifier.heightIn(min = SudsSpacing.minimumTouchTarget),
        colors = ButtonDefaults.textButtonColors(contentColor = SudsColors.cyan),
        contentPadding = PaddingValues(
            horizontal = if (compact) SudsSpacing.xs else SudsSpacing.sm,
            vertical = SudsSpacing.xs,
        ),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (trailingIcon) {
            Spacer(Modifier.width(SudsSpacing.xs))
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}
