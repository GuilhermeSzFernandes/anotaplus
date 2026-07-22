package com.guilherme.anotaplus

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.PopupMenu
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry as ChartEntry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.guilherme.anotaplus.data.AppDatabase
import com.guilherme.anotaplus.data.Entry
import com.guilherme.anotaplus.data.GastoDiario
import com.guilherme.anotaplus.data.Prefs
import com.guilherme.anotaplus.data.SubscriptionPrefs
import com.guilherme.anotaplus.databinding.ActivityFinanceiroBinding
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar

// Extrato único de Gasto + Recebimento (como uma conta bancária de
// verdade, sem separar por tipo): saldo do mês em destaque, gráfico de
// saldo acumulado dia a dia (MPAndroidChart), e a lista combinada logo
// abaixo — sinal +/- por lançamento já indica qual é qual (ver EntryAdapter).
class FinanceiroActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFinanceiroBinding
    private val adapter = EntryAdapter { entry ->
        startActivity(
            Intent(this, EditEntryActivity::class.java)
                .putExtra(EditEntryActivity.EXTRA_ENTRY_ID, entry.id)
        )
    }
    private val mesSelecionado = MutableStateFlow(Calendar.getInstance())
    private var adViewBanner: AdView? = null
    private var lancamentosDoMes: List<Entry> = emptyList()
    private var textoBusca: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFinanceiroBinding.inflate(layoutInflater)
        setContentView(binding.root)
        aplicarEdgeToEdge(binding.root, binding.header, binding.bottomNav.root)

        configurarBottomNav(binding.bottomNav, NavTab.FINANCEIRO)

        binding.btnAddLancamento.setOnClickListener { mostrarMenuAdicionar() }
        binding.btnEmptyAdd.setOnClickListener { mostrarMenuAdicionar() }
        binding.btnCategorias.setOnClickListener {
            startActivity(Intent(this, CategoriasActivity::class.java))
        }
        configurarGrafico()

        binding.recyclerEntries.layoutManager = LinearLayoutManager(this)
        binding.recyclerEntries.adapter = adapter

        binding.editBusca.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                textoBusca = s?.toString().orEmpty()
                aplicarFiltro()
            }
        })

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
            mesSelecionado.flatMapLatest { mes ->
                dao.getFinanceiroPorMes(MesUtil.inicioDoMes(mes), MesUtil.fimDoMes(mes))
            }.collectLatest { entries ->
                lancamentosDoMes = entries
                aplicarFiltro()
            }
        }

        lifecycleScope.launch {
            mesSelecionado.flatMapLatest { mes ->
                val inicio = MesUtil.inicioDoMes(mes)
                val fim = MesUtil.fimDoMes(mes)
                combine(
                    dao.getTotalGasto(inicio, fim),
                    dao.getTotalRecebimento(inicio, fim)
                ) { gasto, recebimento -> recebimento - gasto }
            }.collectLatest { saldo -> renderSaldo(saldo) }
        }

        lifecycleScope.launch {
            mesSelecionado.flatMapLatest { mes ->
                val inicio = MesUtil.inicioDoMes(mes)
                val fim = MesUtil.fimDoMes(mes)
                combine(
                    dao.getGastoPorDia(inicio, fim),
                    dao.getRecebimentoPorDia(inicio, fim)
                ) { gastos, recebimentos -> calcularSaldoAcumulado(mes, gastos, recebimentos) }
            }.collectLatest { pontos -> renderGrafico(pontos) }
        }

        TutorialTourManager.processar(this, TelaTutorial.FINANCEIRO)
    }

    // Filtro client-side sobre a lista já carregada do mês selecionado (não
    // é uma query nova): texto livre e categoria, sem diferenciar
    // maiúscula/minúscula.
    private fun aplicarFiltro() {
        val query = textoBusca.trim()
        val filtrados = if (query.isEmpty()) {
            lancamentosDoMes
        } else {
            lancamentosDoMes.filter {
                it.texto.contains(query, ignoreCase = true) ||
                    it.categoria?.contains(query, ignoreCase = true) == true
            }
        }
        adapter.submitList(filtrados)
        binding.layoutEmpty.visibility = if (filtrados.isEmpty()) View.VISIBLE else View.GONE
        binding.textEmpty.text = if (query.isNotEmpty() && lancamentosDoMes.isNotEmpty()) {
            getString(R.string.empty_busca_sem_resultado)
        } else {
            getString(R.string.empty_history)
        }
        binding.btnEmptyAdd.visibility = if (query.isNotEmpty() && lancamentosDoMes.isNotEmpty()) View.GONE else View.VISIBLE
    }

    private fun renderSaldo(saldo: Double) {
        binding.textSaldoValor.text = "R$ %.2f".format(MesUtil.locale, saldo)
        binding.textSaldoValor.setTextColor(
            ContextCompat.getColor(this, if (saldo < 0) R.color.gasto_color else R.color.color_positivo)
        )
    }

    // O banco não guarda saldo corrido, então reconstrói aqui: pega o net
    // (recebimento - gasto) de cada dia que teve lançamento, preenche os
    // dias sem lançamento com 0, e acumula em soma corrida do dia 1 até o
    // último dia do mês selecionado.
    private fun calcularSaldoAcumulado(
        mes: Calendar,
        gastos: List<GastoDiario>,
        recebimentos: List<GastoDiario>
    ): List<Double> {
        val diasFormat = SimpleDateFormat("yyyy-MM-dd", MesUtil.locale)
        val gastosPorDia = gastos.associate { it.dia to it.total }
        val recebimentosPorDia = recebimentos.associate { it.dia to it.total }
        val ultimoDia = (mes.clone() as Calendar).getActualMaximum(Calendar.DAY_OF_MONTH)

        var acumulado = 0.0
        return (1..ultimoDia).map { dia ->
            val data = (mes.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, dia) }
            val chave = diasFormat.format(data.time)
            val net = (recebimentosPorDia[chave] ?: 0.0) - (gastosPorDia[chave] ?: 0.0)
            acumulado += net
            acumulado
        }
    }

    // Configuração visual pra não parecer o padrão de fábrica da lib —
    // sem legenda/descrição/eixo direito, sem grade, sem interação (é só
    // um gráfico de leitura, não precisa de zoom/pan).
    private fun configurarGrafico() {
        binding.chartSaldo.apply {
            description.isEnabled = false
            legend.isEnabled = false
            setTouchEnabled(false)
            setPinchZoom(false)
            setScaleEnabled(false)
            setDrawGridBackground(false)
            axisRight.isEnabled = false
            axisLeft.setDrawGridLines(false)
            axisLeft.setDrawAxisLine(false)
            axisLeft.setDrawLabels(false)
            xAxis.setDrawGridLines(false)
            xAxis.setDrawAxisLine(false)
            xAxis.setDrawLabels(false)
            xAxis.position = XAxis.XAxisPosition.BOTTOM
        }
    }

    private fun renderGrafico(pontos: List<Double>) {
        val chartEntries = pontos.mapIndexed { index, valor -> ChartEntry((index + 1).toFloat(), valor.toFloat()) }
        val cor = ContextCompat.getColor(this, R.color.brass)
        val dataSet = LineDataSet(chartEntries, "saldo").apply {
            color = cor
            lineWidth = 2f
            setDrawCircles(false)
            setDrawValues(false)
            mode = LineDataSet.Mode.CUBIC_BEZIER
            setDrawFilled(true)
            fillColor = cor
            fillAlpha = 40
        }
        binding.chartSaldo.data = LineData(dataSet)
        binding.chartSaldo.invalidate()
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
