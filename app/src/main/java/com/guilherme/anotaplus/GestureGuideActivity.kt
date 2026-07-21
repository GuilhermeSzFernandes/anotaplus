package com.guilherme.anotaplus

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Build
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
        // específica do gesto em qualquer fabricante — o nome e o caminho
        // do menu variam demais (Moto, Samsung, Xiaomi...). Por isso só
        // temos um atalho direto conhecido pro caso Moto; pros demais o
        // guia escrito + Configurações genéricas é o melhor que dá pra
        // oferecer sem chutar um package/intent que não existe.
        private const val PACOTE_MOTO_ACTIONS = "com.motorola.actions"
    }

    private lateinit var binding: ActivityGestureGuideBinding
    private val isMotorola = Build.MANUFACTURER.equals("motorola", ignoreCase = true)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGestureGuideBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (isMotorola) {
            binding.textIntro.text = getString(R.string.guia_gesto_intro_moto)
            binding.textPasso1.text = getString(R.string.guia_gesto_passo_1_moto)
            binding.textPasso2.text = getString(R.string.guia_gesto_passo_2_moto)
            binding.textPasso3.text = getString(R.string.guia_gesto_passo_3_moto)
            binding.textPasso4.text = getString(R.string.guia_gesto_passo_4_moto)
            binding.btnAbrirConfigMoto.text = getString(R.string.btn_abrir_config_moto)
        } else {
            binding.textIntro.text = getString(R.string.guia_gesto_intro_generico)
            binding.textPasso1.text = getString(R.string.guia_gesto_passo_1_generico)
            binding.textPasso2.text = getString(R.string.guia_gesto_passo_2_generico)
            binding.textPasso3.text = getString(R.string.guia_gesto_passo_3_generico)
            binding.textPasso4.text = getString(R.string.guia_gesto_passo_4_generico)
            // Configurações Rápidas se edita puxando a barra e tocando no
            // lápis — não existe Intent público pra abrir essa tela direto
            // (diferente de Settings.ACTION_SETTINGS), então não tem pra
            // onde levar o usuário com um botão aqui.
            binding.btnAbrirConfigMoto.visibility = android.view.View.GONE
        }

        binding.btnAbrirConfigMoto.setOnClickListener { abrirConfiguracoesGesto() }
        binding.btnEntendi.setOnClickListener { continuar() }
    }

    private fun abrirConfiguracoesGesto() {
        if (isMotorola) {
            val intentMoto = packageManager.getLaunchIntentForPackage(PACOTE_MOTO_ACTIONS)
            if (intentMoto != null) {
                try {
                    startActivity(intentMoto)
                    return
                } catch (e: ActivityNotFoundException) {
                    // cai pro fallback genérico abaixo
                }
            }
        }
        try {
            startActivity(Intent(Settings.ACTION_SETTINGS))
        } catch (e: ActivityNotFoundException) {
            // Configurações sempre existe em qualquer aparelho Android real;
            // só protege contra o caso extremo de não resolver mesmo assim.
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
