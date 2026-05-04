package com.sudsmobile.feature.products

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AirportShuttle
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.Weekend
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.unit.dp

private data class BookingService(
    val id: String,
    val name: String,
    val description: String,
    val duration: String,
    val passengerPrice: String,
    val icon: ImageVector,
    val popular: Boolean = false,
)

private data class BookingVehicle(
    val id: String,
    val name: String,
    val description: String,
    val icon: ImageVector,
)

private data class BookingDateOption(
    val id: String,
    val dayLabel: String,
    val dateLabel: String,
    val available: Boolean = true,
)

private data class BookingTimeSlot(
    val time: String,
    val available: Boolean = true,
)

private enum class BookingStep {
    Service,
    Vehicle,
    DateTime,
}

private val bookingServices = listOf(
    BookingService(
        id = "standard",
        name = "Lavagem Standard",
        description = "Lavagem completa exterior e interior",
        duration = "30 min",
        passengerPrice = "25,00€",
        icon = Icons.Filled.DirectionsCar,
    ),
    BookingService(
        id = "premium",
        name = "Lavagem Premium",
        description = "Lavagem detalhada com acabamento premium",
        duration = "45 min",
        passengerPrice = "32,00€",
        icon = Icons.Filled.AutoAwesome,
        popular = true,
    ),
    BookingService(
        id = "exterior",
        name = "Lavagem Exterior",
        description = "Apenas lavagem exterior",
        duration = "20 min",
        passengerPrice = "16,00€",
        icon = Icons.Filled.WaterDrop,
    ),
    BookingService(
        id = "interior",
        name = "Limpeza do Interior",
        description = "Apenas limpeza interior",
        duration = "25 min",
        passengerPrice = "16,00€",
        icon = Icons.Filled.Weekend,
    ),
)

private val bookingVehicles = listOf(
    BookingVehicle(
        id = "passenger",
        name = "Passageiros",
        description = "Carros normais, sedans, compactos",
        icon = Icons.Filled.DirectionsCar,
    ),
    BookingVehicle(
        id = "suv",
        name = "SUV",
        description = "SUVs, vans, carrinhas",
        icon = Icons.Filled.AirportShuttle,
    ),
)

private val bookingDateOptions = listOf(
    BookingDateOption("today", "Hoje", "4 mai"),
    BookingDateOption("tomorrow", "Amanhã", "5 mai"),
    BookingDateOption("wed", "Quarta", "6 mai"),
    BookingDateOption("thu", "Quinta", "7 mai"),
    BookingDateOption("fri", "Sexta", "8 mai"),
    BookingDateOption("sat", "Sábado", "9 mai"),
    BookingDateOption("sun", "Domingo", "10 mai", available = false),
)

private val bookingTimeSlots = listOf(
    BookingTimeSlot("09:00"),
    BookingTimeSlot("09:30"),
    BookingTimeSlot("10:00", available = false),
    BookingTimeSlot("10:30"),
    BookingTimeSlot("11:00"),
    BookingTimeSlot("11:30"),
    BookingTimeSlot("12:00"),
    BookingTimeSlot("12:30", available = false),
    BookingTimeSlot("14:00"),
    BookingTimeSlot("14:30"),
    BookingTimeSlot("15:00"),
    BookingTimeSlot("15:30"),
    BookingTimeSlot("16:00"),
    BookingTimeSlot("16:30"),
    BookingTimeSlot("17:00"),
    BookingTimeSlot("17:30"),
    BookingTimeSlot("18:00"),
    BookingTimeSlot("18:30"),
)

@Composable
fun ProductsScreen(contentPadding: PaddingValues) {
    var currentStepName by rememberSaveable { mutableStateOf(BookingStep.Service.name) }
    var selectedServiceId by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedVehicleId by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedDateId by rememberSaveable { mutableStateOf(bookingDateOptions.first().id) }
    var selectedTime by rememberSaveable { mutableStateOf<String?>(null) }
    val currentStep = BookingStep.valueOf(currentStepName)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = contentPadding.calculateBottomPadding() + 104.dp),
        ) {
            when (currentStep) {
                BookingStep.Service -> {
                    BookingServiceHeader()

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                            .padding(top = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        bookingServices.forEach { service ->
                            BookingServiceCard(
                                service = service,
                                selected = selectedServiceId == service.id,
                                onSelected = { selectedServiceId = service.id },
                            )
                        }
                    }
                }

                BookingStep.Vehicle -> {
                    BookingVehicleHeader(
                        onBack = { currentStepName = BookingStep.Service.name },
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                            .padding(top = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Text(
                            text = "Selecione o tipo de veículo para calcular o preço correto",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )

                        bookingVehicles.forEach { vehicle ->
                            BookingVehicleCard(
                                vehicle = vehicle,
                                selected = selectedVehicleId == vehicle.id,
                                onSelected = { selectedVehicleId = vehicle.id },
                            )
                        }

                        VehicleHelpCard()
                    }
                }

                BookingStep.DateTime -> {
                    BookingDateTimeHeader(
                        onBack = { currentStepName = BookingStep.Vehicle.name },
                    )

                    DateTimeStepContent(
                        selectedDateId = selectedDateId,
                        selectedTime = selectedTime,
                        onDateSelected = { dateId ->
                            selectedDateId = dateId
                            selectedTime = null
                        },
                        onTimeSelected = { selectedTime = it },
                    )
                }
            }
        }

        ContinueBar(
            enabled = when (currentStep) {
                BookingStep.Service -> selectedServiceId != null
                BookingStep.Vehicle -> selectedVehicleId != null
                BookingStep.DateTime -> selectedDateId.isNotBlank() && selectedTime != null
            },
            onClick = {
                when (currentStep) {
                    BookingStep.Service -> currentStepName = BookingStep.Vehicle.name
                    BookingStep.Vehicle -> currentStepName = BookingStep.DateTime.name
                    BookingStep.DateTime -> Unit
                }
            },
            contentPadding = contentPadding,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

@Composable
private fun BookingServiceHeader() {
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
        OutlinedButton(
            onClick = {},
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.42f)),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.tertiaryContainer,
            ),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.size(8.dp))
            Text("Voltar", style = MaterialTheme.typography.labelLarge)
        }

        Spacer(Modifier.height(24.dp))

        Text(
            text = "Escolha o Serviço",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.inverseOnSurface,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Passo 1 de 4",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = 0.72f),
        )
    }
}

@Composable
private fun BookingVehicleHeader(onBack: () -> Unit) {
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
        OutlinedButton(
            onClick = onBack,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.42f)),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.tertiaryContainer,
            ),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.size(8.dp))
            Text("Voltar", style = MaterialTheme.typography.labelLarge)
        }

        Spacer(Modifier.height(24.dp))

        Text(
            text = "Tipo de Veículo",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.inverseOnSurface,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Passo 2 de 4",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = 0.72f),
        )
    }
}

@Composable
private fun BookingDateTimeHeader(onBack: () -> Unit) {
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
        OutlinedButton(
            onClick = onBack,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.42f)),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.tertiaryContainer,
            ),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.size(8.dp))
            Text("Voltar", style = MaterialTheme.typography.labelLarge)
        }

        Spacer(Modifier.height(24.dp))

        Text(
            text = "Data e Hora",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.inverseOnSurface,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Passo 3 de 4",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = 0.72f),
        )
    }
}

@Composable
private fun DateTimeStepContent(
    selectedDateId: String,
    selectedTime: String?,
    onDateSelected: (String) -> Unit,
    onTimeSelected: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(top = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        DateSelectionCard(
            selectedDateId = selectedDateId,
            onDateSelected = onDateSelected,
        )

        TimeSelectionCard(
            selectedTime = selectedTime,
            onTimeSelected = onTimeSelected,
        )

        OpeningHoursCard()
    }
}

@Composable
private fun DateSelectionCard(
    selectedDateId: String,
    onDateSelected: (String) -> Unit,
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
            SectionHeader(
                icon = Icons.Filled.CalendarMonth,
                title = "Selecione a Data",
            )

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                bookingDateOptions.forEach { dateOption ->
                    DateOptionRow(
                        dateOption = dateOption,
                        selected = selectedDateId == dateOption.id,
                        onSelected = { onDateSelected(dateOption.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun DateOptionRow(
    dateOption: BookingDateOption,
    selected: Boolean,
    onSelected: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(enabled = dateOption.available, onClick = onSelected),
        shape = RoundedCornerShape(14.dp),
        color = when {
            selected -> MaterialTheme.colorScheme.tertiaryContainer
            dateOption.available -> MaterialTheme.colorScheme.surfaceContainerLow
            else -> MaterialTheme.colorScheme.surfaceContainerHigh
        },
        contentColor = when {
            selected -> MaterialTheme.colorScheme.onTertiaryContainer
            dateOption.available -> MaterialTheme.colorScheme.onSurface
            else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.56f)
        },
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 13.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = dateOption.dayLabel,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = dateOption.dateLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (selected) {
                        MaterialTheme.colorScheme.onTertiaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }

            if (selected) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
            } else if (!dateOption.available) {
                Text(
                    text = "Encerrado",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun TimeSelectionCard(
    selectedTime: String?,
    onTimeSelected: (String) -> Unit,
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
            SectionHeader(
                icon = Icons.Filled.AccessTime,
                title = "Horários Disponíveis",
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                bookingTimeSlots.chunked(3).forEach { slotRow ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        slotRow.forEach { slot ->
                            TimeSlotButton(
                                slot = slot,
                                selected = selectedTime == slot.time,
                                onSelected = { onTimeSelected(slot.time) },
                                modifier = Modifier.weight(1f),
                            )
                        }

                        repeat(3 - slotRow.size) {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TimeSlotButton(
    slot: BookingTimeSlot,
    selected: Boolean,
    onSelected: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = slot.available, onClick = onSelected),
        shape = RoundedCornerShape(12.dp),
        color = when {
            selected -> MaterialTheme.colorScheme.tertiary
            slot.available -> MaterialTheme.colorScheme.surfaceContainerLow
            else -> MaterialTheme.colorScheme.surfaceContainerHigh
        },
        contentColor = when {
            selected -> MaterialTheme.colorScheme.onTertiary
            slot.available -> MaterialTheme.colorScheme.onSurface
            else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.52f)
        },
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = slot.time,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun OpeningHoursCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.20f),
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(18.dp),
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(20.dp),
            )
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Horário de Funcionamento",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "Segunda a Sexta: 09:00 - 19:00",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "Sábado: 09:00 - 13:00",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "Domingo: Encerrado",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(
    icon: ImageVector,
    title: String,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier.size(20.dp),
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun BookingServiceCard(
    service: BookingService,
    selected: Boolean,
    onSelected: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelected),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.18f)
            } else {
                MaterialTheme.colorScheme.surfaceContainerLowest
            },
        ),
        border = BorderStroke(
            width = 2.dp,
            color = if (selected) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.surfaceContainerLowest,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (selected) 8.dp else 4.dp),
        shape = RoundedCornerShape(18.dp),
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Surface(
                modifier = Modifier.size(64.dp),
                shape = RoundedCornerShape(18.dp),
                color = if (selected) {
                    MaterialTheme.colorScheme.tertiary
                } else {
                    MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.30f)
                },
                contentColor = if (selected) {
                    MaterialTheme.colorScheme.onTertiary
                } else {
                    MaterialTheme.colorScheme.tertiary
                },
            ) {
                Icon(
                    imageVector = service.icon,
                    contentDescription = null,
                    modifier = Modifier.padding(16.dp),
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top,
                ) {
                    Text(
                        text = service.name,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                    )
                    if (service.popular) {
                        PopularBadge()
                    }
                }

                Text(
                    text = service.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "A partir de",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = service.passengerPrice,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.tertiary,
                            fontWeight = FontWeight.Bold,
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AccessTime,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            text = service.duration,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            if (selected) {
                Surface(
                    modifier = Modifier.size(24.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.tertiary,
                    contentColor = MaterialTheme.colorScheme.onTertiary,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        modifier = Modifier.padding(5.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun PopularBadge() {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
    ) {
        Text(
            text = "Popular",
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun BookingVehicleCard(
    vehicle: BookingVehicle,
    selected: Boolean,
    onSelected: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelected),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.18f)
            } else {
                MaterialTheme.colorScheme.surfaceContainerLowest
            },
        ),
        border = BorderStroke(
            width = 2.dp,
            color = if (selected) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.surfaceContainerLowest,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (selected) 8.dp else 4.dp),
        shape = RoundedCornerShape(18.dp),
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.size(80.dp),
                shape = RoundedCornerShape(18.dp),
                color = if (selected) {
                    MaterialTheme.colorScheme.tertiary
                } else {
                    MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.30f)
                },
                contentColor = if (selected) {
                    MaterialTheme.colorScheme.onTertiary
                } else {
                    MaterialTheme.colorScheme.tertiary
                },
            ) {
                Icon(
                    imageVector = vehicle.icon,
                    contentDescription = null,
                    modifier = Modifier.padding(20.dp),
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = vehicle.name,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = vehicle.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (selected) {
                Surface(
                    modifier = Modifier.size(32.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.tertiary,
                    contentColor = MaterialTheme.colorScheme.onTertiary,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        modifier = Modifier.padding(7.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun VehicleHelpCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f),
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(18.dp),
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(20.dp),
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Não tem a certeza?",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "Carros SUV incluem veículos maiores como SUVs, vans e carrinhas. O preço é ligeiramente superior devido ao tamanho.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
        }
    }
}

@Composable
private fun ContinueBar(
    enabled: Boolean,
    onClick: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        tonalElevation = 0.dp,
        shadowElevation = 6.dp,
    ) {
        Column {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Button(
                onClick = onClick,
                enabled = enabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(top = 16.dp, bottom = contentPadding.calculateBottomPadding() + 16.dp)
                    .height(56.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.inverseSurface,
                    contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            ) {
                Text(
                    text = "Continuar",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}
