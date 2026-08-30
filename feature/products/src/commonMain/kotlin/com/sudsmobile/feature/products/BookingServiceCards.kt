package com.sudsmobile.feature.products

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import com.sudsmobile.shared.ui.SudsServiceArtwork
import com.sudsmobile.shared.ui.serviceArtworkStyleForKey

@Composable
internal fun BookingServiceCard(
    service: ProductServiceUi,
    selected: Boolean,
    onSelected: () -> Unit,
) {
    SudsGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clip(SudsShapes.card)
            .clickable(onClick = onSelected)
            .semantics {
                role = Role.RadioButton
                stateDescription = if (selected) "Selecionado" else "Não selecionado"
            }
            .border(
                width = if (selected) 2.dp else SudsSpacing.hairline,
                color = if (selected) SudsColors.cyan else SudsColors.glassBorder,
                shape = SudsShapes.card,
            ),
        contentPadding = PaddingValues(SudsSpacing.md),
        containerColor = if (selected) {
            SudsColors.cyan.copy(alpha = 0.14f)
        } else {
            SudsColors.glass
        },
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(SudsSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SudsServiceArtwork(
                style = serviceArtworkStyleForKey("${service.id} ${service.name}"),
                size = 104.dp,
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(SudsSpacing.xs),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(SudsSpacing.xs),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = service.name,
                        modifier = Modifier.weight(1f),
                        color = SudsColors.onBrand,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (selected) SelectionMark()
                }

                if (service.popular) PopularBadge()

                Text(
                    text = service.description,
                    color = SudsColors.onBrandMuted,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom,
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(SudsSpacing.xxs)) {
                        Text(
                            text = "A PARTIR DE",
                            color = SudsColors.cyanMuted,
                            style = MaterialTheme.typography.labelSmall,
                        )
                        Text(
                            text = service.passengerPrice,
                            color = SudsColors.champagne,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(SudsSpacing.xxs),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AccessTime,
                            contentDescription = null,
                            tint = SudsColors.onBrandMuted,
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            text = service.durationLabel,
                            color = SudsColors.onBrandMuted,
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                }
            }
        }
    }
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
        color = SudsColors.champagne,
        contentColor = SudsColors.onAction,
    ) {
        Text(
            text = "MAIS ESCOLHIDO",
            modifier = Modifier.padding(horizontal = SudsSpacing.sm, vertical = SudsSpacing.xxs),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
        )
    }
}
