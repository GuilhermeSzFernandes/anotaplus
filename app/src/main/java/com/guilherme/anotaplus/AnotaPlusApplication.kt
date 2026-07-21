package com.guilherme.anotaplus

import android.app.Activity
import android.app.Application
import android.os.Bundle
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner

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
