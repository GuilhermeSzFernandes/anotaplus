package com.guilherme.anotaplus.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.guilherme.anotaplus.R
import com.guilherme.anotaplus.databinding.ViewCoachMarkTooltipBinding

// Overlay de tutorial "spotlight": escurece a tela inteira e recorta um
// buraco (com PorterDuff.CLEAR, por isso a LAYER_TYPE_SOFTWARE) em volta do
// botão/área real sendo explicado, deixando só ele em destaque. Quando não
// há um alvo (targetView == null), mostra só o card centralizado — usado
// pros passos que falam de uma função em geral, sem um botão específico.
class CoachMarkOverlay(context: Context) : FrameLayout(context) {

    private val scrimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.scrim)
        alpha = 235
    }
    private val holePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
    }
    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2.5f * resources.displayMetrics.density
        color = ContextCompat.getColor(context, R.color.brass)
    }
    private val holePadding = 10f * resources.displayMetrics.density
    private val holeRadius = 14f * resources.displayMetrics.density
    private var holeRect: RectF? = null

    private val tooltip = ViewCoachMarkTooltipBinding.inflate(LayoutInflater.from(context), this, false)

    init {
        setWillNotDraw(false)
        setLayerType(LAYER_TYPE_SOFTWARE, null)
        isClickable = true
        isFocusable = true
        addView(tooltip.root)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val camada = canvas.saveLayer(0f, 0f, width.toFloat(), height.toFloat(), null)
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), scrimPaint)
        holeRect?.let { alvo ->
            canvas.drawRoundRect(alvo, holeRadius, holeRadius, holePaint)
            canvas.drawRoundRect(alvo, holeRadius, holeRadius, ringPaint)
        }
        canvas.restoreToCount(camada)
    }

    fun configurar(
        targetView: View?,
        titulo: String,
        corpo: String,
        indicePagina: Int,
        totalPaginas: Int,
        mostrarVoltar: Boolean,
        mostrarPular: Boolean,
        textoAvancar: String,
        onVoltar: () -> Unit,
        onAvancar: () -> Unit,
        onPular: () -> Unit
    ) {
        tooltip.textTituloCoach.text = titulo
        tooltip.textCorpoCoach.text = corpo
        tooltip.btnVoltarCoach.visibility = if (mostrarVoltar) View.VISIBLE else View.GONE
        tooltip.btnPularCoach.visibility = if (mostrarPular) View.VISIBLE else View.INVISIBLE
        tooltip.btnAvancarCoach.text = textoAvancar
        tooltip.btnVoltarCoach.setOnClickListener { onVoltar() }
        tooltip.btnAvancarCoach.setOnClickListener { onAvancar() }
        tooltip.btnPularCoach.setOnClickListener { onPular() }
        criarIndicadores(totalPaginas, indicePagina)

        tooltip.root.visibility = View.INVISIBLE
        // Espera o layout assentar (tela nova ainda pode não ter medido nada
        // no primeiro frame) antes de calcular a posição do alvo e do card.
        post {
            val alvoValido = targetView?.takeIf { it.width > 0 && it.isVisible && it.isShown }
            holeRect = alvoValido?.let { calcularRetanguloDoAlvo(it) }
            invalidate()
            posicionarTooltip(holeRect)
        }
    }

    private fun calcularRetanguloDoAlvo(alvo: View): RectF {
        val posicaoAlvo = IntArray(2)
        alvo.getLocationOnScreen(posicaoAlvo)
        val posicaoOverlay = IntArray(2)
        getLocationOnScreen(posicaoOverlay)
        val esquerda = (posicaoAlvo[0] - posicaoOverlay[0] - holePadding)
        val topo = (posicaoAlvo[1] - posicaoOverlay[1] - holePadding)
        return RectF(
            esquerda,
            topo,
            esquerda + alvo.width + holePadding * 2,
            topo + alvo.height + holePadding * 2
        )
    }

    private fun criarIndicadores(total: Int, atual: Int) {
        val container = tooltip.layoutIndicadoresCoach
        container.removeAllViews()
        val tamanho = (8 * resources.displayMetrics.density).toInt()
        val margem = (4 * resources.displayMetrics.density).toInt()
        repeat(total) { indice ->
            val dot = View(context).apply {
                layoutParams = LinearLayout.LayoutParams(tamanho, tamanho).apply {
                    marginStart = margem
                    marginEnd = margem
                }
                setBackgroundResource(
                    if (indice == atual) R.drawable.dot_tutorial_ativo else R.drawable.dot_tutorial_inativo
                )
            }
            container.addView(dot)
        }
    }

    private fun posicionarTooltip(alvo: RectF?) {
        val margemLateral = (20 * resources.displayMetrics.density).toInt()
        val margemAlvo = (16 * resources.displayMetrics.density).toInt()
        val params = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
            leftMargin = margemLateral
            rightMargin = margemLateral
        }
        tooltip.root.layoutParams = params
        tooltip.root.requestLayout()
        tooltip.root.post {
            val alturaCard = tooltip.root.height
            val alturaTela = height
            val topo = when {
                alvo == null -> (alturaTela - alturaCard) / 2
                alvo.top > alturaTela / 2f -> (alvo.top - margemAlvo - alturaCard).toInt().coerceAtLeast(margemLateral)
                else -> (alvo.bottom + margemAlvo).toInt()
            }
            (tooltip.root.layoutParams as LayoutParams).topMargin = topo
            tooltip.root.requestLayout()
            tooltip.root.visibility = View.VISIBLE
        }
    }
}
