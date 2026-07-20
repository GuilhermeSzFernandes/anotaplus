package com.guilherme.anotaplus

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.guilherme.anotaplus.data.Prefs
import com.guilherme.anotaplus.databinding.ActivityLoginBinding
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnEntrarGoogle.setOnClickListener { lifecycleScope.launch { entrarComGoogle() } }
        binding.btnContinuarSemConta.setOnClickListener { concluir() }
        binding.linkVerPlanos.setOnClickListener { startActivity(Intent(this, PlansActivity::class.java)) }
    }

    private suspend fun entrarComGoogle() {
        binding.btnEntrarGoogle.isEnabled = false
        binding.textStatus.visibility = View.VISIBLE
        binding.textStatus.text = getString(R.string.conta_entrando)

        try {
            AuthHelper.entrarComGoogle(this)
            concluir()
        } catch (e: Exception) {
            binding.textStatus.text = getString(R.string.login_erro_detalhe, e.message ?: e.toString())
            binding.btnEntrarGoogle.isEnabled = true
        }
    }

    private fun concluir() {
        if (intent.getBooleanExtra(GestureGuideActivity.EXTRA_ONBOARDING, false)) {
            Prefs.marcarOnboardingConcluido(this)
            val abrirHistorico = intent.getBooleanExtra(GestureGuideActivity.EXTRA_ABRIR_HISTORICO, false)
            val destino = if (abrirHistorico) HistoryActivity::class.java else QuickCaptureActivity::class.java
            startActivity(Intent(this, destino))
        }
        finish()
    }
}
