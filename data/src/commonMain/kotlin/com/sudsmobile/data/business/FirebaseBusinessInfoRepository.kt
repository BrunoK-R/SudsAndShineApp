package com.sudsmobile.data.business

class FirebaseBusinessInfoRepository(
    private val api: BusinessInfoFunctionsApi,
) : BusinessInfoRepository {
    override suspend fun getBusinessInfo(): BusinessInfoResult = api.getBusinessInfo()
}
