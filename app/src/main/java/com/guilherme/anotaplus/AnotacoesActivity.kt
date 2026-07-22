package com.guilherme.anotaplus

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.guilherme.anotaplus.data.AppDatabase
import com.guilherme.anotaplus.data.Entry
import com.guilherme.anotaplus.data.EntryType
import com.guilherme.anotaplus.data.SubscriptionPrefs
import com.guilherme.anotaplus.databinding.ActivityAnotacoesBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class AnotacoesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAnotacoesBinding
    private val adapter = NotaAdapter(
        onAbrir = { entry ->
            startActivity(
                Intent(this, ManualIdeiaActivity::class.java)
                    .putExtra(ManualIdeiaActivity.EXTRA_ENTRY_ID, entry.id)
            )
        },
        onSelecaoMudou = { atualizarBarraSelecao() }
    )
    private var adViewBanner: AdView? = null
    private var todasAsNotas: List<Entry> = emptyList()
    private var textoBusca: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAnotacoesBinding.inflate(layoutInflater)
        setContentView(binding.root)
        aplicarEdgeToEdge(binding.root, binding.header, binding.bottomNav.root)

        configurarBottomNav(binding.bottomNav, NavTab.ANOTACOES)
        binding.btnAddIdeia.setOnClickListener {
            startActivity(Intent(this, ManualIdeiaActivity::class.java))
        }
        binding.btnEmptyAdd.setOnClickListener {
            startActivity(Intent(this, ManualIdeiaActivity::class.java))
        }

        binding.btnFecharSelecao.setOnClickListener { adapter.limparSelecao() }
        binding.btnTagSelecao.setOnClickListener { mostrarDialogoTag() }
        binding.btnExcluirSelecao.setOnClickListener { confirmarExclusaoMultipla() }

        binding.recyclerEntries.layoutManager = GridLayoutManager(this, 2)
        binding.recyclerEntries.adapter = adapter

        binding.editBusca.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                textoBusca = s?.toString().orEmpty()
                aplicarFiltro()
            }
        })

        val dao = AppDatabase.getInstance(applicationContext).entryDao()

        lifecycleScope.launch {
            dao.getByType(EntryType.PENSAMENTO).collectLatest { entries ->
                todasAsNotas = entries
                aplicarFiltro()
            }
        }
    }

    // Filtro client-side sobre a lista já carregada (não é uma query nova
    // no banco): título e corpo do texto, sem diferenciar maiúscula/minúscula.
    private fun aplicarFiltro() {
        val query = textoBusca.trim()
        val filtradas = if (query.isEmpty()) {
            todasAsNotas
        } else {
            todasAsNotas.filter {
                it.texto.contains(query, ignoreCase = true) ||
                    it.titulo?.contains(query, ignoreCase = true) == true
            }
        }
        adapter.submitList(filtradas)
        binding.layoutEmpty.visibility = if (filtradas.isEmpty()) View.VISIBLE else View.GONE
        binding.textEmpty.text = if (query.isNotEmpty() && todasAsNotas.isNotEmpty()) {
            getString(R.string.empty_busca_sem_resultado)
        } else {
            getString(R.string.empty_history)
        }
        binding.btnEmptyAdd.visibility = if (query.isNotEmpty() && todasAsNotas.isNotEmpty()) View.GONE else View.VISIBLE
    }

    // Header normal vs barra de seleção múltipla — nunca os dois juntos.
    private fun atualizarBarraSelecao() {
        val emSelecao = adapter.modoSelecao
        binding.header.visibility = if (emSelecao) View.GONE else View.VISIBLE
        binding.layoutSelecao.visibility = if (emSelecao) View.VISIBLE else View.GONE
        if (emSelecao) {
            binding.textQtdSelecionadas.text = getString(R.string.format_qtd_selecionadas, adapter.qtdSelecionados())
        }
    }

    private fun confirmarExclusaoMultipla() {
        val ids = adapter.idsSelecionados()
        if (ids.isEmpty()) return
        MaterialAlertDialogBuilder(this)
            .setMessage(getString(R.string.confirmar_exclusao_multipla, ids.size))
            .setPositiveButton(R.string.btn_excluir) { _, _ ->
                lifecycleScope.launch {
                    AppDatabase.getInstance(applicationContext).entryDao().deleteByIds(ids)
                }
                adapter.limparSelecao()
            }
            .setNegativeButton(R.string.btn_cancelar, null)
            .show()
    }

    private fun mostrarDialogoTag() {
        val ids = adapter.idsSelecionados()
        if (ids.isEmpty()) return

        val editTag = EditText(this).apply {
            hint = getString(R.string.hint_tag_nota)
            setPadding(48, 24, 48, 24)
        }

        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.title_definir_tag, ids.size))
            .setView(editTag)
            .setPositiveButton(R.string.btn_salvar) { _, _ ->
                val tag = editTag.text?.toString()?.trim()?.ifEmpty { null }
                lifecycleScope.launch {
                    AppDatabase.getInstance(applicationContext).entryDao().atualizarCategoriaEmMassa(ids, tag)
                }
                adapter.limparSelecao()
            }
            .setNegativeButton(R.string.btn_cancelar, null)
            .show()
    }

    override fun onResume() {
        super.onResume()
        atualizarBanner()
    }

    // runCatching em volta de tudo: anúncio é sempre secundário, uma falha
    // do AdMob não pode derrubar a tela (ver FinanceiroActivity, mesmo padrão).
    private fun atualizarBanner() {
        runCatching { adViewBanner?.destroy() }
        binding.adContainer.removeAllViews()
        adViewBanner = null

        if (SubscriptionPrefs.isPro(this)) return

        runCatching {
            val adView = AdView(this).apply {
                setAdSize(AdSize.BANNER)
                adUnitId = BuildConfig.AD_BANNER_UNIT_ID
            }
            adViewBanner = adView
            binding.adContainer.addView(adView)
            adView.loadAd(AdRequest.Builder().build())
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        runCatching { adViewBanner?.destroy() }
    }
}
