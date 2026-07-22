package com.guilherme.anotaplus

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.guilherme.anotaplus.data.AppDatabase
import com.guilherme.anotaplus.data.Entry
import com.guilherme.anotaplus.data.EntryType
import com.guilherme.anotaplus.data.SessionPrefs
import com.guilherme.anotaplus.data.SubscriptionPrefs
import com.guilherme.anotaplus.databinding.ActivityEditEntryBinding
import com.guilherme.anotaplus.network.ApiClient
import com.guilherme.anotaplus.network.dto.EntrySyncRequest
import com.guilherme.anotaplus.widget.WidgetUpdater
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.Calendar

/**
 * Edição/exclusão de um Gasto existente — Ideia tem a própria tela
 * (ManualIdeiaActivity, que também edita, não só cria), sem o toggle de
 * tipo: converter uma Ideia em Gasto (ou vice-versa) não fazia sentido
 * pro usuário e só juntava campo de mais nessa tela.
 */
class EditEntryActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_ENTRY_ID = "entry_id"
    }

    private lateinit var binding: ActivityEditEntryBinding
    private var entryId: Long = -1
    private var remoteId: String? = null
    private var tipoOriginal: EntryType = EntryType.GASTO
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

        binding.editCarteira.setOnClickListener { binding.editCarteira.showDropDown() }
        lifecycleScope.launch {
            val nomes = AppDatabase.getInstance(applicationContext).carteiraDao().getNomesOnce()
            binding.editCarteira.setAdapter(
                ArrayAdapter(this@EditEntryActivity, android.R.layout.simple_dropdown_item_1line, nomes)
            )
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
        remoteId = entry.remoteId
        tipoOriginal = entry.type
        dataSelecionada.timeInMillis = entry.timestamp

        binding.editValor.setText(entry.valor?.let { String.format(MesUtil.locale, "%.2f", it) })
        binding.editCategoria.setText(entry.categoria.orEmpty(), false)
        binding.editCarteira.setText(entry.carteira.orEmpty(), false)
        binding.editTexto.setText(entry.texto)
        atualizarTextoData()

        binding.editValor.requestFocus()
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
        val carteira = binding.editCarteira.text?.toString()?.trim()
        val valor = valorTexto?.replace(",", ".")?.toDoubleOrNull()

        if (valor == null) {
            binding.editValor.error = getString(R.string.error_valor_obrigatorio)
            return
        }

        val entryAtualizada = Entry(
            id = entryId,
            type = tipoOriginal,
            texto = texto,
            valor = valor,
            categoria = categoria?.ifEmpty { null },
            carteira = carteira?.ifEmpty { null },
            timestamp = dataSelecionada.timeInMillis,
            remoteId = remoteId
        )

        AppDatabase.getInstance(applicationContext).entryDao().update(entryAtualizada)
        propagarEdicaoPraNuvem(entryAtualizada)
        WidgetUpdater.atualizarTodos(applicationContext)
        finish()
    }

    private suspend fun propagarEdicaoPraNuvem(entry: Entry) {
        val id = entry.remoteId ?: return
        if (!SubscriptionPrefs.podeFazerBackup(this)) return

        runCatching {
            ApiClient.api.atualizarEntry(
                "Bearer ${SessionPrefs.getAccessToken(this)}",
                id,
                EntrySyncRequest(
                    type = entry.type.name,
                    titulo = entry.titulo,
                    texto = entry.texto,
                    valor = entry.valor,
                    categoria = entry.categoria,
                    carteira = entry.carteira,
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
        if (id != null && SubscriptionPrefs.podeFazerBackup(this)) {
            runCatching {
                ApiClient.api.excluirEntry("Bearer ${SessionPrefs.getAccessToken(this)}", id)
            }
        }

        WidgetUpdater.atualizarTodos(applicationContext)
        finish()
    }
}
