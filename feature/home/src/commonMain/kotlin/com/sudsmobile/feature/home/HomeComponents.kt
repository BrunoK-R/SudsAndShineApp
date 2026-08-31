package com.sudsmobile.feature.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Shield
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sudsmobile.shared.theme.SudsColors
import com.sudsmobile.shared.theme.SudsShapes
import com.sudsmobile.shared.theme.SudsSpacing
import com.sudsmobile.shared.ui.SudsGlassCard
import com.sudsmobile.shared.ui.SudsAutomotivePhoto
import com.sudsmobile.shared.ui.SudsAutomotivePhotoKind
import com.sudsmobile.shared.ui.SudsBrandMark
import com.sudsmobile.shared.ui.SudsSectionHeader
import com.sudsmobile.shared.ui.SudsStatus
import com.sudsmobile.shared.ui.SudsStatusCard
import com.sudsmobile.shared.ui.SudsWashCalendarIcon
import com.sudsmobile.shared.ui.automotivePhotoKindForKey

@Composable
internal fun HomeExpandedHeader(
    identity: HomeIdentityUi,
    locationLabel: String,
    collapseProgress: Float,
    onOpenNotifications: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .height(64.dp)
            .padding(horizontal = SudsSpacing.contentGutter)
            .then(
                if (collapseProgress >= 0.5f) Modifier.clearAndSetSemantics { }
                else Modifier,
            ),
        horizontalArrangement = Arrangement.spacedBy(SudsSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SudsBrandMark(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape),
            contentDescription = "Suds & Shine",
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(1.dp),
        ) {
            Text(
                text = identity.greeting,
                color = SudsColors.onBrand,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Filled.Place,
                    contentDescription = null,
                    tint = SudsColors.champagne,
                    modifier = Modifier.size(14.dp),
                )
                Spacer(Modifier.width(SudsSpacing.xxs))
                Text(
                    text = locationLabel,
                    color = SudsColors.onBrandMuted,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Icon(
                    imageVector = Icons.Filled.KeyboardArrowDown,
                    contentDescription = null,
                    tint = SudsColors.onBrandMuted,
                    modifier = Modifier.size(15.dp),
                )
            }
        }
        HomeNotificationAction(onClick = onOpenNotifications)
    }
}

@Composable
internal fun HomeCompactHeader(
    collapseProgress: Float,
    reduceMotion: Boolean,
    onOpenNotifications: () -> Unit,
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
        SudsBrandMark(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape),
            contentDescription = "Suds & Shine",
        )
        Text(
            text = "Início",
            modifier = Modifier.weight(1f),
            color = SudsColors.onBrand,
            style = MaterialTheme.typography.titleLarge,
        )
        HomeNotificationAction(
            onClick = onOpenNotifications,
            enabled = interactive,
        )
    }
}

@Composable
private fun HomeNotificationAction(
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    Surface(
        modifier = Modifier
            .size(SudsSpacing.minimumTouchTarget)
            .semantics {
                role = Role.Button
                contentDescription = "Abrir notificações"
            }
            .clickable(enabled = enabled, onClick = onClick),
        shape = CircleShape,
        color = Color.Transparent,
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Filled.NotificationsNone,
                contentDescription = null,
                tint = SudsColors.onBrand,
                modifier = Modifier.size(26.dp),
            )
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 8.dp, end = 8.dp)
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(SudsColors.champagne),
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
    artworkTranslationPx: Float,
    artworkAlpha: Float,
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
            onBookService = onBookService,
            onViewBookings = onViewBookings,
            artworkTranslationPx = artworkTranslationPx,
            artworkAlpha = artworkAlpha,
            modifier = modifier,
        )
    }
}

@Composable
private fun UpcomingBookingHero(
    booking: HomeBookingUi,
    onBookService: () -> Unit,
    onViewBookings: () -> Unit,
    artworkTranslationPx: Float,
    artworkAlpha: Float,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(SudsSpacing.sm),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(251.dp)
                .clip(SudsShapes.card)
                .border(BorderStroke(SudsSpacing.hairline, SudsColors.glassBorder), SudsShapes.card)
                .semantics {
                    role = Role.Button
                    contentDescription = "${booking.service}, ${booking.date}, ${booking.time}. Ver detalhes"
                }
                .clickable(onClick = onViewBookings),
        ) {
            SudsAutomotivePhoto(
                kind = SudsAutomotivePhotoKind.AppointmentHero,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = 1.06f
                        scaleY = 1.06f
                        translationY = artworkTranslationPx
                        alpha = artworkAlpha
                    },
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            0f to SudsColors.ink.copy(alpha = 0.96f),
                            0.56f to SudsColors.ink.copy(alpha = 0.74f),
                            1f to Color.Transparent,
                        ),
                    ),
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.58f)
                    .align(Alignment.CenterStart)
                    .padding(SudsSpacing.xl),
                verticalArrangement = Arrangement.spacedBy(SudsSpacing.xs),
            ) {
                Text(
                    text = "PRÓXIMA LAVAGEM",
                    color = SudsColors.cyanMuted,
                    style = MaterialTheme.typography.labelMedium,
                )
                Text(
                    text = booking.service,
                    color = SudsColors.onBrand,
                    style = MaterialTheme.typography.headlineLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = SudsSpacing.xxs),
                    color = SudsColors.glassBorder,
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(SudsSpacing.xs),
                ) {
                    Icon(
                        imageVector = Icons.Filled.CalendarMonth,
                        contentDescription = null,
                        tint = SudsColors.champagne,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        text = "${booking.date.substringBefore(",")} · ${booking.time}",
                        color = SudsColors.onBrand,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                    )
                }
                Row(
                    modifier = Modifier.padding(top = SudsSpacing.xs),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Ver detalhes",
                        color = SudsColors.cyanMuted,
                        style = MaterialTheme.typography.labelLarge,
                    )
                    Spacer(Modifier.width(SudsSpacing.xs))
                    Icon(
                        imageVector = Icons.Filled.ChevronRight,
                        contentDescription = null,
                        tint = SudsColors.cyanMuted,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(66.dp)
                .semantics {
                    role = Role.Button
                    contentDescription = "Marcar lavagem"
                }
                .clickable(onClick = onBookService),
            shape = SudsShapes.capsule,
            color = Color.Transparent,
            contentColor = SudsColors.onAction,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.horizontalGradient(SudsColors.actionGradient))
                    .padding(horizontal = SudsSpacing.xl),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SudsWashCalendarIcon(
                    tint = SudsColors.onAction,
                    size = 26.dp,
                )
                Text(
                    text = "Marcar lavagem",
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Icon(
                    imageVector = Icons.Filled.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
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
        title = if (loyalty.rewardReady) "Oferta pronta" else "O seu brilho",
        body = if (loyalty.rewardReady) {
            "Tem uma lavagem grátis disponível para a próxima visita."
        } else {
            "Faltam ${loyalty.remainingWashes} selos para a próxima lavagem grátis."
        },
        onClick = onClick,
        modifier = modifier,
    )
}

@Composable
private fun HomeLoyaltyBubble(
    current: Int,
    target: Int,
    title: String,
    body: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val safeTarget = target.coerceAtLeast(1)
    val visibleTarget = safeTarget.coerceAtMost(10)
    val visibleCurrent = current.coerceIn(0, safeTarget)

    SudsGlassCard(
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                role = Role.Button
                contentDescription = "$title. $body"
                stateDescription = "$visibleCurrent de $safeTarget selos"
                progressBarRangeInfo = ProgressBarRangeInfo(
                    current = visibleCurrent.toFloat(),
                    range = 0f..safeTarget.toFloat(),
                    steps = (safeTarget - 1).coerceAtLeast(0),
                )
            }
            .clickable(onClick = onClick),
        shape = SudsShapes.control,
        contentPadding = PaddingValues(
            horizontal = SudsSpacing.md,
            vertical = SudsSpacing.sm,
        ),
        containerColor = SudsColors.ink.copy(alpha = 0.84f),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(SudsSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.width(104.dp),
                verticalArrangement = Arrangement.spacedBy(1.dp),
            ) {
                Text(
                    text = title,
                    color = SudsColors.onBrand,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "$visibleCurrent",
                        color = SudsColors.cyan,
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    Spacer(Modifier.width(SudsSpacing.xxs))
                    Text(
                        text = "de $safeTarget selos",
                        modifier = Modifier.padding(bottom = 2.dp),
                        color = SudsColors.onBrandMuted,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                repeat(visibleTarget) { index ->
                    val completed = index < visibleCurrent.coerceAtMost(visibleTarget)
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(
                                if (completed) SudsColors.cyan.copy(alpha = 0.16f)
                                else SudsColors.glassStrong,
                            )
                            .border(
                                width = SudsSpacing.hairline,
                                color = if (completed) SudsColors.cyan else SudsColors.glassBorder,
                                shape = CircleShape,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (completed) {
                            Icon(
                                imageVector = Icons.Filled.AutoAwesome,
                                contentDescription = null,
                                tint = SudsColors.cyanMuted,
                                modifier = Modifier.size(9.dp),
                            )
                        }
                    }
                }
            }
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = SudsColors.onBrandMuted,
                modifier = Modifier.size(20.dp),
            )
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
            modifier = Modifier.padding(horizontal = SudsSpacing.contentGutter),
            action = {
                HomeTextAction(
                    label = "Ver todos",
                    onClick = onViewServices,
                    compact = true,
                    contentColor = SudsColors.champagne,
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
                horizontalArrangement = Arrangement.spacedBy(SudsSpacing.xs),
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
    Box(
        modifier = Modifier
            .width(136.dp)
            .height(180.dp)
            .clip(SudsShapes.control)
            .background(SudsColors.glassStrong)
            .border(SudsSpacing.hairline, SudsColors.glassBorder, SudsShapes.control)
            .semantics {
                role = Role.Button
                contentDescription = "${service.name}, a partir de ${service.price}, ${service.duration}"
            }
            .clickable(onClick = onClick),
    ) {
        SudsAutomotivePhoto(
            kind = automotivePhotoKindForKey("${service.id} ${service.name}"),
            modifier = Modifier.fillMaxSize(),
            contentDescription = null,
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        0.5f to SudsColors.ink.copy(alpha = 0.12f),
                        1f to SudsColors.ink.copy(alpha = 0.94f),
                    ),
                ),
        )
        Surface(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(SudsSpacing.sm)
                .size(34.dp),
            shape = CircleShape,
            color = if (service.popular) SudsColors.champagne else SudsColors.glassStrong,
        ) {
            Icon(
                imageVector = if (service.popular) Icons.Filled.AutoAwesome else service.icon,
                contentDescription = null,
                modifier = Modifier.padding(SudsSpacing.xs),
                tint = if (service.popular) SudsColors.onAction else SudsColors.onBrand,
            )
        }
        Text(
            text = service.name,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(SudsSpacing.sm),
            color = SudsColors.onBrand,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
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
    contentColor: Color = SudsColors.cyan,
) {
    TextButton(
        onClick = onClick,
        modifier = Modifier.heightIn(min = SudsSpacing.minimumTouchTarget),
        colors = ButtonDefaults.textButtonColors(contentColor = contentColor),
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
