package com.sudsmobile.navigation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

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
}
