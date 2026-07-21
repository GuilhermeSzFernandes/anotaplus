package com.guilherme.anotaplus.widget

import android.content.Context
import androidx.core.content.edit

/** Guarda qual Entry (Ideia) cada instância do AnotacaoWidgetProvider mostra. */
object AnotacaoWidgetPrefs {
    private const val PREFS_NAME = "anotaplus_anotacao_widget"

    fun salvarEntryId(context: Context, appWidgetId: Int, entryId: Long) {
        prefs(context).edit { putLong(chave(appWidgetId), entryId) }
    }

    fun getEntryId(context: Context, appWidgetId: Int): Long? {
        val valor = prefs(context).getLong(chave(appWidgetId), -1L)
        return if (valor == -1L) null else valor
    }

    fun remover(context: Context, appWidgetId: Int) {
        prefs(context).edit { remove(chave(appWidgetId)) }
    }

    private fun chave(appWidgetId: Int) = "entry_$appWidgetId"

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
