package com.sudsmobile.data.booking

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

interface BookingChangeNotifier {
    val revision: StateFlow<Long>
}

class MutableBookingChangeNotifier : BookingChangeNotifier {
    private val mutableRevision = MutableStateFlow(0L)
    override val revision: StateFlow<Long> = mutableRevision.asStateFlow()

    fun notifyBookingsChanged() {
        mutableRevision.update { current -> current + 1L }
    }
}
