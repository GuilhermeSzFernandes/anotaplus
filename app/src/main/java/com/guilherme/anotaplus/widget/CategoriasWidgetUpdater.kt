package com.guilherme.anotaplus.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import com.guilherme.anotaplus.FinanceiroActivity
import com.guilherme.anotaplus.MesUtil
import com.guilherme.anotaplus.QuickCaptureActivity
import com.guilherme.anotaplus.R
import com.guilherme.anotaplus.data.AppDatabase
import com.guilherme.anotaplus.data.CategoriaEstilo
import com.guilherme.anotaplus.data.EntryType
import java.util.Calendar

/**
 * Redesenha o widget de categorias: gasto do mês em cada categoria, maior
 * pra menor, até 6 linhas fixas (mesmo padrão de AnotacaoWidgetUpdater —
 * ver comentário lá pro porquê de não ser uma coleção de verdade).
 * Chamado de dentro de WidgetUpdater.atualizarTodos, então já roda em todo
 * lugar que mexe num Gasto (criar, editar, excluir, restaurar backup).
 */
object CategoriasWidgetUpdater {

    private const val MAX_LINHAS = 6
    private val IDS_LINHA = intArrayOf(
        R.id.linha_cat_0, R.id.linha_cat_1, R.id.linha_cat_2,
        R.id.linha_cat_3, R.id.linha_cat_4, R.id.linha_cat_5
    )
    private val IDS_CHIP = intArrayOf(
        R.id.chip_cat_0, R.id.chip_cat_1, R.id.chip_cat_2,
        R.id.chip_cat_3, R.id.chip_cat_4, R.id.chip_cat_5
    )
    private val IDS_NOME = intArrayOf(
        R.id.nome_cat_0, R.id.nome_cat_1, R.id.nome_cat_2,
        R.id.nome_cat_3, R.id.nome_cat_4, R.id.nome_cat_5
    )
    private val IDS_VALOR = intArrayOf(
        R.id.valor_cat_0, R.id.valor_cat_1, R.id.valor_cat_2,
        R.id.valor_cat_3, R.id.valor_cat_4, R.id.valor_cat_5
    )

    suspend fun atualizarTodos(context: Context) {
        val manager = AppWidgetManager.getInstance(context)
        val ids = manager.getAppWidgetIds(ComponentName(context, CategoriasWidgetProvider::class.java))
        if (ids.isEmpty()) return

        val db = AppDatabase.getInstance(context)
        val agora = Calendar.getInstance()
        val totais = db.entryDao().getGastoPorCategoriaOnce(MesUtil.inicioDoMes(agora), MesUtil.fimDoMes(agora))
        val categoriasPorNome = db.categoryDao().getAllOnce().associateBy { it.nome }

        val views = RemoteViews(context.packageName, R.layout.widget_categorias)

        views.setViewVisibility(
            R.id.text_widget_categorias_vazio,
            if (totais.isEmpty()) View.VISIBLE else View.GONE
        )

        for (i in 0 until MAX_LINHAS) {
            val item = totais.getOrNull(i)
            if (item == null || item.categoria.isNullOrBlank()) {
                views.setViewVisibility(IDS_LINHA[i], View.GONE)
                continue
            }
            views.setViewVisibility(IDS_LINHA[i], View.VISIBLE)

            val categoriaInfo = categoriasPorNome[item.categoria]
            views.setTextViewText(IDS_CHIP[i], categoriaInfo?.icone?.takeIf { it.isNotBlank() } ?: "●")
            views.setTextColor(IDS_CHIP[i], CategoriaEstilo.corInt(categoriaInfo?.cor))
            views.setTextViewText(IDS_NOME[i], item.categoria)
            views.setTextViewText(IDS_VALOR[i], "R$ %.2f".format(MesUtil.locale, item.total))
        }

        views.setOnClickPendingIntent(
            R.id.widget_categorias_root,
            PendingIntent.getActivity(
                context,
                0,
                Intent(context, FinanceiroActivity::class.java).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        )
        views.setOnClickPendingIntent(
            R.id.btn_widget_categorias_add,
            PendingIntent.getActivity(
                context,
                1,
                Intent(context, QuickCaptureActivity::class.java)
                    .putExtra(QuickCaptureActivity.EXTRA_TIPO_FORCADO, EntryType.GASTO.name)
                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        )

        manager.updateAppWidget(ids, views)
    }
}
