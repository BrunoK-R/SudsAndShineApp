package com.sudsmobile.data.vehicle

import com.sudsmobile.data.auth.AuthRepository
import com.sudsmobile.data.auth.AuthSessionState

class FirebaseUserVehicleRepository(
    private val api: VehicleFunctionsApi,
    private val authRepository: AuthRepository,
) : UserVehicleRepository {
    override suspend fun getMyVehicles(): UserVehicleListResult {
        val idToken = currentIdTokenOrNull()
            ?: return UserVehicleListResult.Failure(unauthenticatedError())

        return api.getMyVehicles(idToken)
    }

    override suspend fun createVehicle(request: UserVehicleSaveRequest): UserVehicleMutationResult {
        val idToken = currentIdTokenOrNull()
            ?: return UserVehicleMutationResult.Failure(unauthenticatedError())
        val validationError = validate(request, requireId = false)
        if (validationError != null) return UserVehicleMutationResult.Failure(validationError)

        return api.createVehicle(request.normalized(), idToken)
    }

    override suspend fun updateVehicle(request: UserVehicleSaveRequest): UserVehicleMutationResult {
        val idToken = currentIdTokenOrNull()
            ?: return UserVehicleMutationResult.Failure(unauthenticatedError())
        val validationError = validate(request, requireId = true)
        if (validationError != null) return UserVehicleMutationResult.Failure(validationError)

        return api.updateVehicle(request.normalized(), idToken)
    }

    override suspend fun deleteVehicle(vehicleId: String): UserVehicleDeleteResult {
        val idToken = currentIdTokenOrNull()
            ?: return UserVehicleDeleteResult.Failure(unauthenticatedError())
        val normalizedVehicleId = vehicleId.trim()
        if (normalizedVehicleId.isBlank()) {
            return UserVehicleDeleteResult.Failure(UserVehicleError.Validation("Escolha um veículo válido."))
        }

        return api.deleteVehicle(normalizedVehicleId, idToken)
    }

    private fun currentIdTokenOrNull(): String? {
        val session = authRepository.sessionState.value as? AuthSessionState.Authenticated
        return session?.session?.idToken
    }

    private fun validate(
        request: UserVehicleSaveRequest,
        requireId: Boolean,
    ): UserVehicleError.Validation? {
        return when {
            requireId && request.id.isNullOrBlank() ->
                UserVehicleError.Validation("Escolha um veículo válido.")
            request.brand.isBlank() ->
                UserVehicleError.Validation("Indique a marca do veículo.")
            request.model.isBlank() ->
                UserVehicleError.Validation("Indique o modelo do veículo.")
            request.plate.trim().length < 2 ->
                UserVehicleError.Validation("Indique a matrícula do veículo.")
            request.type.normalizedVehicleType() !in validVehicleTypes ->
                UserVehicleError.Validation("Escolha um tipo de veículo válido.")
            else -> null
        }
    }

    private fun UserVehicleSaveRequest.normalized(): UserVehicleSaveRequest = copy(
        id = id?.trim()?.takeIf { it.isNotBlank() },
        brand = brand.trim(),
        model = model.trim(),
        plate = plate.trim().uppercase(),
        color = color.trim(),
        type = type.normalizedVehicleType(),
    )
}

private val validVehicleTypes = setOf("passenger", "suv")

private fun String.normalizedVehicleType(): String {
    return when (trim().lowercase()) {
        "passageiros" -> "passenger"
        else -> trim().lowercase()
    }
}

private fun unauthenticatedError(): UserVehicleError.Unauthenticated {
    return UserVehicleError.Unauthenticated("Inicie sessão para gerir os seus veículos.")
}
