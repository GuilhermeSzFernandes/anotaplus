package com.guilherme.anotaplus

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.PopupMenu
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.guilherme.anotaplus.data.AppDatabase
import com.guilherme.anotaplus.data.EntryType
import com.guilherme.anotaplus.data.Prefs
import com.guilherme.anotaplus.data.SubscriptionPrefs
import com.guilherme.anotaplus.databinding.ActivityFinanceiroBinding
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import java.util.Calendar

// Extrato de Gasto + Recebimento (o "Início" antigo, antes dele virar
// dashboard) — toggle escolhe o tipo, mês continua filtrando os dois.
class FinanceiroActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFinanceiroBinding
    private val adapter = EntryAdapter { entry ->
        startActivity(
            Intent(this, EditEntryActivity::class.java)
                .putExtra(EditEntryActivity.EXTRA_ENTRY_ID, entry.id)
        )
    }
    private val tipoSelecionado = MutableStateFlow(EntryType.GASTO)
    private val mesSelecionado = MutableStateFlow(Calendar.getInstance())
    private var adViewBanner: AdView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFinanceiroBinding.inflate(layoutInflater)
        setContentView(binding.root)
        aplicarEdgeToEdge(binding.root, binding.header, binding.bottomNav.root)

        configurarBottomNav(binding.bottomNav, NavTab.FINANCEIRO)

        binding.btnAddLancamento.setOnClickListener { mostrarMenuAdicionar() }

        binding.toggleTipoFinanceiro.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            tipoSelecionado.value = if (checkedId == binding.btnTipoFinanceiroRecebimento.id) {
                EntryType.RECEBIMENTO
            } else {
                EntryType.GASTO
            }
        }

        binding.recyclerEntries.layoutManager = LinearLayoutManager(this)
        binding.recyclerEntries.adapter = adapter

        mostrarIntersticialOcasionalSeFree()

        binding.rowMes.btnMesAnterior.setOnClickListener { mudarMes(-1) }
        binding.rowMes.btnMesProximo.setOnClickListener { mudarMes(1) }

        lifecycleScope.launch {
            mesSelecionado.collectLatest { calendar ->
                binding.rowMes.textMes.text = MesUtil.formatar(calendar)
                val temProximo = !MesUtil.estaNoMesAtual(calendar)
                binding.rowMes.btnMesProximo.isEnabled = temProximo
                binding.rowMes.btnMesProximo.alpha = if (temProximo) 1f else 0.3f
            }
        }

        val dao = AppDatabase.getInstance(applicationContext).entryDao()

        lifecycleScope.launch {
            combine(tipoSelecionado, mesSelecionado) { tipo, mes -> tipo to mes }
                .flatMapLatest { (tipo, mes) ->
                    dao.getByTypeAndRange(tipo, MesUtil.inicioDoMes(mes), MesUtil.fimDoMes(mes))
                }
                .collectLatest { entries ->
                    adapter.submitList(entries)
                    binding.textEmpty.visibility =
                        if (entries.isEmpty()) View.VISIBLE else View.GONE
                }
        }
    }

    private fun mostrarMenuAdicionar() {
        PopupMenu(this, binding.btnAddLancamento).apply {
            menuInflater.inflate(R.menu.add_lancamento_menu, menu)
            setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.action_add_gasto -> {
                        startActivity(Intent(this@FinanceiroActivity, ManualGastoActivity::class.java))
                        true
                    }
                    R.id.action_add_recebimento -> {
                        startActivity(Intent(this@FinanceiroActivity, ManualRecebimentoActivity::class.java))
                        true
                    }
                    else -> false
                }
            }
            show()
        }
    }

    private fun mudarMes(delta: Int) {
        val calendar = mesSelecionado.value.clone() as Calendar
        calendar.add(Calendar.MONTH, delta)
        mesSelecionado.value = calendar
    }

    override fun onResume() {
        super.onResume()
        // Em onResume (não só onCreate): status PRO pode ter mudado em
        // Conta (ex: switch de debug) sem essa Activity ser recriada —
        // voltar com o botão "voltar" só chama onResume, então o banner
        // precisa reavaliar toda vez, senão fica preso no estado de quando
        // a tela abriu.
        atualizarBanner()
    }

    // runCatching em volta de tudo: anúncio é sempre secundário, uma
    // falha do AdMob não pode nunca mais derrubar a tela (já aconteceu).
    private fun atualizarBanner() {
        runCatching { adViewBanner?.destroy() }
        binding.adContainer.removeAllViews()
        adViewBanner = null

        if (SubscriptionPrefs.isPro(this)) return

        runCatching {
            val adView = AdView(this).apply {
                setAdSize(AdSize.BANNER)
                adUnitId = BuildConfig.AD_BANNER_UNIT_ID
                if (BuildConfig.DEBUG) {
                    adListener = object : AdListener() {
                        override fun onAdLoaded() {
                            Toast.makeText(this@FinanceiroActivity, "Anúncio (banner) carregou", Toast.LENGTH_SHORT).show()
                        }

                        override fun onAdFailedToLoad(error: LoadAdError) {
                            Toast.makeText(
                                this@FinanceiroActivity,
                                "Anúncio (banner) falhou: ${error.message}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
            }
            adViewBanner = adView
            binding.adContainer.addView(adView)
            adView.loadAd(AdRequest.Builder().build())
        }.onFailure {
            if (BuildConfig.DEBUG) {
                Toast.makeText(this, "Erro ao criar anúncio: ${it.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    // Só na abertura de verdade (onCreate), não em todo onResume — senão
    // voltar de EditEntryActivity/outra tela contaria como "abertura" e
    // mostraria intersticial demais.
    private fun mostrarIntersticialOcasionalSeFree() {
        if (SubscriptionPrefs.isPro(this)) return

        val aberturas = Prefs.incrementarAberturasHistorico(this)
        if (aberturas % ABERTURAS_POR_INTERSTICIAL == 0) {
            runCatching {
                InterstitialAd.load(
                    this,
                    BuildConfig.AD_INTERSTITIAL_UNIT_ID,
                    AdRequest.Builder().build(),
                    object : InterstitialAdLoadCallback() {
                        override fun onAdLoaded(ad: InterstitialAd) {
                            runCatching { ad.show(this@FinanceiroActivity) }
                        }

                        override fun onAdFailedToLoad(error: LoadAdError) {
                            if (BuildConfig.DEBUG) {
                                Toast.makeText(
                                    this@FinanceiroActivity,
                                    "Anúncio (intersticial) falhou: ${error.message}",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    }
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        runCatching { adViewBanner?.destroy() }
    }

    companion object {
        private const val ABERTURAS_POR_INTERSTICIAL = 4
    }
}
