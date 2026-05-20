package com.sudsmobile.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
)

internal data class VehicleDraftUi(
    val id: String? = null,
    val brand: String = "",
    val model: String = "",
    val plate: String = "",
    val color: String = "",
    val type: VehicleTypeUi = VehicleTypeUi.Passenger,
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
    private val _uiState = MutableStateFlow<VehiclesUiState>(VehiclesUiState.Idle)
    val uiState: StateFlow<VehiclesUiState> = _uiState.asStateFlow()

    private val _mutationState = MutableStateFlow<VehicleMutationUiState>(VehicleMutationUiState.Idle)
    val mutationState: StateFlow<VehicleMutationUiState> = _mutationState.asStateFlow()

    fun loadVehicles() {
        if (_uiState.value is VehiclesUiState.Loading) return

        val session = authRepository.sessionState.value as? AuthSessionState.Authenticated
        if (session == null) {
            _uiState.value = VehiclesUiState.Unauthenticated
            return
        }

        val requestedUid = session.session.user.uid
        viewModelScope.launch {
            _uiState.value = VehiclesUiState.Loading
            val nextState = when (val result = vehicleRepository.getMyVehicles()) {
                is UserVehicleListResult.Success -> result.vehicles.toUiState()
                is UserVehicleListResult.Failure -> result.error.toVehiclesUiState()
            }
            val currentUid = (authRepository.sessionState.value as? AuthSessionState.Authenticated)?.session?.user?.uid
            _uiState.value = if (currentUid == requestedUid) nextState else VehiclesUiState.Unauthenticated
        }
    }

    fun saveVehicle(draft: VehicleDraftUi) {
        if (_mutationState.value is VehicleMutationUiState.Loading) return

        val request = draft.toSaveRequestOrNull()
        if (request == null) {
            _mutationState.value = VehicleMutationUiState.ValidationError("Complete os dados obrigatórios do veículo.")
            return
        }

        viewModelScope.launch {
            _mutationState.value = VehicleMutationUiState.Loading
            val result = if (request.id == null) {
                vehicleRepository.createVehicle(request)
            } else {
                vehicleRepository.updateVehicle(request)
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

        viewModelScope.launch {
            _mutationState.value = VehicleMutationUiState.Loading
            _mutationState.value = when (val result = vehicleRepository.deleteVehicle(vehicleId)) {
                UserVehicleDeleteResult.Success -> {
                    publishDeletedVehicle(vehicleId)
                    VehicleMutationUiState.Success("Veículo removido.")
                }
                is UserVehicleDeleteResult.Failure -> result.error.toMutationUiState()
            }
        }
    }

    fun clearMutationState() {
        _mutationState.value = VehicleMutationUiState.Idle
    }

    private fun publishSavedVehicle(vehicle: VehicleUi) {
        val currentVehicles = (_uiState.value as? VehiclesUiState.Loaded)?.vehicles.orEmpty()
        val nextVehicles = if (currentVehicles.any { it.id == vehicle.id }) {
            currentVehicles.map { if (it.id == vehicle.id) vehicle else it }
        } else {
            currentVehicles + vehicle
        }
        _uiState.value = VehiclesUiState.Loaded(nextVehicles.sortedWith(vehicleComparator))
    }

    private fun publishDeletedVehicle(vehicleId: String) {
        val currentVehicles = (_uiState.value as? VehiclesUiState.Loaded)?.vehicles.orEmpty()
        val nextVehicles = currentVehicles.filterNot { it.id == vehicleId }
        _uiState.value = if (nextVehicles.isEmpty()) {
            VehiclesUiState.Empty
        } else {
            VehiclesUiState.Loaded(nextVehicles)
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

private val vehicleComparator = compareBy<VehicleUi> { it.brand.lowercase() }
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
)
