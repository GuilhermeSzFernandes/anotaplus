package com.guilherme.anotaplus.network

import com.guilherme.anotaplus.network.dto.AuthResponse
import com.guilherme.anotaplus.network.dto.GoogleLoginRequest
import retrofit2.http.Body
import retrofit2.http.POST

interface AnotaApi {
    @POST("auth/google")
    suspend fun loginWithGoogle(@Body body: GoogleLoginRequest): AuthResponse
}
