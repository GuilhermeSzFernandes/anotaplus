package com.guilherme.anotaplus.network

import com.guilherme.anotaplus.BuildConfig
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {
    val api: AnotaApi by lazy {
        // Timeout generoso: o Render (plano free) "dorme" o serviço depois de
        // inatividade e pode levar 30-60s pra acordar na primeira chamada.
        val client = OkHttpClient.Builder()
            .connectTimeout(45, TimeUnit.SECONDS)
            .readTimeout(45, TimeUnit.SECONDS)
            .build()

        Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AnotaApi::class.java)
    }
}
