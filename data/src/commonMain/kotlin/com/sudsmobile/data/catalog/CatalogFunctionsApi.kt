package com.sudsmobile.data.catalog

interface CatalogFunctionsApi {
    suspend fun getServiceCatalog(): ServiceCatalogResult
}
