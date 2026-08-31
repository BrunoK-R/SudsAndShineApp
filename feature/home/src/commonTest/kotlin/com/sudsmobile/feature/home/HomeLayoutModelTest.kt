package com.sudsmobile.feature.home

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import com.sudsmobile.shared.loyalty.toLoyaltyProgress
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HomeLayoutModelTest {
    @Test
    fun collapseProgressTracksHeaderOffsetAndPinsAfterFirstItem() {
        assertEquals(0f, calculateHomeCollapseProgress(0, 0, 200))
        assertEquals(0.5f, calculateHomeCollapseProgress(0, 100, 200))
        assertEquals(1f, calculateHomeCollapseProgress(0, 240, 200))
        assertEquals(1f, calculateHomeCollapseProgress(1, 0, 200))
    }

    @Test
    fun artworkFadesSubtlyWithoutDisappearing() {
        assertFloatEquals(1f, homeArtworkAlpha(0f))
        assertFloatEquals(0.86f, homeArtworkAlpha(0.5f))
        assertFloatEquals(0.72f, homeArtworkAlpha(1f))
    }

    @Test
    fun everyStateKeepsStableKeyedSectionOrder() {
        val expected = listOf(
            HomeSection.Header,
            HomeSection.Booking,
            HomeSection.Services,
            HomeSection.Loyalty,
            HomeSection.Stats,
            HomeSection.Benefits,
        )
        homeStates().forEach { state ->
            assertEquals(expected, homeSections(state))
            assertEquals(expected.map(HomeSection::key), homeSections(state).map(HomeSection::key))
        }
    }

    @Test
    fun headerUsesCompactBookingLocationWhenAvailable() {
        val state = loadedState(
            nextBooking = booking().copy(location = "Suds & Shine - Leiria, Piso -1"),
        )

        assertEquals("Leiria", state.homeHeaderLocationLabel())
        assertEquals("Bem-vindo de volta", emptyState().homeHeaderLocationLabel())
    }

    @Test
    fun bookingPresentationReflectsEachHomeState() {
        assertEquals(HomeBookingPresentation.Loading, homeBookingPresentation(HomeUiState.Idle))
        assertEquals(HomeBookingPresentation.Loading, homeBookingPresentation(HomeUiState.Loading))
        assertEquals(HomeBookingPresentation.Guest, homeBookingPresentation(unauthenticatedState()))
        assertEquals(HomeBookingPresentation.Empty, homeBookingPresentation(emptyState()))
        assertEquals(HomeBookingPresentation.Error, homeBookingPresentation(errorState()))
        assertEquals(HomeBookingPresentation.Empty, homeBookingPresentation(loadedState(nextBooking = null)))
        assertEquals(
            HomeBookingPresentation.Upcoming,
            homeBookingPresentation(loadedState(nextBooking = booking())),
        )
    }

    @Test
    fun pixelReferenceFixtureKeepsAcceptanceCopyAndOrderDeterministic() {
        val fixture = homePixelReferenceState()

        assertEquals("Olá, Bruno", fixture.identity.greeting)
        assertEquals("Leiria", fixture.homeHeaderLocationLabel())
        assertEquals("Lavagem Premium", fixture.nextBooking?.service)
        assertEquals("Ter, 1 de setembro", fixture.nextBooking?.date)
        assertEquals("10:30", fixture.nextBooking?.time)
        assertEquals(7, fixture.loyalty.currentWashes)
        assertEquals(
            listOf("Exterior", "Completa", "Detailing"),
            fixture.featuredServices.map(HomeFeaturedServiceUi::name),
        )
    }

    private fun homeStates(): List<HomeUiState> = listOf(
        HomeUiState.Idle,
        HomeUiState.Loading,
        unauthenticatedState(),
        emptyState(),
        loadedState(nextBooking = null),
        loadedState(nextBooking = booking()),
        errorState(),
    )

    private fun unauthenticatedState() = HomeUiState.Unauthenticated(
        identity = identity(),
        featuredServices = emptyList(),
        stats = emptyList(),
    )

    private fun emptyState() = HomeUiState.Empty(
        identity = identity(),
        loyalty = 0.toLoyaltyProgress(),
        featuredServices = emptyList(),
        stats = emptyList(),
    )

    private fun loadedState(nextBooking: HomeBookingUi?) = HomeUiState.Loaded(
        identity = identity(),
        nextBooking = nextBooking,
        loyalty = 4.toLoyaltyProgress(),
        featuredServices = emptyList(),
        stats = emptyList(),
    )

    private fun errorState() = HomeUiState.Error(
        identity = identity(),
        message = "Indisponível",
        retryable = true,
        featuredServices = emptyList(),
        stats = emptyList(),
    )

    private fun identity() = HomeIdentityUi(
        greeting = "Olá, Bruno!",
        subtitle = "Bem-vindo de volta",
        initials = "BR",
    )

    private fun booking() = HomeBookingUi(
        id = "booking-1",
        service = "Lavagem Premium",
        date = "30 de agosto, 2026",
        time = "10:00",
        location = "Suds & Shine",
        vehicle = "BMW 320d",
        price = "32,00€",
        statusLabel = "Confirmado",
        icon = Icons.Filled.DirectionsCar,
    )

    private fun assertFloatEquals(expected: Float, actual: Float) {
        assertTrue(abs(expected - actual) < 0.0001f, "Expected $expected, got $actual")
    }
}
