package com.guilherme.anotaplus

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.guilherme.anotaplus.data.Prefs
import com.guilherme.anotaplus.databinding.ActivitySalarioOnboardingBinding

// Onboarding gamificado (3 passos) rodado uma única vez, logo após a
// criação da conta (ver LoginActivity.concluir()) — pergunta salário
// mensal, horas por dia e dias por semana trabalhados, pra calcular o
// valor-hora usado na calculadora "Vale a pena comprar?". Repassa
// EXTRA_ONBOARDING/EXTRA_ABRIR_HISTORICO adiante pra QuickAccessChooserActivity,
// seguindo o mesmo padrão de QuickAccessFlow. Também reaberta (fora do
// onboarding) via "Dados salariais" em Perfil, mas nesse caso via dialog
// simples (SettingsActivity.abrirDialogDadosSalariais), não esta tela.
class SalarioOnboardingActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySalarioOnboardingBinding
    private var passo = 0

    private val onboarding: Boolean
        get() = intent.getBooleanExtra(QuickAccessFlow.EXTRA_ONBOARDING, false)
    private val abrirHistorico: Boolean
        get() = intent.getBooleanExtra(QuickAccessFlow.EXTRA_ABRIR_HISTORICO, false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySalarioOnboardingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnProximoOnboarding.setOnClickListener { avancar() }
        binding.btnPularOnboardingSalario.setOnClickListener { concluir() }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (passo > 0) {
                    passo -= 1
                    atualizarPasso()
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })

        atualizarPasso()
    }

    private fun avancar() {
        when (passo) {
            0 -> {
                val valor = valorDe(binding.editSalarioMensal.text.toString())
                if (valor == null) {
                    binding.editSalarioMensal.error = getString(R.string.error_valor_obrigatorio)
                    return
                }
                passo = 1
                atualizarPasso()
            }
            1 -> {
                val valor = valorDe(binding.editHorasPorDia.text.toString())
                if (valor == null) {
                    binding.editHorasPorDia.error = getString(R.string.error_valor_obrigatorio)
                    return
                }
                passo = 2
                atualizarPasso()
            }
            else -> {
                val diasPorSemana = valorDe(binding.editDiasPorSemana.text.toString())
                if (diasPorSemana == null) {
                    binding.editDiasPorSemana.error = getString(R.string.error_valor_obrigatorio)
                    return
                }
                val salario = valorDe(binding.editSalarioMensal.text.toString()) ?: 0f
                val horasPorDia = valorDe(binding.editHorasPorDia.text.toString()) ?: 0f
                Prefs.setDadosSalariais(this, salario, horasPorDia, diasPorSemana)
                concluir()
            }
        }
    }

    private fun valorDe(texto: String): Float? =
        texto.replace(",", ".").toFloatOrNull()?.takeIf { it > 0f }

    private fun atualizarPasso() {
        binding.paginaSalario.visibility = if (passo == 0) View.VISIBLE else View.GONE
        binding.paginaHoras.visibility = if (passo == 1) View.VISIBLE else View.GONE
        binding.paginaDias.visibility = if (passo == 2) View.VISIBLE else View.GONE

        val pergunta = when (passo) {
            0 -> getString(R.string.titulo_onboarding_salario_1)
            1 -> getString(R.string.titulo_onboarding_salario_2)
            else -> getString(R.string.titulo_onboarding_salario_3)
        }
        binding.onboardingHeader.bind(activity = this, progresso = 0.40f, pergunta = pergunta)

        binding.btnProximoOnboarding.text =
            if (passo == 2) getString(R.string.btn_concluir_onboarding) else getString(R.string.btn_proximo_onboarding)
    }

    private fun concluir() {
        startActivity(
            Intent(this, MetaOnboardingActivity::class.java)
                .putExtra(QuickAccessFlow.EXTRA_ONBOARDING, onboarding)
                .putExtra(QuickAccessFlow.EXTRA_ABRIR_HISTORICO, abrirHistorico)
        )
    }
}
