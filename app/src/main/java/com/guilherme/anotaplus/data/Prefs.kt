package com.guilherme.anotaplus.data

import android.content.Context
import androidx.core.content.edit

object Prefs {
    private const val PREFS_NAME = "anotaplus_prefs"
    private const val KEY_TIPO_PADRAO = "tipo_padrao"
    private const val KEY_ONBOARDING_CONCLUIDO = "onboarding_concluido"

    fun getTipoPadrao(context: Context): EntryType {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val nome = prefs.getString(KEY_TIPO_PADRAO, EntryType.PENSAMENTO.name)
        return runCatching { EntryType.valueOf(nome ?: EntryType.PENSAMENTO.name) }
            .getOrDefault(EntryType.PENSAMENTO)
    }

    fun setTipoPadrao(context: Context, tipo: EntryType) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
            putString(KEY_TIPO_PADRAO, tipo.name)
        }
    }

    // Controla a tela de boas-vindas/login: só aparece na primeiríssima vez
    // que o app é aberto (via ícone ou gesto, o que vier primeiro).
    fun isOnboardingConcluido(context: Context): Boolean =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_ONBOARDING_CONCLUIDO, false)

    fun marcarOnboardingConcluido(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
            putBoolean(KEY_ONBOARDING_CONCLUIDO, true)
        }
    }
}
