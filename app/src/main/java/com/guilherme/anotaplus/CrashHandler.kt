package com.guilherme.anotaplus

import android.content.Context
import android.util.Log

/**
 * Sem Android Studio nem adb à mão, não tem como pegar um logcat quando o
 * app crasha antes mesmo de mostrar qualquer tela — então guarda o
 * stacktrace do último crash em SharedPreferences (commit síncrono, porque
 * o processo pode morrer logo em seguida) e deixa QuickCaptureActivity
 * mostrar ele num diálogo copiável na próxima abertura, antes de mais nada.
 */
object CrashHandler : Thread.UncaughtExceptionHandler {
    private const val PREFS_NAME = "anotaplus_crash"
    private const val KEY_ULTIMO_CRASH = "ultimo_crash"

    private var original: Thread.UncaughtExceptionHandler? = null
    private lateinit var appContext: Context

    fun instalar(context: Context) {
        appContext = context.applicationContext
        original = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler(this)
    }

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        runCatching {
            appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_ULTIMO_CRASH, Log.getStackTraceString(throwable))
                .commit()
        }
        // Deixa o comportamento padrão continuar (diálogo "app parou" e
        // morte do processo) — isso aqui só grava, não impede o crash.
        original?.uncaughtException(thread, throwable)
    }

    fun pegarUltimoCrash(context: Context): String? =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString(KEY_ULTIMO_CRASH, null)

    fun limparUltimoCrash(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().remove(KEY_ULTIMO_CRASH).apply()
    }
}
