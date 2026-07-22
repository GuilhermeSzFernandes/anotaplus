package com.guilherme.anotaplus.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import com.guilherme.anotaplus.R

/**
 * Barras verticais simples (sem eixo/rótulo/tooltip) — mesmo padrão do
 * TrendChartView/PerforationView: Canvas puro, sem lib de gráficos.
 */
class BarChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private var valores: List<Double> = emptyList()
    private val density = resources.displayMetrics.density

    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColor(R.color.gasto_color)
        style = Paint.Style.FILL
    }
    private val baselinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColor(R.color.paper_dim)
        strokeWidth = 1.5f * density
    }
    private val rect = RectF()

    fun setDados(novosValores: List<Double>) {
        valores = novosValores
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        canvas.drawLine(0f, h, w, h, baselinePaint)
        if (valores.isEmpty() || w <= 0f || h <= 0f) return

        val maiorValor = valores.max().takeIf { it > 0 } ?: return
        val gap = 8f * density
        val barWidth = (w - gap * (valores.size - 1)) / valores.size
        val topPadding = h * 0.08f
        val usableHeight = h - topPadding
        val radius = (barWidth / 2f).coerceAtMost(6f * density)

        valores.forEachIndexed { index, valor ->
            val alturaBarra = (valor / maiorValor * usableHeight).toFloat().coerceAtLeast(3f * density)
            val left = index * (barWidth + gap)
            rect.set(left, h - alturaBarra, left + barWidth, h)
            canvas.drawRoundRect(rect, radius, radius, barPaint)
        }
    }
}
