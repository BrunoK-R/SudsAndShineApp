package com.sudsmobile.feature.products

import kotlin.test.Test
import kotlin.test.assertEquals

class ProductsCatalogFormattingTest {
    @Test
    fun formatsBackendPriceCentsForPortugueseCatalog() {
        assertEquals("32,00€", 3200.toEuroLabel())
        assertEquals("18,50€", 1850.toEuroLabel())
        assertEquals("0,05€", 5.toEuroLabel())
    }

    @Test
    fun resolvesInitialServiceIdFromLoadedCatalogIds() {
        val serviceIds = listOf("standard", "premium", "interior")

        assertEquals("premium", resolveInitialServiceId(" premium ", serviceIds))
        assertEquals(null, resolveInitialServiceId("missing", serviceIds))
        assertEquals(null, resolveInitialServiceId(" ", serviceIds))
    }
}
