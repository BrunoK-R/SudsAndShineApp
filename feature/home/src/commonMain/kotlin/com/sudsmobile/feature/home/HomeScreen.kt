package com.sudsmobile.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sudsmobile.shared.theme.LocalSudsMotionPreferences
import com.sudsmobile.shared.theme.SudsSpacing
import com.sudsmobile.shared.ui.SudsBrandBackground
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun HomeScreen(
    contentPadding: PaddingValues,
    onBookService: () -> Unit = {},
    onBookSelectedService: (String) -> Unit = { onBookService() },
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
        onBookSelectedService = onBookSelectedService,
        onViewServices = onViewServices,
        onViewBookings = onViewBookings,
        onOpenRewards = onOpenRewards,
        onOpenProfile = onOpenProfile,
        onRequestSignIn = onRequestSignIn,
        onRetry = viewModel::retry,
    )
}

@Composable
internal fun HomeScreenContent(
    contentPadding: PaddingValues,
    uiState: HomeUiState,
    onBookService: () -> Unit,
    onBookSelectedService: (String) -> Unit,
    onViewServices: () -> Unit,
    onViewBookings: () -> Unit,
    onOpenRewards: () -> Unit,
    onOpenProfile: () -> Unit,
    onRequestSignIn: () -> Unit,
    onRetry: () -> Unit,
) {
    val listState = rememberLazyListState()
    val reduceMotion = LocalSudsMotionPreferences.current.reduceMotion
    val collapseRangePx = with(LocalDensity.current) { HomeCollapseRange.roundToPx() }
    val collapseProgress by remember(listState, collapseRangePx) {
        derivedStateOf {
            calculateHomeCollapseProgress(
                firstVisibleItemIndex = listState.firstVisibleItemIndex,
                firstVisibleItemScrollOffset = listState.firstVisibleItemScrollOffset,
                collapseRangePx = collapseRangePx,
            )
        }
    }
    val artworkTranslationPx by remember(listState, reduceMotion) {
        derivedStateOf {
            if (reduceMotion || listState.firstVisibleItemIndex > 0) 0f
            else listState.firstVisibleItemScrollOffset * HomeArtworkParallaxFactor
        }
    }
    val identity = uiState.identityOrDefault()

    SudsBrandBackground(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = listState,
                contentPadding = PaddingValues(
                    bottom = contentPadding.calculateBottomPadding() + SudsSpacing.xl,
                ),
                verticalArrangement = Arrangement.spacedBy(SudsSpacing.lg),
            ) {
                homeSections(uiState).forEach { section ->
                    item(key = section.key) {
                        when (section) {
                            HomeSection.Header -> HomeExpandedHeader(
                                identity = identity,
                                artworkTranslationPx = artworkTranslationPx,
                                artworkAlpha = homeArtworkAlpha(collapseProgress),
                                collapseProgress = collapseProgress,
                                onBookService = onBookService,
                                onOpenProfile = onOpenProfile,
                            )

                            HomeSection.Booking -> HomeBookingSection(
                                uiState = uiState,
                                onBookService = onBookService,
                                onViewBookings = onViewBookings,
                                onRequestSignIn = onRequestSignIn,
                                onRetry = onRetry,
                                modifier = Modifier.homeContentPadding(),
                            )

                            HomeSection.Loyalty -> HomeLoyaltySection(
                                uiState = uiState,
                                onOpenRewards = onOpenRewards,
                                onRequestSignIn = onRequestSignIn,
                                modifier = Modifier.homeContentPadding(),
                            )

                            HomeSection.Services -> HomeFeaturedServicesSection(
                                services = uiState.featuredServicesOrEmpty(),
                                warningMessage = uiState.warningMessageOrNull(),
                                warningRetryable = uiState.warningRetryableOrFalse(),
                                onBookSelectedService = onBookSelectedService,
                                onViewServices = onViewServices,
                                onRetry = onRetry,
                            )

                            HomeSection.Stats -> HomeStatsSection(
                                stats = uiState.statsOrDefault(),
                                warningMessage = uiState.statsWarningMessageOrNull(),
                                warningRetryable = uiState.statsWarningRetryableOrFalse(),
                                onRetry = onRetry,
                                modifier = Modifier.homeContentPadding(),
                            )

                            HomeSection.Benefits -> HomeBenefitsSection(
                                modifier = Modifier.homeContentPadding(),
                            )
                        }
                    }
                }
            }

            HomeCompactHeader(
                identity = identity,
                collapseProgress = collapseProgress,
                reduceMotion = reduceMotion,
                onOpenProfile = onOpenProfile,
            )
        }
    }
}

private fun Modifier.homeContentPadding(): Modifier = padding(
    horizontal = SudsSpacing.contentGutter,
)

private val HomeCollapseRange = 156.dp
private const val HomeArtworkParallaxFactor = 0.35f
