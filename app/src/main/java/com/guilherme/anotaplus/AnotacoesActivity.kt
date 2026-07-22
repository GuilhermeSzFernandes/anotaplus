package com.guilherme.anotaplus

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
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
        anexarSwipeParaExcluir()

        val dao = AppDatabase.getInstance(applicationContext).entryDao()

        lifecycleScope.launch {
            dao.getByType(EntryType.PENSAMENTO).collectLatest { entries ->
                adapter.submitList(entries)
                binding.layoutEmpty.visibility = if (entries.isEmpty()) View.VISIBLE else View.GONE
            }
        }
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

    private fun anexarSwipeParaExcluir() {
        val callback = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = false

            // Seleção múltipla usa toque longo, não swipe — desliga o swipe
            // enquanto tiver algo selecionado pra não confundir os dois gestos.
            override fun getSwipeDirs(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int {
                return if (adapter.modoSelecao) 0 else super.getSwipeDirs(recyclerView, viewHolder)
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val posicao = viewHolder.bindingAdapterPosition
                if (posicao == RecyclerView.NO_POSITION) return
                excluirComDesfazer(adapter.entryNaPosicao(posicao))
            }
        }
        ItemTouchHelper(callback).attachToRecyclerView(binding.recyclerEntries)
    }

    private fun excluirComDesfazer(entry: Entry) {
        val dao = AppDatabase.getInstance(applicationContext).entryDao()
        lifecycleScope.launch { dao.deleteById(entry.id) }

        Snackbar.make(binding.root, R.string.nota_excluida, Snackbar.LENGTH_LONG)
            .setAction(R.string.btn_desfazer) {
                // Reinsere como registro novo (id/remoteId novos) — se
                // estava sincronizada antes, a próxima sincronização
                // manda ela de novo pro backend, sem duplicar lá (o
                // registro antigo já foi removido).
                lifecycleScope.launch {
                    dao.insert(entry.copy(id = 0, remoteId = null))
                }
            }
            .show()
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
