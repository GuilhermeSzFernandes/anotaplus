# Anota+ — Design Overview

> Documento voltado a design/UX-UI — descreve o produto do ponto de vista de
> experiência e identidade visual, não de arquitetura técnica (isso está em
> `PROJETO.md`). Última revisão: 2026-07-24.

## O que é

App Android nativo para registrar **gastos**, **recebimentos** e
**pensamentos rápidos**, com a fricção mínima possível entre "lembrei disso"
e "tá salvo". A peça central é a **Captura Rápida**: um modal translúcido
que aparece por cima da tela atual, disparado por qualquer um de vários
caminhos equivalentes — ícone, ladrilho de Configurações Rápidas, 6 widgets
de tela inicial, atalhos de long-press ou o gesto do fabricante — sem abrir
o app inteiro.

## Público e proposta de valor

Alguém que quer anotar um gasto ou uma ideia no exato momento em que ela
acontece, sem o atrito de abrir um app, navegar até uma tela e preencher um
formulário longo. Tudo o resto do app (relatórios, categorias, metas) existe
pra dar sentido aos dados capturados depois, não pra competir com a
velocidade da captura.

## Arquitetura de informação (4 abas + Captura Rápida)

| Aba | Conteúdo |
|---|---|
| **Anotações** | Grid de cards de ideias/notas, busca, seleção múltipla. Formatação rica tipo Markdown (títulos, listas, checklist, negrito/itálico) com um editor "vivo" — os marcadores viram formatação visual na hora, sem editor WYSIWYG de verdade. |
| **Financeiro** | Extrato combinando Gasto e Recebimento, card de saldo do mês + gráfico de linha, busca, gestão de categorias (nome, cor, ícone, limite mensal) e carteiras. |
| **Início** (Acompanhamento) | Card-resumo Receitas/Gastos/Poupança, barra de "meta de economia" (%), insights automáticos em texto, lista de Metas nomeadas com prazo opcional e progresso, grid de estatísticas (maior gasto, categoria mais usada, dia da semana com mais gasto). |
| **Perfil** | Conta/login Google + backup na nuvem, assinatura PRO, personalização (tipo padrão, objetivos, dados salariais, notificações), legal, suporte. |

Trocar de aba nunca empilha — sempre fecha a activity atual antes de abrir a
próxima.

## Identidade visual — duas linguagens coexistindo

Esse é provavelmente o ponto mais importante pra alinhar com um UX/UI: o app
tem **duas paletas visuais deliberadamente distintas**, cada uma com seu
papel.

### 1. "Recibo" / papel e tinta — só na Captura Rápida

A Captura Rápida (e as telas de edição manual de Gasto/Ideia/Recebimento)
mantêm a identidade original do app: papel dessaturado, cantos quase retos,
tipografia monospace pra valores, wordmarks caixa-alta, botões com stroke,
uma "perfuração" de recibo real como elemento decorativo. A ideia é que o
modal pareça literalmente um ticket sendo emitido.

**Tokens:** `paper` `#F6F6F3` · `paper_dim` `#EAEAE5` · `ink` `#1B1B1D` ·
`ink_soft` `#78787A` · `brass` (acento único) `#A8823A` · `gasto_color`
`#9C5B45` · `pensamento_color` `#4A5568`.

### 2. "Fintech moderno" — todo o resto do app

Cantos bem arredondados, cards flat sem stroke, tipografia bold sans-serif,
chips de ícone preto com ícone branco. **Botão primário é sempre preto/tinta
— nunca uma cor cheia** (essa foi uma decisão explícita desta sessão de
redesign, inspirada num onboarding gamificado de referência).

**Tokens novos:**
- `verde_destaque` `#4CAF6D` — cor de marca, usada **só em detalhe**: barra
  de progresso, números em destaque, borda de seleção, mascote. Nunca
  preenche botão ou fundo de tela.
- `color_receita` (verde, `#1E8E3E`) e `color_gasto_vivo` (vermelho,
  `#E5484D`) — semânticos, só pra texto/valores de dinheiro, não confundir
  com o `verde_destaque`.
- Escala de espaçamento: `spacing_xs` 8dp · `spacing_sm` 12dp · `spacing_md`
  16dp · `spacing_lg` 20dp.
- Raios: `radius_card` 20dp · `radius_chip` 14dp · `radius_button` 16dp ·
  `radius_pill` 100dp (pílula completa).

**Por que as duas coexistem:** decisão consciente de que a Captura Rápida é
uma experiência à parte (o "recibo" reforça a sensação de registro rápido e
tangível), enquanto o resto do app é onde a pessoa "gerencia" a vida
financeira — daí o tom mais sóbrio e estruturado do fintech moderno. Vale a
pena um UX/UI validar se essa dualidade ainda faz sentido pro usuário final
ou se lê como inconsistência.

## Mascote

Um personagem — moeda de 1 Real personificada (aro dourado, miolo que vai
virar verde-claro, rosto simples, dois braços curtos, sem pernas) — com 3
variantes de humor: **neutro** (padrão), **feliz** (comemorando, usado
quando uma Meta é batida ou o mês está indo bem) e **preocupado** (usado só
no alerta de orçamento estourado — mantém a paleta âmbar/verde, nunca vira
vermelho, pra não se confundir com a cor semântica de gasto).

Hoje só existem placeholders (vetores simples); a arte final (render 3D,
fundo transparente) ainda precisa ser gerada. **Animação/movimento é o
próximo passo**, ainda não especificado.

**Onde aparece:** em destaque (pequeno, ao lado de um balão de pergunta) em
todas as 7 telas do onboarding, exceto o tour de tutorial; na saudação da
aba Início; na celebração de Meta 100% batida; no ícone grande do alerta de
notificação de orçamento estourado.

## Onboarding (fluxo completo)

Sequência linear, uma pergunta por tela, com barra de progresso + mascote +
botão "voltar" que agora navega de verdade entre as telas:

1. **Login** — Google obrigatório, sem pular.
2. **Como quer ser chamado** — nome de exibição, sugerido a partir da conta
   Google.
3. **Dados salariais** — salário mensal, horas/dia, dias/semana (alimenta a
   calculadora "Vale a pena comprar?", que converte preço em horas de
   trabalho).
4. **Primeira Meta de economia** — nome, valor-alvo, prazo opcional (atalhos
   "6 meses"/"1 ano" ou escolher data), com preview de quanto precisa
   guardar por mês. Pulável.
5. **Escolha de atalho rápido** — gestos, notificação persistente, widget.
6. **Guia de gesto e/ou sugestão de widget** — conforme escolhido no passo
   anterior.
7. **Tour de tutorial "spotlight"** — escurece a tela real e recorta um
   buraco ao redor do elemento sendo explicado, 6 passos cobrindo
   Anotações/Financeiro/Perfil.

## Modelo de negócio

**Free**: uso completo, com banner de anúncio (Anotações e Financeiro) e um
intersticial ocasional (a cada 4 aberturas do Financeiro).
**PRO** (assinatura mensal via Google Play Billing): libera backup/restore
na nuvem, remove anúncios, sincroniza limite de categoria com a nuvem.

## Restrições de plataforma que afetam design

- **Widgets de tela inicial** (6 no total) usam `RemoteViews`, que não
  aceita campo de texto de verdade nem resolve temas Material3 — todo texto/
  cor precisa ser literal, sem estados de foco/edição reais. Um dos widgets
  é um "espelho" visual do modal de captura, mas nenhum campo dele funciona
  de verdade (qualquer toque abre a Captura Rápida real).
- **Notificações**: o ícone pequeno da barra de status precisa ser
  monocromático/silhueta (exigência do Android); o mascote colorido só
  aparece no ícone grande da notificação expandida.

## Perguntas em aberto pro UX/UI

- A dualidade "recibo vs. fintech" ainda serve à experiência, ou confunde
  quem transita entre a Captura Rápida e o resto do app?
- O onboarding tem 7 telas — vale testar se algum passo (dados salariais,
  primeira Meta) pode virar totalmente opcional/adiado sem perder adesão?
- Como comunicar visualmente as limitações dos widgets (campos que não
  funcionam) sem parecer bug?
- As telas "Legal" (Privacidade/Termos) ainda são texto stub — quando
  virarem conteúdo real, como estruturar visualmente um texto jurídico longo
  dentro do design system atual?
- Há alguma sobreposição de números mostrados entre a aba Início e a aba
  Financeiro — vale repensar essa hierarquia?
- Qual personalidade exata o mascote deve comunicar quando a arte final for
  produzida (tom de voz, tipo de movimento na futura animação)?
