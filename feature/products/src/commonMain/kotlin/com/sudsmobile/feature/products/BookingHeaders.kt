package com.sudsmobile.feature.products

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sudsmobile.shared.theme.SudsSpacing
import com.sudsmobile.shared.ui.SudsProgressSegments
import com.sudsmobile.shared.ui.SudsSecondaryTopBar

@Composable
internal fun BookingServiceHeader(onBack: () -> Unit) {
    BookingStepHeader(
        title = "Escolher serviço",
        progressIndex = bookingProgressIndex(BookingStep.Service),
        onBack = onBack,
    )
}

@Composable
internal fun BookingVehicleHeader(onBack: () -> Unit) {
    BookingStepHeader(
        title = "Tipo de veículo",
        progressIndex = bookingProgressIndex(BookingStep.Vehicle),
        onBack = onBack,
    )
}

@Composable
internal fun BookingExtrasHeader(onBack: () -> Unit) {
    BookingStepHeader(
        title = "Personalizar",
        progressIndex = bookingProgressIndex(BookingStep.Extras),
        onBack = onBack,
    )
}

@Composable
internal fun BookingDateTimeHeader(onBack: () -> Unit) {
    BookingStepHeader(
        title = "Data e hora",
        progressIndex = bookingProgressIndex(BookingStep.DateTime),
        onBack = onBack,
    )
}

@Composable
internal fun BookingContactHeader(onBack: () -> Unit) {
    BookingStepHeader(
        title = "Dados de contacto",
        progressIndex = bookingProgressIndex(BookingStep.Contact),
        onBack = onBack,
    )
}

@Composable
internal fun BookingConfirmationHeader(onBack: () -> Unit) {
    BookingStepHeader(
        title = "Rever pedido",
        progressIndex = null,
        onBack = onBack,
    )
}

@Composable
private fun BookingStepHeader(
    title: String,
    progressIndex: Int?,
    onBack: () -> Unit,
) {
    Column {
        SudsSecondaryTopBar(title = title, onBack = onBack)
        if (progressIndex != null) {
            Row(
                modifier = Modifier.fillMaxWidth()
                    .padding(horizontal = SudsSpacing.contentGutter, vertical = SudsSpacing.sm),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Passo ${progressIndex + 1} de $BookingProgressStepCount",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelLarge,
                )
                Column(modifier = Modifier.width(120.dp)) {
                    SudsProgressSegments(
                        currentStepIndex = progressIndex,
                        totalSteps = BookingProgressStepCount,
                    )
                }
            }
        }
    }
}
