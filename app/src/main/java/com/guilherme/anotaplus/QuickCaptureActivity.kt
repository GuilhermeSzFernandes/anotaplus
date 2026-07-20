package com.guilherme.anotaplus

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.guilherme.anotaplus.data.AppDatabase
import com.guilherme.anotaplus.data.Entry
import com.guilherme.anotaplus.data.EntryType
import com.guilherme.anotaplus.data.Prefs
import com.guilherme.anotaplus.databinding.ActivityQuickCaptureBinding
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class QuickCaptureActivity : AppCompatActivity() {

    private lateinit var binding: ActivityQuickCaptureBinding
    private var tipoSelecionado: EntryType = EntryType.PENSAMENTO

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Ícone da tela inicial sempre chega com sourceBounds (a posição do
        // ícone na tela, usada na animação de abertura do launcher). O gesto
        // do Moto Actions dispara o mesmo intent de LAUNCHER, mas não vem de
        // um toque na tela, então nunca tem sourceBounds. É essa diferença
        // que usamos pra separar as duas origens sem precisar mexer na
        // configuração do gesto no aparelho.
        if (intent.sourceBounds != null) {
            startActivity(Intent(this, HistoryActivity::class.java))
            finish()
            return
        }

        // Permite abrir por cima da tela de bloqueio (sem desbloquear o
        // aparelho) e acordar a tela se estiver apagada, do mesmo jeito que
        // o atalho da câmera funciona no lock screen.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }

        binding = ActivityQuickCaptureBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Garante que o teclado abra sozinho assim que o modal aparecer
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)

        // Toque fora do card fecha o modal, como um diálogo normal
        binding.rootScrim.setOnClickListener { finish() }

        val timestampFormat = SimpleDateFormat("dd MMM · HH:mm", Locale("pt", "BR"))
        binding.textTimestamp.text = timestampFormat.format(Date()).uppercase(Locale("pt", "BR"))

        val tipoPadrao = Prefs.getTipoPadrao(this)
        tipoSelecionado = tipoPadrao
        if (tipoPadrao == EntryType.GASTO) {
            binding.btnTipoGasto.isChecked = true
        } else {
            binding.btnTipoPensamento.isChecked = true
        }
        atualizarCamposPorTipo(tipoPadrao)

        lifecycleScope.launch {
            val nomes = AppDatabase.getInstance(applicationContext).categoryDao().getNomesOnce()
            binding.editCategoria.setAdapter(
                ArrayAdapter(this@QuickCaptureActivity, android.R.layout.simple_dropdown_item_1line, nomes)
            )
        }

        binding.toggleTipo.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            tipoSelecionado = if (checkedId == binding.btnTipoGasto.id) EntryType.GASTO else EntryType.PENSAMENTO
            atualizarCamposPorTipo(tipoSelecionado)
        }

        binding.btnSalvar.setOnClickListener { salvar() }

        binding.btnHistorico.setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java))
            finish()
        }

        binding.editTexto.requestFocus()
    }

    private fun atualizarCamposPorTipo(tipo: EntryType) {
        val isGasto = tipo == EntryType.GASTO
        binding.layoutValor.visibility = if (isGasto) android.view.View.VISIBLE else android.view.View.GONE
        binding.editCategoria.visibility = if (isGasto) android.view.View.VISIBLE else android.view.View.GONE
        binding.editTexto.hint = getString(
            if (isGasto) R.string.hint_gasto else R.string.hint_pensamento
        )
    }

    private fun salvar() {
        val texto = binding.editTexto.text?.toString()?.trim().orEmpty()
        val valorTexto = binding.editValor.text?.toString()?.trim()
        val categoria = binding.editCategoria.text?.toString()?.trim()

        if (texto.isEmpty() && valorTexto.isNullOrEmpty()) {
            // nada pra salvar, só fecha
            finish()
            return
        }

        val valor = valorTexto?.replace(",", ".")?.toDoubleOrNull()

        val entry = Entry(
            type = tipoSelecionado,
            texto = texto,
            valor = if (tipoSelecionado == EntryType.GASTO) valor else null,
            categoria = if (tipoSelecionado == EntryType.GASTO) categoria?.ifEmpty { null } else null
        )

        lifecycleScope.launch {
            AppDatabase.getInstance(applicationContext).entryDao().insert(entry)
            finish()
        }
    }
}
