package com.guilherme.anotaplus

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import com.guilherme.anotaplus.databinding.ActivityTutorialBinding

private data class TutorialPagina(val icone: Int, val titulo: Int, val corpo: Int)

// Tutorial simples de funções do app (não confundir com GestureGuideActivity,
// que só ensina a configurar o gesto de abrir o app). Aparece uma vez no
// onboarding (depois do LoginActivity) e pode ser reaberto a qualquer hora
// via link em Configurações — nesse caso EXTRA_ONBOARDING não é passado, e
// concluir()/pular só fecha a tela, sem navegar pra lugar nenhum.
class TutorialActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_ONBOARDING = "onboarding"
        const val EXTRA_ABRIR_HISTORICO = "abrir_historico"
    }

    private lateinit var binding: ActivityTutorialBinding
    private var paginaAtual = 0

    private val paginas = listOf(
        TutorialPagina(R.drawable.ic_add, R.string.tutorial_titulo_captura, R.string.tutorial_corpo_captura),
        TutorialPagina(R.drawable.ic_note, R.string.tutorial_titulo_anotacoes, R.string.tutorial_corpo_anotacoes),
        TutorialPagina(R.drawable.ic_wallet, R.string.tutorial_titulo_financeiro, R.string.tutorial_corpo_financeiro),
        TutorialPagina(R.drawable.ic_tag, R.string.tutorial_titulo_categorias, R.string.tutorial_corpo_categorias),
        TutorialPagina(R.drawable.ic_home, R.string.tutorial_titulo_widgets, R.string.tutorial_corpo_widgets),
        TutorialPagina(R.drawable.ic_account, R.string.tutorial_titulo_conta, R.string.tutorial_corpo_conta)
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTutorialBinding.inflate(layoutInflater)
        setContentView(binding.root)

        criarIndicadores()
        renderPagina()

        binding.btnVoltar.setOnClickListener {
            if (paginaAtual > 0) {
                paginaAtual--
                renderPagina()
            }
        }
        binding.btnAvancar.setOnClickListener {
            if (paginaAtual < paginas.lastIndex) {
                paginaAtual++
                renderPagina()
            } else {
                concluir()
            }
        }
        binding.linkPular.setOnClickListener { concluir() }
    }

    private fun criarIndicadores() {
        val tamanho = (8 * resources.displayMetrics.density).toInt()
        val margem = (4 * resources.displayMetrics.density).toInt()
        binding.layoutIndicadores.removeAllViews()
        repeat(paginas.size) {
            val dot = View(this).apply {
                layoutParams = LinearLayout.LayoutParams(tamanho, tamanho).apply {
                    marginStart = margem
                    marginEnd = margem
                }
            }
            binding.layoutIndicadores.addView(dot)
        }
    }

    private fun renderPagina() {
        val pagina = paginas[paginaAtual]
        binding.imgIcone.setImageResource(pagina.icone)
        binding.textTitulo.setText(pagina.titulo)
        binding.textCorpo.setText(pagina.corpo)

        binding.btnVoltar.visibility = if (paginaAtual == 0) View.INVISIBLE else View.VISIBLE
        binding.linkPular.visibility = if (paginaAtual == paginas.lastIndex) View.INVISIBLE else View.VISIBLE
        binding.btnAvancar.setText(
            if (paginaAtual == paginas.lastIndex) R.string.btn_concluir else R.string.btn_avancar
        )

        for (indice in 0 until binding.layoutIndicadores.childCount) {
            binding.layoutIndicadores.getChildAt(indice).setBackgroundResource(
                if (indice == paginaAtual) R.drawable.dot_tutorial_ativo else R.drawable.dot_tutorial_inativo
            )
        }
    }

    private fun concluir() {
        if (intent.getBooleanExtra(EXTRA_ONBOARDING, false)) {
            val abrirHistorico = intent.getBooleanExtra(EXTRA_ABRIR_HISTORICO, false)
            val destino = if (abrirHistorico) ReportActivity::class.java else QuickCaptureActivity::class.java
            startActivity(Intent(this, destino))
        }
        finish()
    }
}
