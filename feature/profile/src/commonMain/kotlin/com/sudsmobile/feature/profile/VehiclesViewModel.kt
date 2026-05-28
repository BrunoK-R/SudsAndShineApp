package com.sudsmobile.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sudsmobile.data.auth.AuthError
import com.sudsmobile.data.auth.AuthRepository
import com.sudsmobile.data.auth.AuthSessionState
import com.sudsmobile.data.vehicle.UserVehicle
import com.sudsmobile.data.vehicle.UserVehicleDeleteResult
import com.sudsmobile.data.vehicle.UserVehicleError
import com.sudsmobile.data.vehicle.UserVehicleListResult
import com.sudsmobile.data.vehicle.UserVehicleMutationResult
import com.sudsmobile.data.vehicle.UserVehicleRepository
import com.sudsmobile.data.vehicle.UserVehicleSaveRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal enum class VehicleTypeUi(
    val id: String,
    val label: String,
) {
    Passenger("passenger", "Passageiros"),
    Suv("suv", "SUV"),
}

internal data class VehicleUi(
    val id: String,
    val brand: String,
    val model: String,
    val plate: String,
    val color: String,
    val type: VehicleTypeUi,
    val isDefault: Boolean,
)

internal data class VehicleDraftUi(
    val id: String? = null,
    val brand: String = "",
    val model: String = "",
    val plate: String = "",
    val color: String = "",
    val type: VehicleTypeUi = VehicleTypeUi.Passenger,
    val isDefault: Boolean = false,
) {
    val canSubmit: Boolean
        get() = brand.isNotBlank() && model.isNotBlank() && plate.trim().length >= 2
}

internal sealed interface VehiclesUiState {
    data object Idle : VehiclesUiState
    data object Loading : VehiclesUiState
    data object Unauthenticated : VehiclesUiState
    data object Empty : VehiclesUiState
    data class Loaded(val vehicles: List<VehicleUi>) : VehiclesUiState
    data class Error(val message: String, val retryable: Boolean) : VehiclesUiState
}

internal sealed interface VehicleMutationUiState {
    data object Idle : VehicleMutationUiState
    data object Loading : VehicleMutationUiState
    data class Success(val message: String) : VehicleMutationUiState
    data class ValidationError(val message: String) : VehicleMutationUiState
    data class Error(val message: String, val retryable: Boolean) : VehicleMutationUiState
}

internal class VehiclesViewModel(
    private val authRepository: AuthRepository,
    private val vehicleRepository: UserVehicleRepository,
) : ViewModel() {
    val sessionState: StateFlow<AuthSessionState> = authRepository.sessionState
    private val _uiState = MutableStateFlow<VehiclesUiState>(VehiclesUiState.Idle)
    val uiState: StateFlow<VehiclesUiState> = _uiState.asStateFlow()

    private val _mutationState = MutableStateFlow<VehicleMutationUiState>(VehicleMutationUiState.Idle)
    val mutationState: StateFlow<VehicleMutationUiState> = _mutationState.asStateFlow()
    private var loadedUid: String? = null

    fun refreshForSession() {
        val session = when (val currentSessionState = sessionState.value) {
            AuthSessionState.Restoring -> {
                clearLoadedSession()
                _uiState.value = VehiclesUiState.Loading
                clearMutationStateIfIdle()
                return
            }
            is AuthSessionState.RestoreFailed -> {
                clearLoadedSession()
                _uiState.value = currentSessionState.error.toVehiclesUiState()
                clearMutationStateIfIdle()
                return
            }
            AuthSessionState.Unauthenticated -> {
                clearLoadedSession()
                _uiState.value = VehiclesUiState.Unauthenticated
                clearMutationStateIfIdle()
                return
            }
            is AuthSessionState.Authenticated -> currentSessionState
        }

        val uid = session.session.user.uid
        val alreadyLoadedForUser = loadedUid == uid &&
            (_uiState.value is VehiclesUiState.Loaded || _uiState.value is VehiclesUiState.Empty)
        if (alreadyLoadedForUser) return
        loadVehicles()
    }

    fun loadVehicles() {
        val session = when (val currentSessionState = sessionState.value) {
            AuthSessionState.Restoring -> {
                clearLoadedSession()
                _uiState.value = VehiclesUiState.Loading
                return
            }
            is AuthSessionState.RestoreFailed -> {
                clearLoadedSession()
                _uiState.value = currentSessionState.error.toVehiclesUiState()
                return
            }
            AuthSessionState.Unauthenticated -> {
                clearLoadedSession()
                _uiState.value = VehiclesUiState.Unauthenticated
                return
            }
            is AuthSessionState.Authenticated -> currentSessionState
        }

        val requestedUid = session.session.user.uid
        viewModelScope.launch {
            _uiState.value = VehiclesUiState.Loading
            val nextState = when (val result = vehicleRepository.getMyVehicles()) {
                is UserVehicleListResult.Success -> result.vehicles.toUiState()
                is UserVehicleListResult.Failure -> result.error.toVehiclesUiState()
            }
            when (val currentSessionState = sessionState.value) {
                AuthSessionState.Restoring -> {
                    clearLoadedSession()
                    _uiState.value = VehiclesUiState.Loading
                }
                is AuthSessionState.RestoreFailed -> {
                    clearLoadedSession()
                    _uiState.value = currentSessionState.error.toVehiclesUiState()
                }
                AuthSessionState.Unauthenticated -> {
                    clearLoadedSession()
                    _uiState.value = VehiclesUiState.Unauthenticated
                }
                is AuthSessionState.Authenticated -> {
                    if (currentSessionState.session.user.uid == requestedUid) {
                        loadedUid = requestedUid
                        _uiState.value = nextState
                    } else {
                        clearLoadedSession()
                        _uiState.value = VehiclesUiState.Unauthenticated
                    }
                }
            }
        }
    }

    fun saveVehicle(draft: VehicleDraftUi) {
        if (_mutationState.value is VehicleMutationUiState.Loading) return

        val request = draft.toSaveRequestOrNull()
        if (request == null) {
            _mutationState.value = VehicleMutationUiState.ValidationError("Complete os dados obrigatórios do veículo.")
            return
        }

        val requestedUid = authenticatedUidOrUpdateState() ?: return

        viewModelScope.launch {
            _mutationState.value = VehicleMutationUiState.Loading
            val result = if (request.id == null) {
                vehicleRepository.createVehicle(request)
            } else {
                vehicleRepository.updateVehicle(request)
            }

            if (!sessionStillMatches(requestedUid)) {
                handleSessionChangedDuringMutation()
                return@launch
            }

            _mutationState.value = when (result) {
                is UserVehicleMutationResult.Success -> {
                    publishSavedVehicle(result.vehicle.toUiModel())
                    VehicleMutationUiState.Success("Veículo guardado.")
                }
                is UserVehicleMutationResult.Failure -> result.error.toMutationUiState()
            }
        }
    }

    fun deleteVehicle(vehicleId: String) {
        if (_mutationState.value is VehicleMutationUiState.Loading) return

        val requestedUid = authenticatedUidOrUpdateState() ?: return

        viewModelScope.launch {
            _mutationState.value = VehicleMutationUiState.Loading
            val result = vehicleRepository.deleteVehicle(vehicleId)
            if (!sessionStillMatches(requestedUid)) {
                handleSessionChangedDuringMutation()
                return@launch
            }

            _mutationState.value = when (result) {
                UserVehicleDeleteResult.Success -> {
                    publishDeletedVehicle(vehicleId)
                    VehicleMutationUiState.Success("Veículo removido.")
                }
                is UserVehicleDeleteResult.Failure -> result.error.toMutationUiState()
            }
        }
    }

    fun setDefaultVehicle(vehicle: VehicleUi) {
        if (_mutationState.value is VehicleMutationUiState.Loading || vehicle.isDefault) return

        val request = vehicle.toDraft().copy(isDefault = true).toSaveRequestOrNull()
        if (request == null) {
            _mutationState.value = VehicleMutationUiState.ValidationError("Escolha um veículo válido.")
            return
        }

        val requestedUid = authenticatedUidOrUpdateState() ?: return

        viewModelScope.launch {
            _mutationState.value = VehicleMutationUiState.Loading
            val result = vehicleRepository.updateVehicle(request)
            if (!sessionStillMatches(requestedUid)) {
                handleSessionChangedDuringMutation()
                return@launch
            }

            _mutationState.value = when (result) {
                is UserVehicleMutationResult.Success -> {
                    publishSavedVehicle(result.vehicle.toUiModel())
                    VehicleMutationUiState.Success("Veículo predefinido atualizado.")
                }
                is UserVehicleMutationResult.Failure -> result.error.toMutationUiState()
            }
        }
    }

    fun clearMutationState() {
        _mutationState.value = VehicleMutationUiState.Idle
    }

    private fun authenticatedUidOrUpdateState(): String? {
        return when (val currentSessionState = sessionState.value) {
            AuthSessionState.Restoring -> {
                clearLoadedSession()
                _uiState.value = VehiclesUiState.Loading
                _mutationState.value = VehicleMutationUiState.Idle
                null
            }
            is AuthSessionState.RestoreFailed -> {
                clearLoadedSession()
                _uiState.value = currentSessionState.error.toVehiclesUiState()
                _mutationState.value = VehicleMutationUiState.Idle
                null
            }
            AuthSessionState.Unauthenticated -> {
                clearLoadedSession()
                _uiState.value = VehiclesUiState.Unauthenticated
                _mutationState.value = VehicleMutationUiState.Idle
                null
            }
            is AuthSessionState.Authenticated -> currentSessionState.session.user.uid
        }
    }

    private fun sessionStillMatches(uid: String): Boolean {
        return (sessionState.value as? AuthSessionState.Authenticated)?.session?.user?.uid == uid
    }

    private fun handleSessionChangedDuringMutation() {
        when (val currentSessionState = sessionState.value) {
            AuthSessionState.Restoring -> {
                clearLoadedSession()
                _uiState.value = VehiclesUiState.Loading
            }
            is AuthSessionState.RestoreFailed -> {
                clearLoadedSession()
                _uiState.value = currentSessionState.error.toVehiclesUiState()
            }
            AuthSessionState.Unauthenticated -> {
                clearLoadedSession()
                _uiState.value = VehiclesUiState.Unauthenticated
            }
            is AuthSessionState.Authenticated -> {
                clearLoadedSession()
                _uiState.value = VehiclesUiState.Unauthenticated
            }
        }
        _mutationState.value = VehicleMutationUiState.Idle
    }

    private fun clearLoadedSession() {
        loadedUid = null
    }

    private fun clearMutationStateIfIdle() {
        if (_mutationState.value !is VehicleMutationUiState.Loading) {
            _mutationState.value = VehicleMutationUiState.Idle
        }
    }

    private fun publishSavedVehicle(vehicle: VehicleUi) {
        loadedUid = (authRepository.sessionState.value as? AuthSessionState.Authenticated)?.session?.user?.uid
        val currentVehicles = (_uiState.value as? VehiclesUiState.Loaded)?.vehicles.orEmpty()
        val nextVehicles = if (currentVehicles.any { it.id == vehicle.id }) {
            currentVehicles.map { currentVehicle ->
                when {
                    currentVehicle.id == vehicle.id -> vehicle
                    vehicle.isDefault -> currentVehicle.copy(isDefault = false)
                    else -> currentVehicle
                }
            }
        } else {
            val existingVehicles = if (vehicle.isDefault) {
                currentVehicles.map { it.copy(isDefault = false) }
            } else {
                currentVehicles
            }
            existingVehicles + vehicle
        }
        _uiState.value = VehiclesUiState.Loaded(nextVehicles.sortedWith(vehicleComparator))
    }

    private fun publishDeletedVehicle(vehicleId: String) {
        val currentVehicles = (_uiState.value as? VehiclesUiState.Loaded)?.vehicles.orEmpty()
        val removedVehicle = currentVehicles.firstOrNull { it.id == vehicleId }
        val remainingVehicles = currentVehicles.filterNot { it.id == vehicleId }
        val nextVehicles = if (removedVehicle?.isDefault == true && remainingVehicles.none { it.isDefault }) {
            remainingVehicles.mapIndexed { index, vehicle ->
                if (index == 0) vehicle.copy(isDefault = true) else vehicle
            }
        } else {
            remainingVehicles
        }
        loadedUid = (authRepository.sessionState.value as? AuthSessionState.Authenticated)?.session?.user?.uid
        _uiState.value = if (nextVehicles.isEmpty()) {
            VehiclesUiState.Empty
        } else {
            VehiclesUiState.Loaded(nextVehicles.sortedWith(vehicleComparator))
        }
    }

    private fun List<UserVehicle>.toUiState(): VehiclesUiState {
        val vehicles = mapNotNull { it.toUiModelOrNull() }.sortedWith(vehicleComparator)
        return if (vehicles.isEmpty()) VehiclesUiState.Empty else VehiclesUiState.Loaded(vehicles)
    }

    private fun UserVehicleError.toVehiclesUiState(): VehiclesUiState {
        return when (this) {
            is UserVehicleError.Unauthenticated -> VehiclesUiState.Unauthenticated
            is UserVehicleError.Permission -> VehiclesUiState.Error(message = message, retryable = false)
            is UserVehicleError.Validation -> VehiclesUiState.Error(message = message, retryable = false)
            is UserVehicleError.NotFound -> VehiclesUiState.Error(message = message, retryable = true)
            is UserVehicleError.Unavailable,
            is UserVehicleError.Backend -> VehiclesUiState.Error(message = message, retryable = true)
        }
    }

    private fun UserVehicleError.toMutationUiState(): VehicleMutationUiState {
        return when (this) {
            is UserVehicleError.Validation -> VehicleMutationUiState.ValidationError(message)
            is UserVehicleError.Unauthenticated,
            is UserVehicleError.Permission,
            is UserVehicleError.NotFound -> VehicleMutationUiState.Error(message = message, retryable = false)
            is UserVehicleError.Unavailable,
            is UserVehicleError.Backend -> VehicleMutationUiState.Error(message = message, retryable = true)
        }
    }
}

private fun AuthError.toVehiclesUiState(): VehiclesUiState.Error {
    return VehiclesUiState.Error(message = message, retryable = isRetryableSessionError())
}

private fun AuthError.isRetryableSessionError(): Boolean {
    return this is AuthError.Unavailable || this is AuthError.Backend
}

private val vehicleComparator = compareByDescending<VehicleUi> { it.isDefault }
    .thenBy { it.brand.lowercase() }
    .thenBy { it.model.lowercase() }
    .thenBy { it.plate.lowercase() }

private fun VehicleDraftUi.toSaveRequestOrNull(): UserVehicleSaveRequest? {
    if (!canSubmit) return null
    return UserVehicleSaveRequest(
        id = id,
        brand = brand,
        model = model,
        plate = plate,
        color = color,
        type = type.id,
        isDefault = isDefault,
    )
}

private fun UserVehicle.toUiModelOrNull(): VehicleUi? {
    if (id.isBlank() || brand.isBlank() || model.isBlank() || plate.isBlank()) return null
    return toUiModel()
}

private fun UserVehicle.toUiModel(): VehicleUi = VehicleUi(
    id = id,
    brand = brand,
    model = model,
    plate = plate,
    color = color,
    type = type.toVehicleTypeUi(),
    isDefault = isDefault,
)

private fun String.toVehicleTypeUi(): VehicleTypeUi = when (lowercase()) {
    "suv" -> VehicleTypeUi.Suv
    else -> VehicleTypeUi.Passenger
}

internal fun VehicleUi.toDraft(): VehicleDraftUi = VehicleDraftUi(
    id = id,
    brand = brand,
    model = model,
    plate = plate,
    color = color,
    type = type,
    isDefault = isDefault,
)
