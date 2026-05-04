package com.sudsmobile.data

interface AppRepository {
    suspend fun ping(): String
}
