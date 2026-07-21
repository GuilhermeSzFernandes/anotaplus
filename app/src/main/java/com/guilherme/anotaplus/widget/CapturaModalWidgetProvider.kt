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
 * Widget "espelho" do modal de Captura Rápida — mesmo visual (toggle,
 * campo de valor, campo de texto, botão Salvar), mas nenhum campo
 * funciona de verdade: RemoteViews não suporta EditText (nenhum widget de
 * nenhum app consegue ter campo de texto editável de verdade). Tocar em
 * qualquer parte abre QuickCaptureActivity pra digitar; tocar
 * especificamente no toggle Gasto/Anotações já abre com esse tipo
 * pré-selecionado (QuickCaptureActivity.EXTRA_TIPO_FORCADO).
 *
 * Sem consulta ao Room — conteúdo é 100% estático — então não precisa de
 * goAsync/coroutine, só monta os PendingIntents.
 */
class CapturaModalWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val views = RemoteViews(context.packageName, R.layout.widget_captura_modal)

        views.setOnClickPendingIntent(R.id.widget_modal_root, abrirModal(context, null, 0))
        views.setOnClickPendingIntent(R.id.toggle_anotacoes, abrirModal(context, EntryType.PENSAMENTO, 1))
        views.setOnClickPendingIntent(R.id.toggle_gasto, abrirModal(context, EntryType.GASTO, 2))

        appWidgetManager.updateAppWidget(appWidgetIds, views)
    }

    private fun abrirModal(context: Context, tipoForcado: EntryType?, requestCode: Int): PendingIntent {
        val intent = Intent(context, QuickCaptureActivity::class.java)
            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (tipoForcado != null) {
            intent.putExtra(QuickCaptureActivity.EXTRA_TIPO_FORCADO, tipoForcado.name)
        }
        return PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
