package com.guilherme.anotaplus

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.guilherme.anotaplus.billing.BillingManager
import com.guilherme.anotaplus.data.Prefs
import com.guilherme.anotaplus.data.SubscriptionPrefs
import com.guilherme.anotaplus.databinding.ActivityLoginBinding
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val billingManager by lazy { BillingManager(applicationContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnEntrarGoogle.setOnClickListener { lifecycleScope.launch { entrarComGoogle() } }
        binding.linkVerPlanos.setOnClickListener { startActivity(Intent(this, PlansActivity::class.java)) }
    }

    private suspend fun entrarComGoogle() {
        binding.btnEntrarGoogle.isEnabled = false
        binding.textStatus.visibility = View.VISIBLE
        binding.textStatus.text = getString(R.string.conta_entrando)

        try {
            AuthHelper.entrarComGoogle(this)
            // Reconsulta o Play Billing (fonte da verdade) antes de decidir
            // se restaura backup — o valor que veio no login é só o último
            // que o backend tinha guardado, pode estar desatualizado.
            // runCatching: dependência externa instável nesse estágio (app
            // ainda não é instalado via Play Store) — uma falha aqui não
            // pode impedir o resto do login de completar.
            runCatching { billingManager.atualizarStatusAssinatura() }

            if (SubscriptionPrefs.podeFazerBackup(this)) {
                // Restaura primeiro (nuvem -> local, importante se for uma
                // reinstalação) e só depois agenda o push do que for
                // local-only — assim nada que acabou de ser restaurado é
                // reenviado à toa.
                binding.textStatus.text = getString(R.string.backup_restaurando)
                SyncManager.restaurarTudo(this)
                SyncWorker.agendar(applicationContext)
            }
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

    override fun onDestroy() {
        super.onDestroy()
        runCatching { billingManager.encerrar() }
    }
}
