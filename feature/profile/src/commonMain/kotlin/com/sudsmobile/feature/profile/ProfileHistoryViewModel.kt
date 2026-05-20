package com.sudsmobile.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sudsmobile.data.auth.AuthRepository
import com.sudsmobile.data.auth.AuthSessionState
import com.sudsmobile.data.booking.BookingHistory
import com.sudsmobile.data.booking.BookingHistoryError
import com.sudsmobile.data.booking.BookingHistoryReservation
import com.sudsmobile.data.booking.BookingHistoryResult
import com.sudsmobile.data.booking.BookingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal data class ProfileHistorySummaryUi(
    val washCount: String,
    val totalSpent: String,
)

internal enum class ProfileHistoryStatusUi(val label: String) {
    Completed("Concluído"),
    Cancelled("Cancelado"),
    Past("Concluído"),
}

internal data class ProfileHistoryItemUi(
    val id: String,
    val service: String,
    val date: String,
    val vehicle: String,
    val price: String,
    val priceCents: Int?,
    val status: ProfileHistoryStatusUi,
)

internal sealed interface ProfileHistoryUiState {
    data object Idle : ProfileHistoryUiState
    data object Loading : ProfileHistoryUiState
    data object Unauthenticated : ProfileHistoryUiState
    data object Empty : ProfileHistoryUiState
    data class Loaded(
        val summary: ProfileHistorySummaryUi,
        val items: List<ProfileHistoryItemUi>,
    ) : ProfileHistoryUiState
    data class Error(val message: String, val retryable: Boolean) : ProfileHistoryUiState
}

internal class ProfileHistoryViewModel(
    private val bookingRepository: BookingRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {
    val sessionState: StateFlow<AuthSessionState> = authRepository.sessionState
    private val _uiState = MutableStateFlow<ProfileHistoryUiState>(ProfileHistoryUiState.Idle)
    val uiState: StateFlow<ProfileHistoryUiState> = _uiState.asStateFlow()
    private var loadedUid: String? = null

    fun refreshForSession() {
        val session = sessionState.value as? AuthSessionState.Authenticated
        if (session == null) {
            loadedUid = null
            _uiState.value = ProfileHistoryUiState.Unauthenticated
            return
        }

        val uid = session.session.user.uid
        if (loadedUid == uid && _uiState.value is ProfileHistoryUiState.Loaded) return
        loadHistory()
    }

    fun loadHistory() {
        if (_uiState.value is ProfileHistoryUiState.Loading) return

        val session = sessionState.value as? AuthSessionState.Authenticated
        if (session == null) {
            loadedUid = null
            _uiState.value = ProfileHistoryUiState.Unauthenticated
            return
        }
        val requestedUid = session.session.user.uid

        viewModelScope.launch {
            _uiState.value = ProfileHistoryUiState.Loading
            val nextState = when (val result = bookingRepository.getMyBookings()) {
                is BookingHistoryResult.Success -> result.history.toUiState()
                is BookingHistoryResult.Failure -> result.error.toUiState()
            }
            val currentUid = (sessionState.value as? AuthSessionState.Authenticated)?.session?.user?.uid
            if (currentUid == requestedUid) {
                loadedUid = requestedUid
                _uiState.value = nextState
            } else {
                loadedUid = null
                _uiState.value = ProfileHistoryUiState.Unauthenticated
            }
        }
    }

    private fun BookingHistory.toUiState(): ProfileHistoryUiState {
        val items = reservations
            .filterNot { it.upcoming }
            .filterNot { it.isCancelled() }
            .mapNotNull { it.toHistoryItemOrNull() }

        if (items.isEmpty()) return ProfileHistoryUiState.Empty

        val totalCents = items.sumOf { it.priceCents ?: 0 }
        return ProfileHistoryUiState.Loaded(
            summary = ProfileHistorySummaryUi(
                washCount = items.size.toString(),
                totalSpent = totalCents.toEuroLabel(),
            ),
            items = items,
        )
    }

    private fun BookingHistoryError.toUiState(): ProfileHistoryUiState {
        return when (this) {
            is BookingHistoryError.Unauthenticated -> ProfileHistoryUiState.Unauthenticated
            is BookingHistoryError.Permission -> ProfileHistoryUiState.Error(message = message, retryable = false)
            is BookingHistoryError.Unavailable,
            is BookingHistoryError.Backend -> ProfileHistoryUiState.Error(message = message, retryable = true)
        }
    }
}

private fun BookingHistoryReservation.toHistoryItemOrNull(): ProfileHistoryItemUi? {
    if (id.isBlank() || slotStartIso.isBlank()) return null

    return ProfileHistoryItemUi(
        id = id,
        service = serviceName.ifBlank { "Serviço" },
        date = slotStartIso.toDateLabel(),
        vehicle = vehicleLabel?.takeIf { it.isNotBlank() } ?: vehicleType.toVehicleLabel(),
        price = priceCents?.toEuroLabel() ?: "A confirmar",
        priceCents = priceCents,
        status = status.toHistoryStatusUi(),
    )
}

private fun BookingHistoryReservation.isCancelled(): Boolean {
    val normalized = status.lowercase()
    return normalized in setOf("cancelled", "canceled", "cancelado")
}

private fun String.toHistoryStatusUi(): ProfileHistoryStatusUi {
    val normalized = lowercase()
    return when {
        normalized in setOf("cancelled", "canceled", "cancelado") -> ProfileHistoryStatusUi.Cancelled
        normalized in setOf("completed", "concluido", "concluído", "complete", "done") ->
            ProfileHistoryStatusUi.Completed
        else -> ProfileHistoryStatusUi.Past
    }
}

private fun String.toVehicleLabel(): String = when (lowercase()) {
    "suv" -> "SUV"
    "passageiros", "passenger" -> "Passageiros"
    else -> replaceFirstChar { it.titlecase() }
}

private fun String.toDateLabel(): String {
    val date = substringBefore("T")
    val parts = date.split("-")
    if (parts.size != 3) return date.ifBlank { "Data a confirmar" }
    val year = parts[0]
    val month = parts[1].toIntOrNull()?.let { monthNames.getOrNull(it - 1) } ?: return date
    val day = parts[2].toIntOrNull()?.toString() ?: parts[2]
    return "$day de $month, $year"
}

private fun Int.toEuroLabel(): String {
    val euros = this / 100
    val remainder = this % 100
    return "$euros,${remainder.toString().padStart(2, '0')}€"
}

private val monthNames = listOf(
    "janeiro",
    "fevereiro",
    "março",
    "abril",
    "maio",
    "junho",
    "julho",
    "agosto",
    "setembro",
    "outubro",
    "novembro",
    "dezembro",
)
