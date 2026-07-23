package com.guilherme.anotaplus

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.guilherme.anotaplus.billing.BillingManager
import com.guilherme.anotaplus.data.EntryType
import com.guilherme.anotaplus.data.Prefs
import com.guilherme.anotaplus.data.SessionPrefs
import com.guilherme.anotaplus.data.SubscriptionPrefs
import com.guilherme.anotaplus.databinding.ActivitySettingsBinding
import com.guilherme.anotaplus.databinding.DialogAjustarObjetivosBinding
import com.guilherme.anotaplus.databinding.DialogTipoPadraoBinding
import com.guilherme.anotaplus.network.ApiClient
import com.guilherme.anotaplus.network.dto.BillingSyncRequest
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private val billingManager by lazy { BillingManager(applicationContext) }

    // Precisa ser registrado como campo (antes de STARTED) — não dá pra
    // chamar registerForActivityResult dentro do onCreate depois de outras
    // coisas ou dentro de um listener.
    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { concedida ->
        if (concedida) {
            Prefs.setNotificacaoCapturaAtiva(this, true)
            NotificationQuickAdd.atualizar(this)
        } else {
            binding.rowNotificacoes.switchRow.isChecked = false
            Prefs.setNotificacaoCapturaAtiva(this, false)
            Toast.makeText(this, R.string.aviso_notificacao_permissao_negada, Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        aplicarEdgeToEdge(binding.root, binding.header, binding.bottomNav.root)

        configurarBottomNav(binding.bottomNav, NavTab.CONTA)

        atualizarUiConta()
        binding.btnEntrarGoogle.setOnClickListener { lifecycleScope.launch { entrarComGoogle() } }
        binding.btnSair.setOnClickListener { lifecycleScope.launch { sairDaConta() } }
        binding.btnVirarPro.setOnClickListener {
            startActivity(Intent(this, PlansActivity::class.java))
        }
        binding.btnFazerBackup.setOnClickListener { lifecycleScope.launch { fazerBackupAgora() } }
        binding.btnRestaurarBackup.setOnClickListener { lifecycleScope.launch { restaurarBackupAgora() } }

        configurarLinhasDePerfil()

        if (SessionPrefs.estaLogado(this)) {
            // runCatching: Play Billing é uma dependência externa instável
            // nesse estágio (app ainda não é instalado via Play Store) —
            // uma falha aqui não pode derrubar a tela inteira.
            lifecycleScope.launch {
                runCatching { billingManager.atualizarStatusAssinatura() }
                atualizarUiConta()
            }
        }

        // Debug: sem conta no Play Console ainda, não tem como comprar de
        // verdade — esse switch força "sou PRO" (local + backend) só pra
        // testar a UI/backup. Nunca aparece fora de build debug.
        if (BuildConfig.DEBUG) {
            binding.cardDebug.visibility = View.VISIBLE
            binding.switchForcarPro.isChecked = SubscriptionPrefs.isForcarProDebugAtivo(this)
            binding.switchForcarPro.setOnCheckedChangeListener { _, ativo ->
                SubscriptionPrefs.setForcarProDebug(this, ativo)
                atualizarUiConta()
                lifecycleScope.launch { sincronizarForcarProDebug(ativo) }
            }
        }

        // Só mostra "ligado" se o pref diz ativo E a permissão (Android 13+)
        // já foi concedida — senão a notificação não aparece de verdade e o
        // switch ficaria mentindo pro usuário.
        binding.rowNotificacoes.switchRow.isChecked =
            Prefs.isNotificacaoCapturaAtiva(this) && NotificationQuickAdd.temPermissao(this)
        binding.rowNotificacoes.switchRow.setOnCheckedChangeListener { _, ativo ->
            if (ativo) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !NotificationQuickAdd.temPermissao(this)) {
                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    Prefs.setNotificacaoCapturaAtiva(this, true)
                    NotificationQuickAdd.atualizar(this)
                }
            } else {
                Prefs.setNotificacaoCapturaAtiva(this, false)
                NotificationQuickAdd.cancelar(this)
            }
        }

        TutorialTourManager.processar(this, TelaTutorial.CONTA)
    }

    private fun configurarLinhasDePerfil() {
        binding.rowEstatisticas.iconRow.setImageResource(R.drawable.ic_chart)
        binding.rowEstatisticas.textRowTitle.text = getString(R.string.label_estatisticas)
        binding.rowEstatisticas.root.setOnClickListener {
            startActivity(Intent(this, ReportActivity::class.java))
        }

        binding.rowHistorico.iconRow.setImageResource(R.drawable.ic_history)
        binding.rowHistorico.textRowTitle.text = getString(R.string.row_titulo_historico)
        binding.rowHistorico.root.setOnClickListener {
            startActivity(Intent(this, FinanceiroActivity::class.java))
        }

        binding.rowMeuPlano.iconRow.setImageResource(R.drawable.ic_star)
        binding.rowMeuPlano.textRowTitle.text = getString(R.string.row_titulo_meu_plano)
        binding.rowMeuPlano.root.setOnClickListener {
            startActivity(Intent(this, PlansActivity::class.java))
        }

        binding.rowTipoPadrao.iconRow.setImageResource(R.drawable.ic_note)
        binding.rowTipoPadrao.textRowTitle.text = getString(R.string.row_titulo_tipo_padrao)
        binding.rowTipoPadrao.root.setOnClickListener { abrirDialogTipoPadrao() }

        binding.rowAjustarObjetivos.iconRow.setImageResource(R.drawable.ic_target)
        binding.rowAjustarObjetivos.textRowTitle.text = getString(R.string.titulo_ajustar_objetivos)
        binding.rowAjustarObjetivos.root.setOnClickListener { abrirDialogAjustarObjetivos() }

        binding.rowNotificacoes.iconRow.setImageResource(R.drawable.ic_bell)
        binding.rowNotificacoes.textRowTitle.text = getString(R.string.label_notificacao_captura)

        binding.rowPrivacidade.iconRow.setImageResource(R.drawable.ic_shield)
        binding.rowPrivacidade.textRowTitle.text = getString(R.string.row_titulo_privacidade)
        binding.rowPrivacidade.root.setOnClickListener {
            MaterialAlertDialogBuilder(this)
                .setTitle(R.string.titulo_privacidade_stub)
                .setMessage(R.string.conteudo_privacidade_stub)
                .setPositiveButton(R.string.btn_fechar_dialog, null)
                .show()
        }

        binding.rowTermos.iconRow.setImageResource(R.drawable.ic_document)
        binding.rowTermos.textRowTitle.text = getString(R.string.row_titulo_termos)
        binding.rowTermos.root.setOnClickListener {
            MaterialAlertDialogBuilder(this)
                .setTitle(R.string.titulo_termos_stub)
                .setMessage(R.string.conteudo_termos_stub)
                .setPositiveButton(R.string.btn_fechar_dialog, null)
                .show()
        }

        binding.rowTutorial.iconRow.setImageResource(R.drawable.ic_help)
        binding.rowTutorial.textRowTitle.text = getString(R.string.row_titulo_tutorial)
        binding.rowTutorial.root.setOnClickListener {
            TutorialTourManager.iniciar(onboarding = false)
            startActivity(Intent(this, AnotacoesActivity::class.java))
        }

        binding.rowGuiaGesto.iconRow.setImageResource(R.drawable.ic_help)
        binding.rowGuiaGesto.textRowTitle.text = getString(R.string.row_titulo_guia_gesto)
        binding.rowGuiaGesto.root.setOnClickListener {
            startActivity(Intent(this, GestureGuideActivity::class.java))
        }

        binding.rowEscolherAtalho.iconRow.setImageResource(R.drawable.ic_help)
        binding.rowEscolherAtalho.textRowTitle.text = getString(R.string.row_titulo_escolher_atalho)
        binding.rowEscolherAtalho.root.setOnClickListener {
            startActivity(Intent(this, QuickAccessChooserActivity::class.java))
        }
    }

    private fun abrirDialogTipoPadrao() {
        val dialogBinding = DialogTipoPadraoBinding.inflate(layoutInflater)
        when (Prefs.getTipoPadrao(this)) {
            EntryType.GASTO -> dialogBinding.btnPadraoGasto.isChecked = true
            EntryType.RECEBIMENTO -> dialogBinding.btnPadraoRecebimento.isChecked = true
            EntryType.PENSAMENTO -> dialogBinding.btnPadraoPensamento.isChecked = true
        }
        dialogBinding.toggleTipoPadrao.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            val tipo = when (checkedId) {
                dialogBinding.btnPadraoGasto.id -> EntryType.GASTO
                dialogBinding.btnPadraoRecebimento.id -> EntryType.RECEBIMENTO
                else -> EntryType.PENSAMENTO
            }
            Prefs.setTipoPadrao(this, tipo)
        }
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.row_titulo_tipo_padrao)
            .setView(dialogBinding.root)
            .setPositiveButton(R.string.btn_fechar_dialog, null)
            .show()
    }

    private fun abrirDialogAjustarObjetivos() {
        val dialogBinding = DialogAjustarObjetivosBinding.inflate(layoutInflater)
        val valorAtual = Prefs.getMetaEconomiaValor(this)
        if (valorAtual > 0) {
            dialogBinding.editMetaEconomiaValor.setText("%.2f".format(valorAtual))
        }
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.titulo_ajustar_objetivos)
            .setView(dialogBinding.root)
            .setPositiveButton(R.string.btn_salvar_meta) { _, _ ->
                val valor = dialogBinding.editMetaEconomiaValor.text.toString()
                    .replace(",", ".").toFloatOrNull() ?: 0f
                Prefs.setMetaEconomiaValor(this, valor)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun atualizarUiConta() {
        val logado = SessionPrefs.estaLogado(this)

        binding.textContaStatus.visibility = if (logado) View.GONE else View.VISIBLE
        if (!logado) {
            binding.textContaStatus.text = getString(R.string.conta_nao_logado)
        }

        binding.layoutContaLogada.visibility = if (logado) View.VISIBLE else View.GONE
        binding.textContaEmail.text = SessionPrefs.getEmail(this).orEmpty()

        binding.btnEntrarGoogle.visibility = if (logado) View.GONE else View.VISIBLE
        binding.btnSair.visibility = if (logado) View.VISIBLE else View.GONE

        // Backup na nuvem é exclusivo do plano PRO — quem só tem conta
        // logada (mas não assinou) vê o convite pra virar PRO em vez dos
        // botões de backup.
        val pro = logado && SubscriptionPrefs.isPro(this)
        binding.layoutBackupBotoes.visibility = if (pro) View.VISIBLE else View.GONE
        binding.layoutBackupBloqueado.visibility = if (logado && !pro) View.VISIBLE else View.GONE
    }

    private suspend fun entrarComGoogle() {
        binding.btnEntrarGoogle.isEnabled = false
        // Fica visível até a próxima tentativa (em vez de um Toast rápido
        // que passa despercebido) — o backend no Render pode levar bem mais
        // que alguns segundos pra responder se estiver "dormindo".
        binding.textContaStatus.text = getString(R.string.conta_entrando)

        try {
            val resposta = AuthHelper.entrarComGoogle(this)
            atualizarUiConta()
            Toast.makeText(this, getString(R.string.login_sucesso, resposta.user.email), Toast.LENGTH_SHORT).show()
            // Restaura primeiro (nuvem -> local, importante se essa conta já
            // tinha backup de uma instalação anterior) e só depois manda pra
            // fila o que sobrar local-only (categorias/entries criadas antes
            // de logar nessa instalação).
            SyncManager.restaurarTudo(this)
            SyncWorker.agendar(applicationContext)
        } catch (e: Exception) {
            // Cobre tanto falha no picker de contas (GetCredentialException,
            // inclusive o usuário cancelando) quanto erro de rede pro backend.
            // Mostra a mensagem real da exceção pra dar pra diagnosticar sem
            // precisar de logcat.
            mostrarErroLogin(e.message ?: e.toString())
        } finally {
            binding.btnEntrarGoogle.isEnabled = true
        }
    }

    private suspend fun sairDaConta() {
        AuthHelper.sair(this)
        atualizarUiConta()
    }

    private fun mostrarErroLogin(detalhe: String) {
        binding.textContaStatus.text = getString(R.string.login_erro_detalhe, detalhe)
        Toast.makeText(this, R.string.login_erro, Toast.LENGTH_LONG).show()
    }

    private suspend fun fazerBackupAgora() {
        binding.btnFazerBackup.isEnabled = false
        binding.textBackupStatus.visibility = View.VISIBLE
        binding.textBackupStatus.text = getString(R.string.backup_sincronizando)

        try {
            val quantidade = SyncManager.sincronizarTudo(this)
            binding.textBackupStatus.text = if (quantidade > 0) {
                getString(R.string.backup_resultado, quantidade)
            } else {
                getString(R.string.backup_tudo_sincronizado)
            }
        } catch (e: Exception) {
            binding.textBackupStatus.text = getString(R.string.backup_erro)
        } finally {
            binding.btnFazerBackup.isEnabled = true
        }
    }

    private suspend fun restaurarBackupAgora() {
        binding.btnRestaurarBackup.isEnabled = false
        binding.textBackupStatus.visibility = View.VISIBLE
        binding.textBackupStatus.text = getString(R.string.backup_restaurando)

        try {
            val quantidade = SyncManager.restaurarTudo(this)
            binding.textBackupStatus.text = if (quantidade > 0) {
                getString(R.string.backup_restaurado, quantidade)
            } else {
                getString(R.string.backup_nada_pra_restaurar)
            }
        } catch (e: Exception) {
            binding.textBackupStatus.text = getString(R.string.backup_erro)
        } finally {
            binding.btnRestaurarBackup.isEnabled = true
        }
    }

    // Sincroniza o override de debug com o backend, pra ProActiveGuard
    // também liberar (senão o switch só mudaria a UI local, mas o backup
    // continuaria tomando 403). Reaproveita o mesmo endpoint que o
    // BillingManager usa pra compra de verdade — o backend não distingue
    // uma coisa da outra, só confia no que o app relata.
    private suspend fun sincronizarForcarProDebug(ativo: Boolean) {
        if (!SessionPrefs.estaLogado(this)) return
        runCatching {
            ApiClient.api.sincronizarAssinatura(
                "Bearer ${SessionPrefs.getAccessToken(this)}",
                BillingSyncRequest(
                    productId = "debug_forcado",
                    purchaseToken = if (ativo) "debug" else "",
                    active = ativo
                )
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        runCatching { billingManager.encerrar() }
    }
}
