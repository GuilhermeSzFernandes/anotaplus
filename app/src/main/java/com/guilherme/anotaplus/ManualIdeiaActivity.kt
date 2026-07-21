package com.guilherme.anotaplus

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.guilherme.anotaplus.data.AppDatabase
import com.guilherme.anotaplus.data.Entry
import com.guilherme.anotaplus.data.EntryType
import com.guilherme.anotaplus.data.SessionPrefs
import com.guilherme.anotaplus.data.SubscriptionPrefs
import com.guilherme.anotaplus.databinding.ActivityManualIdeiaBinding
import com.guilherme.anotaplus.network.ApiClient
import com.guilherme.anotaplus.network.dto.EntrySyncRequest
import com.guilherme.anotaplus.widget.AnotacaoWidgetUpdater
import kotlinx.coroutines.launch
import java.time.Instant

/**
 * "Bloco de notas": cria E edita uma Ideia — diferente da Captura Rápida
 * (pequena, instantânea de propósito) e do editor de Gasto
 * (EditEntryActivity). Sem toggle de tipo: uma Ideia nunca vira Gasto por
 * aqui, então a tela fica só com os campos que fazem sentido pra ela.
 */
class ManualIdeiaActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_ENTRY_ID = "entry_id"
    }

    private lateinit var binding: ActivityManualIdeiaBinding
    private var entryId: Long = -1
    private var remoteId: String? = null
    private var timestampOriginal: Long = System.currentTimeMillis()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityManualIdeiaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        entryId = intent.getLongExtra(EXTRA_ENTRY_ID, -1)

        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.btnSalvar.setOnClickListener { lifecycleScope.launch { salvar() } }
        binding.btnExcluir.setOnClickListener { confirmarExclusao() }

        RichTextEngine.instalarFormatacaoLive(binding.editNota)
        RichTextEngine.instalarToqueChecklist(binding.editNota)
        binding.btnFormatoTitulo1.setOnClickListener { RichTextEngine.alternarTitulo1(binding.editNota) }
        binding.btnFormatoTitulo2.setOnClickListener { RichTextEngine.alternarTitulo2(binding.editNota) }
        binding.btnFormatoNegrito.setOnClickListener { RichTextEngine.alternarNegrito(binding.editNota) }
        binding.btnFormatoItalico.setOnClickListener { RichTextEngine.alternarItalico(binding.editNota) }
        binding.btnFormatoSublinhado.setOnClickListener { RichTextEngine.alternarSublinhado(binding.editNota) }
        binding.btnFormatoLista.setOnClickListener { RichTextEngine.alternarLista(binding.editNota) }
        binding.btnFormatoChecklist.setOnClickListener { RichTextEngine.alternarChecklist(binding.editNota) }
        binding.btnFormatoLimpar.setOnClickListener { RichTextEngine.limparFormatacao(binding.editNota) }

        if (entryId == -1L) {
            binding.editTitulo.requestFocus()
        } else {
            binding.toolbar.title = getString(R.string.title_editar_ideia)
            binding.btnExcluir.visibility = View.VISIBLE
            lifecycleScope.launch {
                val entry = AppDatabase.getInstance(applicationContext).entryDao().getById(entryId)
                if (entry == null) {
                    finish()
                    return@launch
                }
                preencherCampos(entry)
            }
        }
    }

    private fun preencherCampos(entry: Entry) {
        remoteId = entry.remoteId
        timestampOriginal = entry.timestamp
        binding.editTitulo.setText(entry.titulo.orEmpty())
        binding.editNota.setText(entry.texto)
    }

    private suspend fun salvar() {
        val titulo = binding.editTitulo.text?.toString()?.trim()
        val nota = binding.editNota.text?.toString()?.trim().orEmpty()
        val criandoNova = entryId == -1L

        if (criandoNova && titulo.isNullOrEmpty() && nota.isEmpty()) {
            finish()
            return
        }

        val entry = Entry(
            id = if (criandoNova) 0 else entryId,
            type = EntryType.PENSAMENTO,
            titulo = titulo?.ifEmpty { null },
            texto = nota,
            timestamp = timestampOriginal,
            remoteId = remoteId
        )

        val dao = AppDatabase.getInstance(applicationContext).entryDao()
        if (criandoNova) {
            dao.insert(entry)
        } else {
            dao.update(entry)
            propagarEdicaoPraNuvem(entry)
        }

        if (SubscriptionPrefs.podeFazerBackup(this)) {
            SyncWorker.agendar(applicationContext)
        }
        AnotacaoWidgetUpdater.atualizarTodos(applicationContext)
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

        AnotacaoWidgetUpdater.atualizarTodos(applicationContext)
        finish()
    }
}
