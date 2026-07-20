package com.guilherme.anotaplus

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.guilherme.anotaplus.data.AppDatabase
import com.guilherme.anotaplus.data.CategoriaTotal
import com.guilherme.anotaplus.databinding.ActivityReportBinding
import com.guilherme.anotaplus.databinding.ItemReportCategoriaBinding
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import java.util.Calendar

class ReportActivity : AppCompatActivity() {

    private lateinit var binding: ActivityReportBinding
    private val locale = MesUtil.locale
    private val mesSelecionado = MutableStateFlow(Calendar.getInstance())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReportBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener { finish() }

        binding.rowMes.btnMesAnterior.setOnClickListener { mudarMes(-1) }
        binding.rowMes.btnMesProximo.setOnClickListener { mudarMes(1) }

        val dao = AppDatabase.getInstance(applicationContext).entryDao()

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
                dao.getTotalIdeias(MesUtil.inicioDoMes(calendar), MesUtil.fimDoMes(calendar))
            }.collectLatest { count ->
                binding.textTotalIdeias.text = getString(R.string.format_ideias_mes, count)
            }
        }

        lifecycleScope.launch {
            mesSelecionado.flatMapLatest { calendar ->
                dao.getGastoPorCategoria(MesUtil.inicioDoMes(calendar), MesUtil.fimDoMes(calendar))
            }.collectLatest { categorias -> renderCategorias(categorias) }
        }
    }

    private fun mudarMes(delta: Int) {
        val calendar = mesSelecionado.value.clone() as Calendar
        calendar.add(Calendar.MONTH, delta)
        mesSelecionado.value = calendar
    }

    private fun renderCategorias(categorias: List<CategoriaTotal>) {
        binding.containerCategorias.removeAllViews()
        binding.textEmptyReport.visibility = if (categorias.isEmpty()) View.VISIBLE else View.GONE

        val maiorTotal = categorias.maxOfOrNull { it.total } ?: 0.0

        categorias.forEach { item ->
            val row = ItemReportCategoriaBinding.inflate(
                LayoutInflater.from(this),
                binding.containerCategorias,
                false
            )
            row.textNomeCategoria.text = item.categoria?.takeIf { it.isNotBlank() }
                ?: getString(R.string.sem_categoria)
            row.textValorCategoria.text = "R$ %.2f".format(locale, item.total)

            val percentual = if (maiorTotal > 0) {
                (item.total / maiorTotal * 100).toInt().coerceIn(2, 100)
            } else {
                2
            }
            val params = row.barFill.layoutParams as LinearLayout.LayoutParams
            params.weight = percentual.toFloat()
            row.barFill.layoutParams = params

            binding.containerCategorias.addView(row.root)
        }
    }
}
