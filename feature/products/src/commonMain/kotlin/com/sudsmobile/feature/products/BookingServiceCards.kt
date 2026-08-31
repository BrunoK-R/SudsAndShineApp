package com.sudsmobile.feature.products

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sudsmobile.shared.theme.SudsColors
import com.sudsmobile.shared.theme.SudsShapes
import com.sudsmobile.shared.theme.SudsSpacing
import com.sudsmobile.shared.ui.SudsGlassCard
import com.sudsmobile.shared.ui.SudsAutomotivePhoto
import com.sudsmobile.shared.ui.automotivePhotoKindForKey

@Composable
internal fun BookingServiceCard(
    service: ProductServiceUi,
    selected: Boolean,
    onSelected: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (selected) 195.dp else 181.dp)
            .semantics {
                role = Role.RadioButton
                stateDescription = if (selected) "Selecionado" else "Não selecionado"
            }
            .clickable(onClick = onSelected),
        shape = SudsShapes.card,
        color = SudsColors.glass,
        border = BorderStroke(
            width = if (selected) 2.dp else SudsSpacing.hairline,
            color = if (selected) SudsColors.cyan else SudsColors.glassBorder,
        ),
        shadowElevation = if (selected) 12.dp else 0.dp,
    ) {
        Box(Modifier.fillMaxSize()) {
            SudsAutomotivePhoto(
                kind = automotivePhotoKindForKey("${service.id} ${service.name}"),
                modifier = Modifier.fillMaxSize(),
                contentDescription = null,
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            0f to SudsColors.ink.copy(alpha = 0.98f),
                            0.54f to SudsColors.ink.copy(alpha = 0.74f),
                            1f to Color.Transparent,
                        ),
                    ),
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(SudsSpacing.md),
                verticalArrangement = Arrangement.spacedBy(SudsSpacing.xxs),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Surface(
                        modifier = Modifier.size(42.dp),
                        shape = CircleShape,
                        color = SudsColors.navyElevated.copy(alpha = 0.92f),
                        contentColor = SudsColors.onBrand,
                        border = BorderStroke(SudsSpacing.hairline, SudsColors.cyan.copy(alpha = 0.7f)),
                    ) {
                        Icon(
                            imageVector = service.icon,
                            contentDescription = null,
                            modifier = Modifier.padding(10.dp),
                        )
                    }
                    if (selected) SelectionMark()
                }

                Spacer(Modifier.weight(1f))
                Text(
                    text = service.name,
                    color = SudsColors.onBrand,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (service.description.isNotBlank()) {
                    Text(
                        text = service.bookingReferenceDescription(),
                        color = SudsColors.onBrandMuted,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (service.popular) PopularBadge()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom,
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(SudsSpacing.xs),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AccessTime,
                            contentDescription = null,
                            tint = SudsColors.onBrandMuted,
                            modifier = Modifier.size(17.dp),
                        )
                        Text(
                            text = service.durationLabel,
                            color = SudsColors.onBrandMuted,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    Text(
                        text = service.passengerPrice.bookingCompactPrice(),
                        color = SudsColors.cyan,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

internal fun String.bookingCompactPrice(): String = replace(",00€", "€")

internal fun ProductServiceUi.bookingReferenceDescription(): String = when {
    name.contains("premium", ignoreCase = true) -> "Acabamento premium"
    name.contains("standard", ignoreCase = true) || name.contains("completa", ignoreCase = true) -> {
        "Exterior e interior"
    }
    name.contains("exterior", ignoreCase = true) -> "Exterior cuidado"
    else -> description
}

@Composable
internal fun BookingExtrasSelectionSection(
    extras: List<ProductExtraUi>,
    selectedExtraIds: List<String>,
    onExtraToggled: (ProductExtraUi) -> Unit,
) {
    if (extras.isEmpty()) return

    Spacer(Modifier.height(SudsSpacing.sm))
    Text(
        text = "Personalize o cuidado",
        color = SudsColors.onBrand,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
    )
    Text(
        text = "Extras opcionais, adicionados ao preço final",
        color = SudsColors.onBrandMuted,
        style = MaterialTheme.typography.bodySmall,
    )

    Column(verticalArrangement = Arrangement.spacedBy(SudsSpacing.sm)) {
        extras.forEach { extra ->
            key(extra.id) {
                BookingExtraCard(
                    extra = extra,
                    selected = extra.id in selectedExtraIds,
                    onSelected = { onExtraToggled(extra) },
                )
            }
        }
    }
}

@Composable
private fun BookingExtraCard(
    extra: ProductExtraUi,
    selected: Boolean,
    onSelected: () -> Unit,
) {
    SudsGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clip(SudsShapes.control)
            .clickable(onClick = onSelected)
            .semantics {
                role = Role.Checkbox
                stateDescription = if (selected) "Selecionado" else "Não selecionado"
            }
            .border(
                width = if (selected) 2.dp else SudsSpacing.hairline,
                color = if (selected) SudsColors.cyan else SudsColors.glassBorder,
                shape = SudsShapes.control,
            ),
        shape = SudsShapes.control,
        contentPadding = PaddingValues(SudsSpacing.md),
        containerColor = if (selected) {
            SudsColors.cyan.copy(alpha = 0.12f)
        } else {
            SudsColors.glass
        },
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(SudsSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.size(44.dp),
                shape = CircleShape,
                color = if (selected) SudsColors.cyan else SudsColors.glassStrong,
                contentColor = if (selected) SudsColors.onAction else SudsColors.cyanMuted,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = extra.icon,
                        contentDescription = null,
                        modifier = Modifier.size(SudsSpacing.lg),
                    )
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(SudsSpacing.xxs),
            ) {
                Text(
                    text = extra.name,
                    color = SudsColors.onBrand,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                if (extra.description.isNotBlank()) {
                    Text(
                        text = extra.description,
                        color = SudsColors.onBrandMuted,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    text = "+ ${extra.price}",
                    color = SudsColors.champagne,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                )
            }
            Surface(
                modifier = Modifier.size(32.dp),
                shape = CircleShape,
                color = if (selected) SudsColors.cyan else SudsColors.glassStrong,
                contentColor = if (selected) SudsColors.onAction else SudsColors.onBrandMuted,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (selected) Icons.Filled.Check else Icons.Filled.Add,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun SelectionMark() {
    Surface(
        modifier = Modifier.size(28.dp),
        shape = CircleShape,
        color = SudsColors.cyan,
        contentColor = SudsColors.onAction,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

@Composable
private fun PopularBadge() {
    Surface(
        shape = SudsShapes.capsule,
        color = SudsColors.champagne.copy(alpha = 0.16f),
        contentColor = SudsColors.champagne,
        border = BorderStroke(SudsSpacing.hairline, SudsColors.champagne.copy(alpha = 0.34f)),
    ) {
        Text(
            text = "★  Mais escolhido",
            modifier = Modifier.padding(horizontal = SudsSpacing.sm, vertical = SudsSpacing.xxs),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
        )
    }
}
