package com.sudsmobile.feature.products

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AirportShuttle
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Euro
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sudsmobile.data.booking.BookingAvailabilityDay
import com.sudsmobile.data.booking.BookingAvailabilityMonth
import com.sudsmobile.data.booking.BookingAvailabilitySlot
import org.koin.compose.viewmodel.koinViewModel

private data class BookingVehicle(
    val id: String,
    val name: String,
    val description: String,
    val icon: ImageVector,
)

private enum class BookingStep {
    Service,
    Vehicle,
    DateTime,
    Contact,
    Confirmation,
    Success,
}

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

@Composable
fun ProductsScreen(
    contentPadding: PaddingValues,
    onBack: () -> Unit = {},
    onViewBooking: () -> Unit = {},
    onHome: () -> Unit = {},
) {
    val viewModel: ProductsBookingViewModel = koinViewModel()
    val catalogViewModel: ProductsCatalogViewModel = koinViewModel()
    val availabilityState by viewModel.availabilityState.collectAsStateWithLifecycle()
    val submitState by viewModel.submitState.collectAsStateWithLifecycle()
    val catalogState by catalogViewModel.catalogState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        catalogViewModel.loadCatalog()
    }

    ProductsScreenContent(
        contentPadding = contentPadding,
        catalogState = catalogState,
        availabilityState = availabilityState,
        submitState = submitState,
        onLoadCatalog = catalogViewModel::loadCatalog,
        onLoadAvailability = viewModel::loadAvailability,
        onSubmitBooking = viewModel::submitBooking,
        onClearSubmitError = viewModel::clearSubmitError,
        onSubmitSuccessConsumed = viewModel::consumeSuccess,
        onBack = onBack,
        onViewBooking = onViewBooking,
        onHome = onHome,
    )
}

@Composable
private fun ProductsScreenContent(
    contentPadding: PaddingValues,
    catalogState: ProductCatalogUiState,
    availabilityState: BookingAvailabilityUiState,
    submitState: BookingSubmitUiState,
    onLoadCatalog: () -> Unit,
    onLoadAvailability: (Int, String?) -> Unit,
    onSubmitBooking: (ProductsBookingDraft?) -> Unit,
    onClearSubmitError: () -> Unit,
    onSubmitSuccessConsumed: () -> Unit,
    onBack: () -> Unit = {},
    onViewBooking: () -> Unit = {},
    onHome: () -> Unit = {},
) {
    var currentStepName by rememberSaveable { mutableStateOf(BookingStep.Service.name) }
    var selectedServiceId by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedVehicleId by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedDateId by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedTime by rememberSaveable { mutableStateOf<String?>(null) }
    var availabilityAnchorDate by rememberSaveable { mutableStateOf<String?>(null) }
    var minimumAvailabilityMonthAnchor by rememberSaveable { mutableStateOf<String?>(null) }
    var contactName by rememberSaveable { mutableStateOf("") }
    var contactPhone by rememberSaveable { mutableStateOf("") }
    var contactEmail by rememberSaveable { mutableStateOf("") }
    var contactNotes by rememberSaveable { mutableStateOf("") }
    var acceptsPrivacy by rememberSaveable { mutableStateOf(false) }
    var reservationCode by rememberSaveable { mutableStateOf<String?>(null) }
    val currentStep = BookingStep.valueOf(currentStepName)
    val contactFormValid = contactName.isNotBlank() &&
        contactPhone.isNotBlank() &&
        contactEmail.isNotBlank() &&
        acceptsPrivacy
    val loadedServices = (catalogState as? ProductCatalogUiState.Loaded)?.services.orEmpty()
    val selectedService = loadedServices.firstOrNull { it.id == selectedServiceId }
    val selectedVehicle = bookingVehicles.firstOrNull { it.id == selectedVehicleId }
    val availabilityMonth = when (availabilityState) {
        is BookingAvailabilityUiState.Empty -> availabilityState.month
        is BookingAvailabilityUiState.Loaded -> availabilityState.month
        else -> null
    }
    val selectedDate = availabilityMonth?.days?.firstOrNull { it.id == selectedDateId }
    val bookingDraft = buildBookingDraft(
        service = selectedService,
        vehicle = selectedVehicle,
        date = selectedDate,
        time = selectedTime,
        name = contactName,
        phone = contactPhone,
        email = contactEmail,
        notes = contactNotes,
        acceptsPrivacy = acceptsPrivacy,
    )

    LaunchedEffect(submitState) {
        val state = submitState
        if (state is BookingSubmitUiState.Success) {
            reservationCode = state.receipt.reservationCode
            currentStepName = BookingStep.Success.name
            onSubmitSuccessConsumed()
        }
    }

    LaunchedEffect(currentStep, selectedService?.id, availabilityAnchorDate) {
        if (currentStep == BookingStep.DateTime && selectedService != null) {
            onLoadAvailability(selectedService.durationMinutes, availabilityAnchorDate)
        }
    }

    LaunchedEffect(catalogState) {
        if (catalogState is ProductCatalogUiState.Loaded &&
            selectedServiceId != null &&
            catalogState.services.none { it.id == selectedServiceId }
        ) {
            selectedServiceId = null
            selectedDateId = null
            selectedTime = null
        }
    }

    LaunchedEffect(availabilityMonth) {
        val days = availabilityMonth?.days.orEmpty()
        if (days.isEmpty()) return@LaunchedEffect
        if (minimumAvailabilityMonthAnchor == null) {
            minimumAvailabilityMonthAnchor = availabilityMonth?.monthAnchorDate()
        }

        val selectedDateStillAvailable = days.any { day ->
            day.id == selectedDateId && day.available
        }
        if (!selectedDateStillAvailable) {
            selectedDateId = days.firstOrNull { it.available }?.id
            selectedTime = null
        }
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
                .padding(bottom = contentPadding.calculateBottomPadding() + 104.dp),
        ) {
            when (currentStep) {
                BookingStep.Service -> {
                    BookingServiceHeader(onBack = onBack)

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                            .padding(top = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        BookingServiceStepContent(
                            catalogState = catalogState,
                            selectedServiceId = selectedServiceId,
                            onRetryCatalog = onLoadCatalog,
                            onServiceSelected = { service ->
                                if (selectedServiceId != service.id) {
                                    selectedDateId = null
                                    selectedTime = null
                                    availabilityAnchorDate = null
                                    minimumAvailabilityMonthAnchor = null
                                }
                                selectedServiceId = service.id
                                onClearSubmitError()
                            },
                        )
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
                            .offset(y = (-16).dp)
                            .padding(top = 0.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Text(
                            text = "Selecione o tipo de veículo para calcular o preço correto",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = 0.78f),
                        )

                        bookingVehicles.forEach { vehicle ->
                            BookingVehicleCard(
                                vehicle = vehicle,
                                selected = selectedVehicleId == vehicle.id,
                                onSelected = {
                                    selectedVehicleId = vehicle.id
                                    onClearSubmitError()
                                },
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
                        availabilityState = availabilityState,
                        selectedDateId = selectedDateId,
                        selectedTime = selectedTime,
                        onDateSelected = { dateId ->
                            selectedDateId = dateId
                            selectedTime = null
                            onClearSubmitError()
                        },
                        onTimeSelected = {
                            selectedTime = it
                            onClearSubmitError()
                        },
                        onRetryAvailability = {
                            selectedService?.let {
                                onLoadAvailability(it.durationMinutes, availabilityAnchorDate)
                            }
                        },
                        minimumMonthAnchor = minimumAvailabilityMonthAnchor,
                        onPreviousMonth = {
                            val currentAnchor = availabilityMonth?.monthAnchorDate()
                            val previousAnchor = shiftMonthAnchorDate(currentAnchor, monthOffset = -1)
                            val minimumAnchor = minimumAvailabilityMonthAnchor
                            if (
                                previousAnchor != null &&
                                minimumAnchor != null &&
                                previousAnchor >= minimumAnchor
                            ) {
                                selectedDateId = null
                                selectedTime = null
                                availabilityAnchorDate = if (previousAnchor == minimumAnchor) {
                                    null
                                } else {
                                    previousAnchor
                                }
                                onClearSubmitError()
                            }
                        },
                        onNextMonth = {
                            val nextAnchor = shiftMonthAnchorDate(
                                availabilityMonth?.monthAnchorDate(),
                                monthOffset = 1,
                            )
                            if (nextAnchor != null) {
                                selectedDateId = null
                                selectedTime = null
                                availabilityAnchorDate = nextAnchor
                                onClearSubmitError()
                            }
                        },
                    )
                }

                BookingStep.Contact -> {
                    BookingContactHeader(
                        onBack = { currentStepName = BookingStep.DateTime.name },
                    )

                    BookingContactContent(
                        name = contactName,
                        phone = contactPhone,
                        email = contactEmail,
                        notes = contactNotes,
                        acceptsPrivacy = acceptsPrivacy,
                        onNameChange = {
                            contactName = it
                            onClearSubmitError()
                        },
                        onPhoneChange = {
                            contactPhone = it
                            onClearSubmitError()
                        },
                        onEmailChange = {
                            contactEmail = it
                            onClearSubmitError()
                        },
                        onNotesChange = {
                            contactNotes = it
                            onClearSubmitError()
                        },
                        onAcceptsPrivacyChange = {
                            acceptsPrivacy = it
                            onClearSubmitError()
                        },
                    )
                }

                BookingStep.Confirmation -> {
                    BookingConfirmationHeader(
                        onBack = { currentStepName = BookingStep.Contact.name },
                    )

                    BookingConfirmationContent(
                        service = selectedService,
                        vehicle = selectedVehicle,
                        date = selectedDate,
                        time = selectedTime,
                        name = contactName,
                        phone = contactPhone,
                        email = contactEmail,
                        notes = contactNotes,
                        onEditService = {
                            currentStepName = BookingStep.Service.name
                            onClearSubmitError()
                        },
                        onEditDateTime = {
                            currentStepName = BookingStep.DateTime.name
                            onClearSubmitError()
                        },
                        onEditContact = {
                            currentStepName = BookingStep.Contact.name
                            onClearSubmitError()
                        },
                        submitState = submitState,
                    )
                }

                BookingStep.Success -> {
                    BookingSuccessContent(
                        service = selectedService,
                        date = selectedDate,
                        time = selectedTime,
                        phone = contactPhone,
                        reservationCode = reservationCode,
                        onAddToCalendar = {},
                        onViewBooking = onViewBooking,
                        onHome = onHome,
                    )
                }
            }
        }

        if (currentStep != BookingStep.Success) {
            ContinueBar(
                enabled = when (currentStep) {
                    BookingStep.Service -> selectedService != null
                    BookingStep.Vehicle -> selectedVehicleId != null
                    BookingStep.DateTime -> selectedDate != null && selectedTime != null
                    BookingStep.Contact -> contactFormValid
                    BookingStep.Confirmation -> selectedService != null &&
                        selectedVehicle != null &&
                        selectedDate != null &&
                        selectedTime != null &&
                        contactFormValid &&
                        submitState !is BookingSubmitUiState.Loading
                    BookingStep.Success -> false
                },
                onClick = {
                    when (currentStep) {
                        BookingStep.Service -> currentStepName = BookingStep.Vehicle.name
                        BookingStep.Vehicle -> currentStepName = BookingStep.DateTime.name
                        BookingStep.DateTime -> currentStepName = BookingStep.Contact.name
                        BookingStep.Contact -> currentStepName = BookingStep.Confirmation.name
                        BookingStep.Confirmation -> onSubmitBooking(bookingDraft)
                        BookingStep.Success -> Unit
                    }
                },
                label = when (currentStep) {
                    BookingStep.Contact -> "Rever Marcação"
                    BookingStep.Confirmation -> if (submitState is BookingSubmitUiState.Loading) {
                        "A confirmar..."
                    } else if (submitState is BookingSubmitUiState.Error && submitState.retryable) {
                        "Tentar novamente"
                    } else {
                        "Confirmar Marcação"
                    }
                    else -> "Continuar"
                },
                contentPadding = contentPadding,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}

@Composable
private fun BookingServiceHeader(onBack: () -> Unit) {
    BookingStepHeader(
        title = "Escolha o Serviço",
        subtitle = "Passo 1 de 4",
        onBack = onBack,
    )
}

@Composable
private fun BookingVehicleHeader(onBack: () -> Unit) {
    BookingStepHeader(
        title = "Tipo de Veículo",
        subtitle = "Passo 2 de 4",
        onBack = onBack,
    )
}

@Composable
private fun BookingDateTimeHeader(onBack: () -> Unit) {
    BookingStepHeader(
        title = "Data e Hora",
        subtitle = "Passo 3 de 4",
        onBack = onBack,
    )
}

@Composable
private fun BookingContactHeader(onBack: () -> Unit) {
    BookingStepHeader(
        title = "Dados de Contacto",
        subtitle = "Passo 4 de 4",
        onBack = onBack,
    )
}

@Composable
private fun BookingConfirmationHeader(onBack: () -> Unit) {
    BookingStepHeader(
        title = "Confirmar Marcação",
        subtitle = "Reveja os detalhes antes de confirmar",
        onBack = onBack,
    )
}

@Composable
private fun BookingStepHeader(
    title: String,
    subtitle: String,
    onBack: () -> Unit,
) {
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
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.inverseOnSurface,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = 0.72f),
        )
    }
}

@Composable
private fun BookingConfirmationContent(
    service: ProductServiceUi?,
    vehicle: BookingVehicle?,
    date: BookingAvailabilityDay?,
    time: String?,
    name: String,
    phone: String,
    email: String,
    notes: String,
    onEditService: () -> Unit,
    onEditDateTime: () -> Unit,
    onEditContact: () -> Unit,
    submitState: BookingSubmitUiState,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .offset(y = (-16).dp)
            .padding(top = 0.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ConfirmationCard(
            title = "Detalhes do Serviço",
            onEdit = onEditService,
        ) {
            ConfirmationIconRow(
                icon = service?.icon ?: Icons.Filled.AutoAwesome,
                title = service?.name ?: "Serviço por selecionar",
                body = "Veículo: ${vehicle?.name ?: "Por selecionar"}",
            )
        }

        ConfirmationCard(
            title = "Data e Hora",
            onEdit = onEditDateTime,
        ) {
            ConfirmationLine(
                icon = Icons.Filled.CalendarMonth,
                text = date?.summaryLabel ?: "Data por selecionar",
            )
            ConfirmationLine(
                icon = Icons.Filled.AccessTime,
                text = time ?: "Hora por selecionar",
            )
        }

        ConfirmationCard(
            title = "Seus Dados",
            onEdit = onEditContact,
        ) {
            ConfirmationLine(icon = Icons.Filled.Person, text = name)
            ConfirmationLine(icon = Icons.Filled.Phone, text = phone)
            ConfirmationLine(icon = Icons.Filled.Email, text = email)
            if (notes.isNotBlank()) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Text(
                    text = notes,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontStyle = FontStyle.Italic,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }

        ConfirmationCard(title = "Localização") {
            ConfirmationIconRow(
                icon = Icons.Filled.LocationOn,
                title = "Suds & Shine Solutions",
                body = "Shopping Norte Sul, Piso -1\nLeiria, Portugal",
            )
        }

        PriceSummaryCard(
            serviceName = service?.name ?: "Serviço",
            price = service?.priceForVehicle(vehicle?.id) ?: "0,00€",
        )

        BookingSubmitStatusCard(submitState)
    }
}

@Composable
private fun BookingSubmitStatusCard(
    submitState: BookingSubmitUiState,
) {
    when (submitState) {
        BookingSubmitUiState.Idle, is BookingSubmitUiState.Success -> Unit

        BookingSubmitUiState.Loading -> {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f),
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                shape = RoundedCornerShape(18.dp),
            ) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = MaterialTheme.colorScheme.secondary,
                        strokeWidth = 2.dp,
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "A confirmar marcação",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "Estamos a validar o horário com o sistema de reservas.",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        }

        is BookingSubmitUiState.Error -> {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                shape = RoundedCornerShape(18.dp),
            ) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Info,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Não foi possível confirmar",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = submitState.message,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ConfirmationCard(
    title: String,
    onEdit: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        shape = RoundedCornerShape(18.dp),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                )
                if (onEdit != null) {
                    Surface(
                        modifier = Modifier.clickable(onClick = onEdit),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.24f),
                        contentColor = MaterialTheme.colorScheme.tertiary,
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Edit,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                            )
                            Text(
                                text = "Editar",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }

            content()
        }
    }
}

@Composable
private fun ConfirmationIconRow(
    icon: ImageVector,
    title: String,
    body: String,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            modifier = Modifier.size(42.dp),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.30f),
            contentColor = MaterialTheme.colorScheme.tertiary,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.padding(10.dp),
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = body,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ConfirmationLine(
    icon: ImageVector,
    text: String,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier.size(20.dp),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun PriceSummaryCard(
    serviceName: String,
    price: String,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.inverseSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        shape = RoundedCornerShape(18.dp),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Total a Pagar",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.inverseOnSurface,
                    fontWeight = FontWeight.Bold,
                )
                Icon(
                    imageVector = Icons.Filled.Euro,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiaryContainer,
                    modifier = Modifier.size(22.dp),
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = serviceName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = 0.76f),
                )
                Text(
                    text = price,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = 0.76f),
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = 0.18f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Total",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.inverseOnSurface,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = price,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun BookingSuccessContent(
    service: ProductServiceUi?,
    date: BookingAvailabilityDay?,
    time: String?,
    phone: String,
    reservationCode: String?,
    onAddToCalendar: () -> Unit,
    onViewBooking: () -> Unit,
    onHome: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(horizontal = 24.dp)
            .padding(top = 36.dp, bottom = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(20.dp))

        Box(
            modifier = Modifier.size(128.dp),
            contentAlignment = Alignment.Center,
        ) {
            Surface(
                modifier = Modifier.size(128.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.24f),
            ) {}
            Surface(
                modifier = Modifier.size(112.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.tertiary,
                contentColor = MaterialTheme.colorScheme.onTertiary,
                shadowElevation = 8.dp,
            ) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.padding(24.dp),
                )
            }
        }

        Spacer(Modifier.height(28.dp))

        Text(
            text = "Marcação Confirmada!",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "A sua marcação foi criada com sucesso",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(20.dp))

        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.24f),
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "Referência",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = reservationCode ?: "A confirmar",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        Spacer(Modifier.height(28.dp))

        SuccessSummaryCard(
            service = service,
            date = date,
            time = time,
            phone = phone,
        )

        Spacer(Modifier.height(16.dp))

        ConfirmationSentCard()

        Spacer(Modifier.height(28.dp))

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedButton(
                onClick = onAddToCalendar,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.inverseSurface),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.inverseSurface,
                ),
            ) {
                Icon(
                    imageVector = Icons.Filled.CalendarMonth,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text("Adicionar ao Google Calendar", style = MaterialTheme.typography.labelLarge)
            }

            OutlinedButton(
                onClick = onViewBooking,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.inverseSurface),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.inverseSurface,
                ),
            ) {
                Text("Ver Detalhes da Marcação", style = MaterialTheme.typography.labelLarge)
            }

            Button(
                onClick = onHome,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.tertiary,
                    contentColor = MaterialTheme.colorScheme.onTertiary,
                ),
            ) {
                Icon(
                    imageVector = Icons.Filled.Home,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Voltar ao Início",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun SuccessSummaryCard(
    service: ProductServiceUi?,
    date: BookingAvailabilityDay?,
    time: String?,
    phone: String,
) {
    ConfirmationCard(title = "Resumo da Marcação") {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            ConfirmationIconRow(
                icon = Icons.Filled.Event,
                title = "${date?.summaryLabel ?: "Data por confirmar"}, ${time ?: "hora por confirmar"}",
                body = service?.name ?: "Serviço por confirmar",
            )
            ConfirmationIconRow(
                icon = Icons.Filled.LocationOn,
                title = "Suds & Shine Solutions",
                body = "Shopping Norte Sul, Piso -1, Leiria",
            )
            ConfirmationIconRow(
                icon = Icons.Filled.Phone,
                title = phone.ifBlank { "913 005 855" },
                body = "Entre em contacto se necessário",
            )
        }
    }
}

@Composable
private fun ConfirmationSentCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f),
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(18.dp),
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(
                imageVector = Icons.Filled.MarkEmailRead,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(22.dp),
            )
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Confirmação enviada",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "Enviámos um email de confirmação com todos os detalhes da sua marcação.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun BookingContactContent(
    name: String,
    phone: String,
    email: String,
    notes: String,
    acceptsPrivacy: Boolean,
    onNameChange: (String) -> Unit,
    onPhoneChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onNotesChange: (String) -> Unit,
    onAcceptsPrivacyChange: (Boolean) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .offset(y = (-16).dp)
            .padding(top = 0.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
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
                BookingContactField(
                    label = "Nome Completo *",
                    value = name,
                    placeholder = "João Silva",
                    icon = Icons.Filled.Person,
                    onValueChange = onNameChange,
                )
                BookingContactField(
                    label = "Telemóvel *",
                    value = phone,
                    placeholder = "913 005 855",
                    icon = Icons.Filled.Phone,
                    keyboardType = KeyboardType.Phone,
                    onValueChange = onPhoneChange,
                )
                BookingContactField(
                    label = "Email *",
                    value = email,
                    placeholder = "seuemail@exemplo.com",
                    icon = Icons.Filled.Email,
                    keyboardType = KeyboardType.Email,
                    onValueChange = onEmailChange,
                )
                BookingContactField(
                    label = "Observações (Opcional)",
                    value = notes,
                    placeholder = "Alguma informação adicional que devamos saber...",
                    icon = Icons.AutoMirrored.Filled.Notes,
                    singleLine = false,
                    minLines = 4,
                    onValueChange = onNotesChange,
                )
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            shape = RoundedCornerShape(18.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onAcceptsPrivacyChange(!acceptsPrivacy) }
                    .padding(18.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Checkbox(
                    checked = acceptsPrivacy,
                    onCheckedChange = onAcceptsPrivacyChange,
                    colors = CheckboxDefaults.colors(
                        checkedColor = MaterialTheme.colorScheme.tertiary,
                        checkmarkColor = MaterialTheme.colorScheme.onTertiary,
                        uncheckedColor = MaterialTheme.colorScheme.outline,
                    ),
                )
                Text(
                    text = "Aceito a Política de Privacidade e autorizo o processamento dos meus dados para efeitos de marcação. *",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 10.dp),
                )
            }
        }
    }
}

@Composable
private fun BookingContactField(
    label: String,
    value: String,
    placeholder: String,
    icon: ImageVector,
    onValueChange: (String) -> Unit,
    keyboardType: KeyboardType = KeyboardType.Text,
    singleLine: Boolean = true,
    minLines: Int = 1,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
            )
        },
        placeholder = {
            Text(
                text = placeholder,
                style = MaterialTheme.typography.bodyMedium,
            )
        },
        leadingIcon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
            )
        },
        singleLine = singleLine,
        minLines = minLines,
        shape = RoundedCornerShape(14.dp),
        textStyle = MaterialTheme.typography.bodyMedium,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.tertiary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            focusedLabelColor = MaterialTheme.colorScheme.onSurface,
            unfocusedLabelColor = MaterialTheme.colorScheme.onSurface,
            focusedTextColor = MaterialTheme.colorScheme.onSurface,
            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
            focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
            unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
    )
}

@Composable
private fun DateTimeStepContent(
    availabilityState: BookingAvailabilityUiState,
    selectedDateId: String?,
    selectedTime: String?,
    onDateSelected: (String) -> Unit,
    onTimeSelected: (String) -> Unit,
    onRetryAvailability: () -> Unit,
    minimumMonthAnchor: String?,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
) {
    val month = when (availabilityState) {
        is BookingAvailabilityUiState.Empty -> availabilityState.month
        is BookingAvailabilityUiState.Loaded -> availabilityState.month
        else -> null
    }
    val selectedDay = month?.days?.firstOrNull { it.id == selectedDateId }
    val currentMonthAnchor = month?.monthAnchorDate()
    val canNavigatePrevious = currentMonthAnchor != null &&
        minimumMonthAnchor != null &&
        currentMonthAnchor > minimumMonthAnchor

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .offset(y = (-16).dp)
            .padding(top = 0.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        when (availabilityState) {
            BookingAvailabilityUiState.Idle,
            BookingAvailabilityUiState.Loading -> AvailabilityLoadingCard()

            is BookingAvailabilityUiState.Loaded -> {
                CalendarSelectionCard(
                    month = availabilityState.month,
                    selectedDateId = selectedDateId,
                    canNavigatePrevious = canNavigatePrevious,
                    onPreviousMonth = onPreviousMonth,
                    onNextMonth = onNextMonth,
                    onDateSelected = onDateSelected,
                )

                TimeSelectionCard(
                    slots = selectedDay?.slots.orEmpty(),
                    selectedTime = selectedTime,
                    onTimeSelected = onTimeSelected,
                )
            }

            is BookingAvailabilityUiState.Empty -> {
                CalendarSelectionCard(
                    month = availabilityState.month,
                    selectedDateId = selectedDateId,
                    canNavigatePrevious = canNavigatePrevious,
                    onPreviousMonth = onPreviousMonth,
                    onNextMonth = onNextMonth,
                    onDateSelected = onDateSelected,
                )

                AvailabilityStatusCard(
                    title = "Sem horários disponíveis",
                    body = "Não há vagas abertas para ${availabilityState.month.monthTitle}.",
                    onRetry = onRetryAvailability,
                )
            }

            is BookingAvailabilityUiState.Error -> AvailabilityStatusCard(
                title = "Não foi possível carregar horários",
                body = availabilityState.message,
                onRetry = onRetryAvailability,
            )
        }

        OpeningHoursCard()
    }
}

@Composable
private fun AvailabilityLoadingCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        shape = RoundedCornerShape(18.dp),
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                color = MaterialTheme.colorScheme.tertiary,
                strokeWidth = 2.dp,
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "A carregar horários",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "Estamos a consultar a disponibilidade em tempo real.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun AvailabilityStatusCard(
    title: String,
    body: String,
    onRetry: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        shape = RoundedCornerShape(18.dp),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Icon(
                    imageVector = Icons.Filled.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(22.dp),
                )
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = body,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            OutlinedButton(
                onClick = onRetry,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.tertiary,
                ),
            ) {
                Icon(
                    imageVector = Icons.Filled.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text("Tentar novamente", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun CalendarSelectionCard(
    month: BookingAvailabilityMonth,
    selectedDateId: String?,
    canNavigatePrevious: Boolean,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
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

            CalendarMonthPicker(
                month = month,
                selectedDateId = selectedDateId,
                canNavigatePrevious = canNavigatePrevious,
                onPreviousMonth = onPreviousMonth,
                onNextMonth = onNextMonth,
                onDateSelected = onDateSelected,
            )
        }
    }
}

@Composable
private fun CalendarMonthPicker(
    month: BookingAvailabilityMonth,
    selectedDateId: String?,
    canNavigatePrevious: Boolean,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onDateSelected: (String) -> Unit,
) {
    val cells: List<BookingAvailabilityDay?> = List(month.leadingEmptyCells) { null } + month.days
    val weeks = cells.chunked(7)
    val weekdayLabels = listOf("Seg", "Ter", "Qua", "Qui", "Sex", "Sáb", "Dom")

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = onPreviousMonth,
                    enabled = canNavigatePrevious,
                    modifier = Modifier.size(40.dp),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = "Mês anterior",
                        tint = if (canNavigatePrevious) {
                            MaterialTheme.colorScheme.tertiary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.36f)
                        },
                    )
                }

                Text(
                    text = month.monthTitle,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )

                IconButton(
                    onClick = onNextMonth,
                    modifier = Modifier.size(40.dp),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "Mês seguinte",
                        tint = MaterialTheme.colorScheme.tertiary,
                    )
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            weekdayLabels.forEach { label ->
                Text(
                    text = label,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
            }
        }

        weeks.forEach { week ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                week.forEach { dateOption ->
                    if (dateOption == null) {
                        Spacer(
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp),
                        )
                    } else {
                        CalendarDayCell(
                            dateOption = dateOption,
                            selected = selectedDateId == dateOption.id,
                            onSelected = { onDateSelected(dateOption.id) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }

                repeat(7 - week.size) {
                    Spacer(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun CalendarDayCell(
    dateOption: BookingAvailabilityDay,
    selected: Boolean,
    onSelected: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = dateOption.available, onClick = onSelected),
        shape = RoundedCornerShape(12.dp),
        color = when {
            selected -> MaterialTheme.colorScheme.tertiary
            dateOption.available -> MaterialTheme.colorScheme.surfaceContainerLow
            else -> MaterialTheme.colorScheme.surfaceContainerHigh
        },
        contentColor = when {
            selected -> MaterialTheme.colorScheme.onTertiary
            dateOption.available -> MaterialTheme.colorScheme.onSurface
            else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.52f)
        },
    ) {
        Box(
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = dateOption.dayOfMonth.toString(),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun TimeSelectionCard(
    slots: List<BookingAvailabilitySlot>,
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

            if (slots.isEmpty()) {
                Text(
                    text = "Sem horários disponíveis para esta data.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    slots.chunked(3).forEach { slotRow ->
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
}

@Composable
private fun TimeSlotButton(
    slot: BookingAvailabilitySlot,
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
private fun BookingServiceStepContent(
    catalogState: ProductCatalogUiState,
    selectedServiceId: String?,
    onRetryCatalog: () -> Unit,
    onServiceSelected: (ProductServiceUi) -> Unit,
) {
    when (catalogState) {
        ProductCatalogUiState.Idle,
        ProductCatalogUiState.Loading -> CatalogLoadingCard()

        is ProductCatalogUiState.Loaded -> {
            catalogState.services.forEach { service ->
                BookingServiceCard(
                    service = service,
                    selected = selectedServiceId == service.id,
                    onSelected = { onServiceSelected(service) },
                )
            }
        }

        ProductCatalogUiState.Empty -> AvailabilityStatusCard(
            title = "Sem serviços disponíveis",
            body = "O catálogo de serviços ainda não tem opções ativas.",
            onRetry = onRetryCatalog,
        )

        is ProductCatalogUiState.Error -> AvailabilityStatusCard(
            title = "Não foi possível carregar serviços",
            body = catalogState.message,
            onRetry = onRetryCatalog,
        )
    }
}

@Composable
private fun CatalogLoadingCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        shape = RoundedCornerShape(18.dp),
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                color = MaterialTheme.colorScheme.tertiary,
                strokeWidth = 2.dp,
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "A carregar serviços",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "Estamos a consultar o catálogo em tempo real.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun BookingServiceCard(
    service: ProductServiceUi,
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
                            text = service.durationLabel,
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

private fun buildBookingDraft(
    service: ProductServiceUi?,
    vehicle: BookingVehicle?,
    date: BookingAvailabilityDay?,
    time: String?,
    name: String,
    phone: String,
    email: String,
    notes: String,
    acceptsPrivacy: Boolean,
): ProductsBookingDraft? {
    if (service == null || vehicle == null || date == null || time == null) return null
    return ProductsBookingDraft(
        customerName = name,
        customerEmail = email,
        customerPhone = phone,
        serviceId = service.id,
        serviceName = service.name,
        dateId = date.id,
        time = time,
        serviceDurationMinutes = service.durationMinutes,
        vehicleType = vehicle.id,
        gdprConsent = acceptsPrivacy,
        notes = notes,
    )
}

private fun ProductServiceUi.priceForVehicle(vehicleId: String?): String {
    return if (vehicleId == "suv") suvPrice else passengerPrice
}

@Composable
private fun ContinueBar(
    enabled: Boolean,
    onClick: () -> Unit,
    label: String,
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
                    text = label,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}
