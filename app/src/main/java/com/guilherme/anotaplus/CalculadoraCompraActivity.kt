package com.guilherme.anotaplus

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.guilherme.anotaplus.data.AppDatabase
import com.guilherme.anotaplus.data.Entry
import com.guilherme.anotaplus.data.EntryType
import com.guilherme.anotaplus.data.Prefs
import com.guilherme.anotaplus.data.SubscriptionPrefs
import com.guilherme.anotaplus.databinding.ActivityCalculadoraCompraBinding
import com.guilherme.anotaplus.databinding.DialogDadosSalariaisBinding
import com.guilherme.anotaplus.databinding.DialogValeAPenaBinding
import com.guilherme.anotaplus.widget.WidgetUpdater
import kotlinx.coroutines.launch
import kotlin.math.floor
import kotlin.math.roundToInt

// Calculadora "vale a pena comprar?": converte um preço no tempo de
// trabalho equivalente, usando o valor-hora calculado a partir dos dados
// salariais (Prefs.getValorHora — ver SalarioOnboardingActivity). Se
// "Comprar" for escolhido no resultado, grava um Gasto "Cálculo rápido"
// (mesmo padrão de ManualGastoActivity.salvar()).
class CalculadoraCompraActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCalculadoraCompraBinding
    private var digitos = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCalculadoraCompraBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnFecharCalculadora.setOnClickListener { finish() }

        val teclasNumericas = mapOf(
            binding.tecla1 to "1", binding.tecla2 to "2", binding.tecla3 to "3",
            binding.tecla4 to "4", binding.tecla5 to "5", binding.tecla6 to "6",
            binding.tecla7 to "7", binding.tecla8 to "8", binding.tecla9 to "9",
            binding.tecla0 to "0", binding.teclaPonto to ","
        )
        teclasNumericas.forEach { (tecla, digito) ->
            tecla.setOnClickListener { aoTocarDigito(digito) }
        }
        binding.teclaApagar.setOnClickListener { aoApagar() }

        binding.btnCalcular.setOnClickListener { aoTocarCalcular() }
    }

    private fun aoTocarDigito(digito: String) {
        if (digito == "," && digitos.contains(",")) return
        digitos += digito
        atualizarDisplay()
    }

    private fun aoApagar() {
        if (digitos.isNotEmpty()) digitos = digitos.dropLast(1)
        atualizarDisplay()
    }

    private fun atualizarDisplay() {
        binding.textValorDigitado.text = "R$ " + digitos.ifEmpty { "0,00" }
    }

    private fun valorAtual(): Double? = digitos.replace(",", ".").toDoubleOrNull()?.takeIf { it > 0 }

    private fun aoTocarCalcular() {
        val valor = valorAtual() ?: return

        if (!Prefs.temDadosSalariais(this)) {
            abrirDialogDadosSalariais { mostrarResultado(valor) }
        } else {
            mostrarResultado(valor)
        }
    }

    private fun abrirDialogDadosSalariais(aoSalvar: () -> Unit) {
        val dialogBinding = DialogDadosSalariaisBinding.inflate(layoutInflater)
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.titulo_dados_salariais)
            .setView(dialogBinding.root)
            .setPositiveButton(R.string.btn_salvar_meta) { _, _ ->
                val salario = dialogBinding.editSalarioMensal.text.toString()
                    .replace(",", ".").toFloatOrNull() ?: 0f
                val horasPorDia = dialogBinding.editHorasPorDia.text.toString()
                    .replace(",", ".").toFloatOrNull() ?: 0f
                val diasPorSemana = dialogBinding.editDiasPorSemana.text.toString()
                    .replace(",", ".").toFloatOrNull() ?: 0f
                Prefs.setDadosSalariais(this, salario, horasPorDia, diasPorSemana)
                if (Prefs.temDadosSalariais(this)) aoSalvar()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun mostrarResultado(valor: Double) {
        val valorHora = Prefs.getValorHora(this)
        if (valorHora <= 0f) return
        val horasPorDia = Prefs.getHorasPorDia(this)

        val horasNecessarias = valor / valorHora
        var dias = floor(horasNecessarias / horasPorDia).toInt()
        var horasRestantes = (horasNecessarias % horasPorDia).roundToInt()
        if (horasRestantes >= horasPorDia.roundToInt()) {
            dias += 1
            horasRestantes = 0
        }

        val dialogBinding = DialogValeAPenaBinding.inflate(layoutInflater)
        dialogBinding.textTempoTrabalho.text = getString(R.string.format_tempo_trabalho, dias, horasRestantes)

        val dialog = MaterialAlertDialogBuilder(this)
            .setView(dialogBinding.root)
            .setCancelable(true)
            .create()

        dialogBinding.btnNaoComprar.setOnClickListener {
            dialog.dismiss()
            finish()
        }
        dialogBinding.btnComprar.setOnClickListener {
            registrarGastoCalculoRapido(valor)
            dialog.dismiss()
            finish()
        }
        dialogBinding.btnNaoTenhoCerteza.setOnClickListener { dialog.dismiss() }

        dialog.show()
    }

    private fun registrarGastoCalculoRapido(valor: Double) {
        lifecycleScope.launch {
            val entry = Entry(
                type = EntryType.GASTO,
                texto = getString(R.string.texto_calculo_rapido),
                valor = valor
            )
            AppDatabase.getInstance(applicationContext).entryDao().insert(entry)
            WidgetUpdater.atualizarTodos(applicationContext)
            BudgetAlertNotifier.verificarLimite(applicationContext, entry.categoria)
            if (SubscriptionPrefs.podeFazerBackup(this@CalculadoraCompraActivity)) {
                SyncWorker.agendar(applicationContext)
            }
        }
    }
}
