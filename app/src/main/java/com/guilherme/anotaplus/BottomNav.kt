package com.guilherme.anotaplus

import android.content.Intent
import android.content.res.ColorStateList
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.guilherme.anotaplus.databinding.BottomNavBarBinding

enum class NavTab { INICIO, ANOTACOES, RELATORIO, CONTA }

// Barra inferior fixa das 4 telas-raiz (Início/Anotações/Relatório/Conta).
// Troca de aba nunca empilha Activity: sempre finish() na atual antes de
// abrir a nova, senão apertar voltar depois de circular pelas abas ia
// empilhando instâncias repetidas em vez de sair do app.
fun AppCompatActivity.configurarBottomNav(binding: BottomNavBarBinding, atual: NavTab) {
    fun aplicarEstado(pill: LinearLayout, icon: ImageView, label: TextView, ativo: Boolean) {
        pill.background = if (ativo) ContextCompat.getDrawable(this, R.drawable.bg_nav_pill_active) else null
        label.visibility = if (ativo) View.VISIBLE else View.GONE
        val cor = ContextCompat.getColor(this, if (ativo) R.color.brass else R.color.ink_soft)
        icon.imageTintList = ColorStateList.valueOf(cor)
        label.setTextColor(cor)
    }

    aplicarEstado(binding.pillInicio, binding.iconInicio, binding.labelInicio, atual == NavTab.INICIO)
    aplicarEstado(binding.pillAnotacoes, binding.iconAnotacoes, binding.labelAnotacoes, atual == NavTab.ANOTACOES)
    aplicarEstado(binding.pillRelatorio, binding.iconRelatorio, binding.labelRelatorio, atual == NavTab.RELATORIO)
    aplicarEstado(binding.pillConta, binding.iconConta, binding.labelConta, atual == NavTab.CONTA)

    binding.navInicio.setOnClickListener { trocarTab(atual, NavTab.INICIO, HistoryActivity::class.java) }
    binding.navAnotacoes.setOnClickListener { trocarTab(atual, NavTab.ANOTACOES, AnotacoesActivity::class.java) }
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
