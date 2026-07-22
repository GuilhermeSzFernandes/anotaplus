package com.guilherme.anotaplus

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.guilherme.anotaplus.data.AppDatabase
import com.guilherme.anotaplus.databinding.ActivityHistoryBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar

// Rótulos curtos de dia da semana, indexados por Calendar.DAY_OF_WEEK - 1
// (0 = domingo .. 6 = sábado) — só usados no gráfico de barras do Início.
private val NOMES_DIA_CURTO = arrayOf("D", "S", "T", "Q", "Q", "S", "S")

// Início virou dashboard estilo recibo: ticket de saldo em destaque,
// gráfico de barras dos últimos 7 dias, e preview dos últimos lançamentos
// (Gasto + Recebimento) — o extrato completo mora na aba Financeiro.
class HistoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHistoryBinding
    private val adapterRecentes = EntryAdapter { entry ->
        startActivity(
            Intent(this, EditEntryActivity::class.java)
                .putExtra(EditEntryActivity.EXTRA_ENTRY_ID, entry.id)
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)
        aplicarEdgeToEdge(binding.root, binding.header, binding.bottomNav.root)

        configurarBottomNav(binding.bottomNav, NavTab.INICIO)

        val mes = Calendar.getInstance()
        binding.textMesAtual.text = MesUtil.formatar(mes)

        binding.linkVerTudo.setOnClickListener {
            startActivity(Intent(this, FinanceiroActivity::class.java))
        }

        binding.recyclerRecentes.layoutManager = LinearLayoutManager(this)
        binding.recyclerRecentes.adapter = adapterRecentes
        binding.recyclerRecentes.isNestedScrollingEnabled = false

        val dao = AppDatabase.getInstance(applicationContext).entryDao()

        // Saldo do mês (Recebimento - Gasto)
        val inicioMes = MesUtil.inicioDoMes(mes)
        val fimMes = MesUtil.fimDoMes(mes)
        lifecycleScope.launch {
            combine(dao.getTotalGasto(inicioMes, fimMes), dao.getTotalRecebimento(inicioMes, fimMes)) { gasto, recebimento ->
                recebimento - gasto
            }.collectLatest { saldo ->
                binding.textSaldoValor.text = "R$ %.2f".format(MesUtil.locale, saldo)
                binding.textSaldoValor.setTextColor(
                    ContextCompat.getColor(this@HistoryActivity, if (saldo < 0) R.color.gasto_color else R.color.color_positivo)
                )
            }
        }
        binding.textSaldoLabel.text = getString(R.string.stat_saldo_estimado)

        // Gráfico de barras: últimos 7 dias (janela corrida, não é o mês
        // do calendário) — rótulos abaixo já saem prontos, sem precisar de
        // Flow pra isso.
        val diasFormat = SimpleDateFormat("yyyy-MM-dd", MesUtil.locale)
        val calendarHoje = Calendar.getInstance()
        val diasDaSemana = (0..6).map { offset ->
            (calendarHoje.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -(6 - offset)) }
        }
        val labelViews = listOf(
            binding.textDia0, binding.textDia1, binding.textDia2, binding.textDia3,
            binding.textDia4, binding.textDia5, binding.textDia6
        )
        diasDaSemana.forEachIndexed { i, c ->
            labelViews[i].text = NOMES_DIA_CURTO[c.get(Calendar.DAY_OF_WEEK) - 1]
        }
        val inicioPeriodo = (diasDaSemana.first().clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val fimPeriodo = System.currentTimeMillis()

        lifecycleScope.launch {
            dao.getGastoPorDia(inicioPeriodo, fimPeriodo).collectLatest { porDia ->
                val mapa = porDia.associate { it.dia to it.total }
                val valores = diasDaSemana.map { c -> mapa[diasFormat.format(c.time)] ?: 0.0 }
                binding.chartSemana.setDados(valores)
            }
        }

        // Últimos lançamentos (Gasto + Recebimento)
        lifecycleScope.launch {
            dao.getRecentesFinanceiro(4).collectLatest { entries ->
                adapterRecentes.submitList(entries)
                binding.textEmptyRecentes.visibility = if (entries.isEmpty()) View.VISIBLE else View.GONE
            }
        }
    }
}
