package com.guilherme.anotaplus

import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding

// Conteúdo passa a desenhar por trás da status bar/nav bar (sem faixa de
// toolbar opaca comendo espaço no topo) — o header recebe o inset da status
// bar como paddingTop extra, e a barra inferior flutuante recebe o inset da
// nav bar como marginBottom extra, cada um somado ao valor já declarado no
// XML (senão em aparelho com nav bar por gesto o conteúdo fica embaixo dela).
fun AppCompatActivity.aplicarEdgeToEdge(root: View, header: View, bottomNav: View) {
    WindowCompat.setDecorFitsSystemWindows(window, false)
    val headerPaddingBase = header.paddingTop
    val navMarginBase = (bottomNav.layoutParams as ViewGroup.MarginLayoutParams).bottomMargin
    ViewCompat.setOnApplyWindowInsetsListener(root) { _, insets ->
        val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
        header.updatePadding(top = headerPaddingBase + bars.top)
        bottomNav.updateLayoutParams<ViewGroup.MarginLayoutParams> {
            bottomMargin = navMarginBase + bars.bottom
        }
        insets
    }
}
