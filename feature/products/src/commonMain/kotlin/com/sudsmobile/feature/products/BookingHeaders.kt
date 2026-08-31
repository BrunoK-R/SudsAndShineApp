package com.sudsmobile.feature.products

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sudsmobile.shared.theme.SudsColors
import com.sudsmobile.shared.theme.SudsSpacing
import com.sudsmobile.shared.ui.SudsProgressSegments

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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .height(56.dp)
            .padding(horizontal = SudsSpacing.contentGutter),
        horizontalArrangement = Arrangement.spacedBy(SudsSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            modifier = Modifier
                .size(44.dp)
                .clickable(role = Role.Button, onClick = onBack)
                .semantics { contentDescription = "Voltar" },
            shape = CircleShape,
            color = SudsColors.glass,
            border = BorderStroke(SudsSpacing.hairline, SudsColors.glassBorder),
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null,
                    modifier = Modifier.size(SudsSpacing.xl),
                    tint = SudsColors.onBrand,
                )
            }
        }
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            color = SudsColors.onBrand,
            style = MaterialTheme.typography.titleLarge,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        if (progressIndex != null) {
            Column(
                modifier = Modifier.width(120.dp),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(SudsSpacing.sm),
            ) {
                Text(
                    text = "${progressIndex + 1} de 4",
                    color = SudsColors.onBrandMuted,
                    style = MaterialTheme.typography.labelLarge,
                )
                SudsProgressSegments(
                    currentStepIndex = progressIndex,
                    totalSteps = 4,
                )
            }
        }
    }
}
