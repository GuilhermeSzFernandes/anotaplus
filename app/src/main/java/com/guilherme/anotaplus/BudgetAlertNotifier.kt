package com.guilherme.anotaplus

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.guilherme.anotaplus.data.AppDatabase
import java.util.Calendar

/**
 * Alerta de orçamento: dispara uma notificação quando um Gasto empurra o
 * total do mês numa categoria pra cima do limite definido (CategoriasActivity).
 * Chamado depois de salvar/editar um Gasto com categoria (ManualGastoActivity,
 * QuickCaptureActivity, EditEntryActivity) — silencioso se a categoria não
 * tem limite ou se o total ainda está dentro dele.
 *
 * Notificação usa um id fixo por categoria (hash do nome): estourar o limite
 * de novo no mesmo mês atualiza a notificação existente em vez de empilhar
 * uma nova, sem precisar guardar "já notificou" em lugar nenhum.
 */
object BudgetAlertNotifier {

    private const val CHANNEL_ID = "alertas_orcamento"

    suspend fun verificarLimite(context: Context, categoria: String?) {
        if (categoria.isNullOrBlank()) return
        if (!NotificationQuickAdd.temPermissao(context)) return

        val db = AppDatabase.getInstance(context)
        val cat = db.categoryDao().getByNome(categoria) ?: return
        val limite = cat.limite ?: return
        if (limite <= 0) return

        val agora = Calendar.getInstance()
        val total = db.entryDao().getTotalGastoPorCategoriaOnce(
            categoria,
            MesUtil.inicioDoMes(agora),
            MesUtil.fimDoMes(agora)
        )
        if (total < limite) return

        notificar(context, categoria, total, limite)
    }

    private fun notificar(context: Context, categoria: String, total: Double, limite: Double) {
        criarCanalSeNecessario(context)

        val intent = Intent(context, CategoriasActivity::class.java)
            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val pendingIntent = PendingIntent.getActivity(
            context,
            categoria.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_wallet)
            .setContentTitle(context.getString(R.string.alerta_orcamento_titulo, categoria))
            .setContentText(
                context.getString(
                    R.string.alerta_orcamento_texto,
                    "R$ %.2f".format(MesUtil.locale, total),
                    "R$ %.2f".format(MesUtil.locale, limite)
                )
            )
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .build()

        runCatching {
            NotificationManagerCompat.from(context).notify(categoria.hashCode(), notification)
        }
    }

    private fun criarCanalSeNecessario(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        val canal = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.canal_alertas_orcamento_nome),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = context.getString(R.string.canal_alertas_orcamento_descricao)
        }
        manager.createNotificationChannel(canal)
    }
}
