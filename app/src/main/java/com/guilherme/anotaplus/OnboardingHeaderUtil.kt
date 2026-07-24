package com.guilherme.anotaplus

import android.app.Activity
import android.view.View
import com.guilherme.anotaplus.databinding.OnboardingHeaderBinding

// Liga o cabeçalho compartilhado de onboarding (voltar + barra de progresso
// + mascote + balão de pergunta), usado pelas 6 telas do onboarding (Login,
// Nome, Salário, Meta, Escolha de atalho, Guia de Gesto/Sugestão de Widget)
// — o tour de tutorial "spotlight" não usa este cabeçalho (mecanismo
// diferente, overlay sobre as telas reais).
//
// `progresso` é uma fração FIXA por tela (0f a 1f), não um cálculo
// dinâmico — a cauda do fluxo (Gestos/Widget) é opcional e varia de 0 a 2
// telas conforme o que o usuário escolher em QuickAccessChooserActivity,
// então não existe um "total de passos" estável pra calcular a fração de
// verdade. Cada Activity passa o valor combinado abaixo:
//   Login = 0.10f, Nome = 0.25f, Salário (as 3 sub-etapas) = 0.40f,
//   Meta = 0.60f, Escolha de atalho = 0.75f, Guia de Gesto/Sugestão de
//   Widget = 0.90f (mesmo valor pras duas — a ordem entre elas não importa).
fun OnboardingHeaderBinding.bind(
    activity: Activity,
    progresso: Float,
    pergunta: String,
    mascote: Int = R.drawable.ic_mascote_neutro,
    mostrarVoltar: Boolean = true
) {
    progressOnboarding.progress = (progresso * 100).toInt()
    textPerguntaOnboarding.text = pergunta
    imageMascoteOnboarding.setImageResource(mascote)
    btnVoltarOnboarding.visibility = if (mostrarVoltar) View.VISIBLE else View.INVISIBLE
    btnVoltarOnboarding.setOnClickListener { activity.onBackPressedDispatcher.onBackPressed() }
}
