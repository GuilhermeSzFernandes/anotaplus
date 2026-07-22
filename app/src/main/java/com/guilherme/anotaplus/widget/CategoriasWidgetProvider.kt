package com.guilherme.anotaplus.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

// Sexto widget: gasto do mês em cada categoria, na tela inicial — sem tela
// de configuração (mesma lista pra todas as instâncias, igual GastoWidgetProvider).
class CategoriasWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            CategoriasWidgetUpdater.atualizarTodos(context)
            pendingResult.finish()
        }
    }
}
