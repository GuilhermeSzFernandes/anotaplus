package com.guilherme.anotaplus

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.guilherme.anotaplus.data.Prefs
import com.guilherme.anotaplus.databinding.ActivityQuickAccessChooserBinding

/**
 * Tela de "como você quer abrir o Anota+ rápido": cards de Gestos,
 * Notificação e Widget pra escolher (multi-select) quais atalhos usar.
 * Aparece uma vez logo após o login (onboarding, ver LoginActivity.concluir)
 * e pode ser reaberta depois a partir de Conta pra mudar de ideia — nesse
 * caso os switches já vêm pré-marcados com o que já está ativo.
 *
 * Ao confirmar, aplica direto o que dá pra aplicar sem sair da tela
 * (notificação) e encaminha os métodos que têm tutorial próprio (Gestos,
 * Widget) pra fila de QuickAccessFlow.
 */
class QuickAccessChooserActivity : AppCompatActivity() {

    private lateinit var binding: ActivityQuickAccessChooserBinding
    private val isMotorola = Build.MANUFACTURER.equals("motorola", ignoreCase = true)

    // Guarda o que fazer depois que o usuário responder o diálogo de
    // permissão (concedendo ou negando) — o launcher só entrega o resultado
    // de forma assíncrona, então não dá pra simplesmente continuar a função
    // que disparou o launch().
    private var aoResolverPermissao: (() -> Unit)? = null

    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { concedida ->
        Prefs.setNotificacaoCapturaAtiva(this, concedida)
        if (concedida) {
            NotificationQuickAdd.atualizar(this)
        }
        aoResolverPermissao?.invoke()
        aoResolverPermissao = null
    }

    private val onboarding: Boolean
        get() = intent.getBooleanExtra(QuickAccessFlow.EXTRA_ONBOARDING, false)

    private val abrirHistorico: Boolean
        get() = intent.getBooleanExtra(QuickAccessFlow.EXTRA_ABRIR_HISTORICO, false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityQuickAccessChooserBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.onboardingHeader.bind(
            activity = this,
            progresso = 0.75f,
            pergunta = getString(R.string.title_atalho_rapido)
        )

        if (isMotorola) {
            binding.textDescAtalhoGestos.text = getString(R.string.desc_atalho_gestos_moto)
        }

        // Pré-marca conforme o que já está ativo — importante pra quando
        // essa tela é reaberta depois a partir de Conta, senão pareceria que
        // tudo foi desligado mesmo continuando ativo. "Gestos" não tem
        // estado próprio pra restaurar (é só um atalho pro tutorial), então
        // começa sempre desmarcado.
        binding.switchAtalhoNotificacao.isChecked =
            Prefs.isNotificacaoCapturaAtiva(this) && NotificationQuickAdd.temPermissao(this)

        binding.btnContinuar.setOnClickListener { confirmar() }
        binding.linkPularAtalho.setOnClickListener {
            QuickAccessFlow.avancar(this, emptyList(), onboarding, abrirHistorico)
        }
    }

    private fun confirmar() {
        if (binding.switchAtalhoNotificacao.isChecked) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !NotificationQuickAdd.temPermissao(this)) {
                aoResolverPermissao = { prosseguir() }
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                return
            }
            Prefs.setNotificacaoCapturaAtiva(this, true)
            NotificationQuickAdd.atualizar(this)
        } else {
            Prefs.setNotificacaoCapturaAtiva(this, false)
            NotificationQuickAdd.cancelar(this)
        }

        prosseguir()
    }

    private fun prosseguir() {
        val fila = mutableListOf<MetodoAtalho>()
        if (binding.switchAtalhoGestos.isChecked) fila += MetodoAtalho.GESTOS
        if (binding.switchAtalhoWidget.isChecked) fila += MetodoAtalho.WIDGET
        QuickAccessFlow.avancar(this, fila, onboarding, abrirHistorico)
    }
}
