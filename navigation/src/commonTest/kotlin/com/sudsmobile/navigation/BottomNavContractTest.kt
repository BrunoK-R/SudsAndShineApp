package com.sudsmobile.navigation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.math.abs

class BottomNavContractTest {

    @Test
    fun navigationUsesFourTabsAndOneCentralBookingAction() {
        assertEquals(
            listOf(Routes.Home, Routes.Cart, Routes.Loyalty, Routes.Profile),
            mainTabDestinations.map(MainNavDestination::route),
        )
        assertEquals(Routes.Products, bookingDestination.route)
        assertEquals(
            listOf(Routes.Home, Routes.Cart, Routes.Products, Routes.Loyalty, Routes.Profile),
            mainDestinations.map(MainNavDestination::route),
        )
    }

    @Test
    fun selectedRouteMapsToItsNavigationRole() {
        assertEquals(MainNavigationSelection.Home, mainNavigationSelection(Routes.Home))
        assertEquals(MainNavigationSelection.Bookings, mainNavigationSelection(Routes.Cart))
        assertEquals(MainNavigationSelection.Booking, mainNavigationSelection(Routes.Products))
        assertEquals(MainNavigationSelection.Rewards, mainNavigationSelection(Routes.Loyalty))
        assertEquals(MainNavigationSelection.Profile, mainNavigationSelection(Routes.Profile))
        assertNull(mainNavigationSelection(Routes.Services))
        assertTrue(isMainDestinationRoute(Routes.Products))
        assertFalse(isMainDestinationRoute(Routes.Payment))
    }

    @Test
    fun transitionDirectionFollowsVisualDestinationOrder() {
        assertEquals(
            NavigationTransitionDirection.Forward,
            navigationTransitionDirection(Routes.Home, Routes.Products),
        )
        assertEquals(
            NavigationTransitionDirection.Backward,
            navigationTransitionDirection(Routes.Profile, Routes.Cart),
        )
        assertEquals(
            NavigationTransitionDirection.None,
            navigationTransitionDirection(Routes.Home, Routes.Home),
        )
        assertEquals(
            NavigationTransitionDirection.None,
            navigationTransitionDirection(Routes.Home, Routes.Services),
        )
    }

    @Test
    fun compactPortugueseLabelsRemainMeaningful() {
        assertEquals("Agenda", mainTabDestinations.first { it.route == Routes.Cart }.compactLabel)
        assertEquals("Prémios", mainTabDestinations.first { it.route == Routes.Loyalty }.compactLabel)
    }

    @Test
    fun selectedIndicatorCenterMatchesEveryVisualIconSlot() {
        val totalWidth = 377f
        val indicatorSize = 38f
        val slotWidth = totalWidth / 5f

        listOf(0, 1, 3, 4).forEach { slot ->
            val offset = navigationIndicatorOffset(totalWidth, slot, indicatorSize)
            val indicatorCenter = offset + (indicatorSize / 2f)
            val iconSlotCenter = (slotWidth * slot) + (slotWidth / 2f)

            assertTrue(abs(indicatorCenter - iconSlotCenter) < 0.001f)
        }
    }

    @Test
    fun selectedIndicatorCenterMatchesTabIconVertically() {
        val indicatorOffset = navigationIndicatorVerticalOffset(
            totalHeight = 72f,
            topPadding = 4f,
            bottomPadding = 2f,
            iconSize = 22f,
            labelLineHeight = 12f,
            indicatorSize = 38f,
        )

        assertEquals(12f, indicatorOffset)
        assertEquals(31f, indicatorOffset + 19f)
    }
}
