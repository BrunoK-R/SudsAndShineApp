package com.sudsmobile.feature.cart

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
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Recommend
import androidx.compose.material.icons.filled.SentimentSatisfied
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

private enum class BookingsTab(val label: String) {
    Upcoming("Próximas"),
    Completed("Concluídas"),
}

private enum class BookingStatus(val label: String) {
    Confirmed("Confirmado"),
    Completed("Concluído"),
    Cancelled("Cancelado"),
}

private data class BookingSummary(
    val id: Int,
    val service: String,
    val date: String,
    val time: String,
    val vehicle: String,
    val price: String,
    val status: BookingStatus,
    val icon: ImageVector,
    val showLocation: Boolean,
)

private data class RatingTag(
    val id: String,
    val label: String,
    val icon: ImageVector,
)

private val ratingTags = listOf(
    RatingTag("fast", "Rápido", Icons.Filled.FlashOn),
    RatingTag("quality", "Qualidade", Icons.Filled.Shield),
    RatingTag("friendly", "Simpático", Icons.Filled.SentimentSatisfied),
    RatingTag("recommend", "Recomendo", Icons.Filled.Recommend),
)

private val upcomingBookings = listOf(
    BookingSummary(
        id = 1,
        service = "Lavagem Premium",
        date = "25 de Março, 2026",
        time = "14:30",
        vehicle = "BMW 320d",
        price = "32,00€",
        status = BookingStatus.Confirmed,
        icon = Icons.Filled.AutoAwesome,
        showLocation = true,
    ),
    BookingSummary(
        id = 2,
        service = "Lavagem Standard",
        date = "28 de Março, 2026",
        time = "10:00",
        vehicle = "VW Golf",
        price = "25,00€",
        status = BookingStatus.Confirmed,
        icon = Icons.Filled.DirectionsCar,
        showLocation = true,
    ),
)

private val completedBookings = listOf(
    BookingSummary(
        id = 3,
        service = "Lavagem Premium",
        date = "15 de Março, 2026",
        time = "15:00",
        vehicle = "BMW 320d",
        price = "32,00€",
        status = BookingStatus.Completed,
        icon = Icons.Filled.AutoAwesome,
        showLocation = false,
    ),
    BookingSummary(
        id = 4,
        service = "Lavagem Exterior",
        date = "10 de Março, 2026",
        time = "11:30",
        vehicle = "BMW 320d",
        price = "16,00€",
        status = BookingStatus.Completed,
        icon = Icons.Filled.DirectionsCar,
        showLocation = false,
    ),
)

@Composable
fun CartScreen(
    contentPadding: PaddingValues,
    onRateService: () -> Unit = {},
) {
    var selectedTabName by rememberSaveable { mutableStateOf(BookingsTab.Upcoming.name) }
    val selectedTab = BookingsTab.valueOf(selectedTabName)
    val bookings = when (selectedTab) {
        BookingsTab.Upcoming -> upcomingBookings
        BookingsTab.Completed -> completedBookings
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(bottom = contentPadding.calculateBottomPadding() + 24.dp),
    ) {
        BookingsHeader()

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(top = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            BookingsSegmentedTabs(
                selectedTab = selectedTab,
                onTabSelected = { selectedTabName = it.name },
            )

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                bookings.forEach { booking ->
                    BookingSummaryCard(
                        booking = booking,
                        showRatingAction = selectedTab == BookingsTab.Completed,
                        onRateService = onRateService,
                    )
                }
            }
        }
    }
}

@Composable
private fun BookingsHeader() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.inverseSurface,
                        MaterialTheme.colorScheme.secondary,
                    ),
                ),
            )
            .safeDrawingPadding()
            .padding(horizontal = 24.dp)
            .padding(top = 28.dp, bottom = 32.dp),
    ) {
        Text(
            text = "Marcações",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.inverseOnSurface,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "Gerir as suas marcações",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = 0.72f),
        )
    }
}

@Composable
private fun BookingsSegmentedTabs(
    selectedTab: BookingsTab,
    onTabSelected: (BookingsTab) -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        shape = RoundedCornerShape(14.dp),
        shadowElevation = 4.dp,
    ) {
        Row(
            modifier = Modifier.padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            BookingsTab.entries.forEach { tab ->
                val selected = selectedTab == tab
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (selected) {
                                MaterialTheme.colorScheme.tertiary
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerLowest
                            },
                        )
                        .clickable { onTabSelected(tab) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = tab.label,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (selected) {
                            MaterialTheme.colorScheme.onTertiary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    )
                }
            }
        }
    }
}

@Composable
private fun BookingSummaryCard(
    booking: BookingSummary,
    showRatingAction: Boolean,
    onRateService: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 5.dp),
        shape = RoundedCornerShape(18.dp),
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                BookingStatusBadge(status = booking.status)
                IconButton(
                    onClick = {},
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.MoreVert,
                        contentDescription = "Mais opções",
                        tint = MaterialTheme.colorScheme.outline,
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Surface(
                    modifier = Modifier.size(56.dp),
                    color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.34f),
                    contentColor = MaterialTheme.colorScheme.tertiary,
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = booking.icon,
                            contentDescription = null,
                            modifier = Modifier.size(28.dp),
                        )
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Text(
                        text = booking.service,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = booking.vehicle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Text(
                    text = booking.price,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.tertiary,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.End,
                )
            }

            Spacer(Modifier.height(16.dp))

            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                BookingDetailRow(Icons.Filled.CalendarMonth, booking.date)
                BookingDetailRow(Icons.Filled.AccessTime, booking.time)
                if (booking.showLocation) {
                    BookingDetailRow(Icons.Filled.Place, "Shopping Norte Sul, Piso -1")
                }
            }

            if (showRatingAction) {
                Spacer(Modifier.height(16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Text(
                    text = "Avaliar Serviço",
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onRateService)
                        .padding(top = 14.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.tertiary,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
fun RatingScreen(
    contentPadding: PaddingValues,
    onBack: () -> Unit,
    onHome: () -> Unit,
) {
    var rating by rememberSaveable { mutableStateOf(0) }
    var selectedTagIds by rememberSaveable { mutableStateOf(emptyList<String>()) }
    var comment by rememberSaveable { mutableStateOf("") }
    var submitted by rememberSaveable { mutableStateOf(false) }

    if (submitted) {
        RatingSubmittedScreen(
            contentPadding = contentPadding,
            onHome = onHome,
        )
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = contentPadding.calculateBottomPadding() + if (rating > 0) 112.dp else 24.dp),
        ) {
            RatingHeader(onBack = onBack)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(top = 16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                CompletedServiceCard()
                RatingStarsCard(
                    rating = rating,
                    onRatingSelected = { rating = it },
                )

                if (rating > 0) {
                    RatingTagsCard(
                        selectedTagIds = selectedTagIds,
                        onTagToggled = { tagId ->
                            selectedTagIds = if (tagId in selectedTagIds) {
                                selectedTagIds - tagId
                            } else {
                                selectedTagIds + tagId
                            }
                        },
                    )
                    RatingCommentCard(
                        comment = comment,
                        onCommentChange = { comment = it },
                    )
                }
            }
        }

        if (rating > 0) {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceContainerLowest,
                shadowElevation = 6.dp,
            ) {
                Column {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Button(
                        onClick = { submitted = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                            .padding(top = 16.dp, bottom = contentPadding.calculateBottomPadding() + 16.dp)
                            .height(56.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.tertiary,
                            contentColor = MaterialTheme.colorScheme.onTertiary,
                        ),
                    ) {
                        Text(
                            text = "Enviar Avaliação",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RatingHeader(onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.inverseSurface,
                        MaterialTheme.colorScheme.secondary,
                    ),
                ),
            )
            .safeDrawingPadding()
            .padding(horizontal = 24.dp)
            .padding(top = 8.dp, bottom = 28.dp),
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .clickable(onClick = onBack)
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiaryContainer,
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = "Voltar",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.tertiaryContainer,
            )
        }

        Spacer(Modifier.height(24.dp))

        Text(
            text = "Avaliar Serviço",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.inverseOnSurface,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Como foi a sua experiência?",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = 0.72f),
        )
    }
}

@Composable
private fun CompletedServiceCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        shape = RoundedCornerShape(18.dp),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = "Serviço Realizado",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "Lavagem Premium",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "15 de Março, 2026",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun RatingStarsCard(
    rating: Int,
    onRatingSelected: (Int) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        shape = RoundedCornerShape(18.dp),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Classifique o Serviço",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(22.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                (1..5).forEach { star ->
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = "$star estrelas",
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .clickable { onRatingSelected(star) }
                            .padding(2.dp),
                        tint = if (star <= rating) {
                            MaterialTheme.colorScheme.tertiary
                        } else {
                            MaterialTheme.colorScheme.outlineVariant
                        },
                    )
                }
            }
            if (rating > 0) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = ratingLabel(rating),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun RatingTagsCard(
    selectedTagIds: List<String>,
    onTagToggled: (String) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        shape = RoundedCornerShape(18.dp),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "O que destacaria?",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
            )
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ratingTags.chunked(2).forEach { rowTags ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        rowTags.forEach { tag ->
                            RatingTagButton(
                                tag = tag,
                                selected = tag.id in selectedTagIds,
                                onClick = { onTagToggled(tag.id) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RatingTagButton(
    tag: RatingTag,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .height(92.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = if (selected) {
            MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.30f)
        } else {
            MaterialTheme.colorScheme.surfaceContainerLow
        },
        border = androidx.compose.foundation.BorderStroke(
            width = 2.dp,
            color = if (selected) {
                MaterialTheme.colorScheme.tertiary
            } else {
                MaterialTheme.colorScheme.outlineVariant
            },
        ),
        contentColor = if (selected) {
            MaterialTheme.colorScheme.tertiary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = tag.icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = tag.label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun RatingCommentCard(
    comment: String,
    onCommentChange: (String) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        shape = RoundedCornerShape(18.dp),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "Comentário (Opcional)",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
            )
            OutlinedTextField(
                value = comment,
                onValueChange = onCommentChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(128.dp),
                placeholder = {
                    Text(
                        text = "Partilhe mais detalhes sobre a sua experiência...",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                },
                minLines = 4,
                shape = RoundedCornerShape(14.dp),
                textStyle = MaterialTheme.typography.bodyMedium,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.tertiary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            )
        }
    }
}

@Composable
private fun RatingSubmittedScreen(
    contentPadding: PaddingValues,
    onHome: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding()
            .padding(horizontal = 24.dp)
            .padding(top = 48.dp, bottom = contentPadding.calculateBottomPadding() + 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Surface(
            modifier = Modifier.size(112.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.tertiary,
            contentColor = MaterialTheme.colorScheme.onTertiary,
            shadowElevation = 10.dp,
        ) {
            Icon(
                imageVector = Icons.Filled.Star,
                contentDescription = null,
                modifier = Modifier.padding(24.dp),
            )
        }
        Spacer(Modifier.height(32.dp))
        Text(
            text = "Obrigado!",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = "A sua avaliação ajuda-nos a melhorar o serviço",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(32.dp))
        Button(
            onClick = onHome,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.tertiary,
                contentColor = MaterialTheme.colorScheme.onTertiary,
            ),
        ) {
            Text(
                text = "Voltar ao Início",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

private fun ratingLabel(rating: Int): String = when (rating) {
    5 -> "Excelente!"
    4 -> "Muito Bom!"
    3 -> "Bom"
    2 -> "Pode Melhorar"
    else -> "Insatisfeito"
}

@Composable
private fun BookingStatusBadge(status: BookingStatus) {
    val containerColor = when (status) {
        BookingStatus.Confirmed -> MaterialTheme.colorScheme.tertiaryContainer
        BookingStatus.Completed -> MaterialTheme.colorScheme.primaryContainer
        BookingStatus.Cancelled -> MaterialTheme.colorScheme.errorContainer
    }
    val contentColor = when (status) {
        BookingStatus.Confirmed -> MaterialTheme.colorScheme.onTertiaryContainer
        BookingStatus.Completed -> MaterialTheme.colorScheme.onPrimaryContainer
        BookingStatus.Cancelled -> MaterialTheme.colorScheme.onErrorContainer
    }
    val icon = when (status) {
        BookingStatus.Confirmed,
        BookingStatus.Completed -> Icons.Filled.CheckCircle
        BookingStatus.Cancelled -> Icons.Filled.RadioButtonUnchecked
    }

    Surface(
        color = containerColor,
        contentColor = contentColor,
        shape = CircleShape,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
            )
            Text(
                text = status.label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun BookingDetailRow(
    icon: ImageVector,
    text: String,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier.size(18.dp),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
