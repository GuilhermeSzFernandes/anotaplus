package com.guilherme.anotaplus

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import com.guilherme.anotaplus.data.AppDatabase
import com.guilherme.anotaplus.data.Meta
import com.guilherme.anotaplus.databinding.ActivityMetaOnboardingBinding
import kotlinx.coroutines.launch
import java.util.Calendar

// Quarta tela do onboarding: cria a primeira Meta nomeada (não a "meta de
// economia mensal" única guardada em Prefs, que é outra coisa) com um
// prazo opcional. Pulável — nesse caso nenhuma Meta é criada e o
// Acompanhamento segue mostrando o estado vazio de Metas normal. Repassa
// EXTRA_ONBOARDING/EXTRA_ABRIR_HISTORICO adiante pra
// QuickAccessChooserActivity, seguindo o mesmo padrão de QuickAccessFlow.
class MetaOnboardingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMetaOnboardingBinding
    private var prazoSelecionado: Long? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMetaOnboardingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.onboardingHeader.bind(
            activity = this,
            progresso = 0.60f,
            pergunta = getString(R.string.titulo_meta_onboarding)
        )

        fun chips() = listOf(binding.chipPrazo6Meses, binding.chipPrazo1Ano, binding.chipPrazoEscolher)

        fun atualizarPreview() {
            val valorAlvo = binding.editValorAlvoMeta.text.toString().replace(",", ".").toDoubleOrNull()
            val valorMensal = if (valorAlvo != null) {
                MetaCalculo.valorMensalNecessario(valorAlvo, 0.0, prazoSelecionado, System.currentTimeMillis())
            } else {
                null
            }
            if (valorMensal != null) {
                binding.textPreviewValorMensal.text = getString(
                    R.string.formato_valor_mensal_meta,
                    "R$ %.2f".format(MesUtil.locale, valorMensal)
                )
                binding.textPreviewValorMensal.visibility = View.VISIBLE
            } else {
                binding.textPreviewValorMensal.visibility = View.GONE
            }
        }

        fun selecionar(chipSelecionado: View) {
            chips().forEach { it.isSelected = it == chipSelecionado }
        }

        binding.chipPrazo6Meses.setOnClickListener {
            prazoSelecionado = MetaCalculo.prazoEmMeses(System.currentTimeMillis(), 6)
            selecionar(binding.chipPrazo6Meses)
            atualizarPreview()
        }
        binding.chipPrazo1Ano.setOnClickListener {
            prazoSelecionado = MetaCalculo.prazoEmMeses(System.currentTimeMillis(), 12)
            selecionar(binding.chipPrazo1Ano)
            atualizarPreview()
        }
        binding.chipPrazoEscolher.setOnClickListener {
            val agora = Calendar.getInstance()
            DatePickerDialog(
                this,
                { _, ano, mes, dia ->
                    val escolhido = Calendar.getInstance().apply { set(ano, mes, dia, 0, 0, 0) }
                    prazoSelecionado = escolhido.timeInMillis
                    selecionar(binding.chipPrazoEscolher)
                    atualizarPreview()
                },
                agora.get(Calendar.YEAR),
                agora.get(Calendar.MONTH),
                agora.get(Calendar.DAY_OF_MONTH)
            ).show()
        }
        binding.editValorAlvoMeta.addTextChangedListener(afterTextChanged = { atualizarPreview() })

        binding.btnCriarMeta.setOnClickListener {
            val nome = binding.editNomeMeta.text.toString().trim()
            val valorAlvo = binding.editValorAlvoMeta.text.toString().replace(",", ".").toDoubleOrNull()
            if (nome.isEmpty() || valorAlvo == null || valorAlvo <= 0) {
                binding.editNomeMeta.error = if (nome.isEmpty()) getString(R.string.error_valor_obrigatorio) else null
                binding.editValorAlvoMeta.error = if (valorAlvo == null || valorAlvo <= 0) getString(R.string.error_valor_obrigatorio) else null
                return@setOnClickListener
            }
            lifecycleScope.launch {
                AppDatabase.getInstance(applicationContext).metaDao()
                    .insert(Meta(nome = nome, valorAlvo = valorAlvo, prazo = prazoSelecionado))
                concluir()
            }
        }
        binding.btnPularMeta.setOnClickListener { concluir() }
    }

    private fun concluir() {
        startActivity(
            Intent(this, QuickAccessChooserActivity::class.java)
                .putExtra(QuickAccessFlow.EXTRA_ONBOARDING, intent.getBooleanExtra(QuickAccessFlow.EXTRA_ONBOARDING, false))
                .putExtra(QuickAccessFlow.EXTRA_ABRIR_HISTORICO, intent.getBooleanExtra(QuickAccessFlow.EXTRA_ABRIR_HISTORICO, false))
        )
        finish()
    }
}
