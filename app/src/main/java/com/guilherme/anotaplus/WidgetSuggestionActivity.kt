package com.guilherme.anotaplus

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.guilherme.anotaplus.databinding.ActivityWidgetSuggestionBinding
import com.guilherme.anotaplus.widget.CapturaRapidaWidgetProvider

/**
 * Última etapa da fila de QuickAccessFlow quando "Widget" é escolhido em
 * QuickAccessChooserActivity: sugere fixar o widget "Captura Rápida" (1x1,
 * o mais simples dos seis) direto na tela inicial via
 * AppWidgetManager.requestPinAppWidget (Android 8+, e só se o launcher do
 * aparelho suportar o pedido) — sem isso, mostra a instrução manual de
 * sempre (segurar a tela inicial).
 */
class WidgetSuggestionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWidgetSuggestionBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWidgetSuggestionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.onboardingHeader.bind(
            activity = this,
            progresso = 0.90f,
            pergunta = getString(R.string.title_widget_sugestao)
        )

        val appWidgetManager = AppWidgetManager.getInstance(this)
        val suportado = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            appWidgetManager.isRequestPinAppWidgetSupported

        binding.btnAdicionarWidget.visibility = if (suportado) View.VISIBLE else View.GONE
        binding.textWidgetFallback.visibility = if (suportado) View.GONE else View.VISIBLE

        binding.btnAdicionarWidget.setOnClickListener {
            // runCatching: o launcher pode recusar/cancelar o pedido a
            // qualquer momento (inclusive o usuário só fechando o diálogo do
            // sistema) — não é um erro pra tratar, só decidiu não adicionar
            // agora.
            runCatching {
                appWidgetManager.requestPinAppWidget(
                    ComponentName(this, CapturaRapidaWidgetProvider::class.java),
                    null,
                    null
                )
            }
        }

        binding.btnConcluirWidget.setOnClickListener { continuar() }
    }

    private fun continuar() {
        QuickAccessFlow.avancar(
            this,
            QuickAccessFlow.proximaFila(this),
            intent.getBooleanExtra(QuickAccessFlow.EXTRA_ONBOARDING, false),
            intent.getBooleanExtra(QuickAccessFlow.EXTRA_ABRIR_HISTORICO, false)
        )
    }
}
