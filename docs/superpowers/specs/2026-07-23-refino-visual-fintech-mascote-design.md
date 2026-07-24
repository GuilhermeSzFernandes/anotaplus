# Refino visual "fintech moderno" + mascote — design

> Sessão de brainstorming: 2026-07-23. Continuação do redesign "fintech
> moderno" (ver `PROJETO.md`, seção "Design system"), que introduziu cantos
> arredondados, chips de ícone preto e cards flat fora da Captura Rápida.
> Esta spec resolve inconsistências deixadas por aquele redesign e adiciona
> uma cor de destaque própria + um mascote.

## Motivação

O usuário identificou 3 problemas concretos usando o app hoje:

1. **Espaçamento/alinhamento inconsistente entre cards e seções.** Confirmado
   por auditoria de código: não existe uma escala de espaçamento (`dimens.xml`
   só tem raios de borda). O mesmo tipo de elemento usa padding diferente
   sem critério — 12dp/16dp/18dp/20dp/28dp pro que deveria ser o mesmo ritmo
   visual (exemplos com file:line no relatório da sessão: card-resumo do
   Financeiro em 18dp vs. card de Conta no Perfil em 16dp vs. onboarding em
   28dp).
2. **Cores sem graça.** O redesign "fintech" reaproveita os neutros antigos
   (`paper`/`ink`/`paper_dim`) com raios maiores — só 3 tokens são
   genuinamente novos (`color_receita`, `color_gasto_vivo`,
   `chart_teal_tint`, este último usado só no card de Insights). Não existe
   cor de marca/destaque própria do fintech.
3. **Telas de onboarding destoam do resto.** `LoginActivity`,
   `GestureGuideActivity` e `WidgetSuggestionActivity` ainda são 100% estilo
   "recibo" antigo (papel, perfuração, carimbo) — as únicas telas fora da
   Captura Rápida que não migraram. `QuickAccessChooserActivity` está num
   meio-termo (fundo papel + cards flat).

Durante o brainstorming surgiu uma quarta peça: um **mascote** pra dar
personalidade ao app e acompanhar a jornada de economia do usuário.

## Não-metas (explicitamente fora de escopo)

- **A Captura Rápida não muda.** `QuickCaptureActivity`,
  `ManualGastoActivity`, `ManualIdeiaActivity`, `ManualRecebimentoActivity`
  mantêm o visual "recibo" (papel/tinta/carimbo) — decisão já tomada no
  redesign anterior, reconfirmada aqui.
- **Cores semânticas de dinheiro não mudam.** `color_receita` (#1E8E3E) e
  `color_gasto_vivo` (#E5484D) continuam como estão.
- **Animação do mascote fica pra depois.** Esta spec cobre só os assets
  estáticos (parado/feliz/preocupado) e onde cada um aparece. Prompts de
  movimento/animação são um passo seguinte, separado.
- **Geração final da arte do mascote não é feita pelo Claude Code** — os
  assets de imagem são gerados externamente pelo usuário numa IA de imagem,
  a partir do prompt e da referência acordados aqui. Esta spec só define
  conceito, paleta e onde os assets entram no app.

## Decisões de design

### 1. Cor de destaque (accent) da UI

Nova cor primária de UI: **verde-claro**, tom sugerido `#4CAF6D` (ajustável
na implementação para bater com o miolo verde-claro do mascote quando a arte
final existir). Usada em:

- Botões primários (substituindo o que seria `Widget.AnotaPlus.Button.Primary`)
- Chips/pills ativos (segmentos, tabs de filtro)
- Barra de progresso da Meta de economia

Resto da UI continua **neutro** (`paper`/`paper_dim`/`ink` já existentes).
Outras cores (semânticas de dinheiro, dourado do mascote) aparecem só como
**detalhes pontuais**, nunca como cor de fundo de tela ou de botão primário.

**Por quê verde e não a opção inicial (índigo):** a entrada do mascote
"moeda de 1 Real" (dourado + verde) mudou a direção — o usuário preferiu
alinhar a cor de marca do app com a cor da moeda/mascote e com as cores do
Brasil, em vez de uma cor tech genérica sem relação com o produto.

**Risco a observar na implementação:** `#4CAF6D` precisa ficar visualmente
distinto de `color_receita` (#1E8E3E) quando aparecem na mesma tela (ex.:
Início mostra receita em texto E o accent verde no botão/progresso lado a
lado) — são tons diferentes o bastante (um mais claro/vívido, outro mais
escuro/oliva) mas vale conferir em tela real, não só no mockup.

### 2. Escala de espaçamento

Adicionar a `dimens.xml` (ao lado dos `radius_*` já existentes):

| Token | Valor | Uso |
|---|---|---|
| `spacing_xs` | 8dp | Gaps internos de chip/pill |
| `spacing_sm` | 12dp | Cards compactos de lista (nota, stat) |
| `spacing_md` | 16dp | Cards de conteúdo padrão (blocos do Perfil, card Conta) |
| `spacing_lg` | 20dp | Padding de tela e cards-resumo/hero |

Consolida os 5 valores hoje espalhados (12/16/18/20/28dp) nesses 4 tokens:
18dp sobe pra 20 (`spacing_lg`), 28dp do onboarding desce pra 20
(`spacing_lg`, já que essas telas migram pro fintech de qualquer forma —
ver item 3). Aplica-se a toda tela fora da Captura Rápida (que mantém seus
próprios raios/paddings "recibo", já hardcoded por design, sem token
compartilhado).

### 3. Migração das telas de onboarding pro visual fintech

`LoginActivity`, `GestureGuideActivity` e `WidgetSuggestionActivity` trocam
o estilo recibo (papel + `PerforationView` + `Widget.AnotaPlus.Button.Stamp`)
pelo visual fintech: card flat (`bg_card_flat`/`paper_dim`, `radius_card`),
`Widget.AnotaPlus.Button.Primary` com o novo accent verde,
`TextAppearance.AnotaPlus.Heading` pro wordmark. `QuickAccessChooserActivity`
(já híbrida) passa a usar consistentemente os mesmos tokens fintech das
outras três, fechando o grupo.

Direção visual acordada pro Login: fundo com **hero em gradiente** (verde
saindo do topo, suave, com formas geométricas soltas) e o card de ação
flutuando por cima com sombra — não um card flat "sem vida" sobre fundo
plano liso (testado e rejeitado nesta sessão).

### 4. Diálogos "Legal" com mais estrutura

`DialogUtil.criarDialogoFlat` / `dialog_confirmacao_flat.xml` (usado pelos
stubs de Política de Privacidade/Termos de Uso em `SettingsActivity`)
ganham um cabeçalho com ícone (chip circular na cor accent) + título, em vez
de só texto corrido dentro do card — resolve a sensação de "genérico" sem
trocar o mecanismo (continua um diálogo customizado, não
`AlertDialog` padrão).

### 5. Mascote: moeda de 1 Real

**Conceito:** um personagem 3D-render baseado literalmente na moeda de
1 Real — aro externo dourado, miolo circular com o rosto simples (dois
olhos, sorriso), sem pernas, dois braços curtos saindo das laterais. Ver
imagem de referência gerada pelo usuário em
`docs/superpowers/specs/assets/mascote-moeda-real.png` (miolo prateado na
referência atual; a versão final troca o miolo pra **verde-claro**, pra
casar com a nova cor de destaque e com as cores do Brasil/moeda/história,
em vez de prateado ou azul).

**Variantes de humor necessárias** (mesmo estilo/luz/ângulo/fundo entre
elas, pra manter consistência):
- **Neutro/parado** — uso padrão (onboarding, saudação default).
- **Feliz/comemorando** — braços erguidos, olhos em arco, brilhos ao redor.
- **Preocupado** — braços baixos, sobrancelha franzida, boca em linha reta.
  Mantém a cor âmbar/verde do personagem — não migra pra vermelho, pra não
  confundir com `color_gasto_vivo`.

**Onde aparece** (todas as opções que o usuário selecionou):
- Tela de Login/onboarding (hero, ver item 3).
- Saudação no Início/Acompanhamento (variante neutro, ou feliz se a meta
  do mês estiver indo bem).
- Celebração ao bater 100% de uma Meta de economia (variante feliz).
- Alerta de orçamento estourado por categoria (variante preocupado) — pode
  reaproveitar o fluxo existente de `BudgetAlertNotifier`.
- Empty states: "nenhuma nota ainda" (Anotações), "nenhum lançamento ainda"
  (Financeiro) — variante a definir na implementação (provavelmente
  neutro/convidativo).

**Fora de escopo aqui:** animação/movimento do mascote — assets estáticos
apenas nesta spec; prompts de animação são um próximo passo à parte.

## Riscos e pontos em aberto pra implementação

- O hex final do accent verde e do miolo do mascote devem ser calibrados
  juntos quando a arte real (com miolo verde) existir — o valor `#4CAF6D`
  aqui é um ponto de partida, não definitivo.
- Os assets do mascote (3 variantes, fundo transparente) precisam ser
  gerados pelo usuário e importados como drawables antes da implementação
  poder usá-los de verdade — a implementação pode seguir com um placeholder
  até lá.
- Qual variante de humor usar em cada empty state específico não foi
  decidido em detalhe — fica a critério de quem implementar, seguindo o
  espírito "neutro/convidativo, nunca preocupado" nesses casos (empty state
  não é uma situação negativa).
