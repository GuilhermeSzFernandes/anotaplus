# Anota+ — contexto do projeto

> Última revisão completa: 2026-07-23. Este documento é reescrito por inteiro
> de tempo em tempo (não só apendado) pra não acumular seções obsoletas —
> se algo aqui contradiz o código, o código manda; abra uma issue mental e
> corrija este arquivo na próxima sessão que mexer na área afetada.

## O que é

App Android nativo (Kotlin) para registrar **gastos**, **recebimentos** e
**pensamentos rápidos**, com a fricção mínima possível entre "lembrei disso"
e "tá salvo". A peça central continua sendo a **Captura Rápida**: um
modal/card translúcido que aparece por cima da tela atual — como um diálogo
rápido — sem abrir o app inteiro.

O que mudou desde a concepção original: não existe mais **um único** jeito
de abrir esse modal (o gesto exclusivo do Moto Actions). Hoje há vários
caminhos equivalentes — toque no ícone, ladrilho de Configurações Rápidas,
6 widgets de tela inicial, atalhos de ícone (long-press) — e o app cresceu
de "só captura + histórico" pra 4 áreas completas (Anotações, Financeiro,
Acompanhamento, Perfil), com login/backup na nuvem e um plano PRO pago.

## Stack

- Kotlin nativo, Android SDK (minSdk 26, compileSdk/targetSdk 34)
- View Binding (sem Compose)
- Room (SQLite local, versão **8**) — dado local é a fonte de verdade; a
  nuvem é login + backup/restore (push e pull), **não** sincronização
  simultânea entre aparelhos
- Retrofit + OkHttp + Gson pra falar com o backend próprio
- Google Play Billing (assinatura PRO) e Google Mobile Ads (anúncios,
  plano Free)
- **Sem Gradle Wrapper commitado** (nem o jar) — ver seção "Build sem
  Android Studio", é a peça mais importante do fluxo de trabalho deste
  repositório

## Arquitetura de navegação (4 abas)

`BottomNav.kt` define `enum class NavTab { ANOTACOES, FINANCEIRO, INICIO, CONTA }`,
mapeado assim:

| Aba | Activity | Título exibido |
|---|---|---|
| Anotações | `AnotacoesActivity` | "Anotações" |
| Financeiro | `FinanceiroActivity` | "Financeiro" |
| Início | `ReportActivity` | "Acompanhamento" |
| Conta | `SettingsActivity` | "Perfil" |

`NavTab.INICIO` aponta pra `ReportActivity` porque o antigo "Relatório"
separado virou a própria aba Início — não existe mais uma tela de
dashboard distinta. Trocar de aba nunca empilha: sempre `finish()` na
activity atual antes de abrir a nova (sem essa disciplina, apertar voltar
depois de circular pelas abas empilharia instâncias repetidas em vez de
sair do app).

Nenhuma das 4 activities-raiz é `exported` — só `QuickCaptureActivity`
(única `MAIN`/`LAUNCHER`) e `AnotacaoWidgetConfigActivity` (respondendo a
`APPWIDGET_CONFIGURE`) são.

## Como o "modal" da Captura Rápida funciona (parte mais importante da arquitetura)

- `QuickCaptureActivity` é a **única Activity `MAIN`/`LAUNCHER`** do app —
  ícone, ladrilho de Configurações Rápidas, os 6 widgets e os atalhos de
  ícone disparam todos o mesmo intent MAIN/LAUNCHER, então todos caem aqui.
- Pra diferenciar "abri pelo ícone" de "abri por qualquer outro caminho",
  o `onCreate` olha `intent.sourceBounds`: toque no ícone da tela inicial
  sempre chega com `sourceBounds` preenchido (posição do ícone, usada na
  animação do launcher); nenhum dos outros caminhos (tile, widgets,
  atalhos) passa por um toque na tela, então nunca têm `sourceBounds` —
  todos constroem o `Intent` deliberadamente sem esse campo pra caírem no
  mesmo caminho. É uma heurística (não existe um jeito oficial de
  diferenciar origens de um mesmo `MAIN`/`LAUNCHER`), mas é estável.
- Ordem real no `onCreate`:
  1. `CrashHandler.pegarUltimoCrash()` — se o app crashou na última
     execução, mostra um diálogo com a stacktrace copiável antes de
     seguir (facilita reportar bug sem `adb logcat`).
  2. Se `!Prefs.isOnboardingConcluido()` → abre `LoginActivity` com os
     extras de onboarding (ver seção própria) e `finish()`. Esse é o
     único ponto de entrada do fluxo de primeira abertura.
  3. Senão, se veio de toque no ícone (`sourceBounds != null`) → abre
     `ReportActivity` (aba Início) direto e `finish()`.
  4. Senão → mostra o modal de captura de verdade: `setShowWhenLocked` +
     `setTurnScreenOn` (aparece por cima da tela de bloqueio sem exigir
     desbloqueio, igual o atalho da câmera), toggle Gasto/Recebimento/
     Pensamento (abre no tipo padrão de `Prefs.getTipoPadrao()`, a menos
     que o extra `EXTRA_TIPO_FORCADO` tenha vindo de um widget), campo de
     valor + categoria/carteira (combobox real via `AutoCompleteTextView`
     com `inputType="none"`, só escolhe o que já existe cadastrado), botão
     salvar, e um botão "ir ao app" que abre `AnotacoesActivity` ou
     `FinanceiroActivity` dependendo do tipo salvo.
- `Theme.AnotaPlus.Modal` (`res/values/themes.xml`): `windowIsTranslucent`,
  sem título, fundo transparente, sem animação de tela cheia.
- No manifest: `android:noHistory="true"` + `android:excludeFromRecents="true"`
  (não fica "presa" nos recentes, fecha instantâneo) + `android:taskAffinity=""`
  (isolada numa task própria) + `launchMode` padrão (**nunca** `singleTask`
  — já tentamos, quebrava a ilusão de modal trazendo a task antiga de volta
  em vez de empilhar uma instância nova translúcida por cima do app atual).
  Sem `taskAffinity=""`, se o usuário já tivesse navegado pra
  Financeiro/Perfil/etc. e só apertado Home, o próximo toque no ícone
  reaproveitava aquela task em segundo plano em vez de criar uma instância
  nova — o modal simplesmente não aparecia.
- `AnotaPlusApplication.kt` usa `ProcessLifecycleOwner` (diferencia "uma
  activity específica parou" de "o app inteiro saiu de primeiro plano") pra
  chamar `finishAndRemoveTask()` na activity em primeiro plano assim que o
  app inteiro vai pro background — garante que nunca sobra task viva
  quando o app não está em uso, reforçando a garantia acima independente
  do mecanismo exato que cada atalho usa pra "resgatar" o app.
  **Contrapartida assumida**: qualquer tela do app perde estado de
  navegação (posição de rolagem etc.) se a tela apagar sozinha ou o
  usuário for pra outro app — recarrega do zero na próxima abertura. Como
  todas as telas recarregam do Room local instantaneamente (sem chamada de
  rede), o custo é baixo perto da garantia de que o modal sempre funciona.
- Tocar fora do card (scrim escurecido) ou salvar fecha a activity
  (`finish()`).

## Onboarding (primeira abertura) e tour de tutorial

Controlado inteiramente por `Prefs.isOnboardingConcluido()` — acontece uma
única vez na vida do app instalado.

**Sequência real**: `QuickCaptureActivity` (ícone ou qualquer atalho, o que
abrir primeiro) → `LoginActivity` (login Google obrigatório, sem opção de
pular) → **`Prefs.marcarOnboardingConcluido()` é chamado aqui, logo após o
login bem-sucedido** (não no fim de toda a sequência) → `QuickAccessChooserActivity`
(escolher atalhos, ver seção seguinte) → zero, uma ou duas telas em
sequência (`GestureGuideActivity`/`WidgetSuggestionActivity`, conforme
escolhido) → `AnotacoesActivity`, que dispara o **tour de tutorial
"spotlight"** → ao concluir o tour, abre `ReportActivity` (se a abertura
original veio do ícone) ou `QuickCaptureActivity` (se veio de outro
atalho) como destino final.

Fora do onboarding, cada uma dessas telas (`GestureGuideActivity`,
`QuickAccessChooserActivity`, o tour) também pode ser reaberta isolada a
partir do Perfil — nesse caso só fecha ao concluir, sem redirecionar.

### Tour "spotlight" (`TutorialTourManager.kt` + `ui/CoachMarkOverlay.kt`)

Substituiu um tutorial antigo de páginas ilustradas separadas. Em vez
disso, escurece a tela **real** do app e recorta um buraco (via
`PorterDuff.CLEAR` + `LAYER_TYPE_SOFTWARE`) ao redor do botão de verdade
sendo explicado, com um anel cor de latão (`brass`) contornando o recorte
e um tooltip (título/corpo/indicadores/voltar-avançar-pular) posicionado
acima ou abaixo conforme a metade da tela.

- 6 passos fixos, cobrindo `TelaTutorial { ANOTACOES, FINANCEIRO, CONTA }`
  (Início não entra no tour).
- Navega entre telas fazendo `startActivity` + `finish()` normalmente; cada
  `onCreate` (`AnotacoesActivity`/`FinanceiroActivity`/`SettingsActivity`)
  chama `TutorialTourManager.processar(this, tela)`, que só age se houver
  um tour ativo esperando por aquela tela.
- **Estado 100% em memória** (não sobrevive a processo morto) — deliberado:
  "é um passeio guiado efêmero, não precisa disso pra ser simples". Se o
  processo for morto no meio, o tour só some silenciosamente.

## Mecanismos de acesso rápido (sem gesto de acelerômetro — removido)

Uma feature de gesto por acelerômetro (agitar o celular) chegou a ser
implementada, com tela de calibração de sensibilidade inclusive, e foi
**totalmente revertida** — não sobra nenhum código de `SensorManager`/
`Accelerometer` no projeto hoje. O que existe agora, todo universal
(nenhum depende de fabricante específico, exceto o primeiro):

1. **Toque no ícone** — abre direto o Início (`ReportActivity`).
2. **Gesto do fabricante** (`GestureGuideActivity`) — hoje é **só uma tela
   instrutiva**, não uma implementação: explica passo a passo como
   configurar o gesto do próprio sistema/fabricante. No Moto, tenta abrir
   `com.motorola.actions` direto; em qualquer outro aparelho, cai pro
   `Settings.ACTION_SETTINGS` genérico e sugere o ladrilho de
   Configurações Rápidas como alternativa garantida (testamos a hipótese
   de Xiaomi/HyperOS ter algo parecido — não há confirmação de que libere
   escolher um app de terceiros, então não vale a pena instruir por lá).
3. **Ladrilho de Configurações Rápidas** (`CapturaRapidaTileService`) —
   funciona em qualquer Android, puxando a barra de notificação e editando
   os ladrilhos. Suporta tela bloqueada. A partir do Android 14,
   `startActivityAndCollapse(Intent)` foi removido; o código detecta a
   versão e usa a variante com `PendingIntent`.
4. **Atalhos de ícone (long-press)** — `@xml/shortcuts`, meta-data direto
   em `QuickCaptureActivity`.
5. **6 widgets de tela inicial** (ver seção própria abaixo).

Cogitamos `AccessibilityService` como atalho (funcionaria até com tela
bloqueada) e descartamos de propósito: usar a API de Acessibilidade só
como atalho de abrir app, sem função de acessibilidade real, é contra a
política da Play Store.

### Onboarding de escolha de atalho (`QuickAccessChooserActivity` + `QuickAccessFlow.kt`)

Tela com três toggles independentes:

- **Gestos** — não tem estado próprio pra restaurar (sempre volta
  desmarcado ao reabrir); só enfileira `GestureGuideActivity` como próximo
  passo se marcado.
- **Notificação** — aplicado imediatamente ao confirmar: pede permissão
  `POST_NOTIFICATIONS` (Android 13+) se precisar, liga/desliga
  `Prefs.isNotificacaoCapturaAtiva` (padrão `true`) e a notificação
  persistente (`NotificationQuickAdd`).
- **Widget** — enfileira `WidgetSuggestionActivity`, que chama
  `AppWidgetManager.requestPinAppWidget` (Android 8+, depende do launcher
  suportar) pra fixar o widget "Captura Rápida", com instrução manual de
  fallback se não suportar.

`QuickAccessFlow` encadeia as telas escolhidas (`Gestos`/`Widget`, nessa
ordem, viajando como `String[]` no Intent pra sobreviver entre as
activities intermediárias) e no fim aciona o tour de tutorial + abre
`AnotacoesActivity` (se dentro do onboarding) ou só fecha (se reaberto
isolado do Perfil).

## Widgets de tela inicial (6 no total)

Nenhum widget observa o Room reativamente — todos são "puxe sob demanda":
uma função central redesenha depois de qualquer mudança relevante, porque
`AppWidgetProvider` só existe pra reagir a broadcasts do sistema, não pra
ficar vivo escutando `Flow`. `RemoteViews` (mecanismo por trás de
widgets) só infla um conjunto fechado de views nativas — nada de
`PerforationView` (Canvas customizado) nem de estilos que resolvam atributos
de tema `Material3` (o launcher/widget host infla com o próprio tema dele,
não o do app — resolver um `@style` que dependa de `Theme.Material3` falha
silenciosamente como "não foi possível carregar o widget"). Por isso todo
texto/cor nos layouts de widget é literal, e divisores usam
`@drawable/divider_dashed` em vez da `PerforationView` real.

1. **`GastoWidgetProvider`** — total gasto hoje e no mês. Corpo → abre
   Financeiro; "+" → Captura Rápida sem tipo forçado.
2. **`CapturaRapidaWidgetProvider`** — 1x1, só um "+", sem dado nenhum.
   Atalho puro pra quem não quer o widget de totais na tela.
3. **`CapturaModalWidgetProvider`** — "espelho" visual estático do modal
   (toggle Gasto/Anotação, campo de valor, campo de texto, botão Salvar) —
   **limitação de plataforma, não nossa**: `RemoteViews` não suporta
   `EditText` de verdade, então nenhum campo funciona; qualquer toque abre
   a Captura Rápida de verdade pra digitar. Tocar especificamente num dos
   toggles já abre com aquele tipo pré-selecionado
   (`EXTRA_TIPO_FORCADO`).
4. **`AnotacaoWidgetProvider`** — mostra uma Ideia/checklist específica,
   até 6 linhas fixas (não é `RemoteViewsService`/coleção — trade-off
   deliberado de simplicidade/robustez, dado que notas de uso real tendem
   a ser curtas). Único widget com tela de configuração
   (`AnotacaoWidgetConfigActivity`, aberta automaticamente pelo sistema ao
   adicionar uma instância, lista as Ideias existentes reativamente).
   Tocar num "☐"/"☑" alterna o item via `AnotacaoWidgetToggleReceiver`
   (broadcast dedicado) sem abrir o app; tocar no resto abre
   `ManualIdeiaActivity`.
5. **`CategoriasWidgetProvider`** — gasto do mês por categoria, maior pra
   menor, até 6 linhas fixas (mesmo trade-off do widget de Ideia). Sem
   tela de configuração (mesma lista pra qualquer instância). Corpo →
   Financeiro; "+" → Captura Rápida com tipo Gasto forçado.
6. **`AdicionarWidgetProvider`** — widget estreito e alto (1x2 células)
   com dois alvos de toque empilhados: metade de cima abre Captura Rápida
   com tipo Gasto forçado, metade de baixo com tipo Pensamento forçado —
   um atalho duplo sem a "casca" de modal falso do widget 3.

Dois coordenadores centrais: `WidgetUpdater.atualizarTodos` (cobre o
widget de totais **e**, como efeito colateral deliberado, o de categorias
— todo chamador já representa "algo sobre gasto mudou", então cobre os
dois sem precisar editar cada call site) e `AnotacaoWidgetUpdater`
(separado, só pro widget de Ideia/checklist).

## Estrutura de telas (visão geral atualizada)

- **QuickCaptureActivity** — o modal (ver seção própria).
- **AnotacoesActivity** ("Anotações") — grid de cards (`item_nota_card.xml`,
  via `NotaAdapter`), busca por título/corpo, seleção múltipla com exclusão/
  tag em lote, "+" abre `ManualIdeiaActivity`. Banner de anúncio (Free).
- **FinanceiroActivity** ("Financeiro") — extrato combinando Gasto e
  Recebimento (`EntryAdapter`), card de saldo do mês + gráfico de linha
  (MPAndroidChart) do saldo ao longo do período, busca por texto/categoria,
  acesso a `CategoriasActivity`. Banner de anúncio + intersticial ocasional
  (a cada 4 aberturas, só Free).
- **ReportActivity** ("Acompanhamento"/aba Início) — seletor de mês, card-
  resumo Receitas/Gastos/Poupança, barra "Meta de economia alcançada" (%
  calculado a partir de `Prefs.getMetaEconomiaValor()`), Por Categoria com
  barra proporcional, card de Insights (texto automático, ex. maior
  categoria de gasto do mês), seção Metas (lista de metas de economia com
  progresso — ver seção própria), grid de Estatísticas (maior gasto, média,
  categoria mais usada, dia da semana com mais gasto).
- **SettingsActivity** ("Perfil"/aba Conta) — reestruturado (sessão de
  redesign de 2026-07-23) em blocos com ícone+chevron: card Conta (login
  Google, backup na nuvem — inalterado na função, só reestilizado);
  **Progresso** (Estatísticas → Início, Histórico → Financeiro);
  **Personalização** (Meu Plano → `PlansActivity`, Tipo padrão ao abrir →
  dialog, Ajustar Objetivos → dialog que define a meta de economia mensal,
  Notificações → switch da notificação persistente); **Legal** (Política
  de Privacidade / Termos de Uso — **hoje são só diálogos stub**, sem link
  de verdade ainda publicado); **Suporte** (ver tutorial, guia de gesto,
  escolher atalho). Card de debug (só `BuildConfig.DEBUG`) força "sou PRO"
  localmente pra testar sem conta no Play Console.
- **CategoriasActivity** — gestão de categorias (nome, cor, ícone, limite
  mensal) e carteiras (nome), sem `RecyclerView` (listas sempre pequenas).
- **EditEntryActivity** — edição/exclusão de um Gasto ou Recebimento já
  existente; se já tinha `remoteId`, propaga `PATCH`/`DELETE` pro backend.
- **ManualGastoActivity** / **ManualRecebimentoActivity** — lançamento
  retroativo (valor, categoria/carteira, texto, data/hora via
  `DatePickerDialog`/`TimePickerDialog` nativos).
- **ManualIdeiaActivity** — "bloco de notas": cria e edita uma Ideia, com
  barra de formatação rica (ver `RichTextEngine` abaixo).
- **LoginActivity** — tela cheia estilizada como "ticket sendo emitido"
  (fundo escuro + card de papel com borda serrilhada no topo). Login
  **obrigatório**, sem pular.
- **PlansActivity** — comparação Free vs PRO, compra real via Play
  Billing.
- **QuickAccessChooserActivity** / **GestureGuideActivity** /
  **WidgetSuggestionActivity** — onboarding de atalhos (ver seção própria).
- **AnotacaoWidgetConfigActivity** — escolhe qual Ideia um widget mostra.

## Dados (`app/src/main/java/.../data/`) — Room versão 8

- **`Entry.kt`**: `id`, `type` (`enum EntryType { GASTO, PENSAMENTO, RECEBIMENTO }`,
  definido no mesmo arquivo), `titulo: String?` (só `ManualIdeiaActivity`
  preenche), `texto`, `valor: Double?`, `categoria: String?`,
  `carteira: String?`, `timestamp`, `remoteId: String?`.
- **`Category.kt`**: `id`, `nome`, `remoteId`, `limite: Double?` (orçamento
  mensal), `cor: String?` (hex cru, não resource id — funciona igual
  dentro do app e dentro de `RemoteViews`), `icone: String?` (emoji).
  `cor`/`icone` são **só locais**, não sincronizam (schema Prisma do
  backend não tem essas colunas).
- **`Carteira.kt`**: `id`, `nome`, `remoteId`. Sincroniza (só o nome).
- **`Meta.kt`** (novo, sessão de redesign 2026-07-23): `id`, `nome`,
  `valorAlvo: Double`, `valorAtual: Double`, `criadoEm: Long`. **Não tem
  `remoteId` e não sincroniza** — é local-only por enquanto, distinto da
  "meta de economia mensal" (um valor único, guardado em `Prefs`, usado só
  pro cálculo de % no card-resumo do Acompanhamento).
- **`EntryDao.kt`**: CRUD completo + fartas queries de relatório — totais
  por tipo/período (gasto, recebimento, contagem de ideias), quebra por
  categoria (soma e frequência), série diária (pro gráfico do Financeiro),
  maior gasto, média, dia da semana com mais gasto, reassociação em massa
  de categoria (ao renomear).
- **`CategoryDao.kt`** / **`CarteiraDao.kt`**: CRUD + trio de sync
  (`getPendentesDeSync`, `getAllOnce`, `marcarSincronizada`).
- **`MetaDao.kt`**: insert, `getAll()` (Flow), `atualizarValorAtual`,
  `deleteById` — sem nada de sync.
- **`AppDatabase.kt`**: `@Database(entities = [Entry, Category, Carteira, Meta], version = 8)`.
  Migrações reais (não destrutivas), uma por versão:
  - `2→3`: `remoteId` em entries/categories (backup na nuvem).
  - `3→4`: `titulo` em entries (bloco de notas).
  - `4→5`: `limite` em categories (orçamento mensal).
  - `5→6`: `carteira` em entries + tabela `carteiras` (o tipo `RECEBIMENTO`
    em si é só enum em código, não mexe em schema).
  - `6→7`: `cor` e `icone` em categories.
  - `7→8`: tabela `metas` (feature de metas de economia nomeadas).
  - `fallbackToDestructiveMigration()` continua como rede de segurança só
    pra um bump futuro sem migração escrita — nenhuma versão publicada até
    hoje depende dela de verdade.
- **`Prefs.kt`** (`SharedPreferences` simples): `tipo_padrao`,
  `onboarding_concluido`, `aberturas_historico` (nome da chave ficou do
  jeito antigo, hoje conta aberturas do Financeiro), `notificacao_captura_ativa`,
  `meta_economia_valor` (novo — meta de economia mensal única, distinta da
  lista de Metas).
- **`SessionPrefs.kt`**: `access_token`, `email`, `name` da sessão logada.
- **`SubscriptionPrefs.kt`**: `is_pro` (cache do status real, que vive no
  Play Billing), `forcar_pro_debug` (só `BuildConfig.DEBUG`).

## Backend / backup na nuvem

Repo separado: `GuilhermeSzFernandes/anotaplus-backend`, clonado em
`anotaplusdadsa/anotaplus-backend` (pasta irmã deste). NestJS + Prisma +
Neon (Postgres) + Render (deploy), no ar em
`https://anotaplus-backend.onrender.com/`. Auth via Google Sign-In
(Credential Manager, não a `GoogleSignInClient` antiga) → `POST /auth/google`
→ JWT próprio, guardado em `SessionPrefs`, mandado como
`Authorization: Bearer <token>` em toda chamada autenticada. Timeout de
45s no OkHttp — o Render free "dorme" e demora a acordar na primeira
chamada depois de inatividade.

**Push** (`SyncManager.sincronizarTudo`, via `SyncWorker`/WorkManager —
não pode rodar preso ao `lifecycleScope` da activity porque
`QuickCaptureActivity`/`ManualGastoActivity`/`ManualRecebimentoActivity`
chamam `finish()` logo após salvar, o que cancelaria a sincronização antes
de completar):
- Categorias pendentes (`remoteId == null`) → `POST /categories`, só
  `{nome, limite}` — **`cor`/`icone` não vão pro backend**.
- Carteiras pendentes → `POST /carteiras`, só `{nome}`.
- Entries pendentes → `POST /entries`, com todos os campos.
- Edição de limite numa categoria já sincronizada → `PATCH /categories/:id`.
- **Meta não sincroniza** — não existe rota `/metas` no `AnotaApi.kt`.

**Restore** (`SyncManager.restaurarTudo`): `GET /categories`/`/carteiras`
(casadas por **nome**, evita duplicar os padrões que toda instalação já
semeia) e `GET /entries` (casadas por **`remoteId`** — idempotente, seguro
rodar várias vezes). Disparado automaticamente logo após login bem-sucedido
(antes do push) e por botão manual "Restaurar backup" no Perfil.

Disparo de `SyncWorker.agendar()`: ao salvar gasto/ideia/recebimento, ao
adicionar categoria/carteira, e logo após login — só se
`SubscriptionPrefs.podeFazerBackup()` (logado **e** PRO).

## Assinatura PRO e anúncios

- Produto `anotaplus_pro_mensal` (assinatura, `Play Billing`,
  `BuildConfig.SUBSCRIPTION_PRODUCT_ID`). **Preço não é fixo no app** — vem
  ao vivo da Play Store via `ProductDetails`; o texto "R$ 10,00/mês" que
  aparecia em versões antigas deste doc era só o valor configurado no Play
  Console na época, não uma constante no código.
- **Bloqueio ainda de pé**: Play Billing só funciona de verdade com o app
  instalado a partir da Play Store (nem que seja teste interno) — via
  APK avulso (o `app-debug.apk` do GitHub Actions, sempre sideloadado até
  aqui), o `BillingClient` conecta mas nunca encontra o produto. Falta:
  conta de desenvolvedor no Play Console (usuário ainda não tem, $25
  único), subir a pelo menos um canal de teste, cadastrar
  `anotaplus_pro_mensal`, adicionar a própria conta como testador.
- **O que o PRO libera**: backup/restore na nuvem, remoção de anúncios
  (banner some, intersticial nem roda), sincronização do `limite` de
  categoria com a nuvem.
- **`BillingManager`**: `consultarProductDetails()`, `iniciarCompra()`,
  `atualizarStatusAssinatura()` (reconsulta `queryPurchasesAsync`, fonte
  da verdade, e sincroniza local + backend via `POST /billing/sync`) —
  chamado no login, ao abrir Perfil, e no `onResume()` de `PlansActivity`.
- **Gap conhecido, não é bug**: o backend não verifica o `purchaseToken`
  contra a API do Google Play Developer — confia no que o app relata.
  Só faz sentido resolver depois que existir conta de desenvolvedor.
- **Anúncios (AdMob)**: ainda sem conta AdMob própria — usa os **IDs de
  teste oficiais do Google** (`app/build.gradle.kts`). `MobileAds.initialize`
  roda uma vez em `AnotaPlusApplication.onCreate`. Banner em
  **Financeiro e Anotações** (não só numa tela, como versões antigas deste
  doc diziam); intersticial ocasional (a cada 4 aberturas) só em
  Financeiro. Ambos escondidos/pulados pra quem é PRO.

## Formatação rica na Ideia (`RichTextEngine.kt`)

Formatação "viva" (WYSIWYG) sem editor rico de verdade nem mudança de
schema — o texto guardado continua puro, com marcadores tipo Markdown, e
um `TextWatcher` recalcula os spans visuais a cada tecla. Marcadores
reconhecidos: `# ` (título 1), `## ` (título 2 — precisa checar antes de
`# `, já que os dois começam com `#`), `- ` (lista), `☐ `/`☑ ` (checklist,
único marcador que fica **visível** de propósito, porque *é* o checkbox),
`**negrito**`, `_itálico_`, `~sublinhado~`. Marcadores ficam ocultos via
`MarcadorOcultoSpan` (um `ReplacementSpan` de largura zero) — o caractere
continua existindo no `Editable` (cursor/backspace/texto salvo normais),
só não é desenhado. Enter no fim de um item de lista continua a lista
(novo item de checklist sempre nasce desmarcado); Enter num item vazio
sai da lista. Tocar no "☐"/"☑" no editor marca/desmarca sem abrir teclado.
`textoSemMarcadores()` gera o preview sem sintaxe crua, usado no
`EntryAdapter` e nos widgets.

## Alerta de orçamento (`BudgetAlertNotifier.kt`)

Depois de salvar/editar um Gasto com categoria (`ManualGastoActivity`,
`QuickCaptureActivity`, `EditEntryActivity`), confere se o total do mês
naquela categoria passou do `limite` e dispara notificação (canal
`alertas_orcamento`). Id fixo por categoria (`categoria.hashCode()`) —
estourar de novo no mesmo mês atualiza a notificação existente em vez de
empilhar. Tocar abre `CategoriasActivity`.

## Design system — redesign "fintech moderno" (sessão de 2026-07-23)

O app tinha (e a Captura Rápida **continua tendo**) uma identidade visual
deliberada de "papel e tinta" — ledger/caderno vintage: paleta dessaturada
(`ink`/`paper`/`brass`), cantos quase retos, botões com stroke, valores em
monospace, wordmarks caixa-alta. O restante do app foi modernizado pra um
visual "fintech": cantos bem arredondados, chips de ícone preto com ícone
branco, tipografia bold sans-serif, cards flat sem stroke, valores em
verde/vermelho semântico vívido (`color_receita`/`color_gasto_vivo`).

**Estratégia: aditiva, não destrutiva.** Nenhum token antigo (`ink`,
`paper`, `brass`, `TextAppearance.AnotaPlus.Amount`,
`Widget.AnotaPlus.Button.Stamp`/`.Tab`, `bg_card_free`) foi alterado ou
removido — todos continuam definidos exatamente como antes, então
`QuickCaptureActivity`, `ManualGastoActivity`, `ManualIdeiaActivity` e
`ManualRecebimentoActivity` (as únicas telas que mantêm o visual de
recibo) continuam resolvendo os mesmos nomes e renderizando idêntico.
Um conjunto **novo** de tokens (`dimens.xml`, novas cores, novos
drawables/estilos com nomes próprios) foi adicionado lado a lado, e só as
telas fora da Captura Rápida foram repontadas pra eles.

O que esse redesign também trouxe de **novo** (não só visual):
- Card-resumo Receitas/Gastos/Poupança + barra de meta de economia no
  Acompanhamento (usa `EntryDao.getTotalRecebimento`, que já existia mas
  não era usado em lugar nenhum antes).
- Insights automáticos (texto gerado a partir dos dados já calculados de
  categoria).
- Feature de Metas de economia nomeadas (Room: `Meta`/`MetaDao`, migração
  `7→8`).
- Perfil reestruturado em blocos ícone+chevron (Progresso/Personalização/
  Legal/Suporte), reaproveitando toda a funcionalidade que já existia
  (backup, login, tipo padrão, notificação) — só as seções Legal
  (Privacidade/Termos) são novas de fato, e hoje são só diálogos stub
  (sem link publicado ainda).

## Build sem Android Studio (peça central do fluxo de trabalho)

**O usuário não tem Android Studio instalado nem SDK Android configurado
localmente — e este ambiente de sandbox onde o Claude Code roda também
não tem (sem gradle wrapper commitado, sem `local.properties`, sem
`gradle-wrapper.jar`).** Isso significa que **nenhum dos dois lados
consegue compilar o projeto localmente** — o único lugar onde o app
realmente compila é o **GitHub Actions**.

- Workflow: `.github/workflows/build.yml`, dispara a cada push na `main`,
  usa `android-actions/setup-android` + `gradle/actions/setup-gradle` pra
  instalar SDK e Gradle no runner (não depende de wrapper local).
- Gera `app-debug.apk` como artefato pra download e sideload manual no
  celular.
- **Consequência prática pro fluxo de trabalho**: commitar e empurrar pra
  `main` é a única forma de "testar se compila" — não faz sentido segurar
  um push esperando rodar `./gradlew assembleDebug` antes, porque essa
  opção não existe aqui. Ver a memória de sessão sobre "sempre
  commitar/empurrar sem perguntar" — a razão de ser dessa regra é
  justamente esta.

## Próximos passos conhecidos (ainda não implementados)

- `AnotacaoWidgetProvider`/`CategoriasWidgetProvider` limitados a 6 linhas
  fixas — virar `RemoteViewsService`/coleção de verdade é o próximo passo
  se isso incomodar no uso real.
- Sincronização simultânea entre múltiplos aparelhos (resolução de
  conflito) — feature própria do PRO, distinta do backup simples atual.
- Criar conta de desenvolvedor no Play Console + cadastrar
  `anotaplus_pro_mensal` — bloqueia testar compra real.
- Criar conta AdMob + trocar IDs de teste pelos reais.
- Verificar `purchaseToken` contra a API do Google Play Developer (precisa
  da conta de desenvolvedor existir primeiro).
- Excluir categoria em Perfil/Categorias não propaga exclusão pro backend
  (diferente de `Entry`, que já propaga) — "Restaurar backup" depois
  ressuscitaria uma categoria apagada só localmente.
- Meta (lista de metas de economia) não sincroniza com a nuvem — decidir
  se vale a pena estender o backend pra isso ou manter local-only de
  propósito.
- Publicar de verdade a Política de Privacidade e os Termos de Uso — as
  linhas em Perfil > Legal são só diálogos stub hoje.
- Exportar histórico em CSV.

## Preferências do usuário (relevantes pro trabalho neste projeto)

- Desenvolvedor com ~2 anos de experiência, trabalha principalmente com
  C# ASP.NET/.NET Framework, NestJS, Next.js, SQL Server/PostgreSQL —
  Kotlin/Android não é a stack do dia a dia, prefere explicações diretas e
  passo a passo quando o problema for específico de ferramentas Android.
- Comunicação em português brasileiro, estilo casual e direto.
- Sempre commitar e empurrar pra `main` sem pedir confirmação antes —
  não há build local possível (ver seção "Build sem Android Studio"), o
  GitHub Actions é o único lugar onde o app é verificado de verdade.
