package com.guilherme.anotaplus.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View
import com.guilherme.anotaplus.R
import kotlin.math.ceil

/**
 * Fileira de dentes triangulares, como a perfuração de destaque de um recibo.
 * Usada no topo do card de captura (parece que o slip foi arrancado de uma
 * bobina) e como divisor entre itens do histórico.
 */
class PerforationView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()
    private var toothWidthPx: Float
    private var toothHeightPx: Float
    private var toothGapPx: Float

    init {
        val density = resources.displayMetrics.density
        var color = Color.parseColor("#6E6759") // ink_soft, used if no toothColor attr set
        var width = 14f * density
        var height = 8f * density
        var gap = 5f * density

        attrs?.let {
            val a = context.obtainStyledAttributes(it, R.styleable.PerforationView)
            width = a.getDimension(R.styleable.PerforationView_toothWidth, width)
            height = a.getDimension(R.styleable.PerforationView_toothHeight, height)
            gap = a.getDimension(R.styleable.PerforationView_toothGap, gap)
            color = a.getColor(R.styleable.PerforationView_toothColor, color)
            a.recycle()
        }

        toothWidthPx = width
        toothHeightPx = height
        toothGapPx = gap
        paint.color = color
        paint.style = Paint.Style.FILL
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = toothHeightPx.coerceAtMost(height.toFloat())
        val stride = toothWidthPx + toothGapPx
        if (stride <= 0f || w <= 0f) return

        val count = ceil(w / stride).toInt() + 1
        path.reset()
        for (i in 0 until count) {
            val startX = i * stride
            val apexX = startX + toothWidthPx / 2f
            val endX = startX + toothWidthPx
            path.moveTo(startX, 0f)
            path.lineTo(apexX, h)
            path.lineTo(endX, 0f)
            path.close()
        }
        canvas.drawPath(path, paint)
    }
}
