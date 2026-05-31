package com.sudsmobile.navigation

object Routes {
    const val Onboarding = "onboarding"
    const val Auth = "auth"
    const val Main = "main"
    const val Home = "home"
    const val Services = "services"
    const val Products = "products"
    const val Cart = "cart"
    const val RatingReservationIdArg = "reservationId"
    const val Rating = "rating/{$RatingReservationIdArg}"
    const val PaymentReservationIdArg = "reservationId"
    const val PaymentReservation = "payment/{$PaymentReservationIdArg}"
    const val Profile = "profile"
    const val AdminBookings = "admin_bookings"
    const val AdminBusinessInfo = "admin_business_info"
    const val PersonalData = "personal_data"
    const val Vehicles = "vehicles"
    const val History = "history"
    const val Contact = "contact"
    const val Loyalty = "loyalty"
    const val Blog = "blog"
    const val Payment = "payment"

    fun rating(reservationId: String): String = "rating/$reservationId"
    fun payment(reservationId: String): String = "payment/$reservationId"
}
