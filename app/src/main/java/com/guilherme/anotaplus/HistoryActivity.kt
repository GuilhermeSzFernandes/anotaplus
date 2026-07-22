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

// Início virou dashboard estilo recibo: um ticket de saldo em destaque +
// um ticket de itens (linhas, não cards em grade) — o extrato de verdade
// mora nas abas Financeiro (Gasto/Recebimento) e Anotações.
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

        binding.textSaldoLabel.text = getString(R.string.stat_saldo_estimado)
        binding.textGastoLabel.text = getString(R.string.label_gasto_no_mes)
        binding.textCategoriaLabel.text = getString(R.string.stat_categoria_mais_usada)
        binding.textAnotacoesLabel.text = getString(R.string.title_anotacoes)

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
        binding.textSaldoValor.text = "R$ %.2f".format(locale, saldo)
        binding.textSaldoValor.setTextColor(
            ContextCompat.getColor(this, if (saldo < 0) R.color.gasto_color else R.color.color_positivo)
        )

        binding.textGastoValor.text = "R$ %.2f".format(locale, dados.totalGasto)

        val categoriaTop = dados.categoriaTop
        if (categoriaTop != null) {
            binding.textCategoriaValor.text = categoriaTop.categoria ?: getString(R.string.sem_categoria)
            binding.textCategoriaSub.visibility = View.VISIBLE
            binding.textCategoriaSub.text = getString(R.string.format_qtd_curto, categoriaTop.qtd)
        } else {
            binding.textCategoriaValor.text = getString(R.string.stat_sem_dados)
            binding.textCategoriaSub.visibility = View.GONE
        }

        binding.textAnotacoesValor.text = getString(R.string.format_qtd_curto, dados.totalIdeias)
        if (dados.ultimaIdeia.isNullOrBlank()) {
            binding.textAnotacoesSub.visibility = View.GONE
        } else {
            binding.textAnotacoesSub.visibility = View.VISIBLE
            binding.textAnotacoesSub.text = dados.ultimaIdeia
        }
    }
}
