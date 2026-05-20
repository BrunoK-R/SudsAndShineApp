package com.sudsmobile.data.booking

interface BookingFunctionsApi {
    suspend fun getAvailability(request: BookingAvailabilityRequest): BookingAvailabilityResult
    suspend fun createReservation(request: BookingCreateRequest): BookingCreateResult
}
