package com.sudsmobile.feature.products

internal enum class BookingStep {
    Service,
    Vehicle,
    DateTime,
    Contact,
    Confirmation,
    Success,
}

internal enum class BookingStepDirection {
    Forward,
    Backward,
    None,
}

internal fun bookingProgressIndex(step: BookingStep): Int? = when (step) {
    BookingStep.Service -> 0
    BookingStep.Vehicle -> 1
    BookingStep.DateTime -> 2
    BookingStep.Contact -> 3
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
