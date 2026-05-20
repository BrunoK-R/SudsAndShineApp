package com.sudsmobile.data.vehicle

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

interface UserVehicleChangeNotifier {
    val revision: StateFlow<Long>
}

class MutableUserVehicleChangeNotifier : UserVehicleChangeNotifier {
    private val mutableRevision = MutableStateFlow(0L)
    override val revision: StateFlow<Long> = mutableRevision.asStateFlow()

    fun notifyVehiclesChanged() {
        mutableRevision.update { current -> current + 1L }
    }
}
