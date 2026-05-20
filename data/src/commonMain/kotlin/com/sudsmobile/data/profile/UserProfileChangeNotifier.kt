package com.sudsmobile.data.profile

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

interface UserProfileChangeNotifier {
    val revision: StateFlow<Long>
}

class MutableUserProfileChangeNotifier : UserProfileChangeNotifier {
    private val mutableRevision = MutableStateFlow(0L)
    override val revision: StateFlow<Long> = mutableRevision.asStateFlow()

    fun notifyProfileChanged() {
        mutableRevision.update { current -> current + 1L }
    }
}
