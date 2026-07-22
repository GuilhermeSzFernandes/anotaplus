package com.guilherme.anotaplus.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.guilherme.anotaplus.QuickCaptureActivity
import com.guilherme.anotaplus.R
import com.guilherme.anotaplus.data.EntryType

/**
 * Widget estreito e alto (1x2 células): dois alvos de toque empilhados,
 * cada um abre a Captura Rápida já com o tipo certo pré-selecionado
 * (mesmo padrão do CapturaModalWidgetProvider). Sem consulta ao Room —
 * conteúdo é 100% estático — então não precisa de goAsync/coroutine.
 */
class AdicionarWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val views = RemoteViews(context.packageName, R.layout.widget_adicionar)

        views.setOnClickPendingIntent(R.id.area_adicionar_gasto, abrirCaptura(context, EntryType.GASTO, 0))
        views.setOnClickPendingIntent(R.id.area_adicionar_anotacao, abrirCaptura(context, EntryType.PENSAMENTO, 1))

        appWidgetManager.updateAppWidget(appWidgetIds, views)
    }

    private fun abrirCaptura(context: Context, tipoForcado: EntryType, requestCode: Int): PendingIntent {
        val intent = Intent(context, QuickCaptureActivity::class.java)
            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            .putExtra(QuickCaptureActivity.EXTRA_TIPO_FORCADO, tipoForcado.name)
        return PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
