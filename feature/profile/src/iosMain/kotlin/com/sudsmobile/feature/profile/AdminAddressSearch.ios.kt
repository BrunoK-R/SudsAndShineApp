@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.sudsmobile.feature.profile

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.cinterop.useContents
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.CoreLocation.CLGeocoder
import platform.CoreLocation.CLPlacemark
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@Composable
internal actual fun rememberAdminAddressSearch(): suspend (String) -> List<AdminAddressOption> = remember {
    { query ->
        suspendCancellableCoroutine { continuation ->
            val geocoder = CLGeocoder()
            continuation.invokeOnCancellation { geocoder.cancelGeocode() }
            geocoder.geocodeAddressString(query) { placemarks, error ->
                if (continuation.isActive) {
                    if (error != null) continuation.resumeWithException(IllegalStateException("Não foi possível pesquisar esta morada."))
                    else continuation.resume(placemarks.orEmpty().mapNotNull { raw ->
                        val placemark = raw as? CLPlacemark ?: return@mapNotNull null
                        val location = placemark.location ?: return@mapNotNull null
                        val coordinates = location.coordinate.useContents { "$latitude,$longitude" }
                        AdminAddressOption(
                            listOfNotNull(placemark.thoroughfare, placemark.subThoroughfare, placemark.postalCode)
                                .joinToString(", ").ifBlank { placemark.name.orEmpty() },
                            listOfNotNull(placemark.locality, placemark.country).joinToString(", "),
                            "https://www.google.com/maps/search/?api=1&query=$coordinates",
                        )
                    })
                }
            }
        }
    }
}
