package com.sudsmobile.feature.products

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sudsmobile.data.auth.AuthSessionState
import com.sudsmobile.data.booking.BookingAvailabilityDay
import com.sudsmobile.data.booking.BookingAvailabilityMonth
import com.sudsmobile.data.booking.BookingAvailabilitySlot
import com.sudsmobile.data.booking.BookingPaymentStatus
import com.sudsmobile.data.booking.BookingPreset
import com.sudsmobile.data.booking.BookingPresetUpsertRequest
import com.sudsmobile.data.booking.BookingReservationStatus
import com.sudsmobile.data.booking.BookingSelectionPreset
import com.sudsmobile.data.booking.BookingWaitlistEntry
import com.sudsmobile.data.booking.BookingWaitlistJoinRequest
import com.sudsmobile.data.booking.toBookingPaymentStatus
import com.sudsmobile.data.booking.toBookingReservationStatus
import com.sudsmobile.shared.theme.LocalSudsMotionPreferences
import com.sudsmobile.shared.theme.SudsColors
import com.sudsmobile.shared.theme.SudsSpacing
import com.sudsmobile.shared.ui.SudsBrandBackground
import org.koin.compose.viewmodel.koinViewModel

internal val bookingVehicleCategories = listOf(
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
    visualFixtureEnabled: Boolean = false,
    initialServiceId: String? = null,
    initialSelectionPreset: BookingSelectionPreset? = null,
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
    val waitlistState by viewModel.waitlistState.collectAsStateWithLifecycle()
    val presetsState by viewModel.presetsState.collectAsStateWithLifecycle()
    val presetMutationState by viewModel.presetMutationState.collectAsStateWithLifecycle()
    val sessionState by viewModel.sessionState.collectAsStateWithLifecycle()
    val catalogState by catalogViewModel.catalogState.collectAsStateWithLifecycle()

    LaunchedEffect(visualFixtureEnabled) {
        if (!visualFixtureEnabled) catalogViewModel.loadCatalog()
    }

    val renderedCatalogState = if (visualFixtureEnabled) {
        bookingPixelReferenceCatalog()
    } else {
        catalogState
    }

    ProductsScreenContent(
        contentPadding = contentPadding,
        initialServiceId = initialServiceId,
        initialSelectionPreset = initialSelectionPreset,
        initialServiceRequestKey = initialServiceRequestKey,
        catalogState = renderedCatalogState,
        vehiclesState = vehiclesState,
        vehicleRevision = vehicleRevision,
        bookingRevision = bookingRevision,
        contactProfileState = contactProfileState,
        businessInfoState = businessInfoState,
        rewardsState = rewardsState,
        waitlistState = waitlistState,
        presetsState = presetsState,
        presetMutationState = presetMutationState,
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
        onRefreshWaitlistForSession = viewModel::refreshWaitlistForSession,
        onLoadAvailability = viewModel::loadAvailability,
        onSubmitBooking = viewModel::submitBooking,
        onRefreshSubmitForSession = viewModel::refreshSubmitForSession,
        onClearSubmitError = viewModel::clearSubmitError,
        onSubmitSuccessConsumed = viewModel::consumeSuccess,
        onJoinWaitlist = viewModel::joinWaitlist,
        onCancelWaitlist = viewModel::cancelWaitlist,
        onRefreshPresetsForSession = viewModel::refreshPresetsForSession,
        onLoadPresets = viewModel::loadPresets,
        onSavePreset = viewModel::savePreset,
        onDeletePreset = viewModel::deletePreset,
        onDismissPresetMutation = viewModel::clearPresetMutationState,
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
    initialSelectionPreset: BookingSelectionPreset?,
    initialServiceRequestKey: Long,
    catalogState: ProductCatalogUiState,
    vehiclesState: BookingVehiclesUiState,
    vehicleRevision: Long,
    bookingRevision: Long,
    contactProfileState: BookingContactProfileUiState,
    businessInfoState: BookingBusinessInfoUiState,
    rewardsState: BookingRewardsUiState,
    waitlistState: BookingWaitlistUiState,
    presetsState: BookingPresetsUiState,
    presetMutationState: BookingPresetMutationUiState,
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
    onRefreshWaitlistForSession: () -> Unit,
    onLoadAvailability: (Int, String?) -> Unit,
    onSubmitBooking: (ProductsBookingDraft?) -> Unit,
    onRefreshSubmitForSession: () -> Unit,
    onClearSubmitError: () -> Unit,
    onSubmitSuccessConsumed: () -> Unit,
    onJoinWaitlist: (BookingWaitlistJoinRequest) -> Unit,
    onCancelWaitlist: (BookingWaitlistEntry) -> Unit,
    onRefreshPresetsForSession: (Boolean) -> Unit,
    onLoadPresets: () -> Unit,
    onSavePreset: (BookingPresetUpsertRequest) -> Unit,
    onDeletePreset: (String) -> Unit,
    onDismissPresetMutation: () -> Unit,
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
    var successReservationStatus by rememberSaveable { mutableStateOf("") }
    var successPendingExpiresAt by rememberSaveable { mutableStateOf<String?>(null) }
    var appliedInitialServiceRequestKey by rememberSaveable { mutableStateOf<Long?>(null) }
    var unavailableInitialServiceId by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingPresetVehicleId by rememberSaveable { mutableStateOf<String?>(null) }
    val currentStep = BookingStep.valueOf(currentStepName)
    val contactFormValid = contactName.isNotBlank() &&
        contactPhone.trim().length >= 6 &&
        contactEmail.trim().contains("@") &&
        acceptsPrivacy
    val loadedServices = (catalogState as? ProductCatalogUiState.Loaded)?.services.orEmpty()
    val loadedExtras = (catalogState as? ProductCatalogUiState.Loaded)?.extras.orEmpty()
    val selectedService = loadedServices.firstOrNull { it.id == selectedServiceId }
    val eligibleExtras = loadedExtras.filter { extra -> extra.isEligibleFor(selectedServiceId) }
    val selectedExtras = eligibleExtras.filter { extra -> extra.id in selectedExtraIds }
    val savedVehicles = (vehiclesState as? BookingVehiclesUiState.Loaded)?.vehicles.orEmpty()
    val vehicleOptions = savedVehicles + bookingVehicleCategories
    val selectedVehicle = vehicleOptions.firstOrNull { it.id == selectedVehicleId }
    val reduceMotion = LocalSudsMotionPreferences.current.reduceMotion
    val hapticFeedback = LocalHapticFeedback.current
    val stepDistancePx = with(LocalDensity.current) { 24.dp.roundToPx() }
    val selectionPriceLabel = selectedService?.let { service ->
        bookingSelectionPriceLabel(
            passengerPriceLabel = service.passengerPrice,
            passengerPriceCents = service.passengerPriceCents,
            suvPriceCents = service.suvPriceCents,
            vehicleType = selectedVehicle?.type,
            extrasPriceCents = selectedExtras.sumOf { extra -> extra.priceCents },
        )
    }
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

    fun applySelectionPreset(preset: BookingSelectionPreset) {
        val service = loadedServices.firstOrNull { it.id == preset.serviceId } ?: run {
            unavailableInitialServiceId = preset.serviceId
            currentStepName = BookingStep.Service.name
            return
        }
        val eligibleExtraIds = loadedExtras
            .filter { it.isEligibleFor(service.id) }
            .map { it.id }
            .toSet()
        selectedServiceId = service.id
        selectedExtraIds = preset.extraIds.filter { it in eligibleExtraIds }.distinct()
        selectedDateId = null
        selectedTime = null
        availabilityAnchorDate = null
        minimumAvailabilityMonthAnchor = null
        unavailableInitialServiceId = null
        val savedVehicleId = preset.userVehicleId?.trim()?.takeIf { it.isNotBlank() }
        if (savedVehicleId != null) {
            val selectionId = "saved:$savedVehicleId"
            selectedVehicleId = selectionId
            pendingPresetVehicleId = selectionId
            currentStepName = BookingStep.Vehicle.name
        } else {
            selectedVehicleId = if (preset.vehicleType == "suv") "suv" else "passenger"
            pendingPresetVehicleId = null
            currentStepName = BookingStep.DateTime.name
        }
        onClearSubmitError()
    }

    LaunchedEffect(submitState) {
        val state = submitState
        if (state is BookingSubmitUiState.Success) {
            reservationId = state.receipt.reservationId.takeIf { it.isNotBlank() }
            reservationCode = state.receipt.reservationCode
            successLoyaltyRewardApplied = state.receipt.loyaltyRewardApplied
            successLoyaltyRewardCode = state.receipt.loyaltyRewardCode
            successPaymentStatus = state.receipt.paymentStatus
            successReservationStatus = state.receipt.status
            successPendingExpiresAt = state.receipt.pendingExpiresAtIso
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

    LaunchedEffect(sessionState) {
        onRefreshPresetsForSession(false)
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
            onRefreshSubmitForSession()
        }
    }

    LaunchedEffect(currentStep, sessionState) {
        if (currentStep == BookingStep.DateTime) {
            onRefreshWaitlistForSession()
        }
    }

    LaunchedEffect(catalogState, selectedServiceId) {
        if (catalogState is ProductCatalogUiState.Loaded &&
            selectedServiceId != null &&
            catalogState.services.none { it.id == selectedServiceId }
        ) {
            selectedServiceId = null
            selectedDateId = null
            selectedTime = null
        }
        if (catalogState is ProductCatalogUiState.Loaded) {
            val availableExtraIds = catalogState.extras
                .filter { it.isEligibleFor(selectedServiceId) }
                .map { it.id }
                .toSet()
            val validSelectedExtraIds = selectedExtraIds.filter { it in availableExtraIds }
            if (validSelectedExtraIds.size != selectedExtraIds.size) {
                selectedExtraIds = validSelectedExtraIds
            }
        }
    }

    LaunchedEffect(catalogState, initialServiceId, initialSelectionPreset) {
        val loadedCatalog = catalogState as? ProductCatalogUiState.Loaded ?: return@LaunchedEffect
        if (
            selectedServiceId == null &&
            initialServiceId.normalizedInitialServiceId() == null &&
            initialSelectionPreset == null &&
            currentStep == BookingStep.Service
        ) {
            selectedServiceId = preferredBookingServiceId(loadedCatalog.services)
        }
    }

    LaunchedEffect(initialServiceId, initialSelectionPreset, initialServiceRequestKey, catalogState) {
        val loadedCatalog = catalogState as? ProductCatalogUiState.Loaded ?: return@LaunchedEffect
        if (appliedInitialServiceRequestKey == initialServiceRequestKey) return@LaunchedEffect

        val requestedPreset = initialSelectionPreset
        if (requestedPreset != null) {
            appliedInitialServiceRequestKey = initialServiceRequestKey
            applySelectionPreset(requestedPreset)
            return@LaunchedEffect
        }

        val requestedServiceId = initialServiceId.normalizedInitialServiceId() ?: return@LaunchedEffect

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

        val selectedDateStillEligible = days.any { day ->
            day.id == selectedDateId && (day.available || day.waitlistEligible)
        }
        if (!selectedDateStillEligible) {
            selectedDateId = days.firstOrNull { it.available }?.id
            selectedTime = null
        }
    }

    LaunchedEffect(vehiclesState) {
        if (vehiclesState is BookingVehiclesUiState.Idle || vehiclesState is BookingVehiclesUiState.Loading) {
            return@LaunchedEffect
        }
        val defaultVehicleId = savedVehicles.firstOrNull { it.isDefault }?.id
        if (selectedVehicleId != null && vehicleOptions.none { it.id == selectedVehicleId }) {
            selectedVehicleId = defaultVehicleId
        } else if (selectedVehicleId == null) {
            selectedVehicleId = defaultVehicleId
        }
        val requestedVehicleId = pendingPresetVehicleId
        if (requestedVehicleId != null) {
            pendingPresetVehicleId = null
            if (vehicleOptions.any { it.id == requestedVehicleId }) {
                selectedVehicleId = requestedVehicleId
                currentStepName = BookingStep.DateTime.name
            }
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

    BookingTheme {
        SudsBrandBackground(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(SudsColors.ink.copy(alpha = 0.38f)),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(bottom = contentPadding.calculateBottomPadding() + 176.dp),
                ) {
                    AnimatedContent(
                        targetState = currentStep,
                        transitionSpec = {
                            bookingStepTransition(
                                from = initialState,
                                to = targetState,
                                reduceMotion = reduceMotion,
                                distancePx = stepDistancePx,
                            )
                        },
                        contentKey = { step -> step.name },
                        label = "booking step content",
                    ) { animatedStep ->
                        Column(modifier = Modifier.fillMaxWidth()) {
                            when (animatedStep) {
                                BookingStep.Service -> {
                                    BookingServiceHeader(onBack = onBack)

                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = SudsSpacing.contentGutter)
                                            .padding(top = SudsSpacing.xs),
                                        verticalArrangement = Arrangement.spacedBy(12.dp),
                                    ) {
                                        BookingServiceStepContent(
                                            catalogState = catalogState,
                                            presetsState = presetsState,
                                            presetMutationState = presetMutationState,
                                            selectedServiceId = selectedServiceId,
                                            selectedExtraIds = selectedExtraIds,
                                            unavailableInitialServiceId = unavailableInitialServiceId,
                                            onRetryCatalog = onLoadCatalog,
                                            onRetryPresets = onLoadPresets,
                                            onPresetSelected = ::applySelectionPreset,
                                            onDeletePreset = onDeletePreset,
                                            onDismissPresetMutation = onDismissPresetMutation,
                                            onServiceSelected = { service ->
                                                hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
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
                                                hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
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
                                            hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
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
                                        waitlistState = waitlistState,
                                        serviceId = selectedService?.id.orEmpty(),
                                        serviceName = selectedService?.name.orEmpty(),
                                        serviceDurationMinutes = selectedService?.durationMinutes ?: 0,
                                        selectedDateId = selectedDateId,
                                        selectedTime = selectedTime,
                                        onDateSelected = { dateId ->
                                            selectedDateId = dateId
                                            selectedTime = null
                                            onClearSubmitError()
                                        },
                                        onTimeSelected = {
                                            hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            selectedTime = it
                                            onClearSubmitError()
                                        },
                                        onRetryAvailability = {
                                            selectedService?.let {
                                                onLoadAvailability(it.durationMinutes, availabilityAnchorDate)
                                            }
                                        },
                                        onJoinWaitlist = { dateId ->
                                            selectedService?.let { service ->
                                                onJoinWaitlist(
                                                    BookingWaitlistJoinRequest(
                                                        dateId = dateId,
                                                        serviceId = service.id,
                                                        serviceName = service.name,
                                                        serviceDurationMinutes = service.durationMinutes,
                                                    ),
                                                )
                                            }
                                        },
                                        onCancelWaitlist = onCancelWaitlist,
                                        onRequestSignIn = onRequestSignIn,
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
                                        presetsState = presetsState,
                                        presetMutationState = presetMutationState,
                                        onRetryRewards = onLoadRewards,
                                        onSavePreset = onSavePreset,
                                        onDismissPresetMutation = onDismissPresetMutation,
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
                                        reservationStatus = successReservationStatus,
                                        pendingExpiresAtIso = successPendingExpiresAt,
                                        onAddToCalendar = {},
                                        onViewBooking = onViewBooking,
                                        onHome = onHome,
                                        onOpenPayment = onOpenPayment,
                                    )
                                }
                            }
                        }
                    }
                }

                if (currentStep != BookingStep.Success) {
                    ContinueBar(
                        enabled = isBookingContinueEnabled(
                            step = currentStep,
                            hasService = selectedService != null,
                            hasVehicleSelection = selectedVehicleId != null,
                            hasResolvedVehicle = selectedVehicle != null,
                            hasDate = selectedDate != null,
                            hasTime = selectedTime != null,
                            contactFormValid = contactFormValid,
                            submissionLoading = submitState is BookingSubmitUiState.Loading,
                        ),
                        onClick = {
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
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
                                "A enviar pedido..."
                            } else if (submitState is BookingSubmitUiState.Error) {
                                submitState.resolution.continueLabel()
                            } else {
                                "Enviar pedido"
                            }
                            else -> "Continuar"
                        },
                        summaryTitle = if (currentStep == BookingStep.Confirmation) {
                            "Rever e enviar"
                        } else {
                            selectedService?.name
                        },
                        summaryDetail = when {
                            currentStep == BookingStep.Confirmation -> null
                            currentStep == BookingStep.Service && selectedService != null -> {
                                "${selectedService.durationLabel} · ${selectedService.passengerPrice}"
                            }
                            else -> selectionPriceLabel
                        },
                        contentPadding = contentPadding,
                        modifier = Modifier.align(Alignment.BottomCenter),
                    )
                }
            }
        }
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
    presetsState: BookingPresetsUiState,
    presetMutationState: BookingPresetMutationUiState,
    onRetryRewards: () -> Unit,
    onSavePreset: (BookingPresetUpsertRequest) -> Unit,
    onDismissPresetMutation: () -> Unit,
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
            .padding(top = 20.dp),
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

        BookingFavoriteSaveCard(
            service = service,
            selectedExtras = selectedExtras,
            vehicle = vehicle,
            sessionState = sessionState,
            presetsState = presetsState,
            mutationState = presetMutationState,
            onRequestSignIn = onRequestSignIn,
            onSave = onSavePreset,
            onDismissMutation = onDismissPresetMutation,
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
private fun BookingFavoriteSaveCard(
    service: ProductServiceUi?,
    selectedExtras: List<ProductExtraUi>,
    vehicle: BookingVehicleUi?,
    sessionState: AuthSessionState,
    presetsState: BookingPresetsUiState,
    mutationState: BookingPresetMutationUiState,
    onRequestSignIn: () -> Unit,
    onSave: (BookingPresetUpsertRequest) -> Unit,
    onDismissMutation: () -> Unit,
) {
    if (service == null || vehicle == null) return
    val presets = (presetsState as? BookingPresetsUiState.Loaded)?.presets.orEmpty()
    val currentExtraIds = selectedExtras.map { it.id }
    val alreadySaved = presets.any { preset ->
        preset.serviceId == service.id &&
            preset.extraIds.toSet() == currentExtraIds.toSet() &&
            preset.userVehicleId == vehicle.userVehicleId &&
            preset.vehicleType == vehicle.type
    }
    val maxPresets = when (presetsState) {
        is BookingPresetsUiState.Loaded -> presetsState.maxPresets
        is BookingPresetsUiState.Empty -> presetsState.maxPresets
        else -> 5
    }
    val limitReached = !alreadySaved && presets.size >= maxPresets
    val saving = mutationState is BookingPresetMutationUiState.Saving

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.25f),
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Filled.Star,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(22.dp),
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Text(
                        text = if (alreadySaved) "Guardado nos favoritos" else "Repetir em segundos",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = when {
                            alreadySaved -> "Este serviço, extras e veículo já estão guardados."
                            limitReached -> "Já atingiu o limite de $maxPresets favoritos. Elimine um para guardar este."
                            sessionState !is AuthSessionState.Authenticated ->
                                "Inicie sessão para guardar esta combinação e reutilizá-la noutros dispositivos."
                            else -> "Guarde o serviço, os extras e o veículo. A data e a hora serão sempre escolhidas de novo."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (!alreadySaved && !limitReached) {
                OutlinedButton(
                    onClick = {
                        if (sessionState !is AuthSessionState.Authenticated) {
                            onRequestSignIn()
                        } else {
                            onSave(
                                BookingPresetUpsertRequest(
                                    label = "${service.name} · ${vehicle.name}",
                                    serviceId = service.id,
                                    extraIds = currentExtraIds,
                                    userVehicleId = vehicle.userVehicleId,
                                    vehicleType = vehicle.type,
                                    vehicleLabel = vehicle.vehicleLabel ?: vehicle.name,
                                ),
                            )
                        }
                    },
                    enabled = !saving,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.tertiary),
                ) {
                    if (saving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = MaterialTheme.colorScheme.tertiary,
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(if (saving) "A guardar" else if (sessionState is AuthSessionState.Authenticated) "Guardar favorito" else "Entrar para guardar")
                }
            }

            when (mutationState) {
                is BookingPresetMutationUiState.Success -> BookingPresetMutationMessage(
                    message = mutationState.message,
                    error = false,
                    onDismiss = onDismissMutation,
                )
                is BookingPresetMutationUiState.Error -> BookingPresetMutationMessage(
                    message = mutationState.message,
                    error = true,
                    onDismiss = onDismissMutation,
                )
                BookingPresetMutationUiState.Idle,
                BookingPresetMutationUiState.Saving,
                is BookingPresetMutationUiState.Deleting -> Unit
            }
        }
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
                        authenticated -> "O código é validado na sua conta ao enviar o pedido."
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
                            text = "A enviar pedido",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "Estamos a validar o horário e a enviar o pedido à equipa.",
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
        BookingSubmitResolution.None -> "Enviar pedido"
    }
}

private fun BookingSubmitResolution.errorTitle(): String {
    return when (this) {
        BookingSubmitResolution.ChangeSlot -> "Horário indisponível"
        BookingSubmitResolution.Retry -> "Não foi possível enviar"
        BookingSubmitResolution.SignIn -> "Sessão necessária"
        BookingSubmitResolution.None -> "Não foi possível enviar"
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
                        text = "Valida ao enviar",
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
    reservationStatus: String,
    pendingExpiresAtIso: String?,
    onAddToCalendar: () -> Unit,
    onViewBooking: () -> Unit,
    onHome: () -> Unit,
    onOpenPayment: (String?) -> Unit,
) {
    val pendingValidation = reservationStatus.isPendingValidationStatus()
    val title = if (pendingValidation) "Pedido recebido" else "Marcação Confirmada!"
    val body = if (pendingValidation) {
        "A aguardar validação"
    } else {
        "A sua marcação foi criada com sucesso"
    }

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
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = body,
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

        if (pendingValidation) {
            BookingValidationPendingCard(
                pendingExpiresAtIso = pendingExpiresAtIso,
                loyaltyRewardApplied = loyaltyRewardApplied,
            )
            Spacer(Modifier.height(16.dp))
        } else if (loyaltyRewardApplied) {
            LoyaltyRewardAppliedCard(rewardCode = loyaltyRewardCode)
            Spacer(Modifier.height(16.dp))
        } else if (paymentStatus.requiresPaymentAction()) {
            PaymentPendingCard(onOpenPayment = { onOpenPayment(reservationId) })
            Spacer(Modifier.height(16.dp))
        }

        ConfirmationSentCard(pendingValidation = pendingValidation)

        Spacer(Modifier.height(28.dp))

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedButton(
                onClick = onAddToCalendar,
                enabled = !pendingValidation,
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

private fun String.isPendingValidationStatus(): Boolean {
    return trim().isBlank() || toBookingReservationStatus() == BookingReservationStatus.Pending
}

@Composable
private fun BookingValidationPendingCard(
    pendingExpiresAtIso: String?,
    loyaltyRewardApplied: Boolean,
) {
    val expiryLabel = pendingExpiresAtIso?.toPendingExpiryLabel()
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
                imageVector = Icons.Filled.MarkEmailRead,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(22.dp),
            )
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "A aguardar validação",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = buildString {
                        append("A equipa vai validar o pedido antes da confirmação final.")
                        if (expiryLabel != null) {
                            append(" O lugar fica reservado até ")
                            append(expiryLabel)
                            append(".")
                        }
                        if (loyaltyRewardApplied) {
                            append(" A recompensa de fidelização fica reservada até à decisão.")
                        }
                    },
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
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

private fun String.toPendingExpiryLabel(): String? {
    if (isBlank()) return null
    val date = substringBefore("T")
    val parts = date.split("-")
    if (parts.size != 3) return null
    val time = substringAfter("T", missingDelimiterValue = "").takeIf { it.length >= 5 }?.take(5)
    val day = parts[2].toIntOrNull()?.toString() ?: parts[2]
    val month = parts[1].toIntOrNull()?.toString()?.padStart(2, '0') ?: parts[1]
    val year = parts[0]
    return if (time == null) {
        "$day/$month/$year"
    } else {
        "$day/$month/$year às $time"
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
private fun ConfirmationSentCard(pendingValidation: Boolean) {
    val title = if (pendingValidation) "Pedido enviado" else "Confirmação enviada"
    val body = if (pendingValidation) {
        "Enviámos o pedido à equipa. Receberá uma notificação quando for aceite ou recusado."
    } else {
        "Enviámos a confirmação com todos os detalhes da sua marcação."
    }

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
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
internal fun BookingContactProfileCard(
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
internal fun BookingContactField(
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
internal fun AvailabilityLoadingCard() {
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
internal fun AvailabilityStatusCard(
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
internal fun WaitlistAlertCard(
    dateLabel: String,
    serviceName: String,
    state: BookingWaitlistUiState,
    activeEntry: BookingWaitlistEntry?,
    onJoin: () -> Unit,
    onCancel: (BookingWaitlistEntry) -> Unit,
    onRequestSignIn: () -> Unit,
) {
    val busy = state.busyDateId == activeEntry?.dateId ||
        (activeEntry == null && state.busyDateId != null)
    val active = activeEntry != null
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (active) {
                MaterialTheme.colorScheme.tertiaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainerLowest
            },
        ),
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
                Surface(
                    shape = CircleShape,
                    color = if (active) {
                        MaterialTheme.colorScheme.tertiary
                    } else {
                        MaterialTheme.colorScheme.tertiaryContainer
                    },
                ) {
                    Icon(
                        imageVector = if (active) Icons.Filled.Check else Icons.Filled.NotificationsActive,
                        contentDescription = null,
                        tint = if (active) {
                            MaterialTheme.colorScheme.onTertiary
                        } else {
                            MaterialTheme.colorScheme.onTertiaryContainer
                        },
                        modifier = Modifier.padding(10.dp).size(22.dp),
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = if (active) "Aviso de vaga ativo" else "Avise-me se surgir uma vaga",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (active) {
                            MaterialTheme.colorScheme.onTertiaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                    )
                    Text(
                        text = if (active) {
                            "Enviaremos uma notificação quando houver um horário para $serviceName em $dateLabel. O aviso é enviado uma vez."
                        } else {
                            "Ative um aviso para $dateLabel. Se surgir um horário para $serviceName, recebe uma notificação para reservar na app."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (active) {
                            MaterialTheme.colorScheme.onTertiaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
            }

            state.errorMessage?.let { message ->
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            when {
                state.isLoading -> Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.tertiary,
                    )
                    Text(
                        text = "A confirmar os seus avisos",
                        style = MaterialTheme.typography.labelLarge,
                    )
                }

                state.isUnauthenticated -> Button(
                    onClick = onRequestSignIn,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text("Iniciar sessão para ativar")
                }

                active -> OutlinedButton(
                    onClick = { onCancel(requireNotNull(activeEntry)) },
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary),
                ) {
                    if (busy) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(if (busy) "A cancelar" else "Cancelar aviso")
                }

                else -> Button(
                    onClick = onJoin,
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    if (busy) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(if (busy) "A ativar" else "Ativar aviso de vaga")
                }
            }
        }
    }
}

@Composable
internal fun CalendarSelectionCard(
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
    val selectable = dateOption.available || dateOption.waitlistEligible
    val availabilityDescription = when {
        dateOption.available -> "Horários disponíveis"
        dateOption.waitlistEligible -> "Sem vagas; aviso disponível"
        else -> "Indisponível"
    }
    Surface(
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .semantics {
                stateDescription = availabilityDescription
            }
            .clickable(enabled = selectable, role = Role.Button, onClick = onSelected),
        shape = RoundedCornerShape(12.dp),
        color = when {
            selected -> MaterialTheme.colorScheme.tertiary
            dateOption.available -> MaterialTheme.colorScheme.surfaceContainerLow
            dateOption.waitlistEligible -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.58f)
            else -> MaterialTheme.colorScheme.surfaceContainerHigh
        },
        contentColor = when {
            selected -> MaterialTheme.colorScheme.onTertiary
            dateOption.available -> MaterialTheme.colorScheme.onSurface
            dateOption.waitlistEligible -> MaterialTheme.colorScheme.onTertiaryContainer
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
internal fun TimeSelectionCard(
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
                                key(slot.time) {
                                    TimeSlotButton(
                                        slot = slot,
                                        selected = selectedTime == slot.time,
                                        onSelected = { onTimeSelected(slot.time) },
                                        modifier = Modifier.weight(1f),
                                    )
                                }
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
            .height(60.dp)
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 4.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = slot.time,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
            Text(
                text = slot.capacityLabel(),
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
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
internal fun BookingFavoritePresetsSection(
    presetsState: BookingPresetsUiState,
    mutationState: BookingPresetMutationUiState,
    onRetry: () -> Unit,
    onSelected: (BookingPreset) -> Unit,
    onDelete: (String) -> Unit,
    onDismissMutation: () -> Unit,
) {
    when (presetsState) {
        BookingPresetsUiState.Idle,
        BookingPresetsUiState.Unauthenticated,
        is BookingPresetsUiState.Empty -> Unit

        BookingPresetsUiState.Loading -> Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.30f),
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.tertiary,
                    strokeWidth = 2.dp,
                )
                Text(
                    text = "A carregar marcações favoritas",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        is BookingPresetsUiState.Error -> BookingPresetLoadErrorRow(
            message = presetsState.message,
            onRetry = onRetry,
        )

        is BookingPresetsUiState.Loaded -> {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SectionHeader(icon = Icons.Filled.Star, title = "Os seus favoritos")
                Text(
                    text = "${presetsState.presets.size}/${presetsState.maxPresets}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = "Escolha um favorito para avançar diretamente para a data e hora.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            presetsState.presets.forEach { preset ->
                key(preset.id) {
                    FavoriteBookingPresetCard(
                        preset = preset,
                        deleting = mutationState is BookingPresetMutationUiState.Deleting &&
                            mutationState.presetId == preset.id,
                        onSelected = { onSelected(preset) },
                        onDelete = { onDelete(preset.id) },
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
        }
    }

    when (mutationState) {
        is BookingPresetMutationUiState.Success -> BookingPresetMutationMessage(
            message = mutationState.message,
            error = false,
            onDismiss = onDismissMutation,
        )
        is BookingPresetMutationUiState.Error -> BookingPresetMutationMessage(
            message = mutationState.message,
            error = true,
            onDismiss = onDismissMutation,
        )
        BookingPresetMutationUiState.Idle,
        BookingPresetMutationUiState.Saving,
        is BookingPresetMutationUiState.Deleting -> Unit
    }
}

@Composable
private fun BookingPresetLoadErrorRow(
    message: String,
    onRetry: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    ) {
        Row(
            modifier = Modifier.padding(start = 14.dp, top = 10.dp, bottom = 10.dp, end = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Filled.Star,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.tertiary,
            )
            Text(
                text = message,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            IconButton(onClick = onRetry) {
                Icon(
                    imageVector = Icons.Filled.Refresh,
                    contentDescription = "Tentar carregar favoritos novamente",
                )
            }
        }
    }
}

@Composable
private fun FavoriteBookingPresetCard(
    preset: BookingPreset,
    deleting: Boolean,
    onSelected: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !deleting, onClick = onSelected),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.45f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, top = 14.dp, bottom = 14.dp, end = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.size(44.dp),
                shape = RoundedCornerShape(13.dp),
                color = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
            ) {
                Icon(
                    imageVector = Icons.Filled.Star,
                    contentDescription = null,
                    modifier = Modifier.padding(11.dp),
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    text = preset.label,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                )
                val vehicle = preset.vehicleLabel ?: if (preset.vehicleType == "suv") "SUV" else "Passageiros"
                val extras = if (preset.extraIds.isEmpty()) "Sem extras" else "${preset.extraIds.size} extras"
                Text(
                    text = "$vehicle · $extras",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onDelete, enabled = !deleting) {
                if (deleting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = MaterialTheme.colorScheme.error,
                        strokeWidth = 2.dp,
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.DeleteOutline,
                        contentDescription = "Eliminar ${preset.label}",
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}

@Composable
private fun BookingPresetMutationMessage(
    message: String,
    error: Boolean,
    onDismiss: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = if (error) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer,
        contentColor = if (error) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSecondaryContainer,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = message,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodySmall,
            )
            OutlinedButton(onClick = onDismiss, shape = RoundedCornerShape(10.dp)) {
                Text("Fechar")
            }
        }
    }
}

@Composable
internal fun CatalogLoadingCard() {
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
internal fun VehicleSectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurface,
        fontWeight = FontWeight.Bold,
    )
}

@Composable
internal fun VehicleStateCard(
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
internal fun BookingVehicleCard(
    vehicle: BookingVehicleUi,
    selected: Boolean,
    onSelected: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelected)
            .semantics {
                role = Role.RadioButton
                stateDescription = if (selected) "Selecionado" else "Não selecionado"
            },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        ),
        border = BorderStroke(
            width = 2.dp,
            color = if (selected) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.outlineVariant,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
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
internal fun VehicleHelpCard() {
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

internal fun ProductExtraUi.isEligibleFor(serviceId: String?): Boolean {
    return eligibleServiceIds.isEmpty() || serviceId in eligibleServiceIds
}

internal fun BookingAvailabilitySlot.capacityLabel(): String {
    return when {
        !available && remainingCapacity <= 0 -> "Cheio"
        !available -> "Indisponível"
        remainingCapacity == 1 -> "1 vaga"
        remainingCapacity > 1 -> "$remainingCapacity vagas"
        else -> "Disponível"
    }
}

private fun List<ProductExtraUi>.countLabel(): String {
    return if (size == 1) "1 extra" else "$size extras"
}

private fun BookingVehicleUi.icon(): ImageVector {
    return if (type == "suv") Icons.Filled.AirportShuttle else Icons.Filled.DirectionsCar
}
