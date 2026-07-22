package com.guilherme.anotaplus.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import com.guilherme.anotaplus.R

/**
 * Anel proporcional (não pizza cheia) — mesmo padrão Canvas puro do resto
 * das Views de gráfico do app (TrendChartView/BarChartView), sem lib.
 * O texto do total fica por fora (TextView sobreposta via FrameLayout no
 * layout que usa essa View), não é desenhado aqui.
 */
class DonutChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private var fatias: List<Double> = emptyList()
    private val density = resources.displayMetrics.density
    private val strokeWidthPx = 14f * density

    private val cores = intArrayOf(
        context.getColor(R.color.brass),
        context.getColor(R.color.gasto_color),
        context.getColor(R.color.color_positivo),
        context.getColor(R.color.pensamento_color),
        context.getColor(R.color.chart_teal),
        context.getColor(R.color.chart_dusty_rose)
    )

    private val arcPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = strokeWidthPx
        strokeCap = Paint.Cap.BUTT
    }
    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = strokeWidthPx
        color = context.getColor(R.color.paper_dim)
    }
    private val rect = RectF()

    fun setDados(valores: List<Double>) {
        fatias = valores
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return

        val diametro = minOf(w, h) - strokeWidthPx
        val left = (w - diametro) / 2f
        val top = (h - diametro) / 2f
        rect.set(left, top, left + diametro, top + diametro)

        canvas.drawArc(rect, 0f, 360f, false, trackPaint)

        val total = fatias.sum()
        if (total <= 0.0) return

        var anguloAtual = -90f
        fatias.forEachIndexed { index, valor ->
            val varredura = (valor / total * 360.0).toFloat()
            arcPaint.color = cores[index % cores.size]
            canvas.drawArc(rect, anguloAtual, varredura, false, arcPaint)
            anguloAtual += varredura
        }
    }
}
