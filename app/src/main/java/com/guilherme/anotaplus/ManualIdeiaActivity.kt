package com.guilherme.anotaplus

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.guilherme.anotaplus.data.AppDatabase
import com.guilherme.anotaplus.data.Entry
import com.guilherme.anotaplus.data.EntryType
import com.guilherme.anotaplus.data.SubscriptionPrefs
import com.guilherme.anotaplus.databinding.ActivityManualIdeiaBinding
import kotlinx.coroutines.launch

/**
 * "Bloco de notas": jeito separado de criar uma Ideia de dentro do app
 * (a partir do Histórico), diferente da Captura Rápida — essa continua
 * pequena e instantânea de propósito; essa aqui é espaçosa, com título,
 * pensada pra quem quer escrever mais do que uma frase rápida.
 */
class ManualIdeiaActivity : AppCompatActivity() {

    private lateinit var binding: ActivityManualIdeiaBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityManualIdeiaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.editTitulo.requestFocus()
        binding.btnSalvar.setOnClickListener { lifecycleScope.launch { salvar() } }

        RichTextEngine.instalarFormatacaoLive(binding.editNota)
        RichTextEngine.instalarToqueChecklist(binding.editNota)
        binding.btnFormatoTitulo.setOnClickListener { RichTextEngine.alternarTitulo(binding.editNota) }
        binding.btnFormatoNegrito.setOnClickListener { RichTextEngine.alternarNegrito(binding.editNota) }
        binding.btnFormatoItalico.setOnClickListener { RichTextEngine.alternarItalico(binding.editNota) }
        binding.btnFormatoLista.setOnClickListener { RichTextEngine.alternarLista(binding.editNota) }
        binding.btnFormatoChecklist.setOnClickListener { RichTextEngine.alternarChecklist(binding.editNota) }
    }

    private suspend fun salvar() {
        val titulo = binding.editTitulo.text?.toString()?.trim()
        val nota = binding.editNota.text?.toString()?.trim().orEmpty()

        if (titulo.isNullOrEmpty() && nota.isEmpty()) {
            finish()
            return
        }

        val entry = Entry(
            type = EntryType.PENSAMENTO,
            titulo = titulo?.ifEmpty { null },
            texto = nota
        )

        AppDatabase.getInstance(applicationContext).entryDao().insert(entry)
        if (SubscriptionPrefs.podeFazerBackup(this)) {
            SyncWorker.agendar(applicationContext)
        }
        finish()
    }
}
