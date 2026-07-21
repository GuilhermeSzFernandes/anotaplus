package com.guilherme.anotaplus.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class GastoWidgetProvider : AppWidgetProvider() {

    // onUpdate roda no processo do BroadcastReceiver, que o sistema pode
    // matar assim que a função retorna — goAsync() segura o processo vivo
    // até a consulta (suspend) no Room terminar e o widget ser redesenhado.
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            WidgetUpdater.atualizarTodos(context)
            pendingResult.finish()
        }
    }
}
