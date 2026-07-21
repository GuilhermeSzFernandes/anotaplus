package com.guilherme.anotaplus

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.guilherme.anotaplus.data.AppDatabase
import com.guilherme.anotaplus.data.Entry
import com.guilherme.anotaplus.data.EntryType
import com.guilherme.anotaplus.data.SessionPrefs
import com.guilherme.anotaplus.databinding.ActivityEditEntryBinding
import com.guilherme.anotaplus.network.ApiClient
import com.guilherme.anotaplus.network.dto.EntrySyncRequest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.Calendar

class EditEntryActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_ENTRY_ID = "entry_id"
    }

    private lateinit var binding: ActivityEditEntryBinding
    private var entryId: Long = -1
    private var tipoSelecionado: EntryType = EntryType.PENSAMENTO
    private var remoteId: String? = null
    private val dataSelecionada: Calendar = Calendar.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditEntryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        entryId = intent.getLongExtra(EXTRA_ENTRY_ID, -1)
        if (entryId == -1L) {
            finish()
            return
        }

        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.textData.setOnClickListener { abrirSeletorData() }

        binding.editCategoria.setOnClickListener { binding.editCategoria.showDropDown() }
        lifecycleScope.launch {
            val nomes = AppDatabase.getInstance(applicationContext).categoryDao().getNomesOnce()
            binding.editCategoria.setAdapter(
                ArrayAdapter(this@EditEntryActivity, android.R.layout.simple_dropdown_item_1line, nomes)
            )
        }

        binding.toggleTipo.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            tipoSelecionado = if (checkedId == binding.btnTipoGasto.id) EntryType.GASTO else EntryType.PENSAMENTO
            atualizarCamposPorTipo(tipoSelecionado)
            focarValorSeGasto(tipoSelecionado)
        }

        binding.btnSalvar.setOnClickListener { lifecycleScope.launch { salvar() } }
        binding.btnExcluir.setOnClickListener { confirmarExclusao() }

        lifecycleScope.launch {
            val entry = AppDatabase.getInstance(applicationContext).entryDao().getById(entryId)
            if (entry == null) {
                finish()
                return@launch
            }
            preencherCampos(entry)
        }
    }

    private fun preencherCampos(entry: Entry) {
        tipoSelecionado = entry.type
        remoteId = entry.remoteId
        dataSelecionada.timeInMillis = entry.timestamp

        if (entry.type == EntryType.GASTO) {
            binding.btnTipoGasto.isChecked = true
        } else {
            binding.btnTipoPensamento.isChecked = true
        }
        atualizarCamposPorTipo(entry.type)

        binding.editValor.setText(entry.valor?.let { String.format(MesUtil.locale, "%.2f", it) })
        binding.editCategoria.setText(entry.categoria.orEmpty(), false)
        binding.editTexto.setText(entry.texto)
        atualizarTextoData()
        focarValorSeGasto(entry.type)
    }

    // Ao abrir direto em Gasto (ou trocar pra Gasto), o valor é o campo que
    // a pessoa mais provavelmente quer corrigir primeiro.
    private fun focarValorSeGasto(tipo: EntryType) {
        if (tipo != EntryType.GASTO) return
        binding.editValor.requestFocus()
        binding.editValor.post {
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(binding.editValor, InputMethodManager.SHOW_IMPLICIT)
        }
    }

    private fun atualizarCamposPorTipo(tipo: EntryType) {
        val isGasto = tipo == EntryType.GASTO
        binding.layoutValor.visibility = if (isGasto) View.VISIBLE else View.GONE
        binding.editCategoria.visibility = if (isGasto) View.VISIBLE else View.GONE
        binding.editTexto.hint = getString(if (isGasto) R.string.hint_gasto else R.string.hint_pensamento)
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

    private suspend fun salvar() {
        val texto = binding.editTexto.text?.toString()?.trim().orEmpty()
        val valorTexto = binding.editValor.text?.toString()?.trim()
        val categoria = binding.editCategoria.text?.toString()?.trim()
        val valor = valorTexto?.replace(",", ".")?.toDoubleOrNull()

        if (tipoSelecionado == EntryType.GASTO && valor == null) {
            binding.editValor.error = getString(R.string.error_valor_obrigatorio)
            return
        }

        val entryAtualizada = Entry(
            id = entryId,
            type = tipoSelecionado,
            texto = texto,
            valor = if (tipoSelecionado == EntryType.GASTO) valor else null,
            categoria = if (tipoSelecionado == EntryType.GASTO) categoria?.ifEmpty { null } else null,
            timestamp = dataSelecionada.timeInMillis,
            remoteId = remoteId
        )

        AppDatabase.getInstance(applicationContext).entryDao().update(entryAtualizada)
        propagarEdicaoPraNuvem(entryAtualizada)
        finish()
    }

    private suspend fun propagarEdicaoPraNuvem(entry: Entry) {
        val id = entry.remoteId ?: return
        if (!SessionPrefs.estaLogado(this)) return

        runCatching {
            ApiClient.api.atualizarEntry(
                "Bearer ${SessionPrefs.getAccessToken(this)}",
                id,
                EntrySyncRequest(
                    type = entry.type.name,
                    texto = entry.texto,
                    valor = entry.valor,
                    categoria = entry.categoria,
                    timestamp = Instant.ofEpochMilli(entry.timestamp).toString()
                )
            )
        }
    }

    private fun confirmarExclusao() {
        MaterialAlertDialogBuilder(this)
            .setMessage(R.string.confirmar_exclusao)
            .setPositiveButton(R.string.btn_excluir) { _, _ -> lifecycleScope.launch { excluir() } }
            .setNegativeButton(R.string.btn_cancelar, null)
            .show()
    }

    private suspend fun excluir() {
        AppDatabase.getInstance(applicationContext).entryDao().deleteById(entryId)

        val id = remoteId
        if (id != null && SessionPrefs.estaLogado(this)) {
            runCatching {
                ApiClient.api.excluirEntry("Bearer ${SessionPrefs.getAccessToken(this)}", id)
            }
        }

        finish()
    }
}
