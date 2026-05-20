package com.sudsmobile.data.vehicle

interface VehicleFunctionsApi {
    suspend fun getMyVehicles(idToken: String): UserVehicleListResult
    suspend fun createVehicle(request: UserVehicleSaveRequest, idToken: String): UserVehicleMutationResult
    suspend fun updateVehicle(request: UserVehicleSaveRequest, idToken: String): UserVehicleMutationResult
    suspend fun deleteVehicle(vehicleId: String, idToken: String): UserVehicleDeleteResult
}
