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
}
