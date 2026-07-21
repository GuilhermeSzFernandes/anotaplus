package com.guilherme.anotaplus.network

import com.guilherme.anotaplus.network.dto.AuthResponse
import com.guilherme.anotaplus.network.dto.CategoriaSyncRequest
import com.guilherme.anotaplus.network.dto.EntrySyncRequest
import com.guilherme.anotaplus.network.dto.GoogleLoginRequest
import com.guilherme.anotaplus.network.dto.SyncResponse
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface AnotaApi {
    @POST("auth/google")
    suspend fun loginWithGoogle(@Body body: GoogleLoginRequest): AuthResponse

    @POST("categories")
    suspend fun criarCategoria(
        @Header("Authorization") auth: String,
        @Body body: CategoriaSyncRequest
    ): SyncResponse

    @POST("entries")
    suspend fun criarEntry(
        @Header("Authorization") auth: String,
        @Body body: EntrySyncRequest
    ): SyncResponse
}
