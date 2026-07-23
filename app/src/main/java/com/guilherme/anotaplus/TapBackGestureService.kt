package com.guilherme.anotaplus

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.guilherme.anotaplus.data.Prefs
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Detecta "toque duplo na traseira" via acelerômetro — alternativa opt-in
 * pra aparelhos que não deixam configurar esse gesto pelo sistema (a MIUI
 * da Xiaomi, por exemplo, só libera esse menu pra funções do próprio
 * sistema, nunca pra apps externos — ver GestureGuideActivity). Ligado e
 * desligado pelo switch em Conta (Prefs.isGestoAcelerometroAtivo); nunca
 * ativado sozinho, porque precisa rodar como foreground service o tempo
 * todo pra continuar recebendo eventos do sensor com a tela apagada.
 *
 * Heurística: cada "toque" vira um pico breve e isolado na aceleração (a
 * carcaça sendo batida), diferente de um movimento sustentado (andar,
 * sacudir no bolso). Dois picos assim dentro de uma janela curta = toque
 * duplo. Não é sensor de hardware dedicado (só o Pixel tem isso via
 * "Quick Tap"), então falso positivo/negativo acontece — o limiar de pico
 * é calibrável em CalibrarGestoActivity (Prefs.getLimiarGestoAcelerometro),
 * já que o valor ideal varia demais entre aparelho/capinha/jeito de bater.
 */
class TapBackGestureService : Service(), SensorEventListener {

    companion object {
        private const val CHANNEL_ID = "gesto_toque_duplo"
        private const val NOTIFICATION_ID = 1002

        // internal (não private) porque CalibrarGestoActivity reaproveita os
        // mesmos valores no modo de teste ao vivo, pra calibrar contra a
        // mesma lógica de detecção usada aqui de verdade.
        internal const val JANELA_MIN_MS = 60L
        internal const val JANELA_MAX_MS = 450L
        internal const val DEBOUNCE_MS = 1200L

        fun iniciar(context: Context) {
            context.startForegroundService(Intent(context, TapBackGestureService::class.java))
        }

        fun parar(context: Context) {
            context.stopService(Intent(context, TapBackGestureService::class.java))
        }
    }

    private var sensorManager: SensorManager? = null
    private var ultimoPicoEm = 0L
    private var ultimoDisparoEm = 0L

    // Lido uma vez na criação — se o usuário recalibrar em
    // CalibrarGestoActivity com o serviço já rodando, ela reinicia o
    // serviço pra esse valor novo entrar em vigor (ver Prefs.kt).
    private var limiarPico = Prefs.LIMIAR_GESTO_ACELEROMETRO_PADRAO

    override fun onCreate() {
        super.onCreate()
        limiarPico = Prefs.getLimiarGestoAcelerometro(this)
        criarCanalSeNecessario()
        startForeground(NOTIFICATION_ID, construirNotificacao())

        val manager = getSystemService(SensorManager::class.java)
        sensorManager = manager
        val acelerometro = manager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        if (acelerometro == null) {
            // Aparelho sem acelerômetro (raríssimo, mas o switch em
            // Configurações já devia ter ficado escondido nesse caso).
            stopSelf()
            return
        }
        manager.registerListener(this, acelerometro, SensorManager.SENSOR_DELAY_GAME)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        sensorManager?.unregisterListener(this)
        super.onDestroy()
    }

    override fun onSensorChanged(event: SensorEvent) {
        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]
        val desvioDaGravidade = sqrt(x * x + y * y + z * z) - SensorManager.GRAVITY_EARTH
        if (abs(desvioDaGravidade) < limiarPico) return

        val agora = System.currentTimeMillis()
        if (agora - ultimoDisparoEm < DEBOUNCE_MS) return

        val intervaloDesdeOUltimoPico = agora - ultimoPicoEm
        if (intervaloDesdeOUltimoPico in JANELA_MIN_MS..JANELA_MAX_MS) {
            ultimoPicoEm = 0L
            ultimoDisparoEm = agora
            abrirCapturaRapida()
        } else {
            ultimoPicoEm = agora
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun abrirCapturaRapida() {
        val intent = Intent(this, QuickCaptureActivity::class.java)
            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }

    private fun criarCanalSeNecessario() {
        val manager = getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        val canal = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.canal_gesto_toque_duplo_nome),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.canal_gesto_toque_duplo_descricao)
            setShowBadge(false)
        }
        manager.createNotificationChannel(canal)
    }

    private fun construirNotificacao(): android.app.Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, SettingsActivity::class.java).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_add)
            .setContentTitle(getString(R.string.notificacao_gesto_toque_duplo_titulo))
            .setContentText(getString(R.string.notificacao_gesto_toque_duplo_texto))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pendingIntent)
            .build()
    }
}
