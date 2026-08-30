package com.sudsmobile.feature.blog

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CardMembership
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sudsmobile.shared.theme.SudsColors
import com.sudsmobile.shared.theme.SudsCustomerTheme
import com.sudsmobile.shared.ui.SudsBrandBackground
import com.sudsmobile.shared.ui.SudsCompactTopBar
import com.sudsmobile.shared.ui.SudsGlassCard
import org.koin.compose.viewmodel.koinViewModel

@Suppress("DEPRECATION")
@Composable
fun BlogScreen(
    contentPadding: PaddingValues,
    onBookWash: () -> Unit = {},
    onRequestSignIn: () -> Unit = {},
) {
    val viewModel: LoyaltyViewModel = koinViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val sessionState by viewModel.sessionState.collectAsStateWithLifecycle()
    val bookingRevision by viewModel.bookingRevision.collectAsStateWithLifecycle()
    val referralViewModel: ReferralViewModel = koinViewModel()
    val referralUiState by referralViewModel.uiState.collectAsStateWithLifecycle()
    val entitlementsViewModel: ServiceEntitlementsViewModel = koinViewModel()
    val entitlementsUiState by entitlementsViewModel.uiState.collectAsStateWithLifecycle()
    val clipboardManager = LocalClipboardManager.current

    LaunchedEffect(sessionState, bookingRevision) {
        viewModel.refreshForSession()
    }
    LaunchedEffect(sessionState) {
        referralViewModel.refreshForSession()
        entitlementsViewModel.refreshForSession()
    }

    SudsCustomerTheme {
        SudsBrandBackground(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = contentPadding.calculateBottomPadding() + 24.dp),
            ) {
                LoyaltyHeader()

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .padding(top = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    LoyaltyContent(
                uiState = uiState,
                referralUiState = referralUiState,
                entitlementsUiState = entitlementsUiState,
                onRetry = viewModel::loadRewards,
                onRedeemReward = viewModel::redeemReward,
                onRetryReferral = { referralViewModel.refreshForSession(force = true) },
                onRetryEntitlements = { entitlementsViewModel.refreshForSession(force = true) },
                onReferralCodeChange = referralViewModel::updateClaimCode,
                onClaimReferral = referralViewModel::claimReferralCode,
                onCopyReferral = { shareMessage ->
                    clipboardManager.setText(AnnotatedString(shareMessage))
                    referralViewModel.markShareCopied()
                },
                onRequestSignIn = onRequestSignIn,
                        onBookWash = onBookWash,
                    )
                }
            }
        }
    }
}

@Composable
private fun LoyaltyHeader() {
    SudsCompactTopBar(
        title = "Recompensas",
        eyebrow = "Clube de brilho",
        modifier = Modifier
            .statusBarsPadding()
            .padding(top = 8.dp, bottom = 4.dp)
            .semantics { heading() },
    )
}

@Composable
private fun LoyaltyContent(
    uiState: LoyaltyUiState,
    referralUiState: ReferralUiState,
    entitlementsUiState: ServiceEntitlementsUiState,
    onRetry: () -> Unit,
    onRedeemReward: () -> Unit,
    onRetryReferral: () -> Unit,
    onRetryEntitlements: () -> Unit,
    onReferralCodeChange: (String) -> Unit,
    onClaimReferral: () -> Unit,
    onCopyReferral: (String) -> Unit,
    onRequestSignIn: () -> Unit,
    onBookWash: () -> Unit,
) {
    when (uiState) {
        LoyaltyUiState.Idle,
        LoyaltyUiState.Loading -> LoyaltyStatusCard(
            title = "A carregar recompensas",
            body = "Estamos a consultar o seu histórico em tempo real.",
            loading = true,
        )

        LoyaltyUiState.Unauthenticated -> {
            LoyaltyStatusCard(
                title = "Sessão necessária",
                body = "Entre na sua conta para ver selos ganhos e recompensas disponíveis.",
                icon = Icons.Filled.Lock,
                actionLabel = "Entrar ou criar conta",
                onAction = onRequestSignIn,
            )
            HowItWorksCard()
        }

        is LoyaltyUiState.Error -> {
            LoyaltyStatusCard(
                title = "Não foi possível carregar",
                body = uiState.message,
                icon = Icons.Filled.Refresh,
                actionLabel = if (uiState.retryable) "Tentar novamente" else null,
                onAction = if (uiState.retryable) onRetry else null,
            )
            HowItWorksCard()
        }

        is LoyaltyUiState.Empty -> {
            MainProgressCard(progress = uiState.progress)
            RewardClaimCard(
                availableRewards = uiState.availableRewards,
                claimedRewards = uiState.claimedRewards,
                activeRewards = uiState.rewardCodes.count { it.active },
                redemptionState = uiState.redemptionState,
                onRedeemReward = onRedeemReward,
            )
            IssuedRewardCodesCard(rewardCodes = uiState.rewardCodes)
            ReferralCard(
                uiState = referralUiState,
                onRetry = onRetryReferral,
                onCodeChange = onReferralCodeChange,
                onClaim = onClaimReferral,
                onCopy = onCopyReferral,
            )
            ServiceEntitlementsCard(uiState = entitlementsUiState, onRetry = onRetryEntitlements)
            StampGridCard(progress = uiState.progress)
            HowItWorksCard(stampsRequired = uiState.progress.targetWashes)
            StampHistoryCard(history = emptyList())
            BookWashButton(onClick = onBookWash)
        }

        is LoyaltyUiState.Loaded -> {
            MainProgressCard(progress = uiState.progress)
            RewardClaimCard(
                availableRewards = uiState.availableRewards,
                claimedRewards = uiState.claimedRewards,
                activeRewards = uiState.rewardCodes.count { it.active },
                redemptionState = uiState.redemptionState,
                onRedeemReward = onRedeemReward,
            )
            IssuedRewardCodesCard(rewardCodes = uiState.rewardCodes)
            ReferralCard(
                uiState = referralUiState,
                onRetry = onRetryReferral,
                onCodeChange = onReferralCodeChange,
                onClaim = onClaimReferral,
                onCopy = onCopyReferral,
            )
            ServiceEntitlementsCard(uiState = entitlementsUiState, onRetry = onRetryEntitlements)
            StampGridCard(progress = uiState.progress)
            HowItWorksCard(stampsRequired = uiState.progress.targetWashes)
            StampHistoryCard(history = uiState.history)
            BookWashButton(onClick = onBookWash)
        }
    }
}

@Composable
private fun RewardClaimCard(
    availableRewards: Int,
    claimedRewards: Int,
    activeRewards: Int,
    redemptionState: LoyaltyRedemptionUiState,
    onRedeemReward: () -> Unit,
) {
    val shouldShow = availableRewards > 0 ||
        activeRewards > 0 ||
        claimedRewards > 0 ||
        redemptionState !is LoyaltyRedemptionUiState.Idle
    if (!shouldShow) return

    val title: String
    val body: String
    val icon: ImageVector
    val loading: Boolean
    val actionLabel: String?
    val actionEnabled: Boolean

    when (redemptionState) {
        LoyaltyRedemptionUiState.Idle -> {
            when {
                availableRewards > 0 -> {
                    title = "Lavagem grátis disponível"
                    body = "Tem ${availableRewards.toRewardLabel()} pronta para emitir e validar na loja."
                }
                activeRewards > 0 -> {
                    title = "Lavagem grátis emitida"
                    body = "Use o código já emitido na próxima marcação. O banner desaparece quando a lavagem gratuita for concluída."
                }
                else -> {
                    title = "Recompensas usadas"
                    body = "${claimedRewards.toRewardLabel().replaceFirstChar { it.uppercase() }} já foi usada neste ciclo."
                }
            }
            icon = Icons.Filled.CardGiftcard
            loading = false
            actionLabel = if (availableRewards > 0) "Resgatar recompensa" else null
            actionEnabled = availableRewards > 0
        }
        LoyaltyRedemptionUiState.Redeeming -> {
            title = "A emitir recompensa"
            body = "Estamos a guardar a sua lavagem grátis na conta."
            icon = Icons.Filled.CardGiftcard
            loading = true
            actionLabel = null
            actionEnabled = false
        }
        is LoyaltyRedemptionUiState.Success -> {
            title = "Recompensa resgatada"
            body = redemptionState.message
            icon = Icons.Filled.CheckCircle
            loading = false
            actionLabel = null
            actionEnabled = false
        }
        is LoyaltyRedemptionUiState.Error -> {
            title = "Não foi possível resgatar"
            body = redemptionState.message
            icon = Icons.Filled.Refresh
            loading = false
            actionLabel = if (redemptionState.retryable && availableRewards > 0) {
                "Tentar novamente"
            } else {
                null
            }
            actionEnabled = actionLabel != null
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
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
                    modifier = Modifier.size(44.dp),
                    color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.42f),
                    contentColor = MaterialTheme.colorScheme.tertiary,
                    shape = RoundedCornerShape(14.dp),
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
                                imageVector = icon,
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

            if (actionLabel != null) {
                Button(
                    onClick = onRedeemReward,
                    enabled = actionEnabled,
                    modifier = Modifier.fillMaxWidth(),
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
            }
        }
    }
}

@Composable
private fun IssuedRewardCodesCard(rewardCodes: List<LoyaltyRewardCodeUi>) {
    if (rewardCodes.isEmpty()) return

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        shape = RoundedCornerShape(18.dp),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            SectionTitle(
                icon = Icons.Filled.CardGiftcard,
                title = "Códigos Emitidos",
            )
            rewardCodes.forEachIndexed { index, rewardCode ->
                RewardCodeRow(rewardCode = rewardCode)
                if (index != rewardCodes.lastIndex) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }
    }
}

@Composable
private fun RewardCodeRow(rewardCode: LoyaltyRewardCodeUi) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            modifier = Modifier.size(42.dp),
            color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.46f),
            contentColor = MaterialTheme.colorScheme.tertiary,
            shape = RoundedCornerShape(12.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text(
                text = rewardCode.code,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = rewardCode.issuedAt,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Surface(
            color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.42f),
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
            shape = CircleShape,
        ) {
            Text(
                text = rewardCode.statusLabel,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun ServiceEntitlementsCard(
    uiState: ServiceEntitlementsUiState,
    onRetry: () -> Unit,
) {
    when (uiState) {
        ServiceEntitlementsUiState.Idle,
        ServiceEntitlementsUiState.Loading -> LoyaltyStatusCard(
            title = "A carregar planos",
            body = "Estamos a consultar pacotes e planos ativos.",
            loading = true,
            icon = Icons.Filled.CardMembership,
        )
        ServiceEntitlementsUiState.Unauthenticated -> Unit
        is ServiceEntitlementsUiState.Error -> LoyaltyStatusCard(
            title = "Planos indisponíveis",
            body = uiState.message,
            icon = Icons.Filled.CardMembership,
            actionLabel = if (uiState.retryable) "Tentar novamente" else null,
            onAction = if (uiState.retryable) onRetry else null,
        )
        is ServiceEntitlementsUiState.Loaded -> ServiceEntitlementsLoadedCard(uiState.entitlements)
    }
}

@Composable
private fun ServiceEntitlementsLoadedCard(entitlements: List<ServiceEntitlementUi>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        shape = RoundedCornerShape(18.dp),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            SectionTitle(icon = Icons.Filled.CardMembership, title = "Planos e pacotes")
            Text(
                text = "São ativados pela equipa após compra no local. Esta app não faz cobranças nem renovações automáticas.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (entitlements.isEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Text(
                        text = "Ainda não tem um pacote ou plano associado à conta.",
                        modifier = Modifier.padding(14.dp),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            } else {
                entitlements.take(6).forEachIndexed { index, entitlement ->
                    ServiceEntitlementRow(entitlement)
                    if (index < entitlements.take(6).lastIndex) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun ServiceEntitlementRow(entitlement: ServiceEntitlementUi) {
    val active = entitlement.status == "active"
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = entitlement.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "${entitlement.kindLabel} · ${entitlement.code}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Surface(
                color = if (active) {
                    MaterialTheme.colorScheme.tertiaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceContainer
                },
                contentColor = if (active) {
                    MaterialTheme.colorScheme.onTertiaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                shape = CircleShape,
            ) {
                Text(
                    text = entitlement.statusLabel,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
        Text(
            text = "${entitlement.remainingUses} de ${entitlement.totalUses} utilizações disponíveis",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold,
        )
        LinearProgressIndicator(
            progress = {
                if (entitlement.totalUses > 0) {
                    entitlement.usedUses.toFloat() / entitlement.totalUses.toFloat()
                } else {
                    0f
                }
            },
            modifier = Modifier.fillMaxWidth().height(7.dp).clip(CircleShape),
            color = MaterialTheme.colorScheme.tertiary,
            trackColor = MaterialTheme.colorScheme.surfaceContainer,
        )
        if (entitlement.eligibleServicesLabel.isNotBlank()) {
            Text(
                text = "Inclui: ${entitlement.eligibleServicesLabel}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = "Válido até ${entitlement.validUntilLabel}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ReferralCard(
    uiState: ReferralUiState,
    onRetry: () -> Unit,
    onCodeChange: (String) -> Unit,
    onClaim: () -> Unit,
    onCopy: (String) -> Unit,
) {
    when (uiState) {
        ReferralUiState.Idle,
        ReferralUiState.Loading -> LoyaltyStatusCard(
            title = "A preparar o seu convite",
            body = "Estamos a criar um código pessoal e seguro.",
            loading = true,
            icon = Icons.Filled.PersonAdd,
        )
        ReferralUiState.Unauthenticated -> Unit
        is ReferralUiState.Error -> LoyaltyStatusCard(
            title = "Indicações indisponíveis",
            body = uiState.message,
            icon = Icons.Filled.PersonAdd,
            actionLabel = if (uiState.retryable) "Tentar novamente" else null,
            onAction = if (uiState.retryable) onRetry else null,
        )
        is ReferralUiState.Loaded -> ReferralLoadedCard(
            state = uiState,
            onRetry = onRetry,
            onCodeChange = onCodeChange,
            onClaim = onClaim,
            onCopy = onCopy,
        )
    }
}

@Composable
private fun ReferralLoadedCard(
    state: ReferralUiState.Loaded,
    onRetry: () -> Unit,
    onCodeChange: (String) -> Unit,
    onClaim: () -> Unit,
    onCopy: (String) -> Unit,
) {
    val program = state.program
    val submitting = state.actionState is ReferralActionUiState.Submitting

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
            SectionTitle(
                icon = Icons.Filled.PersonAdd,
                title = "Convide um amigo",
            )
            Text(
                text = "Partilhe o seu código. Depois da primeira lavagem paga do seu amigo, cada um recebe ${program.rewardPoints.toStampLabel()} extra.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.42f),
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                shape = RoundedCornerShape(14.dp),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "O seu código",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.72f),
                        )
                        Text(
                            text = program.code,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Icon(
                        imageVector = Icons.Filled.CardGiftcard,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }

            Button(
                onClick = { onCopy(program.shareMessage) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.tertiary,
                    contentColor = MaterialTheme.colorScheme.onTertiary,
                ),
            ) {
                Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Copiar convite", fontWeight = FontWeight.Bold)
            }

            if (program.claimedCount > 0) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    ReferralStat(value = program.qualifiedCount, label = "Concluídas")
                    ReferralStat(value = program.pendingCount, label = "Pendentes")
                    ReferralStat(value = program.bonusPointsEarned, label = "Selos ganhos")
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            if (program.referredByStatus == null && program.canClaimCode) {
                Text(
                    text = "Recebeu um código? Associe-o nos primeiros ${program.attributionDays} dias e antes da primeira lavagem paga.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = state.claimCode,
                    onValueChange = onCodeChange,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !submitting,
                    singleLine = true,
                    label = { Text("Código de indicação") },
                    placeholder = { Text("SUDS-XXXXXXXXXX") },
                )
                OutlinedButton(
                    onClick = onClaim,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !submitting && state.claimCode.isNotBlank(),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    if (submitting) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Associar código", fontWeight = FontWeight.Bold)
                    }
                }
            } else if (program.referredByStatus != null) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                        Text(
                            text = program.referredByStatus,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "Código ${program.referredByCode.orEmpty()}",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            } else {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Text(
                        text = program.claimIneligibleMessage
                            ?: "Este código já não pode ser associado a esta conta.",
                        modifier = Modifier.padding(14.dp),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }

            when (val action = state.actionState) {
                ReferralActionUiState.Idle,
                ReferralActionUiState.Submitting -> Unit
                ReferralActionUiState.Copied -> ReferralFeedback(
                    message = "Convite copiado. Já pode colá-lo numa mensagem.",
                    error = false,
                )
                is ReferralActionUiState.Success -> ReferralFeedback(message = action.message, error = false)
                is ReferralActionUiState.Error -> ReferralFeedback(message = action.message, error = true)
            }

            OutlinedButton(
                onClick = onRetry,
                modifier = Modifier.fillMaxWidth(),
                enabled = !submitting,
                shape = RoundedCornerShape(12.dp),
            ) {
                Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Atualizar estado")
            }
        }
    }
}

@Composable
private fun ReferralStat(value: Int, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.tertiary,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ReferralFeedback(message: String, error: Boolean) {
    Text(
        text = message,
        style = MaterialTheme.typography.bodySmall,
        color = if (error) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary,
    )
}

@Composable
private fun LoyaltyStatusCard(
    title: String,
    body: String,
    loading: Boolean = false,
    icon: ImageVector = Icons.Filled.CardGiftcard,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
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
                    modifier = Modifier.size(44.dp),
                    color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.42f),
                    contentColor = MaterialTheme.colorScheme.tertiary,
                    shape = RoundedCornerShape(14.dp),
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
                                imageVector = icon,
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
private fun MainProgressCard(progress: LoyaltyProgressUi) {
    SudsGlassCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = SudsColors.glassStrong,
    ) {
        Box {
            Icon(
                imageVector = Icons.Filled.EmojiEvents,
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 12.dp, end = 8.dp)
                    .size(128.dp),
                tint = SudsColors.champagne.copy(alpha = 0.10f),
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.CardGiftcard,
                        contentDescription = null,
                        tint = SudsColors.champagne,
                        modifier = Modifier.size(26.dp),
                    )
                    Text(
                        text = "Programa de Fidelização",
                        style = MaterialTheme.typography.titleLarge,
                        color = SudsColors.onBrand,
                        fontWeight = FontWeight.Bold,
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom,
                    ) {
                        ProgressStat(
                            label = "Progresso Atual",
                            value = "${progress.currentWashes}/${progress.targetWashes}",
                        )
                        ProgressStat(
                            label = "Faltam",
                            value = progress.remainingWashes.toString(),
                            alignEnd = true,
                        )
                    }
                    LinearProgressIndicator(
                        progress = { progress.progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(CircleShape),
                        color = SudsColors.champagne,
                        trackColor = SudsColors.glassStrong,
                    )
                }

                Text(
                    text = progress.progressMessage(),
                    style = MaterialTheme.typography.bodySmall,
                    color = SudsColors.onBrandMuted,
                )
            }
        }
    }
}

@Composable
private fun ProgressStat(
    label: String,
    value: String,
    alignEnd: Boolean = false,
) {
    Column(
        horizontalAlignment = if (alignEnd) Alignment.End else Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = SudsColors.onBrandMuted,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.headlineLarge,
            color = SudsColors.onBrand,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun StampGridCard(progress: LoyaltyProgressUi) {
    SudsGlassCard(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "Seus Selos",
                style = MaterialTheme.typography.titleMedium,
                color = SudsColors.onBrand,
                fontWeight = FontWeight.Bold,
            )

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                (0 until progress.targetWashes).chunked(5).forEach { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        rowItems.forEach { index ->
                            StampCell(
                                index = index,
                                earned = index < progress.currentWashes,
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
private fun StampCell(
    index: Int,
    earned: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Surface(
            modifier = Modifier.size(44.dp),
            color = if (earned) SudsColors.champagne else SudsColors.glassStrong,
            contentColor = if (earned) SudsColors.onAction else SudsColors.onBrandMuted,
            shape = CircleShape,
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (earned) {
                    Icon(
                        imageVector = Icons.Filled.AutoAwesome,
                        contentDescription = "Selo ganho",
                        modifier = Modifier.size(22.dp),
                    )
                } else {
                    Text(
                        text = "${index + 1}",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

@Composable
private fun HowItWorksCard(stampsRequired: Int = 10) {
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
            SectionTitle(
                icon = Icons.AutoMirrored.Filled.TrendingUp,
                title = "Como Funciona",
            )
            HowItWorksStep(
                number = "1",
                title = "Ganhe selos",
                description = "Cada lavagem paga conta como 1 selo; indicações qualificadas também dão selos",
            )
            HowItWorksStep(
                number = "2",
                title = "Acumule Selos",
                description = "Junte $stampsRequired selos no total",
            )
            HowItWorksStep(
                number = "3",
                title = "Ganhe Recompensa",
                description = "Receba 1 lavagem grátis!",
            )
        }
    }
}

@Composable
private fun HowItWorksStep(
    number: String,
    title: String,
    description: String,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Surface(
            modifier = Modifier.size(32.dp),
            color = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
            shape = CircleShape,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = number,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun StampHistoryCard(history: List<LoyaltyHistoryItemUi>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        shape = RoundedCornerShape(18.dp),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = "Histórico de Selos",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
            )
            if (history.isEmpty()) {
                Text(
                    text = "Ainda não tem selos no histórico de recompensas.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                history.forEachIndexed { index, item ->
                    LoyaltyHistoryRow(item = item)
                    if (index != history.lastIndex) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun LoyaltyHistoryRow(item: LoyaltyHistoryItemUi) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            modifier = Modifier.size(42.dp),
            color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.46f),
            contentColor = MaterialTheme.colorScheme.tertiary,
            shape = RoundedCornerShape(12.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Filled.AutoAwesome,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
            }
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text(
                text = item.service,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.CalendarMonth,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(14.dp),
                )
                Text(
                    text = item.date,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Surface(
            color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.42f),
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
            shape = CircleShape,
        ) {
            Text(
                text = "+${item.points} selo",
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun BookWashButton(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.inverseSurface,
            contentColor = MaterialTheme.colorScheme.inverseOnSurface,
        ),
        contentPadding = PaddingValues(horizontal = 20.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.CardGiftcard, contentDescription = null)
                Spacer(Modifier.width(10.dp))
                Text(
                    text = "Marcar Nova Lavagem",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
            }
            Icon(Icons.Filled.ChevronRight, contentDescription = null)
        }
    }
}

@Composable
private fun SectionTitle(
    icon: ImageVector,
    title: String,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier.size(20.dp),
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
        )
    }
}

private fun LoyaltyProgressUi.progressMessage(): String {
    return if (rewardReady) {
        "Tem uma recompensa pronta para usar na próxima lavagem."
    } else {
        "Mais $remainingWashes selos para ganhar 1 lavagem grátis."
    }
}

private fun Int.toRewardLabel(): String {
    return if (this == 1) {
        "1 recompensa"
    } else {
        "$this recompensas"
    }
}

private fun Int.toStampLabel(): String = if (this == 1) "1 selo" else "$this selos"
