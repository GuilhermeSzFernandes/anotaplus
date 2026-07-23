package com.guilherme.anotaplus

import android.app.Activity
import android.content.Intent

// Métodos de atalho rápido com tutorial próprio, escolhidos em
// QuickAccessChooserActivity. Notificação não entra aqui: ativar/desativar
// é auto-explicativo (só pede permissão), sem precisar de uma tela de
// tutorial dedicada.
enum class MetodoAtalho { GESTOS, WIDGET }

// Encadeia os tutoriais dos métodos escolhidos em QuickAccessChooserActivity
// (GestureGuideActivity e/ou WidgetSuggestionActivity) e, ao final, retoma o
// fluxo normal: se for onboarding, inicia o tour "spotlight" do
// TutorialTourManager e abre AnotacoesActivity; senão (reaberto avulso a
// partir de Conta) só fecha.
//
// A fila viaja inteira como Intent extra (String[] com os names do enum) em
// vez de estado em memória — diferente do TutorialTourManager (que aceita
// ser efêmero de propósito), aqui cada Activity intermediária já é uma tela
// cheia que o usuário pode demorar pra passar, então precisa sobreviver a
// processo morto.
object QuickAccessFlow {
    const val EXTRA_ONBOARDING = "onboarding"
    const val EXTRA_ABRIR_HISTORICO = "abrir_historico"
    private const val EXTRA_FILA_METODOS = "fila_metodos_atalho"

    fun avancar(activity: Activity, filaRestante: List<MetodoAtalho>, onboarding: Boolean, abrirHistorico: Boolean) {
        val proximo = filaRestante.firstOrNull()
        if (proximo == null) {
            concluir(activity, onboarding, abrirHistorico)
            return
        }
        val resto = filaRestante.drop(1)
        val destino = when (proximo) {
            MetodoAtalho.GESTOS -> GestureGuideActivity::class.java
            MetodoAtalho.WIDGET -> WidgetSuggestionActivity::class.java
        }
        activity.startActivity(
            Intent(activity, destino)
                .putStringArrayListExtra(EXTRA_FILA_METODOS, ArrayList(resto.map { it.name }))
                .putExtra(EXTRA_ONBOARDING, onboarding)
                .putExtra(EXTRA_ABRIR_HISTORICO, abrirHistorico)
        )
        activity.finish()
    }

    // Só existe quando a Activity foi aberta como parte desse fluxo — usado
    // por GestureGuideActivity pra distinguir "faz parte da fila" de "aberta
    // avulsa a partir do link em Conta" (nesse segundo caso, o botão
    // "Entendi" deve só fechar a tela, sem tentar avançar fila nenhuma).
    fun temFilaNoIntent(activity: Activity): Boolean =
        activity.intent.hasExtra(EXTRA_FILA_METODOS)

    fun proximaFila(activity: Activity): List<MetodoAtalho> =
        activity.intent.getStringArrayListExtra(EXTRA_FILA_METODOS)
            ?.mapNotNull { runCatching { MetodoAtalho.valueOf(it) }.getOrNull() }
            ?: emptyList()

    private fun concluir(activity: Activity, onboarding: Boolean, abrirHistorico: Boolean) {
        if (onboarding) {
            TutorialTourManager.iniciar(onboarding = true, abrirHistorico = abrirHistorico)
            activity.startActivity(Intent(activity, AnotacoesActivity::class.java))
        }
        activity.finish()
    }
}
