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
import sudsandshine.shared.generated.resources.suds_brand_mark
import sudsandshine.shared.generated.resources.suds_service_exterior
import sudsandshine.shared.generated.resources.suds_service_premium
import sudsandshine.shared.generated.resources.suds_service_standard

enum class SudsAutomotivePhotoKind {
    AppointmentHero,
    Standard,
    Premium,
    Exterior,
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

private fun SudsAutomotivePhotoKind.resource(): DrawableResource = when (this) {
    SudsAutomotivePhotoKind.AppointmentHero -> Res.drawable.suds_appointment_hero
    SudsAutomotivePhotoKind.Standard -> Res.drawable.suds_service_standard
    SudsAutomotivePhotoKind.Premium -> Res.drawable.suds_service_premium
    SudsAutomotivePhotoKind.Exterior -> Res.drawable.suds_service_exterior
}
