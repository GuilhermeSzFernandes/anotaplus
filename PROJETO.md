# Anota+ — contexto do projeto

## O que é

App Android nativo (Kotlin) para registrar **gastos** e **pensamentos rápidos**,
pensado para abrir instantaneamente via o gesto "toque duas vezes na traseira"
(Moto Actions) do celular Moto G75 do usuário.

A ideia central: em vez de abrir o app inteiro, o gesto abre um **modal/card
translúcido por cima da tela atual** — como um diálogo rápido — onde a pessoa
digita e salva sem sair do que estava fazendo.

## Stack

- Kotlin nativo, Android SDK (minSdk 26, compileSdk/targetSdk 34)
- View Binding (sem Compose por enquanto)
- Room (SQLite local) para persistência — dado local continua sendo a fonte
  de verdade; a nuvem (ver seção "Backend / backup na nuvem") é só login +
  CRUD por enquanto, ainda **sem sync de verdade**
- Sem Gradle Wrapper commitado: o build roda com Gradle instalado direto
  (ver seção de build)

## Como o "modal" funciona (parte mais importante da arquitetura)

- `QuickCaptureActivity` é a **única Activity `MAIN/LAUNCHER`** do app — tanto
  o ícone quanto o gesto Moto disparam o mesmo intent MAIN/LAUNCHER, então os
  dois caem nela.
- Pra separar as duas origens, ela olha `intent.sourceBounds` no `onCreate`:
  toque no ícone da tela inicial sempre chega com `sourceBounds` preenchido
  (posição do ícone, usada na animação do launcher); o gesto do Moto Actions
  não vem de um toque na tela, então nunca tem `sourceBounds`. Com
  `sourceBounds != null` ela redireciona pra `HistoryActivity`; senão, segue
  mostrando o modal de captura. É uma heurística (não existe um jeito
  oficial de diferenciar as duas origens), funciona bem com launchers
  AOSP-like (caso do Moto), mas vale confirmar no aparelho depois de
  instalar.
- Usa um tema translúcido customizado (`Theme.AnotaPlus.Modal`, em
  `res/values/themes.xml`): `windowIsTranslucent`, sem título, fundo
  transparente, sem animação de tela cheia.
- No manifest, essa activity tem `android:noHistory="true"` e
  `android:excludeFromRecents="true"` — assim ela não fica "presa" nos apps
  recentes e a transição de fechar é instantânea (volta pra tela de trás sem
  parecer que "saiu de um app").
- Chama `setShowWhenLocked(true)` + `setTurnScreenOn(true)` (com fallback via
  `WindowManager.LayoutParams` pra API < 27) no início do `onCreate` do modal
  de captura, pra abrir por cima da tela de bloqueio sem exigir desbloqueio —
  igual o atalho da câmera. Só a `QuickCaptureActivity` tem isso; o
  `HistoryActivity` continua exigindo desbloqueio normal, porque mostra dados
  sensíveis.
- Tocar fora do card (no scrim escurecido) ou salvar fecha a activity
  (`finish()`).

## Estrutura de telas

- **QuickCaptureActivity** (`res/layout/activity_quick_capture.xml`): toggle
  Gasto/Pensamento (abre no tipo padrão definido em Configurações), campo de
  valor + categoria (só aparece se "Gasto"), campo de texto livre, botão
  salvar, botão "Histórico". O campo categoria é uma combobox de verdade:
  `AutoCompleteTextView` com `inputType="none"` (sem teclado) e clique
  forçando `showDropDown()`, então só dá pra escolher uma categoria já
  cadastrada, sem digitar texto livre.
- **HistoryActivity** (`res/layout/activity_history.xml`): duas abas
  (`TabLayout`) — Gasto e Ideia — cada uma filtrando a mesma lista
  (RecyclerView + `EntryAdapter`) por `EntryType`. A aba Gasto também tem um
  seletor de mês (`row_month_selector.xml`, incluído via `<include>`) que
  filtra o período — default é o mês atual, com setas pra navegar pra
  meses anteriores (não deixa avançar além do mês atual). A aba Ideia não
  tem esse filtro, mostra tudo. O toolbar tem um menu com três ícones
  visíveis por vez: o "+" alterna entre `action_add_gasto` e
  `action_add_ideia` conforme a aba selecionada (visibilidade trocada no
  listener do `TabLayout`), mais Relatório e Configurações, sempre fixos.
  Tocar num item da lista abre `EditEntryActivity`.
- **EditEntryActivity** (`res/layout/activity_edit_entry.xml`): edição e
  exclusão de um lançamento existente — mesmo formulário do QuickCapture
  (toggle Gasto/Ideia, valor+categoria, texto) mais o seletor de data/hora
  do `ManualGastoActivity`, mas carregando os valores já salvos
  (`EntryDao.getById`). Botão "Excluir" pede confirmação
  (`MaterialAlertDialogBuilder` — não o `AlertDialog` puro do framework,
  que não pega o tema Material3 do app). Se o lançamento já tinha
  `remoteId` (já sincronizado antes), edição e exclusão também propagam
  pro backend (`PATCH`/`DELETE /entries/:id`) — sem isso, um "Restaurar
  backup" mais tarde ressuscitaria algo que foi editado/apagado só
  localmente.
- **ManualGastoActivity** (`res/layout/activity_manual_gasto.xml`):
  formulário pra lançar um gasto retroativo — mesmos campos do
  QuickCapture (valor, categoria em combobox, texto) mais um seletor de
  **data e hora** (`DatePickerDialog` + `TimePickerDialog` nativos, sem
  dependência nova), já que aqui não faz sentido usar o timestamp de
  "agora" como na captura rápida. Sempre grava como `EntryType.GASTO`.
  Acessada pelo ícone "+" no toolbar do Histórico (aba Gasto).
- **ManualIdeiaActivity** (`res/layout/activity_manual_ideia.xml`): "bloco
  de notas" — jeito separado (não é a Captura Rápida, que continua
  pequena/instantânea de propósito) de criar uma Ideia de dentro do app,
  acessado pelo ícone "+" no toolbar do Histórico (aba Ideia, espelhando o
  "+" de Gasto). Campo `Título` grande (`edit_titulo`, opcional) + corpo
  `Nota` sem caixa/sublinhado (`android:background="@null"`, dentro de um
  `NestedScrollView` com `fillViewport` + `layout_weight` pra ocupar o
  espaço todo da tela) — deliberadamente mais espaçoso que qualquer outro
  formulário do app. Sempre grava como `EntryType.PENSAMENTO`. `titulo` é
  exclusivo dessa tela: Gasto e Ideia via Captura Rápida/gesto continuam
  sempre com `titulo = null`. `EditEntryActivity` também ganhou o campo
  (visível só quando o tipo é Ideia) pra poder editar depois, e
  `EntryAdapter`/`item_entry.xml` mostram o título em negrito acima do
  texto quando existir.
- **ReportActivity** (`res/layout/activity_report.xml`): mesmo seletor de mês
  reutilizado (`row_month_selector.xml`), com total gasto, gasto por
  categoria (com barra proporcional) e contagem de ideias — tudo recalculado
  reativamente (`flatMapLatest`) quando o mês selecionado muda. Linhas de
  categoria são infladas programaticamente (sem RecyclerView, lista sempre
  pequena).
- **SettingsActivity** (`res/layout/activity_settings.xml`): escolha do tipo
  padrão ao abrir (salvo em `SharedPreferences` via `Prefs.kt`) e gestão de
  categorias (adicionar/remover, também sem RecyclerView). Cada seção
  (Conta, Tipo padrão, Categorias) é um card próprio com `bg_card_free.xml`
  (mesma borda fina usada no plano Free da `PlansActivity`) em vez de um
  bloco único separado só por divisores — dá hierarquia mais clara entre
  as seções. O e-mail da conta logada fica em linha própria, fonte mono,
  `maxLines="1"` + `ellipsize="middle"` — antes quebrava linha de um jeito
  feio quando o e-mail era longo.
- `MesUtil.kt`: helper compartilhado entre `HistoryActivity` e
  `ReportActivity` — converte um `Calendar` no intervalo de início/fim do
  mês em epoch millis (usado nas queries) e formata o label do mês.

## Dados (`app/src/main/java/.../data/`)

- `Entry.kt`: entidade Room — `id`, `type` (enum `GASTO`/`PENSAMENTO`),
  `titulo: String?` (só preenchido pela `ManualIdeiaActivity`, "bloco de
  notas"), `texto`, `valor: Double?`, `categoria: String?`, `timestamp`.
- `Category.kt` / `CategoryDao.kt`: categorias de gasto definidas pelo
  usuário (`id`, `nome`). Populadas com um seed padrão (Mercado, Transporte,
  Lazer, Contas, Outros) na primeira criação do banco, via
  `RoomDatabase.Callback.onCreate` no `AppDatabase`.
- `EntryDao.kt`: insert, `getAll()`/`getByType()`/`getByTypeAndRange()` como
  `Flow`, delete por id, e queries de relatório (`getGastoPorCategoria`,
  `getTotalGasto`, `getTotalIdeias`) filtradas por intervalo de tempo.
- `AppDatabase.kt`: singleton do Room + `Converters` para o enum. **Versão
  4**: `MIGRATION_2_3` (adiciona `remoteId` em `entries`/`categories`, pro
  backup) e `MIGRATION_3_4` (adiciona `titulo` em `entries`) são migrações
  reais, não destrutivas — `fallbackToDestructiveMigration()` continua lá
  só como rede de segurança pra um bump futuro sem migração escrita.
- `Prefs.kt`: wrapper simples sobre `SharedPreferences` pra guardar o tipo
  padrão (`Gasto`/`Ideia`) que o `QuickCaptureActivity` usa ao abrir.

## Backend / backup na nuvem (novo, em andamento)

Início de uma futura oferta de planos pagos com backup na nuvem. Decisões já
tomadas: NestJS + Prisma, Neon (Postgres), Render (deploy), Google Sign-In
(auth), Google Play Billing (quando chegar a hora de cobrar — ainda não
implementado).

- Repo separado: `https://github.com/GuilhermeSzFernandes/anotaplus-backend`,
  clonado localmente em `anotaplusdadsa/anotaplus-backend` (pasta irmã deste
  repo). Já no ar em `https://anotaplus-backend.onrender.com`.
- Schema Prisma (`User`, `Category`, `Entry`) espelha as entidades do Room,
  com `userId` porque agora é multi-usuário.
- `POST /auth/google` recebe o ID token do Google, valida contra
  `GOOGLE_WEB_CLIENT_ID` via `google-auth-library` e devolve um JWT próprio.
  `GET/POST/DELETE` de `/entries` e `/categories` protegidos por esse JWT.
- **No app Android**: login com Google via Credential Manager (API atual do
  Google, não usa `google-services.json` nem a `GoogleSignInClient` antiga).
  `AuthHelper.kt` concentra o fluxo (pegar credencial do Google → trocar por
  sessão no backend → salvar em `SessionPrefs.kt`), usado tanto pela tela de
  login quanto por Configurações — os dois só cuidam da própria UI
  (loading/erro). Token fica em `SessionPrefs.kt` (SharedPreferences simples,
  sem criptografia — aceitável por enquanto, é só sessão de app, não senha
  do Google).
  - `network/ApiClient.kt` + `network/AnotaApi.kt` (Retrofit + OkHttp +
    Gson) — primeira vez que o app faz qualquer chamada de rede; precisou
    adicionar `INTERNET` permission no manifest. Timeout de 45s no OkHttp
    porque o Render (plano free) dorme e demora a acordar na primeira
    chamada depois de inatividade.
  - `BuildConfig.API_BASE_URL` e `BuildConfig.GOOGLE_WEB_CLIENT_ID` vêm de
    `buildConfigField` no `build.gradle.kts` (valores fixos por enquanto,
    não variam por build type ainda).
- Ver `README.md` do repo do backend pra detalhes de setup/deploy — aqui só
  o resumo do lado Android.

### Backup na nuvem (sync) — implementado

Push (local → nuvem) **e** restore (nuvem → local, útil pra reinstalar o
app ou trocar de aparelho) — mas ainda não é "sincronizar entre aparelhos
ao mesmo tempo" de verdade (sem resolução de conflito se o mesmo usuário
editar em dois lugares); isso continua feature futura, já separada como
bullet própria do Premium na `PlansActivity`.

- `Entry`/`Category` ganharam campo `remoteId: String?` (null = ainda não
  sincronizado). **Migração real** (`MIGRATION_2_3` em `AppDatabase.kt`,
  versão 2 → 3, `ALTER TABLE ... ADD COLUMN`), não a
  `fallbackToDestructiveMigration()` usada nas versões anteriores — dessa
  vez tem gente com dado de verdade no app, seria irônico apagar tudo bem
  na hora de adicionar backup. `fallbackToDestructiveMigration()` continua
  como rede de segurança pra qualquer bump futuro sem migração explícita.
- `SyncManager.sincronizarTudo(context)`: busca tudo com `remoteId IS NULL`
  local, empurra pro backend (`POST /categories` / `POST /entries`, com
  `Authorization: Bearer <token>`), grava o `remoteId` retornado. Cada item
  falha isolado (`runCatching`) — um erro de rede num item não trava o
  lote inteiro.
- `SyncWorker` (`CoroutineWorker` do WorkManager) envolve o `SyncManager` —
  **não dá pra rodar isso numa coroutine presa ao `lifecycleScope` da
  activity**, porque `QuickCaptureActivity`/`ManualGastoActivity` chamam
  `finish()` logo depois de salvar, o que cancelaria a sincronização antes
  de completar. WorkManager sobrevive a isso, só roda com rede disponível
  (`Constraints.NetworkType.CONNECTED`) e tenta de novo sozinho se falhar.
- Disparado (`SyncWorker.agendar(context)`) em: salvar um gasto/ideia
  (`QuickCaptureActivity`, `ManualGastoActivity`), adicionar categoria
  (`SettingsActivity`), e logo após um login bem-sucedido (pra já mandar
  o que tiver acumulado antes de logar). Só dispara se
  `SessionPrefs.estaLogado()`.
- Botão manual "Fazer backup agora" em Configurações (dentro do card
  Conta, só visível logado) — chama `SyncManager.sincronizarTudo()` direto
  (sem passar pelo WorkManager, já que aqui a activity fica viva esperando
  o resultado) e mostra quantos itens foram sincronizados.
- Backend: `CategoriesService.create` virou `upsert` (por `userId_nome`,
  a constraint única do Prisma) em vez de `create` — sem isso, sincronizar
  duas vezes (ou entrar com categorias padrão que já existem) quebraria
  com erro de constraint única.
- `SyncManager.restaurarTudo(context)`: busca `GET /categories` e
  `GET /entries` do backend, insere no Room local o que ainda não existe.
  Categorias são casadas **por nome** (evita duplicar as categorias padrão
  que toda instalação nova já semeia sozinha); entries são casadas **pelo
  `remoteId`** (só insere se não existir localmente nenhuma com aquele
  `remoteId`) — rodar várias vezes é seguro, idempotente, não duplica nada.
  Disparado automaticamente logo após qualquer login bem-sucedido (antes do
  push, pra não reenviar à toa o que acabou de restaurar) e também via
  botão manual "Restaurar backup" em Configurações, ao lado do "Fazer
  backup agora".

## Onboarding (primeira abertura) e tela de Planos

- **Fluxo**: `QuickCaptureActivity` (ícone ou gesto, o que abrir primeiro)
  → `GestureGuideActivity` → `LoginActivity` → destino normal (Histórico ou
  captura, dependendo de qual dos dois foi usado pra abrir o app). Controlado
  por `Prefs.isOnboardingConcluido()` — só acontece uma vez, na vida do
  app instalado; depois disso o app abre direto no fluxo normal.
  - Os extras `GestureGuideActivity.EXTRA_ONBOARDING` /
    `EXTRA_ABRIR_HISTORICO` viajam entre as três activities pra saber (a)
    se está dentro da sequência de onboarding (decide se avança pra próxima
    tela ou só fecha) e (b) qual era o destino original antes do desvio.
  - Cada tela também pode ser aberta isoladamente depois (fora do
    onboarding) a partir de Configurações — "ver planos" e "como configurar
    o gesto" — nesse caso os extras não são passados e a tela só fecha ao
    concluir, sem redirecionar pra mais nada.
- **GestureGuideActivity**: explica o jeito de abrir na hora com passo a
  passo numerado. Detecta o fabricante via `Build.MANUFACTURER`: no Moto,
  texto e botão são específicos ("toque duas vezes na traseira", tenta
  abrir `com.motorola.actions` direto via
  `packageManager.getLaunchIntentForPackage`) e caem pra
  `Settings.ACTION_SETTINGS` se não encontrar.
  - **Confirmado (não é suposição)**: gesto de fabricante configurável por
    apps de terceiros é exclusividade do Moto — testamos a hipótese de a
    Xiaomi/HyperOS ter algo parecido (toque na traseira, atalho de app via
    duplo toque na digital) e não tem confirmação de que libera escolher
    um app de terceiros; provavelmente é restrito a apps do próprio
    fabricante (câmera, carteira/pagamento). Por isso, em qualquer
    aparelho que não seja Moto, o guia agora ensina a adicionar o
    **ladrilho de Configurações Rápidas** (`CapturaRapidaTileService`,
    ver seção própria abaixo) em vez de mandar caçar um gesto que
    provavelmente não existe pro Anota+ — é o único caminho *garantido*
    de funcionar em qualquer Android. Não tem botão de atalho pra essa
    tela (diferente do Moto): editar ladrilhos só se faz puxando a barra
    de notificação manualmente, não existe Intent público pra isso.

## Ladrilho de Configurações Rápidas e widget de atalho (novo)

- **`CapturaRapidaTileService`** (`android.service.quicksettings.TileService`):
  ladrilho que aparece no painel de Configurações Rápidas (puxar a barra de
  notificação + editar) — tocar nele abre a Captura Rápida em qualquer
  Android, sem depender de gesto de fabricante nenhum. Suporta tela
  bloqueada via `isLocked`/`unlockAndRun`. A partir do Android 14,
  `startActivityAndCollapse(Intent)` foi removido — o código detecta a
  versão e usa a variante com `PendingIntent` quando necessário.
  - Cogitamos usar `AccessibilityService` como atalho (funcionaria até com
    tela bloqueada, mais parecido com um gesto de verdade), mas descartado
    de propósito: usar a API de Acessibilidade só como atalho de abrir
    app (sem função de acessibilidade real) é contra a política da Play
    Store e é motivo comum de rejeição/remoção.
- **Segundo widget** (`widget/CapturaRapidaWidgetProvider.kt` +
  `widget_captura_rapida.xml`/`widget_captura_rapida_info.xml`): widget
  pequeno (1x1), só um atalho visual pra Captura Rápida — pra quem não
  quer o widget de totais (`GastoWidgetProvider`) na tela inicial. Sem
  consulta ao Room nenhuma, então `onUpdate` não precisa de
  `goAsync`/coroutine, só monta o `PendingIntent` (sem `sourceBounds`,
  igual todos os outros atalhos, pra cair no modal de captura e não no
  Histórico).
- **LoginActivity**: tela cheia (não é o modal translúcido) — desenhada
  como um "ticket sendo emitido": fundo escuro (`@color/scrim`, igual o
  scrim do modal de captura) com um card de papel centralizado que tem a
  borda serrilhada no topo (mesmo padrão do `card_slip` do QuickCapture),
  criando uma ponte visual deliberada com a experiência principal do app.
  Login é **obrigatório** — não existe opção de pular. Usa `AuthHelper` pro
  login, mais link pra `PlansActivity`.
- **PlansActivity**: comparação Free vs PRO, cards com `bg_card_free.xml` /
  `bg_card_premium.xml` (borda em latão no PRO; nome do recurso continua
  "premium" internamente, só o texto exibido virou "PRO"). Assinatura de
  verdade via Google Play Billing, R$ 10,00/mês — ver seção "Assinatura
  PRO e anúncios" abaixo.

## Assinatura PRO e anúncios (novo)

- **Preço/produto**: R$ 10,00/mês, id do produto de assinatura
  `anotaplus_pro_mensal` (`BuildConfig.SUBSCRIPTION_PRODUCT_ID`). Esse id
  **ainda não existe no Play Console** — precisa ser criado lá com esse
  exato id antes de qualquer compra funcionar.
- **Bloqueio crítico ainda não resolvido**: Google Play Billing só
  funciona de verdade com o app instalado **a partir da Play Store**
  (nem que seja em teste interno/fechado) — instalado via APK avulso, como
  o `app-debug.apk` do GitHub Actions que o usuário sempre sideloadou até
  aqui, o `BillingClient` conecta mas nunca encontra o produto. Antes de
  testar qualquer compra de verdade, precisa: (1) criar conta de
  desenvolvedor no Google Play Console (usuário confirmou que ainda não
  tem, `$25` pagamento único), (2) subir o app pra pelo menos um canal de
  teste, (3) cadastrar lá a assinatura `anotaplus_pro_mensal` a R$
  10,00/mês, (4) adicionar a própria conta Google como testador de
  licença.
- **`BillingManager`** (`app/.../billing/BillingManager.kt`): fina camada
  em cima da Play Billing Library. `consultarProductDetails()` busca
  preço/offer da Play Store; `iniciarCompra()` abre o fluxo de compra;
  `atualizarStatusAssinatura()` reconsulta `queryPurchasesAsync` (fonte da
  verdade) e sincroniza o resultado tanto no cache local
  (`SubscriptionPrefs`) quanto no backend (`POST /billing/sync`) — chamado
  no login, ao abrir Configurações, e no `onResume()` de `PlansActivity`
  (cobre a volta do fluxo de compra).
- **Gate do backup**: `SubscriptionPrefs.podeFazerBackup()` (logado E PRO)
  substitui os antigos checks de "só logado" em
  `QuickCaptureActivity`/`ManualGastoActivity`/`EditEntryActivity`/
  `SettingsActivity` antes de chamar `SyncWorker.agendar()`. No backend,
  `ProActiveGuard` (além do `JwtAuthGuard` de sempre) recusa com 403 quem
  não tem `proAtivo` — aplicado em todo `EntriesController` e
  `CategoriesController`, defesa em profundidade (não só esconder o botão
  na UI).
- **Gap conhecido, documentado (não é bug)**: o backend **não verifica o
  `purchaseToken`** contra a API do Google Play Developer — confia no que
  o app relata em `POST /billing/sync`. Verificação de verdade precisa de
  uma service account do Google Cloud com acesso à Android Publisher API,
  vinculada no Play Console — só faz sentido configurar depois que a conta
  de desenvolvedor existir.
- **Anúncios (AdMob)**: usuário também não tem conta AdMob ainda — o app
  usa os **IDs de teste oficiais do Google**
  (`app/build.gradle.kts`, `buildConfigField AD_BANNER_UNIT_ID` /
  `AD_INTERSTITIAL_UNIT_ID`, e `manifestPlaceholders["adMobAppId"]`),
  seguros de publicar mas que só mostram anúncio de teste, sem receita
  nenhuma. Banner fixo no rodapé do Histórico (`ad_view_banner` em
  `activity_history.xml`, escondido se `SubscriptionPrefs.isPro()`) +
  intersticial ocasional (a cada 4 aberturas do Histórico,
  `Prefs.incrementarAberturasHistorico`) — os dois só pro plano Free.
  Trocar pelos IDs reais assim que a conta AdMob existir.

## Bug corrigido (em três partes): gesto parava de ficar translúcido depois da 1ª abertura

`QuickCaptureActivity` tinha `android:launchMode="singleTask"` no manifest,
o que quebrava a ilusão de modal depois da primeira abertura (o Android
trazia a task antiga pra frente em vez de empilhar uma instância nova por
cima do app atual). Removido — mas isso **não resolveu completamente**: o
mesmo sintoma continuava acontecendo mesmo com `launchMode` padrão, sempre
que o usuário já tinha navegado pra Histórico/Configurações/etc. e só
apertado Home (sem fechar o app). A causa raiz de verdade: todas as
activities do app compartilham a mesma task (mesmo `taskAffinity` padrão,
baseado no pacote), então uma task em segundo plano contendo, por exemplo,
`HistoryActivity` no topo já existia — e o gesto (que dispara o mesmo
intent MAIN/LAUNCHER de sempre) fazia o Android simplesmente **reaproveitar
e trazer essa task de volta pra frente** (mostrando a última tela usada),
sem sequer chegar a criar uma instância nova do `QuickCaptureActivity`. A
correção final foi dar `android:taskAffinity=""` só pro `QuickCaptureActivity`
— isso isola ele numa task própria, que nunca persiste em segundo plano
(porque ele sempre se fecha sozinho via `noHistory` + `finish()`), então
toda vez que o gesto dispara, o Android é obrigado a criar uma instância
nova e fresca, independente do que estiver rolando nas outras telas do app.
Deixei os dois aprendizados comentados no manifest.

**Ainda não resolveu de verdade** — o usuário reportou o mesmo sintoma de
novo depois dessa correção. Hipótese (não 100% confirmável sem debugar o
próprio Moto Actions, que é fechado): o gesto provavelmente não faz um
`startActivity()` "normal" que passaria pela resolução de task por
`taskAffinity` — ele deve simplesmente checar se o app já tem alguma task
viva (usando `ActivityManager`/lista de tasks recentes) e, se tiver, trazer
ela pra frente diretamente (tipo abrir pelos Recentes), sem nunca chegar a
entregar um intent novo pro `QuickCaptureActivity`. Nesse cenário, não
importa qual activity é o alvo do intent — o que importa é se **existe
qualquer task do app viva em segundo plano**, e `HistoryActivity`/
`SettingsActivity`/etc. (que não têm `taskAffinity=""`) continuavam vivas.

Solução (terceira tentativa): `AnotaPlusApplication.kt`, usando
`ProcessLifecycleOwner` (diferencia "uma activity específica parou" de "o
app inteiro saiu de primeiro plano" — só o segundo caso interessa aqui) pra
detectar quando o app inteiro vai pro background e chamar
`finishAndRemoveTask()` na activity que estava em primeiro plano. Isso
garante que nunca sobra task nenhuma do app rodando quando ele não está em
uso — não importa o mecanismo exato que o gesto usa pra "resgatar" o app,
não vai ter nada pra resgatar.

**Contrapartida assumida**: qualquer tela do app (Histórico, Configurações,
Relatório) perde o estado (posição de rolagem, etc.) se a tela do celular
apagar sozinha no meio do uso ou se o usuário for pra outro app — na
próxima vez ela recarrega do zero em vez de continuar de onde parou. Dado
que essas telas recarregam do Room local instantaneamente (sem chamada de
rede), o custo é baixo perto do gesto ter que funcionar sempre.

## Widget de tela inicial

`GastoWidgetProvider` (`app/src/main/java/.../widget/`) mostra o total
gasto **hoje** e **no mês** sem precisar abrir o app. Layout em
`widget_gasto.xml`, metadados em `xml/widget_gasto_info.xml`.

Ponto importante: `RemoteViews` (o mecanismo por trás de widgets) só infla
views nativas do Android — nada de `PerforationView` (o Canvas customizado
usado no resto do app). O divisor "picotado" do widget é um
`@drawable/divider_dashed` (shape de linha tracejada) em vez da view real.

O widget **não fica observando o Room** (nada de `Flow`/`LiveData` reativo
por trás de um `AppWidgetProvider` — ele só existe pra reagir a broadcasts
do sistema). Em vez disso, `WidgetUpdater.atualizarTodos(context)` é uma
função "puxe sob demanda": lê os totais uma vez (`EntryDao.getTotalGastoOnce`)
e redesenha todos os widgets existentes. Ela é chamada manualmente depois de
toda mudança que pode afetar o total de gasto: `QuickCaptureActivity.salvar()`,
`ManualGastoActivity.salvar()`, `EditEntryActivity.salvar()`/`excluir()` e
`SyncManager.restaurarTudo()`. Fora isso, o próprio Android chama
`onUpdate()` periodicamente (`updatePeriodMillis`) e quando o widget é
adicionado à tela — nesse caminho, como `onUpdate()` roda dentro de um
`BroadcastReceiver` (que o sistema pode matar assim que a função retorna),
`GastoWidgetProvider` usa `goAsync()` pra manter o processo vivo até a
consulta suspend no Room terminar.

Toques no widget: tocar no corpo abre `HistoryActivity` direto; tocar no
"+" abre `QuickCaptureActivity` via `PendingIntent` — como esse `Intent`
não vem de um toque na tela (sem `sourceBounds`), ele naturalmente cai no
mesmo caminho do gesto de abrir e mostra o modal de captura, sem precisar
de nenhuma lógica extra pra diferenciar a origem.

## Build sem Android Studio

O usuário **não tem Android Studio instalado** e não quer instalar. A solução
adotada foi **GitHub Actions**: existe um workflow em
`.github/workflows/build.yml` que compila o APK debug na nuvem a cada push
na branch `main`, e disponibiliza `app-debug.apk` como artefato pra download
e instalação manual (sideload) no celular.

Esse workflow usa `android-actions/setup-android` + `gradle/actions/setup-gradle`
para instalar SDK e Gradle no runner (não depende de wrapper local).

## Status atual / histórico recente

- Projeto já está versionado num repositório GitHub pessoal do usuário
  (`GuilhermeSzFernandes/anotaplus`), com push funcionando.
- Teve alguns bugs de setup: erro de identidade do Git (resolvido com
  `git config user.name/email`), erro 403 de permissão no push (resolvido
  ajustando credenciais/token), e um erro de build (`colors.xml` sem a tag
  de fechamento `</resources>`, causando falha no `mergeDebugResources`).
- Objetivo imediato: garantir que o workflow do GitHub Actions completa o
  build com sucesso e gera um APK instalável.

## Próximos passos (ainda não implementados)
- Sincronizar de verdade entre múltiplos aparelhos ao mesmo tempo
  (resolução de conflito quando o mesmo usuário edita em dois lugares) —
  bullet própria do PRO na `PlansActivity`, distinta do backup simples.
- **Criar a conta de desenvolvedor no Google Play Console** ($25, pagamento
  único), subir o Anota+ pra pelo menos teste interno, e cadastrar lá a
  assinatura `anotaplus_pro_mensal` a R$ 10,00/mês — sem isso, o botão
  "Assinar" da `PlansActivity` conecta no Play Billing mas nunca acha
  produto nenhum (ver seção "Assinatura PRO e anúncios").
- **Criar conta AdMob** e cadastrar o Anota+ lá pra conseguir App ID e Ad
  Unit IDs reais — hoje o app usa os IDs de teste oficiais do Google
  (banner + intersticial), que não geram receita nenhuma.
- Verificar o `purchaseToken` do `POST /billing/sync` contra a API do
  Google Play Developer, em vez de confiar no que o app relata — precisa
  de uma service account do Google Cloud com Android Publisher API,
  vinculada no Play Console (só faz sentido depois que a conta existir).
- Exportar histórico em CSV.
- Editar/renomear categoria existente (hoje só dá pra adicionar e remover).
- Excluir categoria em Configurações **não propaga pro backend** (diferente
  de excluir/editar um `Entry`, que já propaga — ver `EditEntryActivity`) —
  um "Restaurar backup" depois ressuscitaria uma categoria apagada
  localmente. Mesmo problema que já foi resolvido pra `Entry`, só falta
  replicar pra `Category` (`DELETE /categories/:id` já existe no backend,
  só falta o app chamar).

## Preferências do usuário (relevantes pro trabalho neste projeto)

- Desenvolvedor com ~2 anos de experiência, trabalha principalmente com
  C# ASP.NET/.NET Framework, NestJS, Next.js, SQL Server/PostgreSQL — Kotlin/
  Android não é sua stack do dia a dia, então prefere explicações diretas e
  passo a passo quando o problema for específico de ferramentas Android.
  Comunicação em português brasileiro, estilo casual e direto.
