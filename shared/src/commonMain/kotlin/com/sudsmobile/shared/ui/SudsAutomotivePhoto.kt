package com.sudsmobile.shared.ui

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import sudsandshine.shared.generated.resources.Res
import sudsandshine.shared.generated.resources.suds_appointment_hero
import sudsandshine.shared.generated.resources.suds_booking_navigation
import sudsandshine.shared.generated.resources.suds_brand_mark
import sudsandshine.shared.generated.resources.suds_service_exterior
import sudsandshine.shared.generated.resources.suds_service_premium
import sudsandshine.shared.generated.resources.suds_service_standard
import sudsandshine.shared.generated.resources.suds_vehicle_passenger
import sudsandshine.shared.generated.resources.suds_vehicle_suv

enum class SudsAutomotivePhotoKind {
    AppointmentHero,
    Standard,
    Premium,
    Exterior,
}

enum class SudsVehiclePhotoKind {
    Passenger,
    Suv,
}

fun automotivePhotoKindForKey(key: String): SudsAutomotivePhotoKind {
    val normalized = key.lowercase()
    return when {
        normalized.contains("premium") || normalized.contains("detail") -> SudsAutomotivePhotoKind.Premium
        normalized.contains("exterior") || normalized.contains("outside") -> SudsAutomotivePhotoKind.Exterior
        else -> SudsAutomotivePhotoKind.Standard
    }
}

@Composable
fun SudsAutomotivePhoto(
    kind: SudsAutomotivePhotoKind,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    contentScale: ContentScale = ContentScale.Crop,
    alignment: Alignment = Alignment.Center,
) {
    Image(
        painter = painterResource(kind.resource()),
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = contentScale,
        alignment = alignment,
    )
}

@Composable
fun SudsBrandMark(
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
) {
    Image(
        painter = painterResource(Res.drawable.suds_brand_mark),
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = ContentScale.Fit,
    )
}

@Composable
fun SudsVehiclePhoto(
    kind: SudsVehiclePhotoKind,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    contentScale: ContentScale = ContentScale.Crop,
    alignment: Alignment = Alignment.Center,
) {
    Image(
        painter = painterResource(kind.resource()),
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = contentScale,
        alignment = alignment,
    )
}

@Composable
fun SudsBookingNavigationMark(
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
) {
    Image(
        painter = painterResource(Res.drawable.suds_booking_navigation),
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = ContentScale.Crop,
    )
}

private fun SudsAutomotivePhotoKind.resource(): DrawableResource = when (this) {
    SudsAutomotivePhotoKind.AppointmentHero -> Res.drawable.suds_appointment_hero
    SudsAutomotivePhotoKind.Standard -> Res.drawable.suds_service_standard
    SudsAutomotivePhotoKind.Premium -> Res.drawable.suds_service_premium
    SudsAutomotivePhotoKind.Exterior -> Res.drawable.suds_service_exterior
}

private fun SudsVehiclePhotoKind.resource(): DrawableResource = when (this) {
    SudsVehiclePhotoKind.Passenger -> Res.drawable.suds_vehicle_passenger
    SudsVehiclePhotoKind.Suv -> Res.drawable.suds_vehicle_suv
}
