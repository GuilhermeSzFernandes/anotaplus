package com.guilherme.anotaplus

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.guilherme.anotaplus.data.AppDatabase
import com.guilherme.anotaplus.data.CategoriaContagem
import com.guilherme.anotaplus.data.EntryType
import com.guilherme.anotaplus.databinding.ActivityHistoryBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.util.Calendar

private data class DashboardDados(
    val totalGasto: Double,
    val totalRecebimento: Double,
    val categoriaTop: CategoriaContagem?,
    val totalIdeias: Int,
    val ultimaIdeia: String?
)

// Início virou dashboard (cards do mês) — o extrato de verdade agora mora
// nas abas Financeiro (Gasto/Recebimento) e Anotações.
class HistoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHistoryBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)
        aplicarEdgeToEdge(binding.root, binding.header, binding.bottomNav.root)

        configurarBottomNav(binding.bottomNav, NavTab.INICIO)

        val mes = Calendar.getInstance()
        binding.textMesAtual.text = MesUtil.formatar(mes)
        val inicio = MesUtil.inicioDoMes(mes)
        val fim = MesUtil.fimDoMes(mes)

        binding.statSaldo.textStatLabel.text = getString(R.string.stat_saldo_estimado)
        binding.statGastoMes.textStatLabel.text = getString(R.string.label_gasto_no_mes)
        binding.statCategoria.textStatLabel.text = getString(R.string.stat_categoria_mais_usada)
        binding.statAnotacoes.textStatLabel.text = getString(R.string.title_anotacoes)

        val dao = AppDatabase.getInstance(applicationContext).entryDao()

        lifecycleScope.launch {
            combine(
                dao.getTotalGasto(inicio, fim),
                dao.getTotalRecebimento(inicio, fim),
                dao.getCategoriaMaisUsada(inicio, fim),
                dao.getTotalIdeias(inicio, fim),
                dao.getByType(EntryType.PENSAMENTO)
            ) { totalGasto, totalRecebimento, categoriaTop, totalIdeias, ideias ->
                val ultimaIdeia = ideias.firstOrNull()?.texto?.let { RichTextEngine.textoSemMarcadores(it) }
                DashboardDados(totalGasto, totalRecebimento, categoriaTop, totalIdeias, ultimaIdeia)
            }.collectLatest { dados -> renderDashboard(dados) }
        }
    }

    private fun renderDashboard(dados: DashboardDados) {
        val locale = MesUtil.locale
        val saldo = dados.totalRecebimento - dados.totalGasto
        binding.statSaldo.textStatValor.text = "R$ %.2f".format(locale, saldo)
        binding.statSaldo.textStatValor.setTextColor(
            ContextCompat.getColor(this, if (saldo < 0) R.color.gasto_color else R.color.color_positivo)
        )

        binding.statGastoMes.textStatValor.text = "R$ %.2f".format(locale, dados.totalGasto)
        binding.statGastoMes.textStatValor.setTextColor(ContextCompat.getColor(this, R.color.gasto_color))

        val categoriaTop = dados.categoriaTop
        if (categoriaTop != null) {
            binding.statCategoria.textStatValor.text = categoriaTop.categoria ?: getString(R.string.sem_categoria)
            binding.statCategoria.textStatSub.visibility = View.VISIBLE
            binding.statCategoria.textStatSub.text = getString(R.string.format_qtd_curto, categoriaTop.qtd)
        } else {
            binding.statCategoria.textStatValor.text = getString(R.string.stat_sem_dados)
            binding.statCategoria.textStatSub.visibility = View.GONE
        }
        binding.statCategoria.textStatValor.setTextColor(ContextCompat.getColor(this, R.color.brass))

        binding.statAnotacoes.textStatValor.text = getString(R.string.format_qtd_curto, dados.totalIdeias)
        binding.statAnotacoes.textStatValor.setTextColor(ContextCompat.getColor(this, R.color.pensamento_color))
        if (dados.ultimaIdeia.isNullOrBlank()) {
            binding.statAnotacoes.textStatSub.visibility = View.GONE
        } else {
            binding.statAnotacoes.textStatSub.visibility = View.VISIBLE
            binding.statAnotacoes.textStatSub.text = dados.ultimaIdeia
        }
    }
}
