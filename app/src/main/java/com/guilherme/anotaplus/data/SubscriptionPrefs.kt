package com.guilherme.anotaplus.data

import android.content.Context
import androidx.core.content.edit
import com.guilherme.anotaplus.BuildConfig

/**
 * Cache local de "sou PRO ou não" — evita ter que consultar o Play Billing
 * (chamada assíncrona, precisa de conexão com o BillingClient) toda vez que
 * uma tela só precisa decidir se mostra os botões de backup ou um anúncio.
 * A fonte da verdade continua sendo o Play Billing (ver BillingManager);
 * isso aqui é só o último valor conhecido.
 */
object SubscriptionPrefs {
    private const val PREFS_NAME = "anotaplus_subscription"
    private const val KEY_IS_PRO = "is_pro"
    private const val KEY_FORCAR_PRO_DEBUG = "forcar_pro_debug"

    // Sem conta no Play Console ainda, não tem como testar assinatura de
    // verdade — esse override (só existe em build debug) deixa forçar
    // "sou PRO" pra testar a UI/backup sem comprar nada. Tem prioridade
    // sobre o valor real: BillingManager.atualizarStatusAssinatura()
    // continua reconciliando o valor real em segundo plano, mas enquanto
    // o override estiver ligado ele não aparece pra ninguém.
    fun isPro(context: Context): Boolean {
        if (BuildConfig.DEBUG && isForcarProDebugAtivo(context)) return true
        return prefs(context).getBoolean(KEY_IS_PRO, false)
    }

    // Backup na nuvem exige as duas coisas: estar logado E ser PRO. O
    // backend também recusa (ProActiveGuard) se só a segunda falhar, mas
    // checar aqui evita chamada de rede à toa.
    fun podeFazerBackup(context: Context): Boolean =
        SessionPrefs.estaLogado(context) && isPro(context)

    fun setPro(context: Context, pro: Boolean) {
        prefs(context).edit { putBoolean(KEY_IS_PRO, pro) }
    }

    fun isForcarProDebugAtivo(context: Context): Boolean =
        prefs(context).getBoolean(KEY_FORCAR_PRO_DEBUG, false)

    fun setForcarProDebug(context: Context, ativo: Boolean) {
        prefs(context).edit { putBoolean(KEY_FORCAR_PRO_DEBUG, ativo) }
    }

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
