package com.guilherme.anotaplus

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.android.billingclient.api.ProductDetails
import com.guilherme.anotaplus.billing.BillingManager
import com.guilherme.anotaplus.data.SubscriptionPrefs
import com.guilherme.anotaplus.databinding.ActivityPlansBinding
import kotlinx.coroutines.launch

class PlansActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPlansBinding
    private val billingManager by lazy { BillingManager(applicationContext) }
    private var productDetailsAtual: ProductDetails? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlansBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.btnAssinar.setOnClickListener { assinar() }

        atualizarUiPlano()
        carregarOferta()
    }

    override fun onResume() {
        super.onResume()
        // Cobre a volta do fluxo de compra (a tela da Play Store fecha e
        // devolve o controle aqui) e qualquer mudança feita em outro
        // aparelho da mesma conta. runCatching porque o Play Billing é uma
        // dependência externa instável nesse estágio (app ainda não é
        // instalado via Play Store) — uma falha aqui não pode derrubar a
        // tela inteira.
        lifecycleScope.launch {
            runCatching { billingManager.atualizarStatusAssinatura() }
            atualizarUiPlano()
        }
    }

    private fun carregarOferta() {
        lifecycleScope.launch {
            binding.btnAssinar.isEnabled = false
            productDetailsAtual = runCatching { billingManager.consultarProductDetails() }.getOrNull()
            atualizarUiPlano()
        }
    }

    private fun assinar() {
        val detalhes = productDetailsAtual
        if (detalhes == null) {
            Toast.makeText(this, R.string.assinatura_indisponivel, Toast.LENGTH_LONG).show()
            return
        }
        val iniciou = runCatching { billingManager.iniciarCompra(this, detalhes) }.getOrDefault(false)
        if (!iniciou) {
            Toast.makeText(this, R.string.assinatura_erro, Toast.LENGTH_LONG).show()
        }
    }

    private fun atualizarUiPlano() {
        val pro = SubscriptionPrefs.isPro(this)
        binding.btnAssinar.isEnabled = !pro && productDetailsAtual != null
        binding.btnAssinar.text = getString(
            if (pro) R.string.assinatura_ativa else R.string.btn_assinar_pro
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        runCatching { billingManager.encerrar() }
    }
}
