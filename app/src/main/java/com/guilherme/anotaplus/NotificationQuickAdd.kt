package com.guilherme.anotaplus

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.guilherme.anotaplus.data.EntryType
import com.guilherme.anotaplus.data.Prefs

/**
 * Notificação fixa (ongoing) com atalhos de captura rápida — outra forma
 * de driblar aparelhos sem gesto de fabricante configurável, ao lado dos
 * widgets, do ladrilho de Configurações Rápidas e dos atalhos de segurar o
 * ícone. Controlada por um switch em Conta (Prefs.isNotificacaoCapturaAtiva).
 *
 * Limitação conhecida: não sobrevive a reboot do aparelho (sem receiver de
 * BOOT_COMPLETED, de propósito — permissão sensível demais pro ganho aqui).
 * Volta a aparecer sozinha na próxima vez que qualquer parte do app rodar
 * (ver AnotaPlusApplication.onCreate).
 */
object NotificationQuickAdd {

    private const val CHANNEL_ID = "captura_rapida"
    private const val NOTIFICATION_ID = 1001

    // Chamado tanto no boot do processo (AnotaPlusApplication) quanto toda
    // vez que o usuário mexe no switch em Conta — decide sozinho se deve
    // mostrar, esconder, ou não fazer nada (sem permissão).
    fun atualizar(context: Context) {
        if (!Prefs.isNotificacaoCapturaAtiva(context)) {
            cancelar(context)
            return
        }
        if (!temPermissao(context)) return

        criarCanalSeNecessario(context)
        runCatching {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, construir(context))
        }
    }

    fun cancelar(context: Context) {
        runCatching { NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID) }
    }

    fun temPermissao(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
    }

    private fun criarCanalSeNecessario(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        val canal = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.canal_captura_rapida_nome),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = context.getString(R.string.canal_captura_rapida_descricao)
            setShowBadge(false)
        }
        manager.createNotificationChannel(canal)
    }

    private fun construir(context: Context): android.app.Notification {
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_add)
            .setContentTitle(context.getString(R.string.notificacao_captura_titulo))
            .setContentText(context.getString(R.string.notificacao_captura_texto))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(abrirCaptura(context, null, 0))
            .addAction(R.drawable.ic_add, context.getString(R.string.btn_gasto), abrirCaptura(context, EntryType.GASTO, 1))
            .addAction(R.drawable.ic_add, context.getString(R.string.btn_toggle_pensamento), abrirCaptura(context, EntryType.PENSAMENTO, 2))
            .addAction(R.drawable.ic_add, context.getString(R.string.btn_toggle_recebimento), abrirCaptura(context, EntryType.RECEBIMENTO, 3))
            .build()
    }

    private fun abrirCaptura(context: Context, tipoForcado: EntryType?, requestCode: Int): PendingIntent {
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
