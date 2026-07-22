package com.guilherme.anotaplus.data

import android.graphics.Color

/**
 * Paleta fixa de cores/ícones pra categorias — hex cru (não resource id),
 * de propósito: funciona igual dentro do app e dentro de RemoteViews
 * (CategoriasWidgetUpdater), que não tem como resolver um @color por nome.
 * As 6 cores são as mesmas já usadas no resto do app (colors.xml), só
 * copiadas aqui como literal pra não precisar de Context/Resources.
 */
object CategoriaEstilo {

    val CORES = listOf(
        "#A8823A", // brass
        "#9C5B45", // gasto_color
        "#4A5568", // pensamento_color
        "#4C6B3E", // color_positivo
        "#4A7C74", // chart_teal
        "#B5766B"  // chart_dusty_rose
    )

    val ICONES = listOf(
        "🛒", "🚗", "🍔", "🎮", "🏠", "💡",
        "📄", "💊", "✈️", "🎓", "🐶", "🎁"
    )

    fun corInt(hex: String?): Int =
        runCatching { Color.parseColor(hex) }.getOrDefault(Color.parseColor(CORES[0]))
}
