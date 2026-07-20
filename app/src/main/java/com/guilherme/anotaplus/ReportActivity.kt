package com.guilherme.anotaplus

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.guilherme.anotaplus.data.AppDatabase
import com.guilherme.anotaplus.databinding.ActivityReportBinding
import com.guilherme.anotaplus.databinding.ItemReportCategoriaBinding
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class ReportActivity : AppCompatActivity() {

    private lateinit var binding: ActivityReportBinding
    private val locale = Locale("pt", "BR")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReportBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener { finish() }

        val calendar = Calendar.getInstance()
        val monthFormat = SimpleDateFormat("MMMM yyyy", locale)
        binding.textMes.text = monthFormat.format(calendar.time).uppercase(locale)

        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val inicioMes = calendar.timeInMillis
        val fimMes = System.currentTimeMillis()

        val dao = AppDatabase.getInstance(applicationContext).entryDao()

        lifecycleScope.launch {
            dao.getTotalGasto(inicioMes, fimMes).collect { total ->
                binding.textTotalGasto.text = "R$ %.2f".format(locale, total)
            }
        }

        lifecycleScope.launch {
            dao.getTotalIdeias(inicioMes, fimMes).collect { count ->
                binding.textTotalIdeias.text = getString(R.string.format_ideias_mes, count)
            }
        }

        lifecycleScope.launch {
            dao.getGastoPorCategoria(inicioMes, fimMes).collect { categorias ->
                binding.containerCategorias.removeAllViews()
                binding.textEmptyReport.visibility =
                    if (categorias.isEmpty()) View.VISIBLE else View.GONE

                val maiorTotal = categorias.maxOfOrNull { it.total } ?: 0.0

                categorias.forEach { item ->
                    val row = ItemReportCategoriaBinding.inflate(
                        LayoutInflater.from(this@ReportActivity),
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
    }
}
