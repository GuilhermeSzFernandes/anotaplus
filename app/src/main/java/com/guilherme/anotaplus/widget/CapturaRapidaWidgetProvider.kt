package com.guilherme.anotaplus.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.guilherme.anotaplus.QuickCaptureActivity
import com.guilherme.anotaplus.R

/**
 * Widget pequeno (1x1), só um atalho pra Captura Rápida — separado do
 * GastoWidgetProvider (que mostra os totais e é maior). Sem consulta ao
 * Room nenhuma, então não precisa de goAsync/coroutine: só monta o
 * PendingIntent uma vez.
 *
 * Sem sourceBounds no Intent (igual o "+" do outro widget e o ladrilho de
 * Configurações Rápidas) — QuickCaptureActivity cai naturalmente no
 * caminho de abrir o modal de captura em vez do Histórico.
 */
class CapturaRapidaWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val views = RemoteViews(context.packageName, R.layout.widget_captura_rapida)
        views.setOnClickPendingIntent(
            R.id.widget_captura_root,
            PendingIntent.getActivity(
                context,
                0,
                Intent(context, QuickCaptureActivity::class.java).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        )
        appWidgetManager.updateAppWidget(appWidgetIds, views)
    }
}
