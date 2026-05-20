package com.sudsmobile.data.catalog

class FirebaseServiceCatalogRepository(
    private val api: CatalogFunctionsApi,
) : ServiceCatalogRepository {
    override suspend fun getServiceCatalog(): ServiceCatalogResult = api.getServiceCatalog()
}
