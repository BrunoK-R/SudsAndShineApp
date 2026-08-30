package com.sudsmobile.feature.home

import com.sudsmobile.shared.theme.calculateCollapseProgress

internal enum class HomeSection(val key: String) {
    Header("home_header"),
    Booking("home_booking"),
    Loyalty("home_loyalty"),
    Services("home_services"),
    Stats("home_stats"),
    Benefits("home_benefits"),
}

internal enum class HomeBookingPresentation {
    Loading,
    Guest,
    Empty,
    Error,
    Upcoming,
}

internal fun homeSections(uiState: HomeUiState): List<HomeSection> = when (uiState) {
    HomeUiState.Idle,
    HomeUiState.Loading,
    is HomeUiState.Unauthenticated,
    is HomeUiState.Empty,
    is HomeUiState.Loaded,
    is HomeUiState.Error -> HomeSection.entries
}

internal fun homeBookingPresentation(uiState: HomeUiState): HomeBookingPresentation = when (uiState) {
    HomeUiState.Idle,
    HomeUiState.Loading -> HomeBookingPresentation.Loading
    is HomeUiState.Unauthenticated -> HomeBookingPresentation.Guest
    is HomeUiState.Empty -> HomeBookingPresentation.Empty
    is HomeUiState.Error -> HomeBookingPresentation.Error
    is HomeUiState.Loaded -> if (uiState.nextBooking == null) {
        HomeBookingPresentation.Empty
    } else {
        HomeBookingPresentation.Upcoming
    }
}

internal fun calculateHomeCollapseProgress(
    firstVisibleItemIndex: Int,
    firstVisibleItemScrollOffset: Int,
    collapseRangePx: Int,
): Float = calculateCollapseProgress(
    firstVisibleItemIndex = firstVisibleItemIndex,
    firstVisibleItemScrollOffset = firstVisibleItemScrollOffset,
    collapseRangePx = collapseRangePx,
)

internal fun homeArtworkAlpha(collapseProgress: Float): Float {
    return 1f - (collapseProgress.coerceIn(0f, 1f) * 0.28f)
}
