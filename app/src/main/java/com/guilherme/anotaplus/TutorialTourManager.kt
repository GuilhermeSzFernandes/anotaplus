package com.guilherme.anotaplus

import android.content.Intent
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.guilherme.anotaplus.ui.CoachMarkOverlay

enum class TelaTutorial { ANOTACOES, FINANCEIRO, CONTA }

private data class PassoTutorial(
    val tela: TelaTutorial,
    val viewId: Int?,
    val titulo: Int,
    val corpo: Int
)

// Tour de tutorial "spotlight": em vez de páginas ilustradas separadas
// (como era antes), escurece a tela real do app e destaca o botão de
// verdade sendo explicado — navega pelas próprias Anotações/Financeiro/
// Conta usando a mesma navegação da bottom nav. Estado em memória só
// (não sobrevive a processo morto): é um passeio guiado efêmero, não
// precisa disso pra ser "simples".
object TutorialTourManager {

    private const val TAG_OVERLAY = "coach_mark_overlay"

    private val passos = listOf(
        PassoTutorial(TelaTutorial.ANOTACOES, null, R.string.tutorial_titulo_captura, R.string.tutorial_corpo_captura),
        PassoTutorial(TelaTutorial.ANOTACOES, R.id.btn_add_ideia, R.string.tutorial_titulo_anotacoes, R.string.tutorial_corpo_anotacoes),
        PassoTutorial(TelaTutorial.FINANCEIRO, R.id.section_saldo, R.string.tutorial_titulo_financeiro, R.string.tutorial_corpo_financeiro),
        PassoTutorial(TelaTutorial.FINANCEIRO, R.id.btn_categorias, R.string.tutorial_titulo_categorias, R.string.tutorial_corpo_categorias),
        PassoTutorial(TelaTutorial.CONTA, R.id.switch_notificacao_captura, R.string.tutorial_titulo_widgets, R.string.tutorial_corpo_widgets),
        PassoTutorial(TelaTutorial.CONTA, R.id.link_ver_tutorial, R.string.tutorial_titulo_conta, R.string.tutorial_corpo_conta)
    )

    private var ativo = false
    private var indice = 0
    private var onboarding = false
    private var abrirHistoricoAoConcluir = false
    private var backCallback: OnBackPressedCallback? = null

    // onboarding=true encadeia com o fluxo de primeira abertura (LoginActivity):
    // ao concluir/pular o tour, segue pro destino normal (Início ou Captura
    // Rápida). onboarding=false (aberto via link em Conta) só fecha o overlay
    // ao terminar, sem navegar pra lugar nenhum.
    fun iniciar(onboarding: Boolean, abrirHistorico: Boolean = false) {
        ativo = true
        indice = 0
        this.onboarding = onboarding
        this.abrirHistoricoAoConcluir = abrirHistorico
    }

    // Chamado no onCreate de AnotacoesActivity/FinanceiroActivity/SettingsActivity.
    // Não faz nada se não houver tour ativo, ou se o passo atual for de outra tela.
    fun processar(activity: AppCompatActivity, tela: TelaTutorial) {
        if (!ativo || indice !in passos.indices || passos[indice].tela != tela) return
        mostrarPassoAtual(activity, tela)
    }

    private fun mostrarPassoAtual(activity: AppCompatActivity, tela: TelaTutorial) {
        val passo = passos[indice]
        val conteudo = activity.window.decorView.findViewById<ViewGroup>(android.R.id.content)
        conteudo.findViewWithTag<View>(TAG_OVERLAY)?.let { conteudo.removeView(it) }

        val overlay = CoachMarkOverlay(activity)
        overlay.tag = TAG_OVERLAY
        conteudo.addView(overlay, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))

        // Sem isso, "voltar" do sistema cairia no comportamento padrão da
        // tela por trás (ex: fechar a Activity) em vez de encerrar o tour —
        // o overlay ficaria "preso" ativo e reapareceria depois do nada.
        backCallback?.remove()
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                encerrar(activity)
            }
        }
        activity.onBackPressedDispatcher.addCallback(activity, callback)
        backCallback = callback

        val primeiroDaTela = indice == 0 || passos[indice - 1].tela != tela
        val ultimoPasso = indice == passos.lastIndex

        overlay.configurar(
            targetView = passo.viewId?.let { activity.findViewById(it) },
            titulo = activity.getString(passo.titulo),
            corpo = activity.getString(passo.corpo),
            indicePagina = indice,
            totalPaginas = passos.size,
            mostrarVoltar = !primeiroDaTela,
            mostrarPular = !ultimoPasso,
            textoAvancar = activity.getString(if (ultimoPasso) R.string.btn_concluir else R.string.btn_avancar),
            onVoltar = {
                indice--
                mostrarPassoAtual(activity, tela)
            },
            onAvancar = {
                if (ultimoPasso) {
                    concluir(activity)
                } else {
                    indice++
                    val proximaTela = passos[indice].tela
                    if (proximaTela == tela) {
                        mostrarPassoAtual(activity, tela)
                    } else {
                        navegarPara(activity, proximaTela)
                    }
                }
            },
            onPular = { encerrar(activity) }
        )
    }

    private fun navegarPara(activity: AppCompatActivity, tela: TelaTutorial) {
        val destino = when (tela) {
            TelaTutorial.ANOTACOES -> AnotacoesActivity::class.java
            TelaTutorial.FINANCEIRO -> FinanceiroActivity::class.java
            TelaTutorial.CONTA -> SettingsActivity::class.java
        }
        activity.startActivity(Intent(activity, destino))
        activity.finish()
    }

    private fun concluir(activity: AppCompatActivity) {
        val eraOnboarding = onboarding
        val abrirHistorico = abrirHistoricoAoConcluir
        encerrar(activity)
        if (eraOnboarding) {
            val destino = if (abrirHistorico) ReportActivity::class.java else QuickCaptureActivity::class.java
            activity.startActivity(Intent(activity, destino))
            activity.finish()
        }
    }

    private fun encerrar(activity: AppCompatActivity) {
        ativo = false
        indice = 0
        backCallback?.remove()
        backCallback = null
        val conteudo = activity.window.decorView.findViewById<ViewGroup>(android.R.id.content)
        conteudo.findViewWithTag<View>(TAG_OVERLAY)?.let { conteudo.removeView(it) }
    }
}
