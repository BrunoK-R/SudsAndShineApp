package com.sudsmobile.feature.products

internal enum class BookingStep {
    Service,
    Extras,
    Vehicle,
    DateTime,
    Contact,
    Confirmation,
    Success,
}

internal const val BookingProgressStepCount = 5

internal enum class BookingStepDirection {
    Forward,
    Backward,
    None,
}

internal enum class BookingServiceFilter(val label: String) {
    All("Todos"),
    Wash("Lavagem"),
    Detailing("Detailing"),
}

internal fun preferredBookingServiceId(services: List<ProductServiceUi>): String? {
    return services.firstOrNull(ProductServiceUi::popular)?.id ?: services.firstOrNull()?.id
}

internal fun List<ProductServiceUi>.filteredBy(filter: BookingServiceFilter): List<ProductServiceUi> {
    return when (filter) {
        BookingServiceFilter.All -> this
        BookingServiceFilter.Wash -> filterNot(ProductServiceUi::isDetailingService)
        BookingServiceFilter.Detailing -> filter(ProductServiceUi::isDetailingService)
    }
}

private fun ProductServiceUi.isDetailingService(): Boolean {
    val searchable = "$id $name $description".lowercase()
    return listOf("detail", "premium", "acabamento", "ceramic").any(searchable::contains)
}

internal fun bookingProgressIndex(step: BookingStep): Int? = when (step) {
    BookingStep.Service -> 0
    BookingStep.Extras -> 1
    BookingStep.Vehicle -> 2
    BookingStep.DateTime -> 3
    BookingStep.Contact -> 4
    BookingStep.Confirmation,
    BookingStep.Success -> null
}

internal fun bookingStepDirection(
    from: BookingStep,
    to: BookingStep,
): BookingStepDirection = when {
    from == to -> BookingStepDirection.None
    to.ordinal > from.ordinal -> BookingStepDirection.Forward
    else -> BookingStepDirection.Backward
}

internal fun isBookingContinueEnabled(
    step: BookingStep,
    hasService: Boolean,
    hasVehicleSelection: Boolean,
    hasResolvedVehicle: Boolean,
    hasDate: Boolean,
    hasTime: Boolean,
    contactFormValid: Boolean,
    submissionLoading: Boolean,
): Boolean = when (step) {
    BookingStep.Service -> hasService
    BookingStep.Extras -> hasService
    BookingStep.Vehicle -> hasVehicleSelection
    BookingStep.DateTime -> hasDate && hasTime
    BookingStep.Contact -> contactFormValid
    BookingStep.Confirmation -> hasService &&
        hasResolvedVehicle &&
        hasDate &&
        hasTime &&
        contactFormValid &&
        !submissionLoading
    BookingStep.Success -> false
}

internal fun bookingSelectionPriceLabel(
    passengerPriceLabel: String,
    passengerPriceCents: Int,
    suvPriceCents: Int,
    vehicleType: String?,
    extrasPriceCents: Int,
): String = if (vehicleType == null) {
    "A partir de $passengerPriceLabel"
} else {
    val basePriceCents = if (vehicleType == "suv") suvPriceCents else passengerPriceCents
    (basePriceCents + extrasPriceCents).toEuroLabel()
}
