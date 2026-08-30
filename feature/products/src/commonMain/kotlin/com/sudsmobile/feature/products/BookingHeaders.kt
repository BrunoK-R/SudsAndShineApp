package com.sudsmobile.feature.products

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
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
        title = "Escolha o serviço",
        eyebrow = "PASSO 1 DE 4",
        progressIndex = bookingProgressIndex(BookingStep.Service),
        onBack = onBack,
    )
}

@Composable
internal fun BookingVehicleHeader(onBack: () -> Unit) {
    BookingStepHeader(
        title = "Tipo de veículo",
        eyebrow = "PASSO 2 DE 4",
        progressIndex = bookingProgressIndex(BookingStep.Vehicle),
        onBack = onBack,
    )
}

@Composable
internal fun BookingDateTimeHeader(onBack: () -> Unit) {
    BookingStepHeader(
        title = "Data e hora",
        eyebrow = "PASSO 3 DE 4",
        progressIndex = bookingProgressIndex(BookingStep.DateTime),
        onBack = onBack,
    )
}

@Composable
internal fun BookingContactHeader(onBack: () -> Unit) {
    BookingStepHeader(
        title = "Dados de contacto",
        eyebrow = "PASSO 4 DE 4",
        progressIndex = bookingProgressIndex(BookingStep.Contact),
        onBack = onBack,
    )
}

@Composable
internal fun BookingConfirmationHeader(onBack: () -> Unit) {
    BookingStepHeader(
        title = "Rever pedido",
        eyebrow = "CONFIRMAÇÃO",
        progressIndex = null,
        onBack = onBack,
    )
}

@Composable
private fun BookingStepHeader(
    title: String,
    eyebrow: String,
    progressIndex: Int?,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .heightIn(min = if (progressIndex == null) 88.dp else 132.dp)
            .padding(
                horizontal = SudsSpacing.contentGutter,
                vertical = SudsSpacing.sm,
            ),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(SudsSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier
                    .size(SudsSpacing.minimumTouchTarget)
                    .clickable(role = Role.Button, onClick = onBack)
                    .semantics { contentDescription = "Voltar" },
                shape = CircleShape,
                color = SudsColors.glassStrong,
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
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(SudsSpacing.xxs),
            ) {
                Text(
                    text = eyebrow,
                    color = SudsColors.cyanMuted,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = title,
                    color = SudsColors.onBrand,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (progressIndex != null) {
                Text(
                    text = "${progressIndex + 1}/4",
                    color = SudsColors.champagne,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }

        if (progressIndex != null) {
            Spacer(Modifier.height(SudsSpacing.md))
            SudsProgressSegments(
                currentStepIndex = progressIndex,
                totalSteps = 4,
            )
        }
    }
}
