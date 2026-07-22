package com.guilherme.anotaplus

import android.content.Intent
import android.content.res.ColorStateList
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.widget.ImageViewCompat
import com.guilherme.anotaplus.databinding.BottomNavBarBinding

enum class NavTab { ANOTACOES, FINANCEIRO, INICIO, RELATORIO, CONTA }

// Barra inferior fixa das 5 telas-raiz. Troca de aba nunca empilha
// Activity: sempre finish() na atual antes de abrir a nova, senão apertar
// voltar depois de circular pelas abas ia empilhando instâncias repetidas
// em vez de sair do app.
fun AppCompatActivity.configurarBottomNav(binding: BottomNavBarBinding, atual: NavTab) {
    fun aplicarEstado(pill: android.widget.LinearLayout, icon: android.widget.ImageView, ativo: Boolean) {
        pill.background = if (ativo) ContextCompat.getDrawable(this, R.drawable.bg_nav_pill_active) else null
        val cor = ContextCompat.getColor(this, if (ativo) R.color.paper else R.color.ink_soft)
        ImageViewCompat.setImageTintList(icon, ColorStateList.valueOf(cor))
    }

    aplicarEstado(binding.pillAnotacoes, binding.iconAnotacoes, atual == NavTab.ANOTACOES)
    aplicarEstado(binding.pillFinanceiro, binding.iconFinanceiro, atual == NavTab.FINANCEIRO)
    aplicarEstado(binding.pillInicio, binding.iconInicio, atual == NavTab.INICIO)
    aplicarEstado(binding.pillRelatorio, binding.iconRelatorio, atual == NavTab.RELATORIO)
    aplicarEstado(binding.pillConta, binding.iconConta, atual == NavTab.CONTA)

    binding.navAnotacoes.setOnClickListener { trocarTab(atual, NavTab.ANOTACOES, AnotacoesActivity::class.java) }
    binding.navFinanceiro.setOnClickListener { trocarTab(atual, NavTab.FINANCEIRO, FinanceiroActivity::class.java) }
    binding.navInicio.setOnClickListener { trocarTab(atual, NavTab.INICIO, HistoryActivity::class.java) }
    binding.navRelatorio.setOnClickListener { trocarTab(atual, NavTab.RELATORIO, ReportActivity::class.java) }
    binding.navConta.setOnClickListener { trocarTab(atual, NavTab.CONTA, SettingsActivity::class.java) }
}

private fun AppCompatActivity.trocarTab(atual: NavTab, alvo: NavTab, destino: Class<*>) {
    if (atual == alvo) return
    startActivity(Intent(this, destino))
    @Suppress("DEPRECATION")
    overridePendingTransition(0, 0)
    finish()
}
