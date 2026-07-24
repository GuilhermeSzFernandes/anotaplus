package com.guilherme.anotaplus

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import com.guilherme.anotaplus.data.AppDatabase
import com.guilherme.anotaplus.data.CategoriaContagem
import com.guilherme.anotaplus.data.CategoriaEstilo
import com.guilherme.anotaplus.data.CategoriaTotal
import com.guilherme.anotaplus.data.Category
import com.guilherme.anotaplus.data.DiaSemanaTotal
import com.guilherme.anotaplus.data.Entry
import com.guilherme.anotaplus.data.Meta
import com.guilherme.anotaplus.data.MetaDao
import com.guilherme.anotaplus.data.Prefs
import com.guilherme.anotaplus.databinding.ActivityReportBinding
import com.guilherme.anotaplus.databinding.DialogNovaMetaBinding
import com.guilherme.anotaplus.databinding.ItemMetaBinding
import com.guilherme.anotaplus.databinding.ItemReportCategoriaBinding
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import java.util.Calendar
import kotlin.math.roundToInt

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReportBinding.inflate(layoutInflater)
        setContentView(binding.root)
        aplicarEdgeToEdge(binding.root, binding.header, binding.bottomNav.root)

        configurarBottomNav(binding.bottomNav, NavTab.INICIO)
        configurarBotaoCalculadora(binding.btnCalculadoraNav)

        binding.rowMes.btnMesAnterior.setOnClickListener { mudarMes(-1) }
        binding.rowMes.btnMesProximo.setOnClickListener { mudarMes(1) }

        val db = AppDatabase.getInstance(applicationContext)
        val dao = db.entryDao()
        val categoryDao = db.categoryDao()
        val metaDao = db.metaDao()

        binding.btnAddMeta.setOnClickListener { abrirDialogNovaMeta(metaDao) }
        binding.btnCriarPrimeiraMeta.setOnClickListener { abrirDialogNovaMeta(metaDao) }

        lifecycleScope.launch {
            metaDao.getAll().collectLatest { metas -> renderMetas(metas, metaDao) }
        }

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
                combine(
                    dao.getTotalGasto(MesUtil.inicioDoMes(calendar), MesUtil.fimDoMes(calendar)),
                    dao.getTotalRecebimento(MesUtil.inicioDoMes(calendar), MesUtil.fimDoMes(calendar))
                ) { gasto, receita -> gasto to receita }
            }.collectLatest { (gasto, receita) -> renderResumo(gasto, receita) }
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

    private fun renderResumo(gasto: Double, receita: Double) {
        binding.textTotalGasto.text = "R$ %.2f".format(locale, gasto)
        binding.textTotalReceitas.text = "R$ %.2f".format(locale, receita)

        val poupanca = receita - gasto
        binding.textTotalPoupanca.text = "R$ %.2f".format(locale, poupanca)
        binding.textTotalPoupanca.setTextColor(
            ContextCompat.getColor(this, if (poupanca < 0) R.color.color_gasto_vivo else R.color.color_receita)
        )

        val metaValor = Prefs.getMetaEconomiaValor(this)
        val percentual = if (metaValor > 0) {
            (poupanca / metaValor * 100).roundToInt().coerceIn(0, 100)
        } else {
            0
        }
        binding.textMetaEconomiaPercentual.text = getString(R.string.format_percentual, percentual)
        binding.progressMetaEconomia.progress = percentual

        atualizarSaudacao(poupanca)
    }

    private fun atualizarSaudacao(poupanca: Double) {
        val nome = Prefs.getNomeExibicao(this)
        val hora = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val saudacao = when {
            hora < 12 -> getString(R.string.saudacao_bom_dia)
            hora < 18 -> getString(R.string.saudacao_boa_tarde)
            else -> getString(R.string.saudacao_boa_noite)
        }
        binding.textSaudacaoTitulo.text = if (nome.isNullOrBlank()) {
            saudacao
        } else {
            getString(R.string.formato_saudacao_com_nome, saudacao, nome)
        }
        if (poupanca > 0) {
            binding.imageMascoteSaudacao.setImageResource(R.drawable.ic_mascote_feliz)
            binding.textSaudacaoSub.text = getString(
                R.string.formato_saudacao_guardou,
                "R$ %.2f".format(locale, poupanca)
            )
        } else {
            binding.imageMascoteSaudacao.setImageResource(R.drawable.ic_mascote_neutro)
            binding.textSaudacaoSub.text = getString(R.string.saudacao_neutra)
        }
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
        val categoriasPorNome = todasCategorias.associateBy { it.nome }

        categorias.forEach { item ->
            val row = ItemReportCategoriaBinding.inflate(
                LayoutInflater.from(this),
                binding.containerCategorias,
                false
            )
            val nomeCategoria = item.categoria?.takeIf { it.isNotBlank() }
            val categoriaInfo = nomeCategoria?.let { categoriasPorNome[it] }
            val icone = categoriaInfo?.icone?.let { "$it " }.orEmpty()
            row.textNomeCategoria.text = icone + (nomeCategoria ?: getString(R.string.sem_categoria))

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
                val corCategoria = categoriaInfo?.cor?.let { CategoriaEstilo.corInt(it) }
                    ?: ContextCompat.getColor(this, R.color.gasto_color)
                row.barFill.setBackgroundColor(corCategoria)
            }
            row.barFill.layoutParams = params
            val trackParams = row.barTrack.layoutParams as LinearLayout.LayoutParams
            trackParams.weight = 100f - params.weight
            row.barTrack.layoutParams = trackParams

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

        renderInsight(categorias)
    }

    private fun renderInsight(categorias: List<CategoriaTotal>) {
        val maior = categorias.maxByOrNull { it.total }
        if (maior == null || maior.total <= 0.0) {
            binding.labelInsights.visibility = View.GONE
            binding.cardInsight.visibility = View.GONE
            return
        }
        val nome = maior.categoria?.takeIf { it.isNotBlank() } ?: getString(R.string.sem_categoria)
        binding.textInsight.text = getString(R.string.insight_maior_categoria, nome, maior.total)
        binding.labelInsights.visibility = View.VISIBLE
        binding.cardInsight.visibility = View.VISIBLE
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

    private fun renderMetas(metas: List<Meta>, metaDao: MetaDao) {
        binding.containerMetas.removeAllViews()
        val temMetas = metas.isNotEmpty()
        binding.containerMetas.visibility = if (temMetas) View.VISIBLE else View.GONE
        binding.layoutEmptyMetas.visibility = if (temMetas) View.GONE else View.VISIBLE

        metas.forEach { meta ->
            val row = ItemMetaBinding.inflate(LayoutInflater.from(this), binding.containerMetas, false)
            row.textNomeMeta.text = meta.nome
            row.textValorMeta.text = getString(
                R.string.format_meta_progresso,
                "R$ %.2f".format(locale, meta.valorAtual),
                "R$ %.2f".format(locale, meta.valorAlvo)
            )
            val percentual = if (meta.valorAlvo > 0) {
                (meta.valorAtual / meta.valorAlvo * 100).roundToInt().coerceIn(0, 100)
            } else {
                0
            }
            val params = row.barFillMeta.layoutParams as LinearLayout.LayoutParams
            params.weight = percentual.toFloat()
            row.barFillMeta.layoutParams = params
            val trackParams = row.barTrackMeta.layoutParams as LinearLayout.LayoutParams
            trackParams.weight = 100f - percentual
            row.barTrackMeta.layoutParams = trackParams
            row.layoutMetaCelebracao.visibility = if (percentual >= 100) View.VISIBLE else View.GONE
            row.btnExcluirMeta.setOnClickListener {
                lifecycleScope.launch { metaDao.deleteById(meta.id) }
            }
            binding.containerMetas.addView(row.root)
        }
    }

    private fun abrirDialogNovaMeta(metaDao: MetaDao) {
        val dialogBinding = DialogNovaMetaBinding.inflate(layoutInflater)
        val dialog = criarDialogoFlat(dialogBinding.root)
        var prazoSelecionado: Long? = null

        fun chips() = listOf(
            dialogBinding.chipPrazo6Meses,
            dialogBinding.chipPrazo1Ano,
            dialogBinding.chipPrazoEscolher
        )

        fun atualizarPreview() {
            val valorAlvo = dialogBinding.editValorAlvoMeta.text.toString()
                .replace(",", ".").toDoubleOrNull()
            val valorMensal = if (valorAlvo != null) {
                MetaCalculo.valorMensalNecessario(valorAlvo, 0.0, prazoSelecionado, System.currentTimeMillis())
            } else {
                null
            }
            if (valorMensal != null) {
                dialogBinding.textPreviewValorMensal.text = getString(
                    R.string.formato_valor_mensal_meta,
                    "R$ %.2f".format(locale, valorMensal)
                )
                dialogBinding.textPreviewValorMensal.visibility = View.VISIBLE
            } else {
                dialogBinding.textPreviewValorMensal.visibility = View.GONE
            }
        }

        fun selecionar(chipSelecionado: View) {
            chips().forEach { it.isSelected = it == chipSelecionado }
        }

        dialogBinding.chipPrazo6Meses.setOnClickListener {
            prazoSelecionado = MetaCalculo.prazoEmMeses(System.currentTimeMillis(), 6)
            selecionar(dialogBinding.chipPrazo6Meses)
            atualizarPreview()
        }
        dialogBinding.chipPrazo1Ano.setOnClickListener {
            prazoSelecionado = MetaCalculo.prazoEmMeses(System.currentTimeMillis(), 12)
            selecionar(dialogBinding.chipPrazo1Ano)
            atualizarPreview()
        }
        dialogBinding.chipPrazoEscolher.setOnClickListener {
            val agora = Calendar.getInstance()
            DatePickerDialog(
                this,
                { _, ano, mes, dia ->
                    val escolhido = Calendar.getInstance().apply { set(ano, mes, dia, 0, 0, 0) }
                    prazoSelecionado = escolhido.timeInMillis
                    selecionar(dialogBinding.chipPrazoEscolher)
                    atualizarPreview()
                },
                agora.get(Calendar.YEAR),
                agora.get(Calendar.MONTH),
                agora.get(Calendar.DAY_OF_MONTH)
            ).show()
        }
        dialogBinding.editValorAlvoMeta.addTextChangedListener(afterTextChanged = { atualizarPreview() })

        dialogBinding.btnCancelarMeta.setOnClickListener { dialog.dismiss() }
        dialogBinding.btnSalvarNovaMeta.setOnClickListener {
            val nome = dialogBinding.editNomeMeta.text.toString().trim()
            val valorAlvo = dialogBinding.editValorAlvoMeta.text.toString()
                .replace(",", ".").toDoubleOrNull()
            if (nome.isNotEmpty() && valorAlvo != null && valorAlvo > 0) {
                lifecycleScope.launch {
                    metaDao.insert(Meta(nome = nome, valorAlvo = valorAlvo, prazo = prazoSelecionado))
                }
                dialog.dismiss()
            }
        }
        dialog.show()
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
