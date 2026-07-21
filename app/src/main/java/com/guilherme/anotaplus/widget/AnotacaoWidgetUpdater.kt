package com.guilherme.anotaplus.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import com.guilherme.anotaplus.ManualIdeiaActivity
import com.guilherme.anotaplus.R
import com.guilherme.anotaplus.RichTextEngine
import com.guilherme.anotaplus.data.AppDatabase

/**
 * Redesenha o widget de uma ideia específica. Linhas fixas (6, ver
 * widget_anotacao.xml) em vez de um RemoteViewsService/coleção — mais
 * simples e robusto, ao custo de um limite de linhas visíveis por
 * widget (documentado no PROJETO.md).
 */
object AnotacaoWidgetUpdater {

    private const val MAX_LINHAS = 6
    private val IDS_LINHA = intArrayOf(
        R.id.linha_0, R.id.linha_1, R.id.linha_2, R.id.linha_3, R.id.linha_4, R.id.linha_5
    )
    private val IDS_CHECK = intArrayOf(
        R.id.check_0, R.id.check_1, R.id.check_2, R.id.check_3, R.id.check_4, R.id.check_5
    )
    private val IDS_TEXTO = intArrayOf(
        R.id.texto_0, R.id.texto_1, R.id.texto_2, R.id.texto_3, R.id.texto_4, R.id.texto_5
    )

    suspend fun atualizar(context: Context, appWidgetId: Int) {
        val manager = AppWidgetManager.getInstance(context)
        val entryId = AnotacaoWidgetPrefs.getEntryId(context, appWidgetId)
        val entry = entryId?.let { AppDatabase.getInstance(context).entryDao().getById(it) }

        val views = RemoteViews(context.packageName, R.layout.widget_anotacao)

        if (entry == null) {
            views.setTextViewText(R.id.text_anotacao_titulo, context.getString(R.string.widget_anotacao_nao_configurado))
            IDS_LINHA.forEach { views.setViewVisibility(it, View.GONE) }
            views.setOnClickPendingIntent(
                R.id.widget_anotacao_root,
                PendingIntent.getActivity(
                    context,
                    appWidgetId,
                    Intent(context, AnotacaoWidgetConfigActivity::class.java)
                        .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )
            manager.updateAppWidget(appWidgetId, views)
            return
        }

        views.setTextViewText(
            R.id.text_anotacao_titulo,
            entry.titulo?.takeIf { it.isNotBlank() } ?: context.getString(R.string.widget_anotacao_sem_titulo)
        )
        views.setOnClickPendingIntent(
            R.id.widget_anotacao_root,
            PendingIntent.getActivity(
                context,
                appWidgetId,
                Intent(context, ManualIdeiaActivity::class.java)
                    .putExtra(ManualIdeiaActivity.EXTRA_ENTRY_ID, entry.id)
                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        )

        val linhas = entry.texto.lines()
        for (i in 0 until MAX_LINHAS) {
            val linha = linhas.getOrNull(i)
            if (linha.isNullOrBlank()) {
                views.setViewVisibility(IDS_LINHA[i], View.GONE)
                continue
            }
            views.setViewVisibility(IDS_LINHA[i], View.VISIBLE)

            val marcada = linha.startsWith(RichTextEngine.MARCA_CHECKLIST_MARCADA)
            val vazia = linha.startsWith(RichTextEngine.MARCA_CHECKLIST_VAZIA)

            if (marcada || vazia) {
                views.setTextViewText(IDS_CHECK[i], if (marcada) "☑" else "☐")
                views.setTextViewText(
                    IDS_TEXTO[i],
                    linha.removePrefix(if (marcada) RichTextEngine.MARCA_CHECKLIST_MARCADA else RichTextEngine.MARCA_CHECKLIST_VAZIA)
                )
                views.setOnClickPendingIntent(
                    IDS_CHECK[i],
                    PendingIntent.getBroadcast(
                        context,
                        appWidgetId * 100 + i,
                        Intent(context, AnotacaoWidgetToggleReceiver::class.java).apply {
                            action = AnotacaoWidgetToggleReceiver.ACAO_TOGGLE
                            putExtra(AnotacaoWidgetToggleReceiver.EXTRA_ENTRY_ID, entry.id)
                            putExtra(AnotacaoWidgetToggleReceiver.EXTRA_LINHA, i)
                            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                        },
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                )
            } else {
                views.setTextViewText(IDS_CHECK[i], "")
                views.setTextViewText(IDS_TEXTO[i], RichTextEngine.textoSemMarcadores(linha))
            }
        }

        manager.updateAppWidget(appWidgetId, views)
    }

    /** Chamar depois de criar/editar/excluir uma Ideia — sem isso, o widget fica com o conteúdo antigo até a próxima atualização periódica. */
    suspend fun atualizarTodos(context: Context) {
        val manager = AppWidgetManager.getInstance(context)
        val ids = manager.getAppWidgetIds(ComponentName(context, AnotacaoWidgetProvider::class.java))
        ids.forEach { atualizar(context, it) }
    }
}
