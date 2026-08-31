package com.sudsmobile.feature.products

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.sudsmobile.data.booking.BookingSelectionPreset
import com.sudsmobile.data.booking.BookingWaitlistEntry
import com.sudsmobile.data.booking.toSelectionPreset
import com.sudsmobile.shared.theme.SudsColors
import com.sudsmobile.shared.theme.SudsShapes
import com.sudsmobile.shared.theme.SudsSpacing

@Composable
internal fun BookingServiceStepContent(
    catalogState: ProductCatalogUiState,
    presetsState: BookingPresetsUiState,
    presetMutationState: BookingPresetMutationUiState,
    selectedServiceId: String?,
    selectedExtraIds: List<String>,
    unavailableInitialServiceId: String?,
    onRetryCatalog: () -> Unit,
    onRetryPresets: () -> Unit,
    onPresetSelected: (BookingSelectionPreset) -> Unit,
    onDeletePreset: (String) -> Unit,
    onDismissPresetMutation: () -> Unit,
    onServiceSelected: (ProductServiceUi) -> Unit,
    onExtraToggled: (ProductExtraUi) -> Unit,
) {
    var selectedFilterName by rememberSaveable { mutableStateOf(BookingServiceFilter.All.name) }
    val selectedFilter = BookingServiceFilter.entries
        .firstOrNull { it.name == selectedFilterName }
        ?: BookingServiceFilter.All

    BookingServiceFilters(
        selected = selectedFilter,
        onSelected = { selectedFilterName = it.name },
    )

    BookingFavoritePresetsSection(
        presetsState = presetsState,
        mutationState = presetMutationState,
        onRetry = onRetryPresets,
        onSelected = { onPresetSelected(it.toSelectionPreset()) },
        onDelete = onDeletePreset,
        onDismissMutation = onDismissPresetMutation,
    )

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
            catalogState.services.filteredBy(selectedFilter).forEach { service ->
                key(service.id) {
                    BookingServiceCard(
                        service = service,
                        selected = selectedServiceId == service.id,
                        onSelected = { onServiceSelected(service) },
                    )
                }
            }
            BookingExtrasSelectionSection(
                extras = catalogState.extras.filter { it.isEligibleFor(selectedServiceId) },
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
private fun BookingServiceFilters(
    selected: BookingServiceFilter,
    onSelected: (BookingServiceFilter) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(SudsSpacing.sm),
    ) {
        BookingServiceFilter.entries.forEach { filter ->
            val isSelected = filter == selected
            Surface(
                modifier = Modifier
                    .height(44.dp)
                    .clickable { onSelected(filter) },
                shape = SudsShapes.capsule,
                color = if (isSelected) SudsColors.cyan.copy(alpha = 0.12f) else SudsColors.transparent,
                contentColor = if (isSelected) SudsColors.onBrand else SudsColors.onBrandMuted,
                border = BorderStroke(
                    width = if (isSelected) 2.dp else SudsSpacing.hairline,
                    color = if (isSelected) SudsColors.cyan else SudsColors.glassBorder,
                ),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = SudsSpacing.lg),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = filter.label,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}

@Composable
internal fun BookingVehicleStepContent(
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
            .padding(top = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "Selecione um veículo guardado ou apenas a categoria para calcular o preço correto",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                    key(vehicle.id) {
                        BookingVehicleCard(
                            vehicle = vehicle,
                            selected = selectedVehicleId == vehicle.id,
                            onSelected = { onVehicleSelected(vehicle) },
                        )
                    }
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
            key(vehicle.id) {
                BookingVehicleCard(
                    vehicle = vehicle,
                    selected = selectedVehicleId == vehicle.id,
                    onSelected = { onVehicleSelected(vehicle) },
                )
            }
        }
        VehicleHelpCard()
    }
}

@Composable
internal fun DateTimeStepContent(
    availabilityState: BookingAvailabilityUiState,
    waitlistState: BookingWaitlistUiState,
    serviceId: String,
    serviceName: String,
    serviceDurationMinutes: Int,
    selectedDateId: String?,
    selectedTime: String?,
    onDateSelected: (String) -> Unit,
    onTimeSelected: (String) -> Unit,
    onRetryAvailability: () -> Unit,
    onJoinWaitlist: (String) -> Unit,
    onCancelWaitlist: (BookingWaitlistEntry) -> Unit,
    onRequestSignIn: () -> Unit,
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
    val activeWaitlistEntry = waitlistState.entries.firstOrNull { entry ->
        entry.status.equals("active", ignoreCase = true) &&
            entry.dateId == selectedDateId &&
            entry.serviceId == serviceId &&
            entry.serviceDurationMinutes == serviceDurationMinutes
    }
    val currentMonthAnchor = month?.monthAnchorDate()
    val canNavigatePrevious = currentMonthAnchor != null &&
        minimumMonthAnchor != null &&
        currentMonthAnchor > minimumMonthAnchor

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(top = 20.dp),
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
                if (selectedDay?.available == false && selectedDay.waitlistEligible) {
                    WaitlistAlertCard(
                        dateLabel = selectedDay.summaryLabel,
                        serviceName = serviceName,
                        state = waitlistState,
                        activeEntry = activeWaitlistEntry,
                        onJoin = { onJoinWaitlist(selectedDay.id) },
                        onCancel = onCancelWaitlist,
                        onRequestSignIn = onRequestSignIn,
                    )
                }
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
                if (selectedDay?.waitlistEligible == true) {
                    WaitlistAlertCard(
                        dateLabel = selectedDay.summaryLabel,
                        serviceName = serviceName,
                        state = waitlistState,
                        activeEntry = activeWaitlistEntry,
                        onJoin = { onJoinWaitlist(selectedDay.id) },
                        onCancel = onCancelWaitlist,
                        onRequestSignIn = onRequestSignIn,
                    )
                } else {
                    AvailabilityStatusCard(
                        title = "Sem horários disponíveis",
                        body = "Escolha um dia útil para receber um aviso se surgir uma vaga em ${availabilityState.month.monthTitle}.",
                        onRetry = onRetryAvailability,
                    )
                }
            }

            is BookingAvailabilityUiState.Error -> AvailabilityStatusCard(
                title = "Não foi possível carregar horários",
                body = availabilityState.message,
                onRetry = onRetryAvailability,
            )
        }
    }
}

@Composable
internal fun BookingContactContent(
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
            .padding(top = 20.dp),
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
