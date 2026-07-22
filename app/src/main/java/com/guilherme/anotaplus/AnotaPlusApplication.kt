package com.guilherme.anotaplus

import android.app.Activity
import android.app.Application
import android.os.Bundle
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.google.android.gms.ads.MobileAds
import com.guilherme.anotaplus.data.Prefs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Garante que nenhuma task do app sobrevive em segundo plano.
 *
 * Motivo: o gesto "toque duas vezes na traseira" do Moto Actions dispara o
 * mesmo intent MAIN/LAUNCHER de sempre, mas se o Android/Moto detectar que
 * o app já tem uma task viva (ex: usuário navegou pro Histórico e só
 * apertou Home, sem fechar), ele parece só trazer essa task de volta pra
 * frente — igual reabrir pelos Recentes — em vez de entregar o intent pro
 * QuickCaptureActivity de novo. Nesse caso o modal nunca aparece, só a
 * última tela que tava aberta. `taskAffinity=""` no QuickCaptureActivity
 * não resolveu isso sozinho porque o problema é a task das OUTRAS
 * activities (Histórico, Configurações etc.) que continua viva.
 *
 * A solução: ao detectar que o app INTEIRO saiu de primeiro plano (não
 * apenas trocou de tela internamente — é isso que o ProcessLifecycleOwner
 * garante, diferente do onStop() de cada Activity individual), derruba a
 * task da última activity visível via finishAndRemoveTask(). Assim, da
 * próxima vez (ícone ou gesto), não existe task nenhuma pra "resgatar" —
 * sempre é uma abertura do zero.
 */
class AnotaPlusApplication : Application() {

    private var activityAtual: Activity? = null

    override fun onCreate() {
        super.onCreate()

        // Antes de qualquer outra coisa: instala o coletor de crash (ver
        // CrashHandler.kt) — sem Android Studio/adb, é a única forma
        // prática de descobrir o que está derrubando o app.
        CrashHandler.instalar(this)

        // Inicialização pode levar um tempo (rede) e não precisa bloquear
        // nada — os primeiros loadAd() de banner/intersticial esperam ela
        // terminar sozinhos internamente. runCatching porque isso roda em
        // TODO início do app (Application.onCreate) sem nenhum outro try/
        // catch por perto — uma exceção aqui sem proteção derruba o app
        // inteiro em toda abertura, não só uma tela.
        CoroutineScope(Dispatchers.IO).launch {
            runCatching { MobileAds.initialize(this@AnotaPlusApplication) }
        }

        // Garante que a notificação de captura rápida reflete o switch de
        // Conta a cada início de processo — não sobrevive a reboot (ver
        // NotificationQuickAdd.kt), isso aqui é o que traz ela de volta.
        runCatching { NotificationQuickAdd.atualizar(this) }

        // Mesma lógica pro gesto de toque duplo via acelerômetro: se o
        // usuário tinha ligado, o serviço também não sobrevive a reboot
        // (ver TapBackGestureService.kt) — reinicia aqui.
        if (Prefs.isGestoAcelerometroAtivo(this)) {
            runCatching { TapBackGestureService.iniciar(this) }
        }

        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityResumed(activity: Activity) {
                activityAtual = activity
            }

            override fun onActivityDestroyed(activity: Activity) {
                if (activityAtual == activity) activityAtual = null
            }

            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
            override fun onActivityStarted(activity: Activity) {}
            override fun onActivityPaused(activity: Activity) {}
            override fun onActivityStopped(activity: Activity) {}
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
        })

        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStop(owner: LifecycleOwner) {
                activityAtual?.finishAndRemoveTask()
            }
        })
    }
}
