package com.guilherme.anotaplus

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity
import com.guilherme.anotaplus.databinding.ActivityGestureGuideBinding

class GestureGuideActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_ONBOARDING = "onboarding"
        const val EXTRA_ABRIR_HISTORICO = "abrir_historico"

        // Package do app "Moto" (gestos/ações) nos aparelhos Motorola. Não
        // existe um jeito público/oficial de linkar direto pra tela
        // específica do gesto — o nome e o caminho do menu variam por
        // versão do Android/Moto UI, por isso o botão só tenta abrir o app
        // e o guia escrito cobre o resto.
        private const val PACOTE_MOTO_ACTIONS = "com.motorola.actions"
    }

    private lateinit var binding: ActivityGestureGuideBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGestureGuideBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnAbrirConfigMoto.setOnClickListener { abrirConfiguracoesMoto() }
        binding.btnEntendi.setOnClickListener { continuar() }
    }

    private fun abrirConfiguracoesMoto() {
        val intentMoto = packageManager.getLaunchIntentForPackage(PACOTE_MOTO_ACTIONS)
        try {
            if (intentMoto != null) {
                startActivity(intentMoto)
            } else {
                startActivity(Intent(Settings.ACTION_SETTINGS))
            }
        } catch (e: ActivityNotFoundException) {
            startActivity(Intent(Settings.ACTION_SETTINGS))
        }
    }

    private fun continuar() {
        if (intent.getBooleanExtra(EXTRA_ONBOARDING, false)) {
            val abrirHistorico = intent.getBooleanExtra(EXTRA_ABRIR_HISTORICO, false)
            startActivity(
                Intent(this, LoginActivity::class.java)
                    .putExtra(EXTRA_ONBOARDING, true)
                    .putExtra(EXTRA_ABRIR_HISTORICO, abrirHistorico)
            )
        }
        finish()
    }
}
