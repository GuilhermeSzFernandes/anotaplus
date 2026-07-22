package com.guilherme.anotaplus

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.guilherme.anotaplus.data.AppDatabase
import com.guilherme.anotaplus.data.CategoriaContagem
import com.guilherme.anotaplus.data.CategoriaTotal
import com.guilherme.anotaplus.data.Category
import com.guilherme.anotaplus.data.DiaSemanaTotal
import com.guilherme.anotaplus.data.Entry
import com.guilherme.anotaplus.data.GastoDiario
import com.guilherme.anotaplus.databinding.ActivityReportBinding
import com.guilherme.anotaplus.databinding.ItemReportCategoriaBinding
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.Calendar
import kotlin.math.roundToInt

private const val DIA_MS = 24L * 60 * 60 * 1000

private data class EstatisticasMes(
    val maiorGasto: Entry?,
    val media: Double,
    val categoriaMaisUsada: CategoriaContagem?,
    val diaSemanaTop: DiaSemanaTotal?
)

class ReportActivity : AppCompatActivity() {

    private lateinit var binding: ActivityReportBinding
    private val locale = MesUtil.locale
    private val mesSelecionado = MutableStateFlow(Calendar.getInstance())
    private val periodoTendencia = MutableStateFlow(30)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReportBinding.inflate(layoutInflater)
        setContentView(binding.root)
        aplicarEdgeToEdge(binding.root, binding.header, binding.bottomNav.root)

        configurarBottomNav(binding.bottomNav, NavTab.RELATORIO)

        binding.rowMes.btnMesAnterior.setOnClickListener { mudarMes(-1) }
        binding.rowMes.btnMesProximo.setOnClickListener { mudarMes(1) }

        binding.togglePeriodoTendencia.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            periodoTendencia.value = when (checkedId) {
                binding.btnPeriodo90.id -> 90
                binding.btnPeriodo365.id -> 365
                else -> 30
            }
        }

        val db = AppDatabase.getInstance(applicationContext)
        val dao = db.entryDao()
        val categoryDao = db.categoryDao()

        lifecycleScope.launch {
            mesSelecionado.collectLatest { calendar ->
                binding.rowMes.textMes.text = MesUtil.formatar(calendar)
                val temProximo = !MesUtil.estaNoMesAtual(calendar)
                binding.rowMes.btnMesProximo.isEnabled = temProximo
                binding.rowMes.btnMesProximo.alpha = if (temProximo) 1f else 0.3f
            }
        }

        lifecycleScope.launch {
            mesSelecionado.flatMapLatest { calendar ->
                dao.getTotalGasto(MesUtil.inicioDoMes(calendar), MesUtil.fimDoMes(calendar))
            }.collectLatest { total ->
                binding.textTotalGasto.text = "R$ %.2f".format(locale, total)
            }
        }

        lifecycleScope.launch {
            mesSelecionado.flatMapLatest { calendar ->
                val anterior = MesUtil.mesAnterior(calendar)
                combine(
                    dao.getTotalGasto(MesUtil.inicioDoMes(calendar), MesUtil.fimDoMes(calendar)),
                    dao.getTotalGasto(MesUtil.inicioDoMes(anterior), MesUtil.fimDoMes(anterior))
                ) { atual, anteriorTotal -> atual to anteriorTotal }
            }.collectLatest { (atual, anteriorTotal) -> renderComparativo(atual, anteriorTotal) }
        }

        lifecycleScope.launch {
            mesSelecionado.flatMapLatest { calendar ->
                dao.getTotalIdeias(MesUtil.inicioDoMes(calendar), MesUtil.fimDoMes(calendar))
            }.collectLatest { count ->
                binding.textTotalIdeias.text = getString(R.string.format_ideias_mes, count)
            }
        }

        lifecycleScope.launch {
            mesSelecionado.flatMapLatest { calendar ->
                val anterior = MesUtil.mesAnterior(calendar)
                combine(
                    dao.getGastoPorCategoria(MesUtil.inicioDoMes(calendar), MesUtil.fimDoMes(calendar)),
                    dao.getGastoPorCategoria(MesUtil.inicioDoMes(anterior), MesUtil.fimDoMes(anterior)),
                    categoryDao.getAll()
                ) { atual, anteriorLista, categorias -> Triple(atual, anteriorLista, categorias) }
            }.collectLatest { (atual, anteriorLista, categorias) ->
                renderCategorias(atual, anteriorLista, categorias)
            }
        }

        lifecycleScope.launch {
            periodoTendencia.flatMapLatest { dias ->
                val fim = System.currentTimeMillis()
                val inicio = fim - dias * DIA_MS
                dao.getGastoPorDia(inicio, fim).map { pontos -> dias to pontos }
            }.collectLatest { (dias, pontos) -> renderTendencia(dias, pontos) }
        }

        lifecycleScope.launch {
            mesSelecionado.flatMapLatest { calendar ->
                val inicio = MesUtil.inicioDoMes(calendar)
                val fim = MesUtil.fimDoMes(calendar)
                combine(
                    dao.getMaiorGasto(inicio, fim),
                    dao.getMediaGasto(inicio, fim),
                    dao.getCategoriaMaisUsada(inicio, fim),
                    dao.getDiaSemanaMaisGasto(inicio, fim)
                ) { maior, media, categoriaTop, diaSemanaTop ->
                    EstatisticasMes(maior, media, categoriaTop, diaSemanaTop)
                }
            }.collectLatest { stats -> renderEstatisticas(stats) }
        }
    }

    private fun mudarMes(delta: Int) {
        val calendar = mesSelecionado.value.clone() as Calendar
        calendar.add(Calendar.MONTH, delta)
        mesSelecionado.value = calendar
    }

    private fun renderComparativo(atual: Double, anterior: Double) {
        if (anterior <= 0.0) {
            binding.textComparativoMes.visibility = View.GONE
            return
        }
        binding.textComparativoMes.visibility = View.VISIBLE
        val percentual = ((atual - anterior) / anterior * 100).roundToInt()
        if (percentual >= 0) {
            binding.textComparativoMes.text = getString(R.string.comparativo_alta, percentual)
            binding.textComparativoMes.setTextColor(ContextCompat.getColor(this, R.color.gasto_color))
        } else {
            binding.textComparativoMes.text = getString(R.string.comparativo_queda, -percentual)
            binding.textComparativoMes.setTextColor(ContextCompat.getColor(this, R.color.color_positivo))
        }
    }

    private fun renderCategorias(
        categorias: List<CategoriaTotal>,
        categoriasAnterior: List<CategoriaTotal>,
        todasCategorias: List<Category>
    ) {
        binding.containerCategorias.removeAllViews()
        binding.textEmptyReport.visibility = if (categorias.isEmpty()) View.VISIBLE else View.GONE

        val maiorTotal = categorias.maxOfOrNull { it.total } ?: 0.0
        val limitesPorNome = todasCategorias.associate { it.nome to it.limite }

        categorias.forEach { item ->
            val row = ItemReportCategoriaBinding.inflate(
                LayoutInflater.from(this),
                binding.containerCategorias,
                false
            )
            val nomeCategoria = item.categoria?.takeIf { it.isNotBlank() }
            row.textNomeCategoria.text = nomeCategoria ?: getString(R.string.sem_categoria)

            val limite = nomeCategoria?.let { limitesPorNome[it] }
            val params = row.barFill.layoutParams as LinearLayout.LayoutParams
            if (limite != null && limite > 0) {
                row.textValorCategoria.text = getString(
                    R.string.format_valor_limite,
                    "R$ %.2f".format(locale, item.total),
                    "R$ %.2f".format(locale, limite)
                )
                params.weight = (item.total / limite * 100).roundToInt().coerceIn(2, 100).toFloat()
                val corBarra = if (item.total > limite) R.color.gasto_color else R.color.color_positivo
                row.barFill.setBackgroundColor(ContextCompat.getColor(this, corBarra))
            } else {
                row.textValorCategoria.text = "R$ %.2f".format(locale, item.total)
                val percentual = if (maiorTotal > 0) {
                    (item.total / maiorTotal * 100).toInt().coerceIn(2, 100)
                } else {
                    2
                }
                params.weight = percentual.toFloat()
                row.barFill.setBackgroundColor(ContextCompat.getColor(this, R.color.gasto_color))
            }
            row.barFill.layoutParams = params

            val totalAnterior = categoriasAnterior.find { it.categoria == item.categoria }?.total ?: 0.0
            if (totalAnterior > 0) {
                row.textDeltaCategoria.visibility = View.VISIBLE
                val delta = ((item.total - totalAnterior) / totalAnterior * 100).roundToInt()
                if (delta >= 0) {
                    row.textDeltaCategoria.text = getString(R.string.comparativo_alta, delta)
                    row.textDeltaCategoria.setTextColor(ContextCompat.getColor(this, R.color.gasto_color))
                } else {
                    row.textDeltaCategoria.text = getString(R.string.comparativo_queda, -delta)
                    row.textDeltaCategoria.setTextColor(ContextCompat.getColor(this, R.color.color_positivo))
                }
            } else {
                row.textDeltaCategoria.visibility = View.GONE
            }

            binding.containerCategorias.addView(row.root)
        }
    }

    // Pra períodos maiores que 30 dias, reagrupa a série diária em buckets
    // de 7 pontos (aproximação de semana) — senão o gráfico de 365 dias
    // teria um ponto por dia e viraria ilegível.
    private fun renderTendencia(dias: Int, pontosDiarios: List<GastoDiario>) {
        if (pontosDiarios.size < 2) {
            binding.chartTendencia.visibility = View.GONE
            binding.textEmptyTendencia.visibility = View.VISIBLE
            return
        }
        binding.chartTendencia.visibility = View.VISIBLE
        binding.textEmptyTendencia.visibility = View.GONE

        val valores = pontosDiarios.map { it.total }
        binding.chartTendencia.setDados(
            if (dias > 30) valores.chunked(7).map { it.sum() } else valores
        )
    }

    private fun renderEstatisticas(stats: EstatisticasMes) {
        binding.statMaiorGasto.textStatLabel.text = getString(R.string.stat_maior_gasto)
        binding.statMaiorGasto.textStatValor.text = stats.maiorGasto?.let {
            "R$ %.2f".format(locale, it.valor ?: 0.0)
        } ?: getString(R.string.stat_sem_dados)

        binding.statMediaGasto.textStatLabel.text = getString(R.string.stat_media_gasto)
        binding.statMediaGasto.textStatValor.text = "R$ %.2f".format(locale, stats.media)

        binding.statCategoriaMaisUsada.textStatLabel.text = getString(R.string.stat_categoria_mais_usada)
        binding.statCategoriaMaisUsada.textStatValor.text = stats.categoriaMaisUsada?.let { c ->
            val nome = c.categoria?.takeIf { it.isNotBlank() } ?: getString(R.string.sem_categoria)
            getString(R.string.format_stat_qtd, nome, c.qtd)
        } ?: getString(R.string.stat_sem_dados)

        binding.statDiaSemanaTop.textStatLabel.text = getString(R.string.stat_dia_semana_top)
        binding.statDiaSemanaTop.textStatValor.text = stats.diaSemanaTop?.let {
            nomeDiaSemana(it.diaSemana)
        } ?: getString(R.string.stat_sem_dados)
    }

    private fun nomeDiaSemana(codigo: String): String = when (codigo) {
        "0" -> getString(R.string.dia_semana_dom)
        "1" -> getString(R.string.dia_semana_seg)
        "2" -> getString(R.string.dia_semana_ter)
        "3" -> getString(R.string.dia_semana_qua)
        "4" -> getString(R.string.dia_semana_qui)
        "5" -> getString(R.string.dia_semana_sex)
        "6" -> getString(R.string.dia_semana_sab)
        else -> codigo
    }
}
