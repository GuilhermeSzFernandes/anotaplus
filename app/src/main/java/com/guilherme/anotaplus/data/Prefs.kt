package com.guilherme.anotaplus.data

import android.content.Context
import androidx.core.content.edit

object Prefs {
    private const val PREFS_NAME = "anotaplus_prefs"
    private const val KEY_TIPO_PADRAO = "tipo_padrao"
    private const val KEY_ONBOARDING_CONCLUIDO = "onboarding_concluido"
    private const val KEY_ABERTURAS_HISTORICO = "aberturas_historico"
    private const val KEY_NOTIFICACAO_CAPTURA_ATIVA = "notificacao_captura_ativa"
    private const val KEY_GESTO_ACELEROMETRO_ATIVO = "gesto_acelerometro_ativo"
    private const val KEY_LIMIAR_GESTO_ACELEROMETRO = "limiar_gesto_acelerometro"

    // Mesmo valor padrão do TapBackGestureService — repetido aqui (em vez de
    // referenciar a constante de lá) porque data/ não deveria depender de
    // uma classe de Service só pra pegar um número.
    const val LIMIAR_GESTO_ACELEROMETRO_PADRAO = 24f

    fun getTipoPadrao(context: Context): EntryType {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val nome = prefs.getString(KEY_TIPO_PADRAO, EntryType.PENSAMENTO.name)
        return runCatching { EntryType.valueOf(nome ?: EntryType.PENSAMENTO.name) }
            .getOrDefault(EntryType.PENSAMENTO)
    }

    fun setTipoPadrao(context: Context, tipo: EntryType) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
            putString(KEY_TIPO_PADRAO, tipo.name)
        }
    }

    // Controla a tela de boas-vindas/login: só aparece na primeiríssima vez
    // que o app é aberto (via ícone ou gesto, o que vier primeiro).
    fun isOnboardingConcluido(context: Context): Boolean =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_ONBOARDING_CONCLUIDO, false)

    fun marcarOnboardingConcluido(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
            putBoolean(KEY_ONBOARDING_CONCLUIDO, true)
        }
    }

    // Controla o intersticial "ocasional" do plano Free: incrementa a cada
    // abertura do Histórico, HistoryActivity decide o que fazer com o
    // número (mostrar só a cada N aberturas).
    fun incrementarAberturasHistorico(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val novoValor = prefs.getInt(KEY_ABERTURAS_HISTORICO, 0) + 1
        prefs.edit { putInt(KEY_ABERTURAS_HISTORICO, novoValor) }
        return novoValor
    }

    // Notificação persistente de captura rápida (ver NotificationQuickAdd.kt)
    // — ligada por padrão, com opção de desligar em Conta.
    fun isNotificacaoCapturaAtiva(context: Context): Boolean =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_NOTIFICACAO_CAPTURA_ATIVA, true)

    fun setNotificacaoCapturaAtiva(context: Context, ativa: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
            putBoolean(KEY_NOTIFICACAO_CAPTURA_ATIVA, ativa)
        }
    }

    // Gesto de toque duplo na traseira via acelerômetro (ver
    // TapBackGestureService) — alternativa pra aparelhos sem esse gesto
    // configurável pelo sistema (MIUI da Xiaomi, por exemplo). Desligado por
    // padrão: só é sugerido, nunca ativado sozinho, porque roda um
    // foreground service o tempo todo. Ligado/desligado pelo switch em Conta.
    fun isGestoAcelerometroAtivo(context: Context): Boolean =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_GESTO_ACELEROMETRO_ATIVO, false)

    fun setGestoAcelerometroAtivo(context: Context, ativo: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
            putBoolean(KEY_GESTO_ACELEROMETRO_ATIVO, ativo)
        }
    }

    // Limiar (m/s² de desvio da gravidade) que conta como uma "batida" pro
    // gesto acima — calibrável em CalibrarGestoActivity, porque o valor
    // ideal varia demais entre aparelhos (case, sensor, jeito de bater).
    fun getLimiarGestoAcelerometro(context: Context): Float =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getFloat(KEY_LIMIAR_GESTO_ACELEROMETRO, LIMIAR_GESTO_ACELEROMETRO_PADRAO)

    fun setLimiarGestoAcelerometro(context: Context, limiar: Float) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
            putFloat(KEY_LIMIAR_GESTO_ACELEROMETRO, limiar)
        }
    }
}
