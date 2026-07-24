package com.guilherme.anotaplus

import java.util.Calendar

// Matemática de prazo/valor-mensal de uma Meta, extraída como função pura
// (sem Context/Android) de propósito — este projeto não tem infraestrutura
// de teste instrumentado (sem emulador em CI), então qualquer lógica que
// precise de teste real tem que rodar em JVM puro via JUnit.
object MetaCalculo {

    // Meses inteiros entre `agora` e `prazo`, arredondado pra baixo e com
    // mínimo de 1 — mesmo um prazo "esse mês" ainda pede uma parcela única
    // em vez de dividir por zero.
    fun mesesAte(prazoEpochMillis: Long, agoraEpochMillis: Long): Int {
        val agora = Calendar.getInstance().apply { timeInMillis = agoraEpochMillis }
        val prazo = Calendar.getInstance().apply { timeInMillis = prazoEpochMillis }
        val meses = (prazo.get(Calendar.YEAR) - agora.get(Calendar.YEAR)) * 12 +
            (prazo.get(Calendar.MONTH) - agora.get(Calendar.MONTH))
        return meses.coerceAtLeast(1)
    }

    // Valor mensal necessário pra ir de valorAtual até valorAlvo dado um
    // prazo — null se a Meta não tem prazo definido (sem preview nesse
    // caso).
    fun valorMensalNecessario(
        valorAlvo: Double,
        valorAtual: Double,
        prazoEpochMillis: Long?,
        agoraEpochMillis: Long
    ): Double? {
        if (prazoEpochMillis == null) return null
        val restante = (valorAlvo - valorAtual).coerceAtLeast(0.0)
        val meses = mesesAte(prazoEpochMillis, agoraEpochMillis)
        return restante / meses
    }

    // Epoch millis de "daqui a `meses` meses", usado pelos atalhos de
    // prazo ("6 meses"/"1 ano").
    fun prazoEmMeses(agoraEpochMillis: Long, meses: Int): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = agoraEpochMillis
            add(Calendar.MONTH, meses)
        }
        return cal.timeInMillis
    }
}
