package com.sudsmobile.data.booking

interface BookingFunctionsApi {
    suspend fun createReservation(request: BookingCreateRequest): BookingCreateResult
}
