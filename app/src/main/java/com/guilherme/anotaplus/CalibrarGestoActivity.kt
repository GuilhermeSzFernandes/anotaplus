package com.guilherme.anotaplus

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.guilherme.anotaplus.data.Prefs
import com.guilherme.anotaplus.databinding.ActivityCalibrarGestoBinding
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * Tela de calibração do gesto de toque duplo via acelerômetro (ver
 * TapBackGestureService). Mostra a leitura do sensor ao vivo e deixa
 * testar a detecção com o limiar ainda não salvo, porque o valor ideal
 * varia demais entre aparelho/capinha/jeito de bater — ajustar às cegas
 * (só girando um número em Configurações) não dava pra saber se ia
 * funcionar sem instalar, sair da tela e tentar o gesto de verdade.
 */
class CalibrarGestoActivity : AppCompatActivity(), SensorEventListener {

    companion object {
        private const val LIMIAR_MIN = 8f
        private const val LIMIAR_MAX = 45f

        // Teto da barra de leitura ao vivo — folga acima do LIMIAR_MAX pra
        // dar pra ver o pico passando do limiar, não só encostando no topo.
        private const val TETO_BARRA = 55f
    }

    private lateinit var binding: ActivityCalibrarGestoBinding
    private val handler = Handler(Looper.getMainLooper())
    private var sensorManager: SensorManager? = null

    private var limiarAtual = Prefs.LIMIAR_GESTO_ACELEROMETRO_PADRAO
    private var ultimoPicoEm = 0L
    private var ultimoDisparoEm = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCalibrarGestoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sensorManager = getSystemService(SensorManager::class.java)

        limiarAtual = Prefs.getLimiarGestoAcelerometro(this)
        binding.sliderLimiar.valueFrom = LIMIAR_MIN
        binding.sliderLimiar.valueTo = LIMIAR_MAX
        binding.sliderLimiar.value = limiarAtual.coerceIn(LIMIAR_MIN, LIMIAR_MAX)
        atualizarTextoLimiar(binding.sliderLimiar.value)

        binding.sliderLimiar.addOnChangeListener { _, valor, _ ->
            limiarAtual = valor
            atualizarTextoLimiar(valor)
        }

        binding.btnCalibrarRestaurar.setOnClickListener {
            binding.sliderLimiar.value = Prefs.LIMIAR_GESTO_ACELEROMETRO_PADRAO
        }
        binding.btnCalibrarSalvar.setOnClickListener { salvar() }
        binding.btnCalibrarConcluir.setOnClickListener { finish() }
    }

    override fun onResume() {
        super.onResume()
        val acelerometro = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        if (acelerometro != null) {
            sensorManager?.registerListener(this, acelerometro, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    override fun onPause() {
        sensorManager?.unregisterListener(this)
        super.onPause()
    }

    override fun onSensorChanged(event: SensorEvent) {
        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]
        val desvioDaGravidade = abs(sqrt(x * x + y * y + z * z) - SensorManager.GRAVITY_EARTH)

        atualizarLeituraAoVivo(desvioDaGravidade)

        if (desvioDaGravidade < limiarAtual) return

        val agora = System.currentTimeMillis()
        if (agora - ultimoDisparoEm < TapBackGestureService.DEBOUNCE_MS) return

        val intervaloDesdeOUltimoPico = agora - ultimoPicoEm
        if (intervaloDesdeOUltimoPico in TapBackGestureService.JANELA_MIN_MS..TapBackGestureService.JANELA_MAX_MS) {
            ultimoPicoEm = 0L
            ultimoDisparoEm = agora
            mostrarDeteccao()
        } else {
            ultimoPicoEm = agora
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun atualizarLeituraAoVivo(desvio: Float) {
        binding.textLeituraAtual.text = getString(R.string.format_calibrar_leitura, desvio)
        val progresso = (desvio / TETO_BARRA * 100f).roundToInt().coerceIn(0, 100)
        // animated=false: a leitura chega a ~50x/s (SENSOR_DELAY_GAME), animar
        // cada atualização faria a barra brigar com a próxima e parecer travada.
        binding.progressLeitura.setProgressCompat(progresso, false)
    }

    private fun atualizarTextoLimiar(valor: Float) {
        binding.textValorLimiar.text = valor.roundToInt().toString()
    }

    private fun mostrarDeteccao() {
        binding.textStatusTeste.text = getString(R.string.calibrar_teste_detectado)
        binding.textStatusTeste.setTextColor(getColor(R.color.color_positivo))
        vibrar()
        handler.postDelayed({
            binding.textStatusTeste.text = getString(R.string.calibrar_teste_instrucao)
            binding.textStatusTeste.setTextColor(getColor(R.color.ink_soft))
        }, 1200)
    }

    private fun vibrar() {
        val vibrator = getSystemService(Vibrator::class.java) ?: return
        vibrator.vibrate(VibrationEffect.createOneShot(80, VibrationEffect.DEFAULT_AMPLITUDE))
    }

    private fun salvar() {
        Prefs.setLimiarGestoAcelerometro(this, limiarAtual)
        // Se o serviço já tiver rodando, reinicia pra ele reler o novo
        // limiar (só é lido uma vez, no onCreate do serviço).
        if (Prefs.isGestoAcelerometroAtivo(this)) {
            TapBackGestureService.parar(this)
            TapBackGestureService.iniciar(this)
        }
        Toast.makeText(this, R.string.calibrar_salvo_toast, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }
}
