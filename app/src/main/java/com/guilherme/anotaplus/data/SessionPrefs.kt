package com.guilherme.anotaplus.data

import android.content.Context
import androidx.core.content.edit

object SessionPrefs {
    private const val PREFS_NAME = "anotaplus_session"
    private const val KEY_ACCESS_TOKEN = "access_token"
    private const val KEY_EMAIL = "email"
    private const val KEY_NAME = "name"

    fun salvarSessao(context: Context, accessToken: String, email: String, name: String?) {
        prefs(context).edit {
            putString(KEY_ACCESS_TOKEN, accessToken)
            putString(KEY_EMAIL, email)
            putString(KEY_NAME, name)
        }
    }

    fun limparSessao(context: Context) {
        prefs(context).edit { clear() }
    }

    fun getAccessToken(context: Context): String? = prefs(context).getString(KEY_ACCESS_TOKEN, null)

    fun getEmail(context: Context): String? = prefs(context).getString(KEY_EMAIL, null)

    fun estaLogado(context: Context): Boolean = getAccessToken(context) != null

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
