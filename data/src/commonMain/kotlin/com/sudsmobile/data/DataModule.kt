package com.sudsmobile.data

import com.sudsmobile.data.booking.BookingFunctionsApi
import com.sudsmobile.data.booking.BookingRepository
import com.sudsmobile.data.booking.FirebaseBookingRepository
import com.sudsmobile.data.booking.FirebaseFunctionsConfig
import com.sudsmobile.data.booking.KtorBookingFunctionsApi
import com.sudsmobile.data.network.createSudsHttpClient
import io.ktor.client.HttpClient
import org.koin.dsl.module

val dataModule = module {
    single<HttpClient> { createSudsHttpClient() }
    single {
        FirebaseFunctionsConfig.default(
            isDebugBuild = getKoin().getProperty<Boolean>("isDebugBuild") ?: false,
            platformName = get(),
        )
    }
    single<BookingFunctionsApi> { KtorBookingFunctionsApi(get(), get()) }
    single<BookingRepository> { FirebaseBookingRepository(get()) }
    single<AppRepository> { AppRepositoryImpl() }
}
