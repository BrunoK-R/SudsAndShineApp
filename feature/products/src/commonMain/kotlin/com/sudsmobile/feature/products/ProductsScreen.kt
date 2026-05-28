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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CardGiftcard
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
import androidx.compose.material.icons.filled.Lock
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
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sudsmobile.data.auth.AuthSessionState
import com.sudsmobile.data.booking.BookingAvailabilityDay
import com.sudsmobile.data.booking.BookingAvailabilityMonth
import com.sudsmobile.data.booking.BookingAvailabilitySlot
import com.sudsmobile.data.booking.BookingPaymentStatus
import com.sudsmobile.data.booking.toBookingPaymentStatus
import org.koin.compose.viewmodel.koinViewModel

private enum class BookingStep {
    Service,
    Vehicle,
    DateTime,
    Contact,
    Confirmation,
    Success,
}

private val bookingVehicleCategories = listOf(
    BookingVehicleUi(
        id = "passenger",
        name = "Passageiros",
        description = "Carros normais, sedans, compactos",
        type = "passenger",
        userVehicleId = null,
        vehicleLabel = null,
        isDefault = false,
    ),
    BookingVehicleUi(
        id = "suv",
        name = "SUV",
        description = "SUVs, vans, carrinhas",
        type = "suv",
        userVehicleId = null,
        vehicleLabel = null,
        isDefault = false,
    ),
)

@Composable
fun ProductsScreen(
    contentPadding: PaddingValues,
    initialServiceId: String? = null,
    initialServiceRequestKey: Long = 0L,
    onBack: () -> Unit = {},
    onViewBooking: () -> Unit = {},
    onHome: () -> Unit = {},
    onOpenPayment: (String?) -> Unit = {},
    onRequestSignIn: () -> Unit = {},
    onManageVehicles: () -> Unit = {},
) {
    val viewModel: ProductsBookingViewModel = koinViewModel()
    val catalogViewModel: ProductsCatalogViewModel = koinViewModel()
    val availabilityState by viewModel.availabilityState.collectAsStateWithLifecycle()
    val submitState by viewModel.submitState.collectAsStateWithLifecycle()
    val vehiclesState by viewModel.vehiclesState.collectAsStateWithLifecycle()
    val vehicleRevision by viewModel.vehicleRevision.collectAsStateWithLifecycle()
    val bookingRevision by viewModel.bookingRevision.collectAsStateWithLifecycle()
    val contactProfileState by viewModel.contactProfileState.collectAsStateWithLifecycle()
    val businessInfoState by viewModel.businessInfoState.collectAsStateWithLifecycle()
    val rewardsState by viewModel.rewardsState.collectAsStateWithLifecycle()
    val sessionState by viewModel.sessionState.collectAsStateWithLifecycle()
    val catalogState by catalogViewModel.catalogState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        catalogViewModel.loadCatalog()
    }

    ProductsScreenContent(
        contentPadding = contentPadding,
        initialServiceId = initialServiceId,
        initialServiceRequestKey = initialServiceRequestKey,
        catalogState = catalogState,
        vehiclesState = vehiclesState,
        vehicleRevision = vehicleRevision,
        bookingRevision = bookingRevision,
        contactProfileState = contactProfileState,
        businessInfoState = businessInfoState,
        rewardsState = rewardsState,
        sessionState = sessionState,
        availabilityState = availabilityState,
        submitState = submitState,
        onLoadCatalog = catalogViewModel::loadCatalog,
        onLoadVehicles = viewModel::loadVehicles,
        onRefreshVehiclesForSession = viewModel::refreshVehiclesForSession,
        onLoadContactProfile = viewModel::loadContactProfile,
        onRefreshContactProfileForSession = viewModel::refreshContactProfileForSession,
        onLoadBusinessInfo = viewModel::loadBusinessInfo,
        onLoadRewards = viewModel::loadRewards,
        onRefreshRewardsForSession = viewModel::refreshRewardsForSession,
        onLoadAvailability = viewModel::loadAvailability,
        onSubmitBooking = viewModel::submitBooking,
        onClearSubmitError = viewModel::clearSubmitError,
        onSubmitSuccessConsumed = viewModel::consumeSuccess,
        onBack = onBack,
        onViewBooking = onViewBooking,
        onHome = onHome,
        onOpenPayment = onOpenPayment,
        onRequestSignIn = onRequestSignIn,
        onManageVehicles = onManageVehicles,
    )
}

@Composable
private fun ProductsScreenContent(
    contentPadding: PaddingValues,
    initialServiceId: String?,
    initialServiceRequestKey: Long,
    catalogState: ProductCatalogUiState,
    vehiclesState: BookingVehiclesUiState,
    vehicleRevision: Long,
    bookingRevision: Long,
    contactProfileState: BookingContactProfileUiState,
    businessInfoState: BookingBusinessInfoUiState,
    rewardsState: BookingRewardsUiState,
    sessionState: AuthSessionState,
    availabilityState: BookingAvailabilityUiState,
    submitState: BookingSubmitUiState,
    onLoadCatalog: () -> Unit,
    onLoadVehicles: () -> Unit,
    onRefreshVehiclesForSession: () -> Unit,
    onLoadContactProfile: () -> Unit,
    onRefreshContactProfileForSession: () -> Unit,
    onLoadBusinessInfo: (Boolean) -> Unit,
    onLoadRewards: () -> Unit,
    onRefreshRewardsForSession: () -> Unit,
    onLoadAvailability: (Int, String?) -> Unit,
    onSubmitBooking: (ProductsBookingDraft?) -> Unit,
    onClearSubmitError: () -> Unit,
    onSubmitSuccessConsumed: () -> Unit,
    onBack: () -> Unit = {},
    onViewBooking: () -> Unit = {},
    onHome: () -> Unit = {},
    onOpenPayment: (String?) -> Unit = {},
    onRequestSignIn: () -> Unit = {},
    onManageVehicles: () -> Unit = {},
) {
    var currentStepName by rememberSaveable { mutableStateOf(BookingStep.Service.name) }
    var selectedServiceId by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedExtraIds by rememberSaveable { mutableStateOf(emptyList<String>()) }
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
    var loyaltyRewardCode by rememberSaveable { mutableStateOf("") }
    var appliedContactProfileUid by rememberSaveable { mutableStateOf<String?>(null) }
    var appliedContactName by rememberSaveable { mutableStateOf<String?>(null) }
    var appliedContactEmail by rememberSaveable { mutableStateOf<String?>(null) }
    var appliedContactPhone by rememberSaveable { mutableStateOf<String?>(null) }
    var reservationId by rememberSaveable { mutableStateOf<String?>(null) }
    var reservationCode by rememberSaveable { mutableStateOf<String?>(null) }
    var successLoyaltyRewardApplied by rememberSaveable { mutableStateOf(false) }
    var successLoyaltyRewardCode by rememberSaveable { mutableStateOf<String?>(null) }
    var successPaymentStatus by rememberSaveable { mutableStateOf("") }
    var appliedInitialServiceRequestKey by rememberSaveable { mutableStateOf<Long?>(null) }
    var unavailableInitialServiceId by rememberSaveable { mutableStateOf<String?>(null) }
    val currentStep = BookingStep.valueOf(currentStepName)
    val contactFormValid = contactName.isNotBlank() &&
        contactPhone.trim().length >= 6 &&
        contactEmail.trim().contains("@") &&
        acceptsPrivacy
    val loadedServices = (catalogState as? ProductCatalogUiState.Loaded)?.services.orEmpty()
    val loadedExtras = (catalogState as? ProductCatalogUiState.Loaded)?.extras.orEmpty()
    val selectedService = loadedServices.firstOrNull { it.id == selectedServiceId }
    val selectedExtras = loadedExtras.filter { extra -> extra.id in selectedExtraIds }
    val savedVehicles = (vehiclesState as? BookingVehiclesUiState.Loaded)?.vehicles.orEmpty()
    val vehicleOptions = savedVehicles + bookingVehicleCategories
    val selectedVehicle = vehicleOptions.firstOrNull { it.id == selectedVehicleId }
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
        loyaltyRewardCode = loyaltyRewardCode,
        selectedExtras = selectedExtras,
    )

    fun clearAppliedContactProfileIfUnchanged() {
        appliedContactName?.let { if (contactName == it) contactName = "" }
        appliedContactEmail?.let { if (contactEmail == it) contactEmail = "" }
        appliedContactPhone?.let { if (contactPhone == it) contactPhone = "" }
        appliedContactProfileUid = null
        appliedContactName = null
        appliedContactEmail = null
        appliedContactPhone = null
    }

    fun applyContactProfile(profile: BookingContactProfileUi, replaceExisting: Boolean) {
        val nextName = profile.displayName.takeIf { it.isNotBlank() && (replaceExisting || contactName.isBlank()) }
        val nextEmail = profile.email.takeIf { it.isNotBlank() && (replaceExisting || contactEmail.isBlank()) }
        val nextPhone = profile.phoneNumber.takeIf { it.isNotBlank() && (replaceExisting || contactPhone.isBlank()) }

        nextName?.let { contactName = it }
        nextEmail?.let { contactEmail = it }
        nextPhone?.let { contactPhone = it }

        appliedContactProfileUid = profile.uid
        appliedContactName = nextName
        appliedContactEmail = nextEmail
        appliedContactPhone = nextPhone
    }

    LaunchedEffect(submitState) {
        val state = submitState
        if (state is BookingSubmitUiState.Success) {
            reservationId = state.receipt.reservationId.takeIf { it.isNotBlank() }
            reservationCode = state.receipt.reservationCode
            successLoyaltyRewardApplied = state.receipt.loyaltyRewardApplied
            successLoyaltyRewardCode = state.receipt.loyaltyRewardCode
            successPaymentStatus = state.receipt.paymentStatus
            currentStepName = BookingStep.Success.name
            onSubmitSuccessConsumed()
        }
    }

    LaunchedEffect(currentStep, selectedService?.id, availabilityAnchorDate) {
        if (currentStep == BookingStep.DateTime && selectedService != null) {
            onLoadAvailability(selectedService.durationMinutes, availabilityAnchorDate)
        }
    }

    LaunchedEffect(currentStep, sessionState, vehicleRevision) {
        if (currentStep == BookingStep.Vehicle) {
            onRefreshVehiclesForSession()
        }
    }

    LaunchedEffect(currentStep, sessionState) {
        if (currentStep == BookingStep.Contact) {
            onRefreshContactProfileForSession()
        }
    }

    LaunchedEffect(currentStep) {
        if (
            currentStep == BookingStep.DateTime ||
            currentStep == BookingStep.Confirmation ||
            currentStep == BookingStep.Success
        ) {
            onLoadBusinessInfo(false)
        }
    }

    LaunchedEffect(currentStep, sessionState, bookingRevision) {
        if (currentStep == BookingStep.Confirmation) {
            onRefreshRewardsForSession()
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
        if (catalogState is ProductCatalogUiState.Loaded) {
            val availableExtraIds = catalogState.extras.map { it.id }.toSet()
            val validSelectedExtraIds = selectedExtraIds.filter { it in availableExtraIds }
            if (validSelectedExtraIds.size != selectedExtraIds.size) {
                selectedExtraIds = validSelectedExtraIds
            }
        }
    }

    LaunchedEffect(initialServiceId, initialServiceRequestKey, catalogState) {
        val loadedCatalog = catalogState as? ProductCatalogUiState.Loaded ?: return@LaunchedEffect
        val requestedServiceId = initialServiceId.normalizedInitialServiceId() ?: return@LaunchedEffect
        if (appliedInitialServiceRequestKey == initialServiceRequestKey) return@LaunchedEffect

        val resolvedServiceId = resolveInitialServiceId(
            initialServiceId = requestedServiceId,
            serviceIds = loadedCatalog.services.map { it.id },
        )
        if (resolvedServiceId == null) {
            if (selectedServiceId == requestedServiceId) {
                selectedServiceId = null
                selectedDateId = null
                selectedTime = null
            }
            unavailableInitialServiceId = requestedServiceId
            currentStepName = BookingStep.Service.name
        } else {
            appliedInitialServiceRequestKey = initialServiceRequestKey
            selectedServiceId = resolvedServiceId
            selectedDateId = null
            selectedTime = null
            availabilityAnchorDate = null
            minimumAvailabilityMonthAnchor = null
            unavailableInitialServiceId = null
            currentStepName = BookingStep.Vehicle.name
            onClearSubmitError()
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

    LaunchedEffect(vehiclesState) {
        val defaultVehicleId = savedVehicles.firstOrNull { it.isDefault }?.id
        if (selectedVehicleId != null && vehicleOptions.none { it.id == selectedVehicleId }) {
            selectedVehicleId = defaultVehicleId
        } else if (selectedVehicleId == null) {
            selectedVehicleId = defaultVehicleId
        }
    }

    LaunchedEffect(contactProfileState) {
        when (val state = contactProfileState) {
            is BookingContactProfileUiState.Loaded -> {
                if (appliedContactProfileUid != state.profile.uid) {
                    clearAppliedContactProfileIfUnchanged()
                    applyContactProfile(state.profile, replaceExisting = false)
                }
            }
            BookingContactProfileUiState.Unauthenticated -> clearAppliedContactProfileIfUnchanged()
            BookingContactProfileUiState.Empty,
            is BookingContactProfileUiState.Error,
            BookingContactProfileUiState.Idle,
            BookingContactProfileUiState.Loading -> Unit
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
                            selectedExtraIds = selectedExtraIds,
                            unavailableInitialServiceId = unavailableInitialServiceId,
                            onRetryCatalog = onLoadCatalog,
                            onServiceSelected = { service ->
                                if (selectedServiceId != service.id) {
                                    selectedDateId = null
                                    selectedTime = null
                                    availabilityAnchorDate = null
                                    minimumAvailabilityMonthAnchor = null
                                }
                                unavailableInitialServiceId = null
                                selectedServiceId = service.id
                                onClearSubmitError()
                            },
                            onExtraToggled = { extra ->
                                selectedExtraIds = if (extra.id in selectedExtraIds) {
                                    selectedExtraIds - extra.id
                                } else {
                                    selectedExtraIds + extra.id
                                }
                                onClearSubmitError()
                            },
                        )
                    }
                }

                BookingStep.Vehicle -> {
                    BookingVehicleHeader(
                        onBack = { currentStepName = BookingStep.Service.name },
                    )

                    BookingVehicleStepContent(
                        vehiclesState = vehiclesState,
                        selectedVehicleId = selectedVehicleId,
                        onRetryVehicles = onLoadVehicles,
                        onRequestSignIn = onRequestSignIn,
                        onManageVehicles = onManageVehicles,
                        onVehicleSelected = { vehicle ->
                            selectedVehicleId = vehicle.id
                            onClearSubmitError()
                        },
                    )
                }

                BookingStep.DateTime -> {
                    BookingDateTimeHeader(
                        onBack = { currentStepName = BookingStep.Vehicle.name },
                    )

                    DateTimeStepContent(
                        availabilityState = availabilityState,
                        businessInfoState = businessInfoState,
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
                        onRetryBusinessInfo = { onLoadBusinessInfo(true) },
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
                        contactProfileState = contactProfileState,
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
                        onRetryContactProfile = onLoadContactProfile,
                        onRequestSignIn = onRequestSignIn,
                        onApplyContactProfile = { profile ->
                            applyContactProfile(profile, replaceExisting = true)
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
                        selectedExtras = selectedExtras,
                        vehicle = selectedVehicle,
                        date = selectedDate,
                        time = selectedTime,
                        name = contactName,
                        phone = contactPhone,
                        email = contactEmail,
                        notes = contactNotes,
                        loyaltyRewardCode = loyaltyRewardCode,
                        onLoyaltyRewardCodeChange = {
                            loyaltyRewardCode = it
                            onClearSubmitError()
                        },
                        businessInfoState = businessInfoState,
                        onRetryBusinessInfo = { onLoadBusinessInfo(true) },
                        rewardsState = rewardsState,
                        onRetryRewards = onLoadRewards,
                        sessionState = sessionState,
                        onRequestSignIn = onRequestSignIn,
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
                        onSubmitErrorAction = { resolution ->
                            when (resolution) {
                                BookingSubmitResolution.ChangeSlot -> {
                                    selectedTime = null
                                    currentStepName = BookingStep.DateTime.name
                                    onClearSubmitError()
                                    selectedService?.let {
                                        onLoadAvailability(it.durationMinutes, availabilityAnchorDate)
                                    }
                                }
                                BookingSubmitResolution.Retry -> onSubmitBooking(bookingDraft)
                                BookingSubmitResolution.SignIn -> onRequestSignIn()
                                BookingSubmitResolution.None -> Unit
                            }
                        },
                    )
                }

                BookingStep.Success -> {
                    BookingSuccessContent(
                        service = selectedService,
                        selectedExtras = selectedExtras,
                        date = selectedDate,
                        time = selectedTime,
                        businessInfoState = businessInfoState,
                        reservationId = reservationId,
                        reservationCode = reservationCode,
                        loyaltyRewardApplied = successLoyaltyRewardApplied,
                        loyaltyRewardCode = successLoyaltyRewardCode,
                        paymentStatus = successPaymentStatus,
                        onAddToCalendar = {},
                        onViewBooking = onViewBooking,
                        onHome = onHome,
                        onOpenPayment = onOpenPayment,
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
                        BookingStep.Confirmation -> when (
                            (submitState as? BookingSubmitUiState.Error)?.resolution
                        ) {
                            BookingSubmitResolution.ChangeSlot -> {
                                selectedTime = null
                                currentStepName = BookingStep.DateTime.name
                                onClearSubmitError()
                                selectedService?.let {
                                    onLoadAvailability(it.durationMinutes, availabilityAnchorDate)
                                }
                            }
                            BookingSubmitResolution.Retry -> onSubmitBooking(bookingDraft)
                            BookingSubmitResolution.SignIn -> onRequestSignIn()
                            BookingSubmitResolution.None,
                            null -> onSubmitBooking(bookingDraft)
                        }
                        BookingStep.Success -> Unit
                    }
                },
                label = when (currentStep) {
                    BookingStep.Contact -> "Rever Marcação"
                    BookingStep.Confirmation -> if (submitState is BookingSubmitUiState.Loading) {
                        "A confirmar..."
                    } else if (submitState is BookingSubmitUiState.Error) {
                        submitState.resolution.continueLabel()
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
    selectedExtras: List<ProductExtraUi>,
    vehicle: BookingVehicleUi?,
    date: BookingAvailabilityDay?,
    time: String?,
    name: String,
    phone: String,
    email: String,
    notes: String,
    loyaltyRewardCode: String,
    onLoyaltyRewardCodeChange: (String) -> Unit,
    businessInfoState: BookingBusinessInfoUiState,
    onRetryBusinessInfo: () -> Unit,
    rewardsState: BookingRewardsUiState,
    onRetryRewards: () -> Unit,
    sessionState: AuthSessionState,
    onRequestSignIn: () -> Unit,
    onEditService: () -> Unit,
    onEditDateTime: () -> Unit,
    onEditContact: () -> Unit,
    submitState: BookingSubmitUiState,
    onSubmitErrorAction: (BookingSubmitResolution) -> Unit,
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
            if (selectedExtras.isNotEmpty()) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                selectedExtras.forEach { extra ->
                    ConfirmationLine(
                        icon = extra.icon,
                        text = "${extra.name} (+${extra.price})",
                    )
                }
            }
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

        BookingLocationCard(
            state = businessInfoState,
            onRetry = onRetryBusinessInfo,
        )

        LoyaltyRewardCodeCard(
            rewardCode = loyaltyRewardCode,
            rewardsState = rewardsState,
            sessionState = sessionState,
            onRewardCodeChange = onLoyaltyRewardCodeChange,
            onRetryRewards = onRetryRewards,
            onRequestSignIn = onRequestSignIn,
        )

        val basePriceCents = service?.priceCentsForVehicle(vehicle?.type) ?: 0
        val extrasPriceCents = selectedExtras.sumOf { it.priceCents }
        val totalPriceCents = basePriceCents + extrasPriceCents
        val rewardPendingValidation = loyaltyRewardCode.isNotBlank() && sessionState is AuthSessionState.Authenticated
        PriceSummaryCard(
            serviceName = service?.name ?: "Serviço",
            basePrice = basePriceCents.toEuroLabel(),
            extras = selectedExtras,
            discount = null,
            pendingRewardValidation = rewardPendingValidation,
            total = totalPriceCents.toEuroLabel(),
        )

        BookingSubmitStatusCard(
            submitState = submitState,
            onAction = onSubmitErrorAction,
        )
    }
}

@Composable
private fun BookingLocationCard(
    state: BookingBusinessInfoUiState,
    onRetry: () -> Unit,
) {
    val info = state.infoOrDefault()

    ConfirmationCard(title = "Localização") {
        when (state) {
            BookingBusinessInfoUiState.Idle,
            BookingBusinessInfoUiState.Loading -> BookingLocationStatusRow(
                title = "A carregar localização",
                body = "Estamos a consultar os dados públicos do espaço.",
                loading = true,
            )

            is BookingBusinessInfoUiState.Error -> {
                BookingLocationStatusRow(
                    title = "Localização de reserva",
                    body = state.message,
                    loading = false,
                )
                BookingLocationDetails(info = info)
                if (state.retryable) {
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

            is BookingBusinessInfoUiState.Loaded -> BookingLocationDetails(info = info)
        }
    }
}

@Composable
private fun BookingLocationStatusRow(
    title: String,
    body: String,
    loading: Boolean,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Surface(
            modifier = Modifier.size(42.dp),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.30f),
            contentColor = MaterialTheme.colorScheme.tertiary,
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = MaterialTheme.colorScheme.tertiary,
                        strokeWidth = 2.dp,
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.Info,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
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
private fun BookingLocationDetails(info: BookingBusinessInfoUi) {
    ConfirmationIconRow(
        icon = Icons.Filled.LocationOn,
        title = "Suds & Shine Solutions",
        body = listOf(info.addressLine1, info.addressLine2)
            .filter { it.isNotBlank() }
            .joinToString(separator = "\n"),
    )
}

private fun BookingBusinessInfoUi.singleLineAddress(): String {
    return listOf(addressLine1, addressLine2)
        .filter { it.isNotBlank() }
        .joinToString(separator = ", ")
}

@Composable
private fun LoyaltyRewardCodeCard(
    rewardCode: String,
    rewardsState: BookingRewardsUiState,
    sessionState: AuthSessionState,
    onRewardCodeChange: (String) -> Unit,
    onRetryRewards: () -> Unit,
    onRequestSignIn: () -> Unit,
) {
    val authenticated = sessionState is AuthSessionState.Authenticated
    val restoringSession = sessionState == AuthSessionState.Restoring
    val restoreErrorMessage = (sessionState as? AuthSessionState.RestoreFailed)?.error?.message

    ConfirmationCard(title = "Recompensa") {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Surface(
                modifier = Modifier.size(42.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.30f),
                contentColor = MaterialTheme.colorScheme.tertiary,
            ) {
                Icon(
                    imageVector = if (authenticated || restoringSession) Icons.Filled.CardGiftcard else Icons.Filled.Lock,
                    contentDescription = null,
                    modifier = Modifier.padding(10.dp),
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = when {
                        authenticated -> "Aplicar código de lavagem grátis"
                        restoringSession -> "A validar sessão"
                        else -> "Entre para aplicar recompensas"
                    },
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = when {
                        authenticated -> "O código é validado na sua conta ao confirmar a marcação."
                        restoringSession -> "Estamos a confirmar se há recompensas associadas à sua conta."
                        else -> "As recompensas emitidas ficam associadas à sua conta e só podem ser usadas uma vez."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                when {
                    authenticated -> {
                        BookingRewardCodesContent(
                            rewardsState = rewardsState,
                            selectedCode = rewardCode,
                            onRewardCodeSelected = { selectedCode ->
                                onRewardCodeChange(
                                    if (rewardCode.equals(selectedCode, ignoreCase = true)) {
                                        ""
                                    } else {
                                        selectedCode
                                    },
                                )
                            },
                            onRetryRewards = onRetryRewards,
                        )

                        OutlinedTextField(
                            value = rewardCode,
                            onValueChange = { input ->
                                onRewardCodeChange(input.take(80).uppercase())
                            },
                            modifier = Modifier.fillMaxWidth(),
                            label = {
                                Text(
                                    text = "Código manual ou selecionado",
                                    style = MaterialTheme.typography.labelLarge,
                                )
                            },
                            placeholder = {
                                Text(
                                    text = "SS-FREE-XXXX-0001",
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            textStyle = MaterialTheme.typography.bodyMedium,
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Characters,
                            ),
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

                    restoringSession -> BookingRewardStateRow(
                        title = "Sessão em validação",
                        body = "Vamos mostrar os códigos assim que a sessão terminar de carregar.",
                        loading = true,
                    )

                    restoreErrorMessage != null -> BookingRewardStateRow(
                        title = "Não foi possível validar sessão",
                        body = restoreErrorMessage,
                        actionLabel = "Entrar",
                        onAction = onRequestSignIn,
                    )

                    else -> {
                        OutlinedButton(
                            onClick = onRequestSignIn,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.tertiary,
                            ),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Lock,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Entrar", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BookingSubmitStatusCard(
    submitState: BookingSubmitUiState,
    onAction: (BookingSubmitResolution) -> Unit,
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
                            text = submitState.resolution.errorTitle(),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = submitState.message,
                            style = MaterialTheme.typography.bodySmall,
                        )

                        val actionLabel = submitState.resolution.actionLabel()
                        if (actionLabel != null) {
                            OutlinedButton(
                                onClick = { onAction(submitState.resolution) },
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.36f)),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                                ),
                            ) {
                                Icon(
                                    imageVector = submitState.resolution.actionIcon(),
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(actionLabel, style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun BookingSubmitResolution.continueLabel(): String {
    return when (this) {
        BookingSubmitResolution.ChangeSlot -> "Escolher outro horário"
        BookingSubmitResolution.Retry -> "Tentar novamente"
        BookingSubmitResolution.SignIn -> "Entrar para continuar"
        BookingSubmitResolution.None -> "Confirmar Marcação"
    }
}

private fun BookingSubmitResolution.errorTitle(): String {
    return when (this) {
        BookingSubmitResolution.ChangeSlot -> "Horário indisponível"
        BookingSubmitResolution.Retry -> "Não foi possível confirmar"
        BookingSubmitResolution.SignIn -> "Sessão necessária"
        BookingSubmitResolution.None -> "Não foi possível confirmar"
    }
}

private fun BookingSubmitResolution.actionLabel(): String? {
    return when (this) {
        BookingSubmitResolution.ChangeSlot -> "Escolher outro horário"
        BookingSubmitResolution.Retry -> "Tentar novamente"
        BookingSubmitResolution.SignIn -> "Entrar"
        BookingSubmitResolution.None -> null
    }
}

private fun BookingSubmitResolution.actionIcon(): ImageVector {
    return when (this) {
        BookingSubmitResolution.ChangeSlot -> Icons.Filled.CalendarMonth
        BookingSubmitResolution.Retry -> Icons.Filled.Refresh
        BookingSubmitResolution.SignIn -> Icons.Filled.Lock
        BookingSubmitResolution.None -> Icons.Filled.Info
    }
}

@Composable
private fun BookingRewardCodesContent(
    rewardsState: BookingRewardsUiState,
    selectedCode: String,
    onRewardCodeSelected: (String) -> Unit,
    onRetryRewards: () -> Unit,
) {
    when (rewardsState) {
        BookingRewardsUiState.Idle,
        BookingRewardsUiState.Loading -> BookingRewardStateRow(
            title = "A carregar códigos emitidos",
            body = "Estamos a consultar as recompensas disponíveis na sua conta.",
            loading = true,
        )

        BookingRewardsUiState.Unauthenticated -> Unit

        BookingRewardsUiState.Empty -> BookingRewardStateRow(
            title = "Sem códigos disponíveis",
            body = "Quando resgatar uma recompensa, o código aparece aqui para aplicar na marcação.",
        )

        is BookingRewardsUiState.Error -> BookingRewardStateRow(
            title = "Não foi possível carregar códigos",
            body = rewardsState.message,
            actionLabel = if (rewardsState.retryable) "Tentar novamente" else null,
            onAction = if (rewardsState.retryable) onRetryRewards else null,
        )

        is BookingRewardsUiState.Loaded -> {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Códigos disponíveis",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                )
                rewardsState.rewardCodes.forEach { reward ->
                    BookingRewardCodeOption(
                        reward = reward,
                        selected = selectedCode.equals(reward.code, ignoreCase = true),
                        onClick = { onRewardCodeSelected(reward.code) },
                    )
                }
            }
        }
    }
}

@Composable
private fun BookingRewardCodeOption(
    reward: BookingRewardCodeUi,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = if (selected) {
            MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.58f)
        } else {
            MaterialTheme.colorScheme.surfaceContainerLow
        },
        contentColor = if (selected) {
            MaterialTheme.colorScheme.onTertiaryContainer
        } else {
            MaterialTheme.colorScheme.onSurface
        },
        border = BorderStroke(
            1.dp,
            if (selected) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.outlineVariant,
        ),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = if (selected) Icons.Filled.Check else Icons.Filled.CardGiftcard,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = reward.code,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = reward.issuedAt,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (selected) {
                        MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.76f)
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
            Text(
                text = reward.statusLabel,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun BookingRewardStateRow(
    title: String,
    body: String,
    loading: Boolean = false,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.Top,
            ) {
                if (loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = MaterialTheme.colorScheme.tertiary,
                        strokeWidth = 2.dp,
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.Info,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.tertiary,
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelLarge,
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

            if (actionLabel != null && onAction != null) {
                OutlinedButton(
                    onClick = onAction,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.tertiary,
                    ),
                ) {
                    Text(
                        text = actionLabel,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                    )
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
    basePrice: String,
    extras: List<ProductExtraUi>,
    discount: String?,
    pendingRewardValidation: Boolean = false,
    total: String,
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
                    text = basePrice,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = 0.76f),
                )
            }

            extras.forEach { extra ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = extra.name,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = 0.76f),
                    )
                    Text(
                        text = extra.price,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = 0.76f),
                    )
                }
            }

            if (discount != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "Recompensa fidelização",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = 0.76f),
                    )
                    Text(
                        text = "-$discount",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            if (pendingRewardValidation) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Recompensa fidelização",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = 0.76f),
                    )
                    Text(
                        text = "Valida ao confirmar",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        fontWeight = FontWeight.Bold,
                    )
                }
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
                    text = total,
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
    selectedExtras: List<ProductExtraUi>,
    date: BookingAvailabilityDay?,
    time: String?,
    businessInfoState: BookingBusinessInfoUiState,
    reservationId: String?,
    reservationCode: String?,
    loyaltyRewardApplied: Boolean,
    loyaltyRewardCode: String?,
    paymentStatus: String,
    onAddToCalendar: () -> Unit,
    onViewBooking: () -> Unit,
    onHome: () -> Unit,
    onOpenPayment: (String?) -> Unit,
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
            selectedExtras = selectedExtras,
            date = date,
            time = time,
            businessInfo = businessInfoState.infoOrDefault(),
        )

        Spacer(Modifier.height(16.dp))

        if (loyaltyRewardApplied) {
            LoyaltyRewardAppliedCard(rewardCode = loyaltyRewardCode)
            Spacer(Modifier.height(16.dp))
        } else if (paymentStatus.requiresPaymentAction()) {
            PaymentPendingCard(onOpenPayment = { onOpenPayment(reservationId) })
            Spacer(Modifier.height(16.dp))
        }

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

private fun String.requiresPaymentAction(): Boolean {
    return toBookingPaymentStatus() in setOf(
        BookingPaymentStatus.Pending,
        BookingPaymentStatus.Failed,
    )
}

@Composable
private fun PaymentPendingCard(onOpenPayment: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f),
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(18.dp),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Icon(
                    imageVector = Icons.Filled.Euro,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(22.dp),
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = "Pagamento pendente",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "Consulte o valor em aberto e apresente a referência no balcão.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            OutlinedButton(
                onClick = onOpenPayment,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.secondary,
                ),
            ) {
                Text(
                    text = "Ver pagamento",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun LoyaltyRewardAppliedCard(rewardCode: String?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.28f),
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
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
                imageVector = Icons.Filled.CardGiftcard,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(22.dp),
            )
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Recompensa aplicada",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = rewardCode?.let {
                        "A lavagem grátis $it foi associada a esta marcação."
                    } ?: "A lavagem grátis foi associada a esta marcação.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun SuccessSummaryCard(
    service: ProductServiceUi?,
    selectedExtras: List<ProductExtraUi>,
    date: BookingAvailabilityDay?,
    time: String?,
    businessInfo: BookingBusinessInfoUi,
) {
    ConfirmationCard(title = "Resumo da Marcação") {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            ConfirmationIconRow(
                icon = Icons.Filled.Event,
                title = "${date?.summaryLabel ?: "Data por confirmar"}, ${time ?: "hora por confirmar"}",
                body = if (selectedExtras.isEmpty()) {
                    service?.name ?: "Serviço por confirmar"
                } else {
                    "${service?.name ?: "Serviço por confirmar"} + ${selectedExtras.countLabel()}"
                },
            )
            ConfirmationIconRow(
                icon = Icons.Filled.LocationOn,
                title = "Suds & Shine Solutions",
                body = businessInfo.singleLineAddress(),
            )
            ConfirmationIconRow(
                icon = Icons.Filled.Phone,
                title = businessInfo.phone,
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
private fun BookingContactProfileCard(
    state: BookingContactProfileUiState,
    onRetry: () -> Unit,
    onRequestSignIn: () -> Unit,
    onApplyProfile: (BookingContactProfileUi) -> Unit,
) {
    when (state) {
        BookingContactProfileUiState.Idle -> Unit

        BookingContactProfileUiState.Loading -> ContactProfileMessageCard(
            icon = Icons.Filled.Person,
            title = "A carregar dados da conta",
            body = "Estamos a consultar o perfil associado a esta sessão.",
            loading = true,
        )

        BookingContactProfileUiState.Unauthenticated -> ContactProfileMessageCard(
            icon = Icons.Filled.Lock,
            title = "Entrar para preencher dados",
            body = "Pode continuar como convidado ou usar os dados guardados na sua conta.",
            actionLabel = "Entrar",
            actionIcon = Icons.Filled.Lock,
            onAction = onRequestSignIn,
        )

        BookingContactProfileUiState.Empty -> ContactProfileMessageCard(
            icon = Icons.Filled.Info,
            title = "Perfil sem contactos guardados",
            body = "Preencha os dados para esta marcação. Pode guardá-los no perfil mais tarde.",
        )

        is BookingContactProfileUiState.Error -> ContactProfileMessageCard(
            icon = Icons.Filled.Info,
            title = "Não foi possível carregar dados da conta",
            body = state.message,
            actionLabel = if (state.retryable) "Tentar novamente" else null,
            actionIcon = Icons.Filled.Refresh,
            onAction = if (state.retryable) onRetry else null,
        )

        is BookingContactProfileUiState.Loaded -> {
            ContactProfileMessageCard(
                icon = Icons.Filled.CheckCircle,
                title = "Dados da conta prontos",
                body = state.profile.contactSummary(),
                actionLabel = "Usar dados da conta",
                actionIcon = Icons.Filled.Check,
                onAction = { onApplyProfile(state.profile) },
            )
        }
    }
}

@Composable
private fun ContactProfileMessageCard(
    icon: ImageVector,
    title: String,
    body: String,
    loading: Boolean = false,
    actionLabel: String? = null,
    actionIcon: ImageVector = Icons.Filled.Refresh,
    onAction: (() -> Unit)? = null,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(18.dp),
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    color = MaterialTheme.colorScheme.tertiary,
                    strokeWidth = 2.dp,
                )
            } else {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(22.dp),
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                if (actionLabel != null && onAction != null) {
                    OutlinedButton(
                        onClick = onAction,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.tertiary,
                        ),
                    ) {
                        Icon(
                            imageVector = actionIcon,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(actionLabel, style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }
}

@Composable
private fun BookingContactContent(
    contactProfileState: BookingContactProfileUiState,
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
    onRetryContactProfile: () -> Unit,
    onRequestSignIn: () -> Unit,
    onApplyContactProfile: (BookingContactProfileUi) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .offset(y = (-16).dp)
            .padding(top = 0.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        BookingContactProfileCard(
            state = contactProfileState,
            onRetry = onRetryContactProfile,
            onRequestSignIn = onRequestSignIn,
            onApplyProfile = onApplyContactProfile,
        )

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

private fun BookingContactProfileUi.contactSummary(): String {
    val parts = listOfNotNull(
        displayName.takeIf { it.isNotBlank() },
        email.takeIf { it.isNotBlank() },
        phoneNumber.takeIf { it.isNotBlank() },
    )
    return if (parts.isEmpty()) {
        "O perfil não tem dados de contacto completos."
    } else {
        parts.joinToString(separator = " / ")
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
    businessInfoState: BookingBusinessInfoUiState,
    selectedDateId: String?,
    selectedTime: String?,
    onDateSelected: (String) -> Unit,
    onTimeSelected: (String) -> Unit,
    onRetryAvailability: () -> Unit,
    onRetryBusinessInfo: () -> Unit,
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

        OpeningHoursCard(
            state = businessInfoState,
            onRetry = onRetryBusinessInfo,
        )
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
private fun OpeningHoursCard(
    state: BookingBusinessInfoUiState,
    onRetry: () -> Unit,
) {
    val info = state.infoOrDefault()

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.20f),
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(18.dp),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            SectionHeader(
                icon = Icons.Filled.Info,
                title = "Horário de Funcionamento",
            )

            when (state) {
                BookingBusinessInfoUiState.Idle,
                BookingBusinessInfoUiState.Loading -> OpeningHoursStatusRow(
                    title = "A carregar horário",
                    body = "Estamos a consultar o horário atualizado do espaço.",
                    loading = true,
                )

                is BookingBusinessInfoUiState.Error -> {
                    OpeningHoursStatusRow(
                        title = "Horário atualizado indisponível",
                        body = state.message,
                        loading = false,
                        error = true,
                    )
                    OpeningHoursRows(openingHours = info.openingHours)
                    if (state.retryable) {
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
                            Text("Atualizar horário", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }

                is BookingBusinessInfoUiState.Loaded -> OpeningHoursRows(openingHours = info.openingHours)
            }
        }
    }
}

@Composable
private fun OpeningHoursStatusRow(
    title: String,
    body: String,
    loading: Boolean,
    error: Boolean = false,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                color = MaterialTheme.colorScheme.tertiary,
                strokeWidth = 2.dp,
            )
        } else {
            Icon(
                imageVector = Icons.Filled.Info,
                contentDescription = null,
                tint = if (error) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(22.dp),
            )
        }
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
}

@Composable
private fun OpeningHoursRows(openingHours: List<BookingOpeningHoursUi>) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        openingHours.forEachIndexed { index, hours ->
            OpeningHoursRow(hours)
            if (index != openingHours.lastIndex) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
    }
}

@Composable
private fun OpeningHoursRow(hours: BookingOpeningHoursUi) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = hours.dayLabel,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = hours.hoursLabel,
            style = MaterialTheme.typography.labelLarge,
            color = if (hours.closed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
        )
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
    selectedExtraIds: List<String>,
    unavailableInitialServiceId: String?,
    onRetryCatalog: () -> Unit,
    onServiceSelected: (ProductServiceUi) -> Unit,
    onExtraToggled: (ProductExtraUi) -> Unit,
) {
    if (unavailableInitialServiceId != null) {
        AvailabilityStatusCard(
            title = "Serviço indisponível",
            body = "O serviço escolhido já não está disponível no catálogo. Escolha outra opção.",
            onRetry = onRetryCatalog,
        )
    }

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
            BookingExtrasSelectionSection(
                extras = catalogState.extras,
                selectedExtraIds = selectedExtraIds,
                onExtraToggled = onExtraToggled,
            )
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
private fun BookingExtrasSelectionSection(
    extras: List<ProductExtraUi>,
    selectedExtraIds: List<String>,
    onExtraToggled: (ProductExtraUi) -> Unit,
) {
    if (extras.isEmpty()) return

    Spacer(Modifier.height(8.dp))

    Text(
        text = "Extras opcionais",
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
        fontWeight = FontWeight.Bold,
    )
    Text(
        text = "Adicione cuidados ao serviço escolhido",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        extras.chunked(2).forEach { rowExtras ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                rowExtras.forEach { extra ->
                    BookingExtraCard(
                        extra = extra,
                        selected = extra.id in selectedExtraIds,
                        onSelected = { onExtraToggled(extra) },
                        modifier = Modifier.weight(1f),
                    )
                }
                if (rowExtras.size == 1) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun BookingExtraCard(
    extra: ProductExtraUi,
    selected: Boolean,
    onSelected: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
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
            width = 1.5.dp,
            color = if (selected) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.outlineVariant,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (selected) 5.dp else 3.dp),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Surface(
                    modifier = Modifier.size(42.dp),
                    shape = RoundedCornerShape(12.dp),
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
                        imageVector = extra.icon,
                        contentDescription = null,
                        modifier = Modifier.padding(10.dp),
                    )
                }
                Surface(
                    modifier = Modifier.size(22.dp),
                    shape = CircleShape,
                    color = if (selected) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.surfaceContainer,
                    contentColor = if (selected) MaterialTheme.colorScheme.onTertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                ) {
                    Icon(
                        imageVector = if (selected) Icons.Filled.Check else Icons.Filled.Add,
                        contentDescription = null,
                        modifier = Modifier.padding(5.dp),
                    )
                }
            }
            Text(
                text = extra.name,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
            )
            if (extra.description.isNotBlank()) {
                Text(
                    text = extra.description,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = extra.price,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.tertiary,
                fontWeight = FontWeight.Bold,
            )
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
private fun BookingVehicleStepContent(
    vehiclesState: BookingVehiclesUiState,
    selectedVehicleId: String?,
    onRetryVehicles: () -> Unit,
    onRequestSignIn: () -> Unit,
    onManageVehicles: () -> Unit,
    onVehicleSelected: (BookingVehicleUi) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .offset(y = (-16).dp)
            .padding(top = 0.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "Selecione um veículo guardado ou apenas a categoria para calcular o preço correto",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = 0.78f),
        )

        when (vehiclesState) {
            BookingVehiclesUiState.Idle,
            BookingVehiclesUiState.Loading -> VehicleStateCard(
                title = "A carregar veículos",
                body = "Estamos a consultar os veículos associados à sua conta.",
                icon = Icons.Filled.DirectionsCar,
                loading = true,
            )

            BookingVehiclesUiState.Unauthenticated -> VehicleStateCard(
                title = "Use os seus veículos guardados",
                body = "Entre na conta para escolher um veículo registado nesta marcação.",
                icon = Icons.Filled.Lock,
                actionLabel = "Entrar",
                onAction = onRequestSignIn,
            )

            BookingVehiclesUiState.Empty -> VehicleStateCard(
                title = "Sem veículos guardados",
                body = "Pode adicionar veículos ao perfil para acelerar futuras marcações.",
                icon = Icons.Filled.Add,
                actionLabel = "Gerir veículos",
                onAction = onManageVehicles,
            )

            is BookingVehiclesUiState.Error -> VehicleStateCard(
                title = "Não foi possível carregar veículos",
                body = vehiclesState.message,
                icon = Icons.Filled.Info,
                actionLabel = if (vehiclesState.retryable) "Tentar novamente" else null,
                onAction = if (vehiclesState.retryable) onRetryVehicles else null,
            )

            is BookingVehiclesUiState.Loaded -> {
                VehicleSectionTitle("Veículos guardados")
                vehiclesState.vehicles.forEach { vehicle ->
                    BookingVehicleCard(
                        vehicle = vehicle,
                        selected = selectedVehicleId == vehicle.id,
                        onSelected = { onVehicleSelected(vehicle) },
                    )
                }
            }
        }

        VehicleSectionTitle(
            if (vehiclesState is BookingVehiclesUiState.Loaded) {
                "Ou selecione a categoria"
            } else {
                "Tipo de veículo"
            },
        )

        bookingVehicleCategories.forEach { vehicle ->
            BookingVehicleCard(
                vehicle = vehicle,
                selected = selectedVehicleId == vehicle.id,
                onSelected = { onVehicleSelected(vehicle) },
            )
        }

        VehicleHelpCard()
    }
}

@Composable
private fun VehicleSectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurface,
        fontWeight = FontWeight.Bold,
    )
}

@Composable
private fun VehicleStateCard(
    title: String,
    body: String,
    icon: ImageVector,
    loading: Boolean = false,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
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
                if (loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = MaterialTheme.colorScheme.tertiary,
                        strokeWidth = 2.dp,
                    )
                } else {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(22.dp),
                    )
                }
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

            if (actionLabel != null && onAction != null) {
                OutlinedButton(
                    onClick = onAction,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.tertiary,
                    ),
                ) {
                    Text(actionLabel, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
private fun BookingVehicleCard(
    vehicle: BookingVehicleUi,
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
                    imageVector = vehicle.icon(),
                    contentDescription = null,
                    modifier = Modifier.padding(20.dp),
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                if (vehicle.isDefault) {
                    DefaultBookingVehicleChip()
                }
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
private fun DefaultBookingVehicleChip() {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
    ) {
        Text(
            text = "Predefinido",
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
        )
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
    vehicle: BookingVehicleUi?,
    date: BookingAvailabilityDay?,
    time: String?,
    name: String,
    phone: String,
    email: String,
    notes: String,
    acceptsPrivacy: Boolean,
    loyaltyRewardCode: String,
    selectedExtras: List<ProductExtraUi>,
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
        vehicleType = vehicle.type,
        userVehicleId = vehicle.userVehicleId,
        vehicleLabel = vehicle.vehicleLabel,
        gdprConsent = acceptsPrivacy,
        notes = notes,
        loyaltyRewardCode = loyaltyRewardCode.trim().takeIf { it.isNotBlank() },
        extraIds = selectedExtras.map { it.id },
    )
}

internal fun resolveInitialServiceId(
    initialServiceId: String?,
    serviceIds: List<String>,
): String? {
    val normalizedServiceId = initialServiceId.normalizedInitialServiceId() ?: return null
    return serviceIds.firstOrNull { it == normalizedServiceId }
}

private fun String?.normalizedInitialServiceId(): String? = this
    ?.trim()
    ?.takeIf { it.isNotBlank() }

private fun ProductServiceUi.priceCentsForVehicle(vehicleType: String?): Int {
    return if (vehicleType == "suv") suvPriceCents else passengerPriceCents
}

private fun List<ProductExtraUi>.countLabel(): String {
    return if (size == 1) "1 extra" else "$size extras"
}

private fun BookingVehicleUi.icon(): ImageVector {
    return if (type == "suv") Icons.Filled.AirportShuttle else Icons.Filled.DirectionsCar
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
