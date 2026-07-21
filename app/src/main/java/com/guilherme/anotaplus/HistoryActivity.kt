package com.guilherme.anotaplus

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.material.tabs.TabLayout
import com.guilherme.anotaplus.data.AppDatabase
import com.guilherme.anotaplus.data.EntryType
import com.guilherme.anotaplus.data.Prefs
import com.guilherme.anotaplus.data.SubscriptionPrefs
import com.guilherme.anotaplus.databinding.ActivityHistoryBinding
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import java.util.Calendar

class HistoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHistoryBinding
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
        binding = ActivityHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.toolbar.inflateMenu(R.menu.history_menu)
        binding.toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_add_gasto -> {
                    startActivity(Intent(this, ManualGastoActivity::class.java))
                    true
                }
                R.id.action_add_ideia -> {
                    startActivity(Intent(this, ManualIdeiaActivity::class.java))
                    true
                }
                R.id.action_report -> {
                    startActivity(Intent(this, ReportActivity::class.java))
                    true
                }
                R.id.action_settings -> {
                    startActivity(Intent(this, SettingsActivity::class.java))
                    true
                }
                else -> false
            }
        }

        binding.recyclerEntries.layoutManager = LinearLayoutManager(this)
        binding.recyclerEntries.adapter = adapter

        exibirAnunciosSeFree()

        // O filtro por mês só existe na aba Gasto; em Ideia o histórico
        // continua mostrando tudo, sem recorte de período.
        binding.tabTipo.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                val tipo = if (tab.position == 0) EntryType.GASTO else EntryType.PENSAMENTO
                tipoSelecionado.value = tipo
                val isGasto = tipo == EntryType.GASTO
                val visibility = if (isGasto) View.VISIBLE else View.GONE
                binding.rowMes.root.visibility = visibility
                binding.rowMesPerforation.visibility = visibility
                binding.toolbar.menu.findItem(R.id.action_add_gasto)?.isVisible = isGasto
                binding.toolbar.menu.findItem(R.id.action_add_ideia)?.isVisible = !isGasto
            }
            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })

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
                    if (tipo == EntryType.GASTO) {
                        dao.getByTypeAndRange(tipo, MesUtil.inicioDoMes(mes), MesUtil.fimDoMes(mes))
                    } else {
                        dao.getByType(tipo)
                    }
                }
                .collectLatest { entries ->
                    adapter.submitList(entries)
                    binding.textEmpty.visibility =
                        if (entries.isEmpty()) View.VISIBLE else View.GONE
                }
        }
    }

    private fun mudarMes(delta: Int) {
        val calendar = mesSelecionado.value.clone() as Calendar
        calendar.add(Calendar.MONTH, delta)
        mesSelecionado.value = calendar
    }

    // Plano Free tem anúncio; PRO não. Banner fixo sempre que a tela abre
    // (se Free); intersticial só de vez em quando, pra não ser tão chato.
    // runCatching em volta de tudo: anúncio é sempre secundário, uma
    // falha do AdMob não pode nunca mais derrubar a tela (já aconteceu).
    private fun exibirAnunciosSeFree() {
        if (SubscriptionPrefs.isPro(this)) return

        runCatching {
            // AdView criado 100% por código (não declarado no layout XML):
            // o AdMob exige que adSize/adUnitId sejam setados "do mesmo
            // jeito" — os dois via XML ou os dois via código — e a tag
            // <AdView> no XML sem esses atributos entrava em conflito com
            // setar por código depois, mostrando "Required XML attribute
            // 'adSize' was missing" em vez do anúncio de teste.
            val adView = AdView(this).apply {
                setAdSize(AdSize.BANNER)
                adUnitId = BuildConfig.AD_BANNER_UNIT_ID
            }
            adViewBanner = adView
            binding.adContainer.addView(adView)
            adView.loadAd(AdRequest.Builder().build())
        }

        val aberturas = Prefs.incrementarAberturasHistorico(this)
        if (aberturas % ABERTURAS_POR_INTERSTICIAL == 0) {
            runCatching {
                InterstitialAd.load(
                    this,
                    BuildConfig.AD_INTERSTITIAL_UNIT_ID,
                    AdRequest.Builder().build(),
                    object : InterstitialAdLoadCallback() {
                        override fun onAdLoaded(ad: InterstitialAd) {
                            runCatching { ad.show(this@HistoryActivity) }
                        }

                        override fun onAdFailedToLoad(error: LoadAdError) {
                            // Sem anúncio pra mostrar (sem internet, sem
                            // inventário etc.) — não bloqueia o uso do app.
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
