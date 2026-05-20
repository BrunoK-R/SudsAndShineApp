package com.sudsmobile.data.vehicle

import com.sudsmobile.data.booking.FirebaseFunctionsConfig
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.Serializable

class KtorVehicleFunctionsApi(
    private val httpClient: HttpClient,
    private val config: FirebaseFunctionsConfig,
) : VehicleFunctionsApi {
    override suspend fun getMyVehicles(idToken: String): UserVehicleListResult {
        return try {
            val response = httpClient.post(config.getMyVehiclesUrl) {
                callableHeaders(idToken)
                setBody(CallableVehicleListRequest(data = emptyMap()))
            }
            val body = response.body<CallableVehicleListResponse>()
            val error = body.error
            when {
                error != null -> UserVehicleListResult.Failure(error.toVehicleError())
                body.result != null -> UserVehicleListResult.Success(
                    body.result.vehicles.map { it.toUserVehicle() },
                )
                else -> UserVehicleListResult.Failure(
                    UserVehicleError.Backend("A resposta dos veículos veio sem dados."),
                )
            }
        } catch (cause: CancellationException) {
            throw cause
        } catch (cause: Throwable) {
            UserVehicleListResult.Failure(
                UserVehicleError.Unavailable("Não foi possível carregar os veículos. Tente novamente."),
            )
        }
    }

    override suspend fun createVehicle(
        request: UserVehicleSaveRequest,
        idToken: String,
    ): UserVehicleMutationResult {
        return saveVehicle(
            url = config.createVehicleUrl,
            request = request,
            idToken = idToken,
        )
    }

    override suspend fun updateVehicle(
        request: UserVehicleSaveRequest,
        idToken: String,
    ): UserVehicleMutationResult {
        return saveVehicle(
            url = config.updateVehicleUrl,
            request = request,
            idToken = idToken,
        )
    }

    override suspend fun deleteVehicle(vehicleId: String, idToken: String): UserVehicleDeleteResult {
        return try {
            val response = httpClient.post(config.deleteVehicleUrl) {
                callableHeaders(idToken)
                setBody(CallableDeleteVehicleRequest(DeleteVehiclePayload(vehicleId = vehicleId)))
            }
            val body = response.body<CallableDeleteVehicleResponse>()
            val error = body.error
            when {
                error != null -> UserVehicleDeleteResult.Failure(error.toVehicleError())
                body.result?.ok == true -> UserVehicleDeleteResult.Success
                else -> UserVehicleDeleteResult.Failure(
                    UserVehicleError.Backend("A resposta de remoção veio sem confirmação."),
                )
            }
        } catch (cause: CancellationException) {
            throw cause
        } catch (cause: Throwable) {
            UserVehicleDeleteResult.Failure(
                UserVehicleError.Unavailable("Não foi possível remover o veículo. Tente novamente."),
            )
        }
    }

    private suspend fun saveVehicle(
        url: String,
        request: UserVehicleSaveRequest,
        idToken: String,
    ): UserVehicleMutationResult {
        return try {
            val response = httpClient.post(url) {
                callableHeaders(idToken)
                setBody(CallableSaveVehicleRequest(SaveVehiclePayload.from(request)))
            }
            val body = response.body<CallableSaveVehicleResponse>()
            val error = body.error
            when {
                error != null -> UserVehicleMutationResult.Failure(error.toVehicleError())
                body.result?.vehicle != null -> UserVehicleMutationResult.Success(
                    body.result.vehicle.toUserVehicle(),
                )
                else -> UserVehicleMutationResult.Failure(
                    UserVehicleError.Backend("A resposta do veículo veio sem dados."),
                )
            }
        } catch (cause: CancellationException) {
            throw cause
        } catch (cause: Throwable) {
            UserVehicleMutationResult.Failure(
                UserVehicleError.Unavailable("Não foi possível guardar o veículo. Tente novamente."),
            )
        }
    }

    private fun io.ktor.client.request.HttpRequestBuilder.callableHeaders(idToken: String) {
        header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
        header(HttpHeaders.Authorization, "Bearer $idToken")
    }
}

@Serializable
private data class CallableVehicleListRequest(
    val data: Map<String, String>,
)

@Serializable
private data class CallableSaveVehicleRequest(
    val data: SaveVehiclePayload,
)

@Serializable
private data class CallableDeleteVehicleRequest(
    val data: DeleteVehiclePayload,
)

@Serializable
private data class SaveVehiclePayload(
    val vehicleId: String? = null,
    val brand: String,
    val model: String,
    val plate: String,
    val color: String,
    val type: String,
) {
    companion object {
        fun from(request: UserVehicleSaveRequest): SaveVehiclePayload = SaveVehiclePayload(
            vehicleId = request.id,
            brand = request.brand,
            model = request.model,
            plate = request.plate,
            color = request.color,
            type = request.type,
        )
    }
}

@Serializable
private data class DeleteVehiclePayload(
    val vehicleId: String,
)

@Serializable
private data class CallableVehicleListResponse(
    val result: VehicleListResult? = null,
    val error: CallableError? = null,
)

@Serializable
private data class CallableSaveVehicleResponse(
    val result: SaveVehicleResult? = null,
    val error: CallableError? = null,
)

@Serializable
private data class CallableDeleteVehicleResponse(
    val result: DeleteVehicleResult? = null,
    val error: CallableError? = null,
)

@Serializable
private data class VehicleListResult(
    val vehicles: List<VehicleItem>,
)

@Serializable
private data class SaveVehicleResult(
    val vehicle: VehicleItem,
)

@Serializable
private data class DeleteVehicleResult(
    val ok: Boolean = false,
    val vehicleId: String = "",
)

@Serializable
private data class VehicleItem(
    val id: String,
    val brand: String,
    val model: String,
    val plate: String,
    val color: String = "",
    val type: String = "passenger",
) {
    fun toUserVehicle(): UserVehicle = UserVehicle(
        id = id,
        brand = brand,
        model = model,
        plate = plate,
        color = color,
        type = type,
    )
}

@Serializable
private data class CallableError(
    val status: String? = null,
    val code: String? = null,
    val message: String? = null,
) {
    fun toVehicleError(): UserVehicleError {
        val normalizedCode = (status ?: code).orEmpty().lowercase()
        val fallbackMessage = message ?: "Não foi possível gerir os veículos."
        return when (normalizedCode) {
            "invalid_argument", "invalid-argument" -> UserVehicleError.Validation(fallbackMessage)
            "permission_denied", "permission-denied" ->
                UserVehicleError.Permission("Não tem permissões para gerir estes veículos.")
            "unauthenticated" -> UserVehicleError.Unauthenticated("Inicie sessão para gerir os seus veículos.")
            "not_found", "not-found" -> UserVehicleError.NotFound("Este veículo já não existe.")
            "unavailable" -> UserVehicleError.Unavailable("O serviço de veículos está indisponível.")
            else -> UserVehicleError.Backend(fallbackMessage)
        }
    }
}
