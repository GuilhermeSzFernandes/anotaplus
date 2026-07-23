package com.guilherme.anotaplus

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.guilherme.anotaplus.data.AppDatabase
import com.guilherme.anotaplus.data.Entry
import com.guilherme.anotaplus.data.EntryType
import com.guilherme.anotaplus.data.Prefs
import com.guilherme.anotaplus.data.SubscriptionPrefs
import com.guilherme.anotaplus.databinding.ActivityQuickCaptureBinding
import com.guilherme.anotaplus.widget.WidgetUpdater
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class QuickCaptureActivity : AppCompatActivity() {

    companion object {
        // Usado pelo widget "modal" (CapturaModalWidgetProvider): tocar no
        // toggle Gasto/Anotações do widget já abre aqui com esse tipo,
        // em vez do tipo padrão de Configurações.
        const val EXTRA_TIPO_FORCADO = "tipo_forcado"
    }

    private lateinit var binding: ActivityQuickCaptureBinding
    private var tipoSelecionado: EntryType = EntryType.PENSAMENTO
    private var nomesCarteiras: List<String> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Antes de qualquer outra coisa: se o app crashou da última vez,
        // mostra o stacktrace num diálogo copiável em vez de seguir o
        // fluxo normal — sem isso não tem como diagnosticar sem Android
        // Studio/adb. Ver CrashHandler.kt.
        val crash = CrashHandler.pegarUltimoCrash(this)
        if (crash != null) {
            CrashHandler.limparUltimoCrash(this)
            mostrarDialogoCrash(crash)
            return
        }

        // Ícone da tela inicial sempre chega com sourceBounds (a posição do
        // ícone na tela, usada na animação de abertura do launcher). O gesto
        // do Moto Actions dispara o mesmo intent de LAUNCHER, mas não vem de
        // um toque na tela, então nunca tem sourceBounds. É essa diferença
        // que usamos pra separar as duas origens sem precisar mexer na
        // configuração do gesto no aparelho.
        val abrirHistorico = intent.sourceBounds != null

        // Primeiríssima vez que o app é aberto (ícone ou gesto, o que vier
        // primeiro): manda pra tela de login e, depois de logar, pra escolha
        // de atalhos rápidos (ver QuickAccessFlow.kt) antes de seguir pro
        // destino normal. Depois disso nunca mais aparece.
        if (!Prefs.isOnboardingConcluido(this)) {
            startActivity(
                Intent(this, LoginActivity::class.java)
                    .putExtra(QuickAccessFlow.EXTRA_ONBOARDING, true)
                    .putExtra(QuickAccessFlow.EXTRA_ABRIR_HISTORICO, abrirHistorico)
            )
            finish()
            return
        }

        if (abrirHistorico) {
            startActivity(Intent(this, ReportActivity::class.java))
            finish()
            return
        }

        // Permite abrir por cima da tela de bloqueio (sem desbloquear o
        // aparelho) e acordar a tela se estiver apagada, do mesmo jeito que
        // o atalho da câmera funciona no lock screen.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }

        binding = ActivityQuickCaptureBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Garante que o teclado abra sozinho assim que o modal aparecer
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)

        // Toque fora do card fecha o modal, como um diálogo normal
        binding.rootScrim.setOnClickListener { finish() }

        val timestampFormat = SimpleDateFormat("dd MMM · HH:mm", Locale("pt", "BR"))
        binding.textTimestamp.text = timestampFormat.format(Date()).uppercase(Locale("pt", "BR"))

        val tipoForcado = intent.getStringExtra(EXTRA_TIPO_FORCADO)?.let {
            runCatching { EntryType.valueOf(it) }.getOrNull()
        }
        val tipoPadrao = tipoForcado ?: Prefs.getTipoPadrao(this)
        tipoSelecionado = tipoPadrao
        when (tipoPadrao) {
            EntryType.GASTO -> binding.btnTipoGasto.isChecked = true
            EntryType.RECEBIMENTO -> binding.btnTipoRecebimento.isChecked = true
            EntryType.PENSAMENTO -> binding.btnTipoPensamento.isChecked = true
        }
        atualizarCamposPorTipo(tipoPadrao)

        // Combobox: sem teclado, o toque sempre abre a lista inteira em vez
        // de filtrar por texto digitado (não há texto digitado possível).
        binding.editCategoria.setOnClickListener { binding.editCategoria.showDropDown() }

        lifecycleScope.launch {
            val nomes = AppDatabase.getInstance(applicationContext).categoryDao().getNomesOnce()
            binding.editCategoria.setAdapter(
                ArrayAdapter(this@QuickCaptureActivity, android.R.layout.simple_dropdown_item_1line, nomes)
            )
        }

        // Carteira ainda pode estar vazia (não vem com padrão nenhum, ao
        // contrário de categoria) — avisa em vez de abrir um dropdown vazio.
        binding.editCarteira.setOnClickListener {
            if (nomesCarteiras.isEmpty()) {
                Toast.makeText(this, R.string.aviso_sem_carteira, Toast.LENGTH_LONG).show()
            } else {
                binding.editCarteira.showDropDown()
            }
        }
        lifecycleScope.launch {
            nomesCarteiras = AppDatabase.getInstance(applicationContext).carteiraDao().getNomesOnce()
            binding.editCarteira.setAdapter(
                ArrayAdapter(this@QuickCaptureActivity, android.R.layout.simple_dropdown_item_1line, nomesCarteiras)
            )
        }

        // Enter no valor fecha o teclado em vez de pular pro próximo campo
        // (categoria/carteira não têm teclado, então o foco "sumia" e
        // confundia) — diferente do campo de texto (Nota), que continua
        // com o comportamento padrão de multi-linha.
        binding.editValor.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(binding.editValor.windowToken, 0)
                true
            } else {
                false
            }
        }

        binding.toggleTipo.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            tipoSelecionado = when (checkedId) {
                binding.btnTipoGasto.id -> EntryType.GASTO
                binding.btnTipoRecebimento.id -> EntryType.RECEBIMENTO
                else -> EntryType.PENSAMENTO
            }
            atualizarCamposPorTipo(tipoSelecionado)
            focarCampoPrincipal(tipoSelecionado)
        }

        binding.btnSalvar.setOnClickListener { salvar() }

        // "ir ao app" leva pra tela certa conforme o tipo em foco no
        // momento — Financeiro pra Gasto/Recebimento, Anotações pra Ideia.
        binding.btnHistorico.setOnClickListener {
            val destino = if (tipoSelecionado == EntryType.PENSAMENTO) {
                AnotacoesActivity::class.java
            } else {
                FinanceiroActivity::class.java
            }
            startActivity(Intent(this, destino))
            finish()
        }

        focarCampoPrincipal(tipoPadrao)
    }

    private fun atualizarCamposPorTipo(tipo: EntryType) {
        val mostraCamposFinanceiros = tipo == EntryType.GASTO || tipo == EntryType.RECEBIMENTO
        val visibilidade = if (mostraCamposFinanceiros) android.view.View.VISIBLE else android.view.View.GONE
        binding.layoutValor.visibility = visibilidade
        binding.editCategoria.visibility = visibilidade
        binding.editCarteira.visibility = visibilidade
        binding.editTexto.hint = getString(
            when (tipo) {
                EntryType.GASTO -> R.string.hint_gasto
                EntryType.RECEBIMENTO -> R.string.hint_recebimento
                EntryType.PENSAMENTO -> R.string.hint_pensamento
            }
        )
    }

    // Em Gasto/Recebimento, o valor é o dado mais importante e o primeiro
    // que a pessoa quer digitar (o texto é opcional); em Ideia, o texto é o
    // próprio conteúdo, então continua sendo o campo focado.
    private fun focarCampoPrincipal(tipo: EntryType) {
        val campo = if (tipo == EntryType.PENSAMENTO) binding.editTexto else binding.editValor
        campo.requestFocus()
    }

    private fun salvar() {
        val texto = binding.editTexto.text?.toString()?.trim().orEmpty()
        val valorTexto = binding.editValor.text?.toString()?.trim()
        val categoria = binding.editCategoria.text?.toString()?.trim()
        val carteira = binding.editCarteira.text?.toString()?.trim()

        if (texto.isEmpty() && valorTexto.isNullOrEmpty()) {
            // nada pra salvar, só fecha
            finish()
            return
        }

        val valor = valorTexto?.replace(",", ".")?.toDoubleOrNull()
        val ehFinanceiro = tipoSelecionado == EntryType.GASTO || tipoSelecionado == EntryType.RECEBIMENTO

        val entry = Entry(
            type = tipoSelecionado,
            texto = texto,
            valor = if (ehFinanceiro) valor else null,
            categoria = if (ehFinanceiro) categoria?.ifEmpty { null } else null,
            carteira = if (ehFinanceiro) carteira?.ifEmpty { null } else null
        )

        lifecycleScope.launch {
            AppDatabase.getInstance(applicationContext).entryDao().insert(entry)
            if (ehFinanceiro) {
                WidgetUpdater.atualizarTodos(applicationContext)
            }
            if (tipoSelecionado == EntryType.GASTO) {
                BudgetAlertNotifier.verificarLimite(applicationContext, entry.categoria)
            }
            if (SubscriptionPrefs.podeFazerBackup(this@QuickCaptureActivity)) {
                SyncWorker.agendar(applicationContext)
            }
            finish()
        }
    }

    private fun mostrarDialogoCrash(stacktrace: String) {
        MaterialAlertDialogBuilder(this)
            .setTitle("O app crashou da última vez")
            .setMessage(stacktrace)
            .setCancelable(false)
            .setPositiveButton("Copiar") { _, _ ->
                val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("crash", stacktrace))
                Toast.makeText(this, "Copiado", Toast.LENGTH_SHORT).show()
                finish()
            }
            .setNegativeButton("Fechar") { _, _ -> finish() }
            .show()
    }
}
