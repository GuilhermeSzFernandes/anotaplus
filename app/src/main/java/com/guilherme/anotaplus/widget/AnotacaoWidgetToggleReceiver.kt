package com.guilherme.anotaplus.widget

import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.guilherme.anotaplus.RichTextEngine
import com.guilherme.anotaplus.data.AppDatabase
import com.guilherme.anotaplus.data.SessionPrefs
import com.guilherme.anotaplus.data.SubscriptionPrefs
import com.guilherme.anotaplus.network.ApiClient
import com.guilherme.anotaplus.network.dto.EntrySyncRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.Instant

/** Recebe o toque no "☐"/"☑" de uma linha do AnotacaoWidgetProvider e marca/desmarca o item. */
class AnotacaoWidgetToggleReceiver : BroadcastReceiver() {

    companion object {
        const val ACAO_TOGGLE = "com.guilherme.anotaplus.TOGGLE_CHECKLIST_WIDGET"
        const val EXTRA_ENTRY_ID = "entry_id"
        const val EXTRA_LINHA = "linha"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACAO_TOGGLE) return

        val entryId = intent.getLongExtra(EXTRA_ENTRY_ID, -1L)
        val linha = intent.getIntExtra(EXTRA_LINHA, -1)
        val appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        if (entryId == -1L || linha == -1 || appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            runCatching { alternarLinha(context, entryId, linha) }
            runCatching { AnotacaoWidgetUpdater.atualizar(context, appWidgetId) }
            pendingResult.finish()
        }
    }

    private suspend fun alternarLinha(context: Context, entryId: Long, linha: Int) {
        val dao = AppDatabase.getInstance(context).entryDao()
        val entry = dao.getById(entryId) ?: return
        val linhas = entry.texto.lines().toMutableList()
        if (linha >= linhas.size) return

        val atual = linhas[linha]
        val nova = when {
            atual.startsWith(RichTextEngine.MARCA_CHECKLIST_VAZIA) ->
                atual.replaceFirst(RichTextEngine.MARCA_CHECKLIST_VAZIA, RichTextEngine.MARCA_CHECKLIST_MARCADA)
            atual.startsWith(RichTextEngine.MARCA_CHECKLIST_MARCADA) ->
                atual.replaceFirst(RichTextEngine.MARCA_CHECKLIST_MARCADA, RichTextEngine.MARCA_CHECKLIST_VAZIA)
            else -> return
        }
        linhas[linha] = nova

        val entryAtualizada = entry.copy(texto = linhas.joinToString("\n"))
        dao.update(entryAtualizada)

        val remoteId = entryAtualizada.remoteId
        if (remoteId != null && SubscriptionPrefs.podeFazerBackup(context)) {
            runCatching {
                ApiClient.api.atualizarEntry(
                    "Bearer ${SessionPrefs.getAccessToken(context)}",
                    remoteId,
                    EntrySyncRequest(
                        type = entryAtualizada.type.name,
                        titulo = entryAtualizada.titulo,
                        texto = entryAtualizada.texto,
                        valor = entryAtualizada.valor,
                        categoria = entryAtualizada.categoria,
                        timestamp = Instant.ofEpochMilli(entryAtualizada.timestamp).toString()
                    )
                )
            }
        }
    }
}
