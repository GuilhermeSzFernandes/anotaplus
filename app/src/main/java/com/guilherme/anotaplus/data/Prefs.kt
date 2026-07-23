package com.guilherme.anotaplus.data

import android.content.Context
import androidx.core.content.edit

object Prefs {
    private const val PREFS_NAME = "anotaplus_prefs"
    private const val KEY_TIPO_PADRAO = "tipo_padrao"
    private const val KEY_ONBOARDING_CONCLUIDO = "onboarding_concluido"
    private const val KEY_ABERTURAS_HISTORICO = "aberturas_historico"
    private const val KEY_NOTIFICACAO_CAPTURA_ATIVA = "notificacao_captura_ativa"
    private const val KEY_META_ECONOMIA_VALOR = "meta_economia_valor"
    private const val KEY_SALARIO_MENSAL = "salario_mensal"
    private const val KEY_HORAS_POR_DIA = "horas_por_dia"
    private const val KEY_DIAS_POR_SEMANA = "dias_por_semana"

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
    // abertura do Financeiro, FinanceiroActivity decide o que fazer com o
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

    // Valor-alvo de economia mensal, definido em "Ajustar objetivos" (Conta).
    // Usado só pra calcular o % de "meta de economia alcançada" no
    // Acompanhamento — não tem relação com a lista de Metas nomeadas.
    fun getMetaEconomiaValor(context: Context): Float =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getFloat(KEY_META_ECONOMIA_VALOR, 0f)

    fun setMetaEconomiaValor(context: Context, valor: Float) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
            putFloat(KEY_META_ECONOMIA_VALOR, valor)
        }
    }

    // Dados salariais: preenchidos no onboarding gamificado (só na criação
    // da conta) e editáveis depois em Perfil > Dados salariais. Usados só
    // pra calcular o valor-hora na calculadora "Vale a pena comprar?" —
    // não têm relação com nenhum outro dado financeiro do app.
    fun getSalarioMensal(context: Context): Float =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getFloat(KEY_SALARIO_MENSAL, 0f)

    fun getHorasPorDia(context: Context): Float =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getFloat(KEY_HORAS_POR_DIA, 0f)

    fun getDiasPorSemana(context: Context): Float =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getFloat(KEY_DIAS_POR_SEMANA, 0f)

    fun setDadosSalariais(context: Context, salarioMensal: Float, horasPorDia: Float, diasPorSemana: Float) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
            putFloat(KEY_SALARIO_MENSAL, salarioMensal)
            putFloat(KEY_HORAS_POR_DIA, horasPorDia)
            putFloat(KEY_DIAS_POR_SEMANA, diasPorSemana)
        }
    }

    fun temDadosSalariais(context: Context): Boolean =
        getSalarioMensal(context) > 0f && getHorasPorDia(context) > 0f && getDiasPorSemana(context) > 0f

    // Semanas por mês: aproximação padrão (52 semanas / 12 meses).
    fun getValorHora(context: Context): Float {
        val salario = getSalarioMensal(context)
        val horasPorDia = getHorasPorDia(context)
        val diasPorSemana = getDiasPorSemana(context)
        val horasPorMes = diasPorSemana * 4.345f * horasPorDia
        return if (horasPorMes > 0f) salario / horasPorMes else 0f
    }
}
