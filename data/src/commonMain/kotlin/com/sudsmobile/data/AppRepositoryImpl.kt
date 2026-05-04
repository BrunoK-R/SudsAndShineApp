package com.sudsmobile.data

class AppRepositoryImpl : AppRepository {
    override suspend fun ping(): String = "ok"
}
