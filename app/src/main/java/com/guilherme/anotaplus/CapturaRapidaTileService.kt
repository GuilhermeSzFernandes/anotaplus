package com.guilherme.anotaplus

import android.app.PendingIntent
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService

/**
 * Ladrilho de Configurações Rápidas (puxar a barra de notificação + editar
 * ladrilhos) — jeito de abrir a Captura Rápida em qualquer Android, sem
 * depender de gesto de fabricante nenhum. Só o Moto abre o gesto "toque
 * duas vezes na traseira" pra apps de terceiros; testamos e confirmamos
 * que nem Xiaomi/HyperOS libera isso pra outros apps. API pública de
 * verdade (TileService), sem o risco de política que um
 * AccessibilityService usado só como atalho teria na Play Store.
 *
 * Sem sourceBounds no Intent (igual o "+" do widget) — QuickCaptureActivity
 * cai naturalmente no caminho de "abrir o modal de captura" em vez de ir
 * pro Histórico, sem precisar de nenhuma lógica extra aqui.
 */
class CapturaRapidaTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        qsTile?.apply {
            label = getString(R.string.tile_captura_rapida)
            icon = Icon.createWithResource(this@CapturaRapidaTileService, R.drawable.ic_add)
            state = Tile.STATE_ACTIVE
            updateTile()
        }
    }

    override fun onClick() {
        super.onClick()
        val intent = Intent(this, QuickCaptureActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        if (isLocked) {
            unlockAndRun { abrirCaptura(intent) }
        } else {
            abrirCaptura(intent)
        }
    }

    private fun abrirCaptura(intent: Intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            // A partir do Android 14, startActivityAndCollapse(Intent) foi
            // removido — só a variante com PendingIntent continua válida.
            val pendingIntent = PendingIntent.getActivity(
                this, 0, intent, PendingIntent.FLAG_IMMUTABLE
            )
            startActivityAndCollapse(pendingIntent)
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(intent)
        }
    }
}
