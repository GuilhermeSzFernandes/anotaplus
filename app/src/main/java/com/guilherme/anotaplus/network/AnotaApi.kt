package com.guilherme.anotaplus.network

import com.guilherme.anotaplus.network.dto.AuthResponse
import com.guilherme.anotaplus.network.dto.BillingSyncRequest
import com.guilherme.anotaplus.network.dto.BillingSyncResponse
import com.guilherme.anotaplus.network.dto.CarteiraRemota
import com.guilherme.anotaplus.network.dto.CarteiraSyncRequest
import com.guilherme.anotaplus.network.dto.CategoriaLimiteRequest
import com.guilherme.anotaplus.network.dto.CategoriaRemota
import com.guilherme.anotaplus.network.dto.CategoriaSyncRequest
import com.guilherme.anotaplus.network.dto.EntryRemota
import com.guilherme.anotaplus.network.dto.EntrySyncRequest
import com.guilherme.anotaplus.network.dto.GoogleLoginRequest
import com.guilherme.anotaplus.network.dto.SyncResponse
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path

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

    @PATCH("entries/{id}")
    suspend fun atualizarEntry(
        @Header("Authorization") auth: String,
        @Path("id") remoteId: String,
        @Body body: EntrySyncRequest
    )

    @DELETE("entries/{id}")
    suspend fun excluirEntry(
        @Header("Authorization") auth: String,
        @Path("id") remoteId: String
    )

    @PATCH("categories/{id}")
    suspend fun atualizarCategoria(
        @Header("Authorization") auth: String,
        @Path("id") remoteId: String,
        @Body body: CategoriaLimiteRequest
    )

    @GET("categories")
    suspend fun listarCategorias(@Header("Authorization") auth: String): List<CategoriaRemota>

    @POST("carteiras")
    suspend fun criarCarteira(
        @Header("Authorization") auth: String,
        @Body body: CarteiraSyncRequest
    ): SyncResponse

    @GET("carteiras")
    suspend fun listarCarteiras(@Header("Authorization") auth: String): List<CarteiraRemota>

    @GET("entries")
    suspend fun listarEntries(@Header("Authorization") auth: String): List<EntryRemota>

    @POST("billing/sync")
    suspend fun sincronizarAssinatura(
        @Header("Authorization") auth: String,
        @Body body: BillingSyncRequest
    ): BillingSyncResponse
}
