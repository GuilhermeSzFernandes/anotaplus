# Refino visual "fintech moderno" + mascote — design

> Sessão de brainstorming: 2026-07-23. Continuação do redesign "fintech
> moderno" (ver `PROJETO.md`, seção "Design system"), que introduziu cantos
> arredondados, chips de ícone preto e cards flat fora da Captura Rápida.
> Esta spec resolve inconsistências deixadas por aquele redesign, adiciona
> uma cor de destaque própria + um mascote, expande o onboarding com 2
> telas novas (nome de exibição, primeira Meta com prazo) no padrão de um
> onboarding gamificado de referência, e realinha o primeiro bloco do
> Perfil.

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

### 1. Cor de destaque (accent) da UI — verde é detalhe, não preenchimento

Revisão importante desta sessão: depois de ver uma referência concreta de
onboarding gamificado/minimalista trazida pelo usuário, a regra final é
**verde só aparece em detalhes pontuais, nunca como cor de preenchimento de
botão ou tela**. Isso vale pro **app inteiro**, não só onboarding.

- **Botões primários**: usam `ink` (#1B1B1D, token já existente) como
  background, texto em `paper`. Isso substitui a ideia anterior de um botão
  cheio em verde — `Widget.AnotaPlus.Button.Primary` deve resolver pra
  `ink`, não pra um novo token de accent.
- **Verde-claro** (tom de partida `#4CAF6D`, ajustável junto da arte final
  do mascote) aparece só em: barra de progresso (fill), números/textos em
  destaque (ex. "R$ 166,67/mês", "72% da meta"), borda de chip/card
  selecionado ou "campeão" (ex. chip de prazo ativo, card de resultado em
  destaque), e no próprio mascote.
- Resto da UI continua **neutro** (`paper`/`paper_dim`/`ink`/`ink_soft` já
  existentes). Outras cores (semânticas de dinheiro — `color_receita`,
  `color_gasto_vivo`) continuam pontuais, exatamente como já eram.

**Por quê essa revisão:** a referência de onboarding usa exatamente esse
padrão (botão preto "Continuar"/"Pular", verde só na barra de progresso e
em números de destaque) e o usuário confirmou que prefere isso pro app
inteiro — mais alinhado com o pedido original de "verde-claro e neutras +
detalhes mínimos em outras cores" do que a versão anterior (que preenchia
botões inteiros de verde).

**Por quê verde e não a opção inicial (índigo):** a entrada do mascote
"moeda de 1 Real" (dourado + cinza, mirando um miolo verde-claro na versão
final) mudou a direção — o usuário preferiu alinhar a cor de marca do app
com a cor da moeda/mascote e com as cores do Brasil, em vez de uma cor tech
genérica sem relação com o produto.

**Risco a observar na implementação:** `#4CAF6D` precisa ficar visualmente
distinto de `color_receita` (#1E8E3E) quando aparecem na mesma tela — são
tons diferentes o bastante (um mais claro/vívido, outro mais escuro/oliva)
mas vale conferir em tela real, não só no mockup.

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

### 3. Onboarding: migração visual, 2 telas novas, e nova ordem completa

> Referência visual trazida pelo usuário (onboarding de outro app, "TapDin"):
> `docs/superpowers/specs/assets/onboarding-referencia/1.jpeg` a `12.jpeg`,
> em ordem de fluxo. É a fonte do padrão "mascote pequeno + balão de
> pergunta + botão preto" descrito abaixo — vale abrir pra ver o tom
> gamificado/minimalista que se está buscando (nosso mascote substitui o
> deles, o resto da estrutura é o que se aproveita).

**Padrão visual (revisado)**: não é mais o hero-degradê com card flutuante
testado no meio da sessão (aprovado numa rodada, depois **substituído**
quando o usuário trouxe uma referência real de onboarding gamificado). O
padrão final, usado em **todas** as telas de onboarding (exceto o tour de
tutorial "spotlight", que já usa outro mecanismo — overlay sobre as telas
reais):

- Fundo neutro liso (`paper`), sem gradiente.
- Header: seta-voltar + barra de progresso fina (`paper_dim` de base,
  fill em verde-claro) indicando o passo atual dentro do onboarding.
- **Mascote em tamanho pequeno** (~32-40dp, não hero), posicionado ao lado
  de um card de pergunta com **borda verde-clara** (não preenchido) —
  papel de "balão de fala" do mascote guiando a pergunta da tela.
- Inputs e cards de opção: fundo `paper`/branco, borda fina cinza
  (`paper_dim`/`ink_soft`), cantos `radius_card`/`radius_chip`.
- Chip/pill selecionado (ex. atalho de prazo escolhido): borda verde-clara
  em vez de cinza — verde aqui é indicador de seleção, não preenchimento.
- **Botão principal: `ink` preenchido** (não verde — ver item 1), pill
  (`radius_button`), label "Continuar" (ou o verbo específico da tela, ex.
  "Criar meta").
- Onde há opção de pular (ver Meta abaixo), "Pular por agora" é um botão
  secundário de largura total, não um link de texto pequeno.

**Telas migradas pro padrão** (mesmo mecanismo/estado, só o visual muda):
`LoginActivity` (troca perfuração/carimbo por esse padrão; o botão nativo
"Entrar com Google" segue as diretrizes de marca do Google, não vira um
pill `ink`), `GestureGuideActivity`, `WidgetSuggestionActivity`,
`QuickAccessChooserActivity` (já híbrida, passa a ser 100% consistente com
as outras).

**Telas novas** (ver ordem completa abaixo):

- **"Como quer ser chamado"** — um input de texto, pré-preenchido com o
  nome vindo do Google (`SessionPrefs`, hoje salvo mas sem getter/uso —
  precisa de um `getNome()` e de um novo campo pra guardar o apelido
  editado, ex. `Prefs.nome_exibicao`). Esse nome passa a ser usado nas
  saudações do mascote (ex. card do Início, ver item 5). Sem opção de
  pular — é rápido e barato de preencher.
- **"Qual sua primeira meta?"** — cria a primeira Meta nomeada (não a meta
  de economia mensal única, que continua existindo à parte). Campos: nome,
  valor alvo, e um **prazo opcional** (chips de atalho "6 meses"/"1 ano" +
  opção de escolher uma data específica), com uma prévia calculada
  ("≈ R$X/mês pra chegar lá" = valor alvo ÷ meses até o prazo, só quando
  prazo está preenchido). **Pode ser pulada** ("Pular por agora") — nesse
  caso nenhuma Meta é criada, e a tela Início segue mostrando o estado
  vazio de Metas normal.

**Mudança de modelo de dados**: `Meta.kt` ganha um campo `prazo: Long?`
(epoch millis, nullable) — migração Room 8→9. **Vale pra qualquer Meta**,
criada no onboarding ou depois pela aba Acompanhamento — mesmo formulário/
lógica de criação nos dois lugares, campo sempre opcional. A tela/fluxo de
criação de Meta que já existe em `ReportActivity` (fora do onboarding)
precisa ganhar o mesmo campo de prazo pra não ficar inconsistente com a
versão do onboarding.

**Ordem completa do onboarding** (com destaque do mascote em todas, exceto
o tour de tutorial):

1. `LoginActivity` (login Google)
2. **Como quer ser chamado** (novo)
3. `SalarioOnboardingActivity` (já existe — só migra visual, sem mudança
   de campos/lógica)
4. **Qual sua primeira meta?** (novo, com prazo opcional, pulável)
5. `QuickAccessChooserActivity` (já existe)
6. `GestureGuideActivity` / `WidgetSuggestionActivity` (já existem, conforme
   escolhido no passo 5)
7. Tour de tutorial "spotlight" (sem mascote)

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
- **Todas as 7 telas do onboarding, em destaque, exceto o tour de
  tutorial** (ver ordem completa e padrão visual no item 3) — tamanho
  pequeno, ao lado do balão de pergunta, não hero.
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

### 6. Perfil: realinhar o primeiro bloco (card Conta)

Auditoria encontrou a causa exata da queixa "primeiro bloco não alinha com
o resto da tela": `activity_settings.xml` usa `padding="16dp"` (todos os
lados) no card Conta, enquanto os outros 4 blocos (Progresso/
Personalização/Legal/Suporte) usam só `paddingHorizontal="12dp"`, com o
espaçamento vertical vindo de dentro de cada `item_settings_row`. Resultado:
o conteúdo do card Conta começa mais pra dentro (16dp) que o conteúdo dos
blocos abaixo dele (12dp) — desalinhamento horizontal visível ao rolar a
tela.

**Direção da correção**: os 4 blocos que usam 12dp sobem pra
`spacing_md`/16dp horizontal, em vez do card Conta descer pra 12dp — isso
já é consistente com a tabela de espaçamento do item 2, que classifica
"blocos do Perfil, card Conta" inteiros como `spacing_md`. Ou seja, a
correção fica nos **4 blocos existentes**, não no card Conta.

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
- A implementação precisa localizar exatamente onde `ReportActivity` cria
  uma Meta nova hoje (fora do onboarding) pra adicionar o mesmo campo de
  prazo opcional lá — não foi confirmado o file:line exato dessa tela/
  diálogo nesta sessão.
- `SessionPrefs` guarda `name` do Google mas não tem getter hoje — a
  implementação precisa adicionar esse getter (ou ler direto o campo) pra
  pré-preencher a tela "Como quer ser chamado", além do novo campo de
  apelido editável (`Prefs.nome_exibicao` ou equivalente).
