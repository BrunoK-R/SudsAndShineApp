package com.sudsmobile.data

import com.sudsmobile.data.booking.BookingFunctionsApi
import com.sudsmobile.data.booking.BookingChangeNotifier
import com.sudsmobile.data.booking.BookingRepository
import com.sudsmobile.data.booking.FirebaseBookingRepository
import com.sudsmobile.data.booking.FirebaseFunctionsConfig
import com.sudsmobile.data.booking.KtorBookingFunctionsApi
import com.sudsmobile.data.booking.MutableBookingChangeNotifier
import com.sudsmobile.data.auth.AuthApi
import com.sudsmobile.data.auth.AuthRepository
import com.sudsmobile.data.auth.AuthSessionStore
import com.sudsmobile.data.auth.FirebaseAuthConfig
import com.sudsmobile.data.auth.FirebaseAuthRepository
import com.sudsmobile.data.auth.KtorIdentityToolkitAuthApi
import com.sudsmobile.data.business.BusinessInfoFunctionsApi
import com.sudsmobile.data.business.BusinessInfoRepository
import com.sudsmobile.data.business.FirebaseBusinessInfoRepository
import com.sudsmobile.data.business.KtorBusinessInfoFunctionsApi
import com.sudsmobile.data.catalog.CatalogFunctionsApi
import com.sudsmobile.data.catalog.FirebaseServiceCatalogRepository
import com.sudsmobile.data.catalog.KtorCatalogFunctionsApi
import com.sudsmobile.data.catalog.ServiceCatalogRepository
import com.sudsmobile.data.network.createSudsHttpClient
import com.sudsmobile.data.profile.FirebaseUserProfileRepository
import com.sudsmobile.data.profile.KtorProfileFunctionsApi
import com.sudsmobile.data.profile.MutableUserProfileChangeNotifier
import com.sudsmobile.data.profile.ProfileFunctionsApi
import com.sudsmobile.data.profile.UserProfileChangeNotifier
import com.sudsmobile.data.profile.UserProfileRepository
import com.sudsmobile.data.vehicle.FirebaseUserVehicleRepository
import com.sudsmobile.data.vehicle.KtorVehicleFunctionsApi
import com.sudsmobile.data.vehicle.MutableUserVehicleChangeNotifier
import com.sudsmobile.data.vehicle.UserVehicleChangeNotifier
import com.sudsmobile.data.vehicle.UserVehicleRepository
import com.sudsmobile.data.vehicle.VehicleFunctionsApi
import io.ktor.client.HttpClient
import org.koin.dsl.module

val dataModule = module {
    single<HttpClient> { createSudsHttpClient() }
    single {
        FirebaseAuthConfig.default(
            isDebugBuild = getKoin().getProperty<Boolean>("isDebugBuild") ?: false,
            platformName = get(),
            useFirebaseEmulators = getKoin().getProperty<Boolean>("useFirebaseEmulators") ?: false,
        )
    }
    single {
        FirebaseFunctionsConfig.default(
            isDebugBuild = getKoin().getProperty<Boolean>("isDebugBuild") ?: false,
            platformName = get(),
            useFirebaseEmulators = getKoin().getProperty<Boolean>("useFirebaseEmulators") ?: false,
        )
    }
    single<AuthApi> { KtorIdentityToolkitAuthApi(get(), get()) }
    single<AuthRepository> { FirebaseAuthRepository(get(), get<AuthSessionStore>()) }
    single<BookingFunctionsApi> { KtorBookingFunctionsApi(get(), get()) }
    single { MutableBookingChangeNotifier() }
    single<BookingChangeNotifier> { get<MutableBookingChangeNotifier>() }
    single<BookingRepository> { FirebaseBookingRepository(get(), get(), get()) }
    single<CatalogFunctionsApi> { KtorCatalogFunctionsApi(get(), get()) }
    single<ServiceCatalogRepository> { FirebaseServiceCatalogRepository(get()) }
    single<BusinessInfoFunctionsApi> { KtorBusinessInfoFunctionsApi(get(), get()) }
    single<BusinessInfoRepository> { FirebaseBusinessInfoRepository(get()) }
    single<ProfileFunctionsApi> { KtorProfileFunctionsApi(get(), get()) }
    single { MutableUserProfileChangeNotifier() }
    single<UserProfileChangeNotifier> { get<MutableUserProfileChangeNotifier>() }
    single<UserProfileRepository> { FirebaseUserProfileRepository(get(), get(), get()) }
    single<VehicleFunctionsApi> { KtorVehicleFunctionsApi(get(), get()) }
    single { MutableUserVehicleChangeNotifier() }
    single<UserVehicleChangeNotifier> { get<MutableUserVehicleChangeNotifier>() }
    single<UserVehicleRepository> { FirebaseUserVehicleRepository(get(), get(), get()) }
    single<AppRepository> { AppRepositoryImpl() }
}
