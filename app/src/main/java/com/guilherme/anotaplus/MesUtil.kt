package com.guilherme.anotaplus

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Intervalo de um mês em epoch millis (usado nas queries de extrato e
 * relatório) e navegação mês a mês, compartilhados entre FinanceiroActivity
 * e ReportActivity.
 */
object MesUtil {
    val locale: Locale = Locale("pt", "BR")
    private val monthFormat = SimpleDateFormat("MMMM yyyy", locale)

    fun formatar(calendar: Calendar): String = monthFormat.format(calendar.time).uppercase(locale)

    fun inicioDoMes(calendar: Calendar): Long {
        val c = calendar.clone() as Calendar
        c.set(Calendar.DAY_OF_MONTH, 1)
        c.set(Calendar.HOUR_OF_DAY, 0)
        c.set(Calendar.MINUTE, 0)
        c.set(Calendar.SECOND, 0)
        c.set(Calendar.MILLISECOND, 0)
        return c.timeInMillis
    }

    fun fimDoMes(calendar: Calendar): Long {
        val c = calendar.clone() as Calendar
        c.set(Calendar.DAY_OF_MONTH, 1)
        c.set(Calendar.HOUR_OF_DAY, 0)
        c.set(Calendar.MINUTE, 0)
        c.set(Calendar.SECOND, 0)
        c.set(Calendar.MILLISECOND, 0)
        c.add(Calendar.MONTH, 1)
        return c.timeInMillis - 1
    }

    fun mesAnterior(calendar: Calendar): Calendar {
        val c = calendar.clone() as Calendar
        c.add(Calendar.MONTH, -1)
        return c
    }

    fun estaNoMesAtual(calendar: Calendar): Boolean {
        val agora = Calendar.getInstance()
        return calendar.get(Calendar.YEAR) == agora.get(Calendar.YEAR) &&
            calendar.get(Calendar.MONTH) == agora.get(Calendar.MONTH)
    }
}
