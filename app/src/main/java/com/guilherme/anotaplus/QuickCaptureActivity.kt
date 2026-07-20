package com.guilherme.anotaplus

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.guilherme.anotaplus.data.AppDatabase
import com.guilherme.anotaplus.data.Entry
import com.guilherme.anotaplus.data.EntryType
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
        binding = ActivityQuickCaptureBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Garante que o teclado abra sozinho assim que o modal aparecer
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)

        // Toque fora do card fecha o modal, como um diálogo normal
        binding.rootScrim.setOnClickListener { finish() }

        val timestampFormat = SimpleDateFormat("dd MMM · HH:mm", Locale("pt", "BR"))
        binding.textTimestamp.text = timestampFormat.format(Date()).uppercase(Locale("pt", "BR"))

        binding.btnTipoPensamento.isChecked = true
        atualizarCamposPorTipo(EntryType.PENSAMENTO)

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
