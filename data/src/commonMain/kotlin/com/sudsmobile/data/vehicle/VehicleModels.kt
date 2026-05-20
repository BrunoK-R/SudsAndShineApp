package com.sudsmobile.data.vehicle

data class UserVehicle(
    val id: String,
    val brand: String,
    val model: String,
    val plate: String,
    val color: String,
    val type: String,
)

data class UserVehicleSaveRequest(
    val id: String? = null,
    val brand: String,
    val model: String,
    val plate: String,
    val color: String,
    val type: String,
)

sealed interface UserVehicleListResult {
    data class Success(val vehicles: List<UserVehicle>) : UserVehicleListResult
    data class Failure(val error: UserVehicleError) : UserVehicleListResult
}

sealed interface UserVehicleMutationResult {
    data class Success(val vehicle: UserVehicle) : UserVehicleMutationResult
    data class Failure(val error: UserVehicleError) : UserVehicleMutationResult
}

sealed interface UserVehicleDeleteResult {
    data object Success : UserVehicleDeleteResult
    data class Failure(val error: UserVehicleError) : UserVehicleDeleteResult
}

sealed interface UserVehicleError {
    val message: String

    data class Validation(override val message: String) : UserVehicleError
    data class Permission(override val message: String) : UserVehicleError
    data class Unauthenticated(override val message: String) : UserVehicleError
    data class NotFound(override val message: String) : UserVehicleError
    data class Unavailable(override val message: String) : UserVehicleError
    data class Backend(override val message: String) : UserVehicleError
}

interface UserVehicleRepository {
    suspend fun getMyVehicles(): UserVehicleListResult
    suspend fun createVehicle(request: UserVehicleSaveRequest): UserVehicleMutationResult
    suspend fun updateVehicle(request: UserVehicleSaveRequest): UserVehicleMutationResult
    suspend fun deleteVehicle(vehicleId: String): UserVehicleDeleteResult
}
