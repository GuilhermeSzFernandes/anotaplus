package com.guilherme.anotaplus

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.guilherme.anotaplus.billing.BillingManager
import com.guilherme.anotaplus.data.AppDatabase
import com.guilherme.anotaplus.data.Carteira
import com.guilherme.anotaplus.data.Category
import com.guilherme.anotaplus.data.CategoryDao
import com.guilherme.anotaplus.data.EntryType
import com.guilherme.anotaplus.data.Prefs
import com.guilherme.anotaplus.data.SessionPrefs
import com.guilherme.anotaplus.data.SubscriptionPrefs
import com.guilherme.anotaplus.databinding.ActivitySettingsBinding
import com.guilherme.anotaplus.databinding.ItemCarteiraBinding
import com.guilherme.anotaplus.databinding.ItemCategoryBinding
import com.guilherme.anotaplus.network.ApiClient
import com.guilherme.anotaplus.network.dto.BillingSyncRequest
import kotlinx.coroutines.launch

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private val billingManager by lazy { BillingManager(applicationContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        aplicarEdgeToEdge(binding.root, binding.header, binding.bottomNav.root)

        configurarBottomNav(binding.bottomNav, NavTab.CONTA)

        atualizarUiConta()
        binding.btnEntrarGoogle.setOnClickListener { lifecycleScope.launch { entrarComGoogle() } }
        binding.btnSair.setOnClickListener { lifecycleScope.launch { sairDaConta() } }
        binding.linkVerPlanos.setOnClickListener {
            startActivity(Intent(this, PlansActivity::class.java))
        }
        binding.linkGuiaGesto.setOnClickListener {
            startActivity(Intent(this, GestureGuideActivity::class.java))
        }
        binding.btnVirarPro.setOnClickListener {
            startActivity(Intent(this, PlansActivity::class.java))
        }
        binding.btnFazerBackup.setOnClickListener { lifecycleScope.launch { fazerBackupAgora() } }
        binding.btnRestaurarBackup.setOnClickListener { lifecycleScope.launch { restaurarBackupAgora() } }

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

        val tipoPadrao = Prefs.getTipoPadrao(this)
        when (tipoPadrao) {
            EntryType.GASTO -> binding.btnPadraoGasto.isChecked = true
            EntryType.RECEBIMENTO -> binding.btnPadraoRecebimento.isChecked = true
            EntryType.PENSAMENTO -> binding.btnPadraoPensamento.isChecked = true
        }
        binding.toggleTipoPadrao.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            val tipo = when (checkedId) {
                binding.btnPadraoGasto.id -> EntryType.GASTO
                binding.btnPadraoRecebimento.id -> EntryType.RECEBIMENTO
                else -> EntryType.PENSAMENTO
            }
            Prefs.setTipoPadrao(this, tipo)
        }

        val categoryDao = AppDatabase.getInstance(applicationContext).categoryDao()

        binding.btnAddCategoria.setOnClickListener {
            val nome = binding.editNovaCategoria.text?.toString()?.trim().orEmpty()
            if (nome.isNotEmpty()) {
                lifecycleScope.launch {
                    categoryDao.insert(Category(nome = nome))
                    if (SubscriptionPrefs.podeFazerBackup(this@SettingsActivity)) {
                        SyncWorker.agendar(applicationContext)
                    }
                }
                binding.editNovaCategoria.text?.clear()
            }
        }

        lifecycleScope.launch {
            categoryDao.getAll().collect { categorias ->
                binding.containerCategorias.removeAllViews()
                binding.textEmptyCategorias.visibility =
                    if (categorias.isEmpty()) View.VISIBLE else View.GONE

                categorias.forEach { categoria ->
                    val row = ItemCategoryBinding.inflate(
                        LayoutInflater.from(this@SettingsActivity),
                        binding.containerCategorias,
                        false
                    )
                    row.textNomeCategoria.text = categoria.nome
                    if (categoria.limite != null) {
                        row.textLimiteCategoria.visibility = View.VISIBLE
                        row.textLimiteCategoria.text = getString(
                            R.string.format_limite_categoria,
                            String.format(MesUtil.locale, "%.2f", categoria.limite)
                        )
                    } else {
                        row.textLimiteCategoria.visibility = View.GONE
                    }
                    row.rowCategoriaClicavel.setOnClickListener {
                        mostrarDialogoLimite(categoria, categoryDao)
                    }
                    row.btnRemoverCategoria.setOnClickListener {
                        lifecycleScope.launch { categoryDao.deleteById(categoria.id) }
                    }
                    binding.containerCategorias.addView(row.root)
                }
            }
        }

        val carteiraDao = AppDatabase.getInstance(applicationContext).carteiraDao()

        binding.btnAddCarteira.setOnClickListener {
            val nome = binding.editNovaCarteira.text?.toString()?.trim().orEmpty()
            if (nome.isNotEmpty()) {
                lifecycleScope.launch {
                    carteiraDao.insert(Carteira(nome = nome))
                    if (SubscriptionPrefs.podeFazerBackup(this@SettingsActivity)) {
                        SyncWorker.agendar(applicationContext)
                    }
                }
                binding.editNovaCarteira.text?.clear()
            }
        }

        lifecycleScope.launch {
            carteiraDao.getAll().collect { carteiras ->
                binding.containerCarteiras.removeAllViews()
                binding.textEmptyCarteiras.visibility =
                    if (carteiras.isEmpty()) View.VISIBLE else View.GONE

                carteiras.forEach { carteira ->
                    val row = ItemCarteiraBinding.inflate(
                        LayoutInflater.from(this@SettingsActivity),
                        binding.containerCarteiras,
                        false
                    )
                    row.textNomeCarteira.text = carteira.nome
                    row.btnRemoverCarteira.setOnClickListener {
                        lifecycleScope.launch { carteiraDao.deleteById(carteira.id) }
                    }
                    binding.containerCarteiras.addView(row.root)
                }
            }
        }
    }

    // Limite mensal por categoria (orçamento): grava local sempre; só
    // propaga pro backend se o usuário for PRO (mesmo gate do backup) —
    // categoria ainda sem remoteId nem precisa de push isolado aqui, o
    // limite já viaja junto na primeira sincronização.
    private fun mostrarDialogoLimite(categoria: Category, categoryDao: CategoryDao) {
        val editLimite = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            hint = getString(R.string.hint_limite_categoria)
            setPadding(48, 24, 48, 24)
            categoria.limite?.let { setText(String.format(MesUtil.locale, "%.2f", it)) }
        }

        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.title_definir_limite, categoria.nome))
            .setView(editLimite)
            .setPositiveButton(R.string.btn_salvar_limite) { _, _ ->
                val novoLimite = editLimite.text?.toString()?.trim()?.replace(",", ".")?.toDoubleOrNull()
                lifecycleScope.launch {
                    categoryDao.atualizarLimite(categoria.id, novoLimite)
                    if (SubscriptionPrefs.podeFazerBackup(this@SettingsActivity)) {
                        val atualizada = categoria.copy(limite = novoLimite)
                        if (atualizada.remoteId != null) {
                            SyncManager.enviarLimiteCategoria(applicationContext, atualizada)
                        } else {
                            SyncWorker.agendar(applicationContext)
                        }
                    }
                }
            }
            .setNegativeButton(R.string.btn_cancelar, null)
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
