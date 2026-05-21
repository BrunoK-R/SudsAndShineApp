package com.sudsmobile.feature.products

import com.sudsmobile.data.catalog.ServiceCatalog
import com.sudsmobile.data.catalog.ServiceCatalogExtra
import com.sudsmobile.data.catalog.ServiceCatalogResult
import com.sudsmobile.data.catalog.ServiceCatalogService
import com.sudsmobile.data.catalog.ServiceCatalogRepository
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
class ProductsCatalogFormattingTest {
    private val dispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

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

    @Test
    fun mapsBackendExtrasFromServiceCatalog() = runTest {
        val viewModel = ProductsCatalogViewModel(
            StaticServiceCatalogRepository(
                ServiceCatalog(
                    services = listOf(
                        ServiceCatalogService(
                            id = "premium",
                            name = "Lavagem Premium",
                            description = "Lavagem detalhada",
                            durationMinutes = 45,
                            passengerPriceCents = 3200,
                            suvPriceCents = 3400,
                            iconKey = "sparkles",
                            popular = true,
                        ),
                    ),
                    extras = listOf(
                        ServiceCatalogExtra(
                            id = "wax",
                            name = "Enceramento",
                            description = "Proteção extra",
                            priceCents = 1500,
                            iconKey = "shield",
                        ),
                    ),
                ),
            ),
        )

        viewModel.loadCatalog()
        runCurrent()

        val loaded = assertIs<ProductCatalogUiState.Loaded>(viewModel.catalogState.value)
        val extra = loaded.extras.single()
        assertEquals("wax", extra.id)
        assertEquals("Enceramento", extra.name)
        assertEquals("Proteção extra", extra.description)
        assertEquals("15,00€", extra.price)
        assertEquals(1500, extra.priceCents)
    }
}

private class StaticServiceCatalogRepository(
    private val catalog: ServiceCatalog,
) : ServiceCatalogRepository {
    override suspend fun getServiceCatalog(): ServiceCatalogResult = ServiceCatalogResult.Success(catalog)
}
