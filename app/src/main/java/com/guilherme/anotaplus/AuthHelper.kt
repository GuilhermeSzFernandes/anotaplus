package com.guilherme.anotaplus

import android.app.Activity
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.guilherme.anotaplus.data.SessionPrefs
import com.guilherme.anotaplus.data.SubscriptionPrefs
import com.guilherme.anotaplus.network.ApiClient
import com.guilherme.anotaplus.network.dto.AuthResponse
import com.guilherme.anotaplus.network.dto.GoogleLoginRequest

/**
 * Fluxo de login com Google via Credential Manager, compartilhado entre a
 * tela de boas-vindas (LoginActivity) e Configurações — as duas só cuidam
 * da própria UI (loading/erro), a troca de credencial por sessão é sempre
 * a mesma.
 */
object AuthHelper {

    class LoginFalhouException(message: String) : Exception(message)

    suspend fun entrarComGoogle(activity: Activity): AuthResponse {
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(BuildConfig.GOOGLE_WEB_CLIENT_ID)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        val result = CredentialManager.create(activity).getCredential(activity, request)
        val credential = result.credential

        if (credential !is CustomCredential ||
            credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            throw LoginFalhouException("Credencial do Google inesperada")
        }

        val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
        val resposta = ApiClient.api.loginWithGoogle(GoogleLoginRequest(googleIdTokenCredential.idToken))
        SessionPrefs.salvarSessao(activity, resposta.accessToken, resposta.user.email, resposta.user.name)
        // Valor do login já serve de primeiro palpite; BillingManager
        // confirma com o Play Billing local logo em seguida (a fonte da
        // verdade de verdade é o Google, não o que o backend guardou da
        // última vez).
        SubscriptionPrefs.setPro(activity, resposta.user.pro)
        return resposta
    }

    suspend fun sair(activity: Activity) {
        runCatching { CredentialManager.create(activity).clearCredentialState(ClearCredentialStateRequest()) }
        SessionPrefs.limparSessao(activity)
        SubscriptionPrefs.setPro(activity, false)
    }
}
