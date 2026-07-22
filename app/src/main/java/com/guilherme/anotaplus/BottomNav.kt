package com.guilherme.anotaplus

import android.content.Intent
import android.content.res.ColorStateList
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.guilherme.anotaplus.databinding.BottomNavBarBinding

enum class NavTab { INICIO, RELATORIO, CONFIGURACOES }

// Barra inferior fixa das 3 telas-raiz (Início/Relatório/Configurações).
// Troca de aba nunca empilha Activity: sempre finish() na atual antes de
// abrir a nova, senão apertar voltar depois de circular pelas 3 abas ia
// empilhando instâncias repetidas em vez de sair do app.
fun AppCompatActivity.configurarBottomNav(binding: BottomNavBarBinding, atual: NavTab) {
    fun aplicarEstado(icon: android.widget.ImageView, label: android.widget.TextView, ativo: Boolean) {
        val cor = ContextCompat.getColor(this, if (ativo) R.color.brass else R.color.paper)
        icon.imageTintList = ColorStateList.valueOf(cor)
        label.setTextColor(cor)
    }

    aplicarEstado(binding.iconInicio, binding.labelInicio, atual == NavTab.INICIO)
    aplicarEstado(binding.iconRelatorio, binding.labelRelatorio, atual == NavTab.RELATORIO)
    aplicarEstado(binding.iconConfiguracoes, binding.labelConfiguracoes, atual == NavTab.CONFIGURACOES)

    binding.navInicio.setOnClickListener { trocarTab(atual, NavTab.INICIO, HistoryActivity::class.java) }
    binding.navRelatorio.setOnClickListener { trocarTab(atual, NavTab.RELATORIO, ReportActivity::class.java) }
    binding.navConfiguracoes.setOnClickListener { trocarTab(atual, NavTab.CONFIGURACOES, SettingsActivity::class.java) }
}

private fun AppCompatActivity.trocarTab(atual: NavTab, alvo: NavTab, destino: Class<*>) {
    if (atual == alvo) return
    startActivity(Intent(this, destino))
    @Suppress("DEPRECATION")
    overridePendingTransition(0, 0)
    finish()
}
