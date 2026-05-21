package com.sudsmobile.data.business

interface BusinessInfoFunctionsApi {
    suspend fun getBusinessInfo(): BusinessInfoResult
}
