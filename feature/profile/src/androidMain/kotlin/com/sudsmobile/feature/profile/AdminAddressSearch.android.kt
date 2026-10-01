package com.sudsmobile.feature.profile

import android.location.Geocoder
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
internal actual fun rememberAdminAddressSearch(): suspend (String) -> List<AdminAddressOption> {
    val context = LocalContext.current.applicationContext
    return remember(context) {
        { query ->
            withContext(Dispatchers.IO) {
                if (!Geocoder.isPresent()) error("A pesquisa de moradas não está disponível neste dispositivo.")
                @Suppress("DEPRECATION")
                val matches = Geocoder(context, Locale.forLanguageTag("pt-PT")).getFromLocationName(query, 6).orEmpty()
                matches.map { address ->
                    val line1 = listOfNotNull(address.thoroughfare, address.subThoroughfare, address.postalCode)
                        .filter { it.isNotBlank() }.joinToString(", ").ifBlank { address.getAddressLine(0).orEmpty() }
                    val line2 = listOfNotNull(address.locality ?: address.subAdminArea, address.countryName)
                        .filter { it.isNotBlank() }.joinToString(", ")
                    AdminAddressOption(line1, line2,
                        "https://www.google.com/maps/search/?api=1&query=${address.latitude},${address.longitude}")
                }.filter { it.addressLine1.isNotBlank() }
            }
        }
    }
}
