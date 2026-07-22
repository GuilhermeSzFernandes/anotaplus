package com.guilherme.anotaplus

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.guilherme.anotaplus.data.AppDatabase
import com.guilherme.anotaplus.data.Entry
import com.guilherme.anotaplus.data.EntryType
import com.guilherme.anotaplus.data.SubscriptionPrefs
import com.guilherme.anotaplus.databinding.ActivityManualRecebimentoBinding
import com.guilherme.anotaplus.widget.WidgetUpdater
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar

class ManualRecebimentoActivity : AppCompatActivity() {

    private lateinit var binding: ActivityManualRecebimentoBinding
    private var dataSelecionada: Calendar = Calendar.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityManualRecebimentoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener { finish() }

        atualizarTextoData()
        binding.textData.setOnClickListener { abrirSeletorData() }

        binding.editCategoria.setOnClickListener { binding.editCategoria.showDropDown() }
        lifecycleScope.launch {
            val nomes = AppDatabase.getInstance(applicationContext).categoryDao().getNomesOnce()
            binding.editCategoria.setAdapter(
                ArrayAdapter(this@ManualRecebimentoActivity, android.R.layout.simple_dropdown_item_1line, nomes)
            )
        }

        binding.editCarteira.setOnClickListener { binding.editCarteira.showDropDown() }
        lifecycleScope.launch {
            val nomes = AppDatabase.getInstance(applicationContext).carteiraDao().getNomesOnce()
            binding.editCarteira.setAdapter(
                ArrayAdapter(this@ManualRecebimentoActivity, android.R.layout.simple_dropdown_item_1line, nomes)
            )
        }

        binding.btnSalvar.setOnClickListener { salvar() }

        binding.editValor.requestFocus()
        binding.editValor.post {
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(binding.editValor, InputMethodManager.SHOW_IMPLICIT)
        }
    }

    private fun abrirSeletorData() {
        val ano = dataSelecionada.get(Calendar.YEAR)
        val mes = dataSelecionada.get(Calendar.MONTH)
        val dia = dataSelecionada.get(Calendar.DAY_OF_MONTH)

        DatePickerDialog(this, { _, anoEscolhido, mesEscolhido, diaEscolhido ->
            dataSelecionada.set(Calendar.YEAR, anoEscolhido)
            dataSelecionada.set(Calendar.MONTH, mesEscolhido)
            dataSelecionada.set(Calendar.DAY_OF_MONTH, diaEscolhido)
            abrirSeletorHora()
        }, ano, mes, dia).show()
    }

    private fun abrirSeletorHora() {
        val hora = dataSelecionada.get(Calendar.HOUR_OF_DAY)
        val minuto = dataSelecionada.get(Calendar.MINUTE)

        TimePickerDialog(this, { _, horaEscolhida, minutoEscolhido ->
            dataSelecionada.set(Calendar.HOUR_OF_DAY, horaEscolhida)
            dataSelecionada.set(Calendar.MINUTE, minutoEscolhido)
            atualizarTextoData()
        }, hora, minuto, true).show()
    }

    private fun atualizarTextoData() {
        val format = SimpleDateFormat("dd MMM yyyy · HH:mm", MesUtil.locale)
        binding.textData.text = format.format(dataSelecionada.time).uppercase(MesUtil.locale)
    }

    private fun salvar() {
        val texto = binding.editTexto.text?.toString()?.trim().orEmpty()
        val valorTexto = binding.editValor.text?.toString()?.trim()
        val categoria = binding.editCategoria.text?.toString()?.trim()
        val carteira = binding.editCarteira.text?.toString()?.trim()
        val valor = valorTexto?.replace(",", ".")?.toDoubleOrNull()

        if (valor == null) {
            binding.editValor.error = getString(R.string.error_valor_obrigatorio)
            return
        }

        val entry = Entry(
            type = EntryType.RECEBIMENTO,
            texto = texto,
            valor = valor,
            categoria = categoria?.ifEmpty { null },
            carteira = carteira?.ifEmpty { null },
            timestamp = dataSelecionada.timeInMillis
        )

        lifecycleScope.launch {
            AppDatabase.getInstance(applicationContext).entryDao().insert(entry)
            WidgetUpdater.atualizarTodos(applicationContext)
            if (SubscriptionPrefs.podeFazerBackup(this@ManualRecebimentoActivity)) {
                SyncWorker.agendar(applicationContext)
            }
            finish()
        }
    }
}
