package com.guilherme.anotaplus.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.app.PendingIntent
import android.widget.RemoteViews
import com.guilherme.anotaplus.HistoryActivity
import com.guilherme.anotaplus.MesUtil
import com.guilherme.anotaplus.QuickCaptureActivity
import com.guilherme.anotaplus.R
import com.guilherme.anotaplus.data.AppDatabase
import java.util.Calendar

/**
 * Redesenha o widget de gastos da tela inicial. Chamado depois de qualquer
 * mudança nos gastos (inserir, editar, excluir, restaurar backup) — o
 * widget não fica observando o banco sozinho, é atualizado sob demanda.
 */
object WidgetUpdater {

    suspend fun atualizarTodos(context: Context) {
        val manager = AppWidgetManager.getInstance(context)
        val ids = manager.getAppWidgetIds(ComponentName(context, GastoWidgetProvider::class.java))
        if (ids.isEmpty()) return

        val dao = AppDatabase.getInstance(context).entryDao()
        val agora = Calendar.getInstance()

        val inicioHoje = agora.clone() as Calendar
        inicioHoje.set(Calendar.HOUR_OF_DAY, 0)
        inicioHoje.set(Calendar.MINUTE, 0)
        inicioHoje.set(Calendar.SECOND, 0)
        inicioHoje.set(Calendar.MILLISECOND, 0)

        val totalHoje = dao.getTotalGastoOnce(inicioHoje.timeInMillis, System.currentTimeMillis())
        val totalMes = dao.getTotalGastoOnce(MesUtil.inicioDoMes(agora), MesUtil.fimDoMes(agora))

        val views = RemoteViews(context.packageName, R.layout.widget_gasto)
        views.setTextViewText(R.id.text_valor_hoje, "R$ %.2f".format(MesUtil.locale, totalHoje))
        views.setTextViewText(R.id.text_valor_mes, "R$ %.2f".format(MesUtil.locale, totalMes))

        views.setOnClickPendingIntent(
            R.id.widget_root,
            PendingIntent.getActivity(
                context,
                0,
                Intent(context, HistoryActivity::class.java).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        )
        views.setOnClickPendingIntent(
            R.id.btn_widget_add,
            PendingIntent.getActivity(
                context,
                1,
                Intent(context, QuickCaptureActivity::class.java).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        )

        manager.updateAppWidget(ids, views)
    }
}
