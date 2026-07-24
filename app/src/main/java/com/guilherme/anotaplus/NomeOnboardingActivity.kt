package com.guilherme.anotaplus

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.guilherme.anotaplus.data.Prefs
import com.guilherme.anotaplus.data.SessionPrefs
import com.guilherme.anotaplus.databinding.ActivityNomeOnboardingBinding

// Segunda tela do onboarding (logo após o Login): pergunta como o usuário
// quer ser chamado, pré-preenchido com o nome vindo da conta Google
// (SessionPrefs.getName) — campo separado, editável, guardado à parte
// (Prefs.setNomeExibicao) e usado depois nas saudações do mascote (ver
// ReportActivity). Repassa EXTRA_ONBOARDING/EXTRA_ABRIR_HISTORICO adiante
// pra SalarioOnboardingActivity, seguindo o mesmo padrão de QuickAccessFlow.
class NomeOnboardingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNomeOnboardingBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNomeOnboardingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.onboardingHeader.bind(
            activity = this,
            progresso = 0.25f,
            pergunta = getString(R.string.titulo_nome_onboarding)
        )

        val nomeGoogle = SessionPrefs.getName(this)
        if (!nomeGoogle.isNullOrBlank()) {
            binding.editNomeExibicao.setText(nomeGoogle)
            binding.textNomeSugerido.visibility = View.VISIBLE
        }

        binding.btnContinuarNome.setOnClickListener {
            val nome = binding.editNomeExibicao.text.toString().trim()
            if (nome.isEmpty()) {
                binding.editNomeExibicao.error = getString(R.string.error_valor_obrigatorio)
                return@setOnClickListener
            }
            Prefs.setNomeExibicao(this, nome)
            concluir()
        }
    }

    private fun concluir() {
        startActivity(
            Intent(this, SalarioOnboardingActivity::class.java)
                .putExtra(QuickAccessFlow.EXTRA_ONBOARDING, intent.getBooleanExtra(QuickAccessFlow.EXTRA_ONBOARDING, false))
                .putExtra(QuickAccessFlow.EXTRA_ABRIR_HISTORICO, intent.getBooleanExtra(QuickAccessFlow.EXTRA_ABRIR_HISTORICO, false))
        )
        finish()
    }
}
