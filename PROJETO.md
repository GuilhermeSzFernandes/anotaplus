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
  tem esse filtro, mostra tudo. O toolbar tem um menu com três ícones (o
  "+" só aparece na aba Gasto): adicionar gasto manual, Relatório e
  Configurações.
- **ManualGastoActivity** (`res/layout/activity_manual_gasto.xml`):
  formulário pra lançar um gasto retroativo — mesmos campos do
  QuickCapture (valor, categoria em combobox, texto) mais um seletor de
  **data e hora** (`DatePickerDialog` + `TimePickerDialog` nativos, sem
  dependência nova), já que aqui não faz sentido usar o timestamp de
  "agora" como na captura rápida. Sempre grava como `EntryType.GASTO`.
  Acessada pelo ícone "+" no toolbar do Histórico (aba Gasto).
- **ReportActivity** (`res/layout/activity_report.xml`): mesmo seletor de mês
  reutilizado (`row_month_selector.xml`), com total gasto, gasto por
  categoria (com barra proporcional) e contagem de ideias — tudo recalculado
  reativamente (`flatMapLatest`) quando o mês selecionado muda. Linhas de
  categoria são infladas programaticamente (sem RecyclerView, lista sempre
  pequena).
- **SettingsActivity** (`res/layout/activity_settings.xml`): escolha do tipo
  padrão ao abrir (salvo em `SharedPreferences` via `Prefs.kt`) e gestão de
  categorias (adicionar/remover, também sem RecyclerView).
- `MesUtil.kt`: helper compartilhado entre `HistoryActivity` e
  `ReportActivity` — converte um `Calendar` no intervalo de início/fim do
  mês em epoch millis (usado nas queries) e formata o label do mês.

## Dados (`app/src/main/java/.../data/`)

- `Entry.kt`: entidade Room — `id`, `type` (enum `GASTO`/`PENSAMENTO`),
  `texto`, `valor: Double?`, `categoria: String?`, `timestamp`.
- `Category.kt` / `CategoryDao.kt`: categorias de gasto definidas pelo
  usuário (`id`, `nome`). Populadas com um seed padrão (Mercado, Transporte,
  Lazer, Contas, Outros) na primeira criação do banco, via
  `RoomDatabase.Callback.onCreate` no `AppDatabase`.
- `EntryDao.kt`: insert, `getAll()`/`getByType()`/`getByTypeAndRange()` como
  `Flow`, delete por id, e queries de relatório (`getGastoPorCategoria`,
  `getTotalGasto`, `getTotalIdeias`) filtradas por intervalo de tempo.
- `AppDatabase.kt`: singleton do Room + `Converters` para o enum. **Versão 2**
  (adicionou `categories`) usa `fallbackToDestructiveMigration()` — não tem
  migração real escrita, então qualquer bump de versão futuro apaga os dados
  locais existentes no aparelho do usuário. Aceitável agora (projeto ainda em
  fase inicial/teste), mas revisar se isso virar um problema real.
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
  Tela de login fica em Configurações (seção "Conta", topo da tela) — mostra
  "Entrar com Google" ou "Conectado como {email}" + "Sair". O `accessToken`
  retornado pelo backend fica salvo em `SessionPrefs.kt` (SharedPreferences
  simples, sem criptografia — aceitável por enquanto, dado que é só uma
  sessão de app, não a senha do Google).
  - `network/ApiClient.kt` + `network/AnotaApi.kt` (Retrofit + OkHttp +
    Gson) — primeira vez que o app faz qualquer chamada de rede; precisou
    adicionar `INTERNET` permission no manifest.
  - `BuildConfig.API_BASE_URL` e `BuildConfig.GOOGLE_WEB_CLIENT_ID` vêm de
    `buildConfigField` no `build.gradle.kts` (valores fixos por enquanto,
    não variam por build type ainda).
- **Login ainda não faz nada além de autenticar** — não existe sync de
  entries/categories entre o Room local e o backend ainda. É só a base
  (auth) pronta pra construir isso em cima.
- Ver `README.md` do repo do backend pra detalhes de setup/deploy — aqui só
  o resumo do lado Android.

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

- Sync de verdade entre o Room local e o backend (upload/download de
  entries e categories, resolução de conflito entre dispositivos) — hoje
  o backend só tem os endpoints CRUD, nada no app chama `/entries` ou
  `/categories` ainda.
- Modelo de assinatura (`Subscription`) + verificação de recibo do Google
  Play Billing, pra gatear o backup atrás do plano pago.
- Exportar histórico em CSV.
- Widget de tela inicial com total de gastos do dia/mês.
- Editar/renomear categoria existente (hoje só dá pra adicionar e remover).

## Preferências do usuário (relevantes pro trabalho neste projeto)

- Desenvolvedor com ~2 anos de experiência, trabalha principalmente com
  C# ASP.NET/.NET Framework, NestJS, Next.js, SQL Server/PostgreSQL — Kotlin/
  Android não é sua stack do dia a dia, então prefere explicações diretas e
  passo a passo quando o problema for específico de ferramentas Android.
  Comunicação em português brasileiro, estilo casual e direto.
