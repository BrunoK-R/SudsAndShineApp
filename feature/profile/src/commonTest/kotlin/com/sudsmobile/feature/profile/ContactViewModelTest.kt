package com.sudsmobile.feature.profile

import com.sudsmobile.data.business.BusinessFaq
import com.sudsmobile.data.business.BusinessInfo
import com.sudsmobile.data.business.BusinessInfoError
import com.sudsmobile.data.business.BusinessInfoRepository
import com.sudsmobile.data.business.BusinessInfoResult
import com.sudsmobile.data.business.BusinessOpeningHours
import com.sudsmobile.data.business.BusinessStat
import com.sudsmobile.data.business.DefaultBusinessInfo
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
class ContactViewModelTest {
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
    fun loadBusinessInfoMapsBackendContactDetails() = runTest {
        val viewModel = ContactViewModel(
            businessInfoRepository = FakeBusinessInfoRepository(
                BusinessInfoResult.Success(customBusinessInfo()),
            ),
        )

        viewModel.loadBusinessInfo()
        runCurrent()

        val loaded = assertIs<ContactBusinessInfoUiState.Loaded>(viewModel.businessInfoState.value)
        assertEquals("244 000 111", loaded.info.phone)
        assertEquals("Rua Nova 10", loaded.info.addressLine1)
        assertEquals("Dias úteis", loaded.info.openingHours.single().dayLabel)
        assertEquals("Pergunta?", loaded.info.faq.single().question)
        assertEquals("900+", loaded.info.stats.single().value)
    }

    @Test
    fun loadBusinessInfoKeepsFallbackForRetryableBackendError() = runTest {
        val viewModel = ContactViewModel(
            businessInfoRepository = FakeBusinessInfoRepository(
                BusinessInfoResult.Failure(
                    BusinessInfoError.Unavailable("Contactos indisponíveis."),
                ),
            ),
        )

        viewModel.loadBusinessInfo()
        runCurrent()

        val error = assertIs<ContactBusinessInfoUiState.Error>(viewModel.businessInfoState.value)
        assertEquals("Contactos indisponíveis.", error.message)
        assertEquals(true, error.retryable)
        assertEquals(DefaultBusinessInfo.phone, error.fallbackInfo.phone)
        assertEquals(DefaultBusinessInfo.addressLine1, error.fallbackInfo.addressLine1)
    }

    @Test
    fun loadBusinessInfoReusesLoadedStateUnlessForced() = runTest {
        val repository = FakeBusinessInfoRepository(
            BusinessInfoResult.Success(customBusinessInfo(phone = "244 000 111")),
            BusinessInfoResult.Success(customBusinessInfo(phone = "244 000 222")),
        )
        val viewModel = ContactViewModel(repository)

        viewModel.loadBusinessInfo()
        runCurrent()
        viewModel.loadBusinessInfo()
        runCurrent()

        val loaded = assertIs<ContactBusinessInfoUiState.Loaded>(viewModel.businessInfoState.value)
        assertEquals("244 000 111", loaded.info.phone)
        assertEquals(1, repository.calls)

        viewModel.loadBusinessInfo(force = true)
        runCurrent()

        val forced = assertIs<ContactBusinessInfoUiState.Loaded>(viewModel.businessInfoState.value)
        assertEquals("244 000 222", forced.info.phone)
        assertEquals(2, repository.calls)
    }

    @Test
    fun forcedBusinessInfoRefreshKeepsLatestResponse() = runTest {
        val oldResult = CompletableDeferred<BusinessInfoResult>()
        val newResult = CompletableDeferred<BusinessInfoResult>()
        val repository = DeferredBusinessInfoRepository(oldResult, newResult)
        val viewModel = ContactViewModel(repository)

        viewModel.loadBusinessInfo()
        runCurrent()
        viewModel.loadBusinessInfo(force = true)
        runCurrent()

        assertIs<ContactBusinessInfoUiState.Loading>(viewModel.businessInfoState.value)
        assertEquals(2, repository.calls)

        newResult.complete(
            BusinessInfoResult.Success(customBusinessInfo(phone = "244 000 222")),
        )
        runCurrent()

        val latest = assertIs<ContactBusinessInfoUiState.Loaded>(viewModel.businessInfoState.value)
        assertEquals("244 000 222", latest.info.phone)

        oldResult.complete(
            BusinessInfoResult.Success(customBusinessInfo(phone = "244 000 111")),
        )
        runCurrent()

        val stillLatest = assertIs<ContactBusinessInfoUiState.Loaded>(viewModel.businessInfoState.value)
        assertEquals("244 000 222", stillLatest.info.phone)
    }
}

private class FakeBusinessInfoRepository(
    vararg results: BusinessInfoResult,
) : BusinessInfoRepository {
    private val results = results.toMutableList()
    var calls: Int = 0
        private set

    override suspend fun getBusinessInfo(): BusinessInfoResult {
        calls += 1
        return if (results.isNotEmpty()) {
            results.removeAt(0)
        } else {
            BusinessInfoResult.Success(customBusinessInfo())
        }
    }
}

private class DeferredBusinessInfoRepository(
    vararg results: CompletableDeferred<BusinessInfoResult>,
) : BusinessInfoRepository {
    private val pendingResults = results.toMutableList()
    var calls: Int = 0
        private set

    override suspend fun getBusinessInfo(): BusinessInfoResult {
        calls += 1
        return pendingResults.removeAt(0).await()
    }
}

private fun customBusinessInfo(phone: String = "244 000 111"): BusinessInfo = BusinessInfo(
    phone = phone,
    phoneUri = "tel:${phone.filter { it.isDigit() }}",
    email = "geral@example.pt",
    emailUri = "mailto:geral@example.pt",
    addressLine1 = "Rua Nova 10",
    addressLine2 = "Leiria, Portugal",
    mapsUri = "https://maps.example.test",
    whatsappUri = "https://wa.me/351244000111",
    openingHours = listOf(
        BusinessOpeningHours(
            dayLabel = "Dias úteis",
            hoursLabel = "10:00 - 18:00",
            closed = false,
        ),
    ),
    faq = listOf(
        BusinessFaq(
            question = "Pergunta?",
            answer = "Resposta.",
        ),
    ),
    stats = listOf(
        BusinessStat(
            value = "900+",
            label = "Clientes",
        ),
    ),
)
