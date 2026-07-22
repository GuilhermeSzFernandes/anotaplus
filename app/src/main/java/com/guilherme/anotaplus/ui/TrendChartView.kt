package com.guilherme.anotaplus.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View
import com.guilherme.anotaplus.R

/**
 * Gráfico de linha bem simples (sem eixos rotulados, sem tooltip) pra
 * tendência de gastos ao longo do tempo — Canvas customizado em vez de
 * lib de gráficos, mesmo padrão do PerforationView, pra manter a estética
 * de recibo e não depender de nenhuma dependência nova.
 */
class TrendChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private var pontos: List<Double> = emptyList()
    private val density = resources.displayMetrics.density

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColor(R.color.gasto_color)
        style = Paint.Style.STROKE
        strokeWidth = 3f * density
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
    }
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColor(R.color.gasto_color)
        alpha = 40
        style = Paint.Style.FILL
    }
    private val baselinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColor(R.color.paper_dim)
        strokeWidth = 1.5f * density
    }

    private val linePath = Path()
    private val fillPath = Path()

    fun setDados(valores: List<Double>) {
        pontos = valores
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        canvas.drawLine(0f, h, w, h, baselinePaint)
        if (pontos.size < 2 || w <= 0f || h <= 0f) return

        val maiorValor = pontos.max().takeIf { it > 0 } ?: return
        val stepX = w / (pontos.size - 1)
        val topPadding = h * 0.1f
        val usableHeight = h - topPadding

        linePath.reset()
        fillPath.reset()

        pontos.forEachIndexed { index, valor ->
            val x = index * stepX
            val y = topPadding + usableHeight - (valor / maiorValor * usableHeight).toFloat()
            if (index == 0) {
                linePath.moveTo(x, y)
                fillPath.moveTo(x, h)
                fillPath.lineTo(x, y)
            } else {
                linePath.lineTo(x, y)
                fillPath.lineTo(x, y)
            }
        }
        fillPath.lineTo(w, h)
        fillPath.close()

        canvas.drawPath(fillPath, fillPaint)
        canvas.drawPath(linePath, linePaint)
    }
}
