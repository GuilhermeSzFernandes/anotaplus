# Refino Visual "Fintech Moderno" + Mascote Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Ship the design decided in `docs/superpowers/specs/2026-07-23-refino-visual-fintech-mascote-design.md` — a shared spacing/accent-color token set, a redesigned 6-screen onboarding chain (2 brand-new screens: nome de exibição and primeira Meta com prazo) using a small-mascot-plus-question-bubble visual pattern, a Perfil alignment fix, a Legal-dialog polish, and mascot placement (placeholder art) in the Início greeting, Meta celebration, and budget alert notification.

**Architecture:** Pure Kotlin/Android, View Binding, no Compose (matches the existing codebase). New onboarding screens are plain `AppCompatActivity` subclasses chained via `Intent`, exactly like the 5 existing onboarding screens they sit between. A new pure-Kotlin `MetaCalculo` object (no Android dependency) carries the only genuinely testable logic (months-until-date, monthly-savings-needed math) — everything else is layout/wiring change, verified by pushing to `main` and checking the GitHub Actions build + manual sideload QA.

**Tech Stack:** Kotlin, AppCompat, View Binding, Room 2.6.1 (KSP), Material Components. No Compose, no new libraries needed.

## Global Constraints

- **No local build or test execution is possible in this environment** (no Android Studio, no SDK, no committed Gradle wrapper, no `local.properties`) — the ONLY place this project compiles is GitHub Actions (`.github/workflows/build.yml`, job "Compilar APK (debug)", triggered on push to `main`). Every task's "verify" step means: commit, push to `main`, and confirm the GitHub Actions run is green (Task 1 also adds a `./gradlew test` step so the new unit tests actually execute somewhere). For UI-only changes with no new logic, verification is a manual sideload check of the resulting `app-debug.apk` artifact — call this out explicitly per task, don't skip it silently.
- **Commit and push to `main` after every task without asking** — established project workflow (no local verification exists otherwise).
- Room DB is currently version 8 (`AppDatabase.kt:83`). Any schema change bumps to version 9 with a real, additive `Migration(8, 9)` — never rely on `fallbackToDestructiveMigration()` for a change a real user could hit.
- **Captura Rápida is untouched**: `QuickCaptureActivity`, `ManualGastoActivity`, `ManualIdeiaActivity`, `ManualRecebimentoActivity` keep their current "recibo" visual — no task in this plan touches those 4 files or their layouts.
- `color_receita` (#1E8E3E) and `color_gasto_vivo` (#E5484D) keep their current semantic meaning (income/expense text) and are **not** reassigned anywhere except the two literal "progress bar fill" usages this plan explicitly calls out (Task 7) — do not sweep every `color_receita` reference to the new accent.
- **Do not invent a new green button style.** `Widget.AnotaPlus.Button.Primary` (`app/src/main/res/values/themes.xml:159-168`) already resolves to `ink` via the `primary_bg` selector (`app/src/main/res/color/primary_bg.xml`) — it already matches the "botão preto" decision. No task in this plan should change `Widget.AnotaPlus.Button.Primary`.
- Spec: `docs/superpowers/specs/2026-07-23-refino-visual-fintech-mascote-design.md`. Onboarding visual reference: `docs/superpowers/specs/assets/onboarding-referencia/1.jpeg`–`12.jpeg`. Mascot reference photo: `docs/superpowers/specs/assets/mascote-moeda-real.png` (final mascot art — 3 mood variants, transparent PNG — has **not** been produced yet; this plan uses placeholder vector drawables, see Task 3, and swapping them for real art later needs no code change since the drawable names stay the same).

## Scope note: what this plan does NOT cover

Two items from the spec's mascot placement list are deliberately **out of scope** for this plan, to keep it a shippable, reviewable unit rather than an open-ended sprawl:

- **Empty states** ("nenhuma nota ainda" in Anotações, "nenhum lançamento ainda" in Financeiro) — needs its own look at `AnotacoesActivity`/`FinanceiroActivity`'s empty-state layouts, not yet investigated. Natural follow-up plan once this one ships.
- **Mascot animation/movement** — explicitly deferred in the spec itself, not part of this plan.

Everything else in the spec (tokens, onboarding migration + 2 new screens + Meta prazo migration, Perfil alignment, Legal dialog, Início greeting, Meta-row celebration, budget alert notification icon) is covered below.

---

## File Structure

**New files:**
- `app/src/test/java/com/guilherme/anotaplus/MetaCalculoTest.kt` — unit tests for the prazo/valor-mensal math (Task 1, 4)
- `app/src/main/java/com/guilherme/anotaplus/MetaCalculo.kt` — pure prazo/valor-mensal calculation, shared by onboarding and the existing Meta dialog (Task 4)
- `app/src/main/java/com/guilherme/anotaplus/OnboardingHeaderUtil.kt` — one-line binder for the shared onboarding header (Task 9)
- `app/src/main/java/com/guilherme/anotaplus/NomeOnboardingActivity.kt` — new "como quer ser chamado" screen (Task 10)
- `app/src/main/java/com/guilherme/anotaplus/MetaOnboardingActivity.kt` — new "primeira meta" screen (Task 11)
- `app/src/main/res/layout/onboarding_header.xml` — shared back+progress+mascote+balão header, `<include>`d by all 6 onboarding screens (Task 9)
- `app/src/main/res/layout/activity_nome_onboarding.xml` (Task 10)
- `app/src/main/res/layout/activity_meta_onboarding.xml` (Task 11)
- `app/src/main/res/drawable/ic_mascote_neutro.xml`, `ic_mascote_feliz.xml`, `ic_mascote_preocupado.xml` — placeholder mascot art (Task 3)
- `app/src/main/res/drawable/bg_balao_pergunta.xml` — question-bubble background (Task 9)
- `app/src/main/res/drawable/bg_chip_prazo.xml` + `app/src/main/res/color/chip_prazo_text.xml` — prazo-chip selected/unselected states (Task 6)
- `app/src/main/res/drawable/bg_icon_chip_accent.xml` — circular accent-colored icon chip for the Legal dialog header (Task 18)

**Modified files:** `data/Prefs.kt`, `data/SessionPrefs.kt`, `data/Meta.kt`, `data/AppDatabase.kt`, `ReportActivity.kt` + `activity_report.xml` + `item_meta.xml` + `dialog_nova_meta.xml`, `LoginActivity.kt` + `activity_login.xml`, `SalarioOnboardingActivity.kt` + `activity_salario_onboarding.xml`, `GestureGuideActivity.kt` + `activity_gesture_guide.xml`, `WidgetSuggestionActivity.kt` + `activity_widget_suggestion.xml`, `QuickAccessChooserActivity.kt` + `activity_quick_access_chooser.xml`, `activity_settings.xml`, `DialogUtil.kt` (unchanged signature, just the layout it wraps) + `dialog_confirmacao_flat.xml` + `SettingsActivity.kt`, `BudgetAlertNotifier.kt`, `AndroidManifest.xml`, `res/values/colors.xml`, `res/values/dimens.xml`, `res/values/strings.xml`, `app/build.gradle.kts`, `.github/workflows/build.yml`.

---

### Task 1: Unit-test infrastructure (JUnit + CI step)

This repo has zero tests today (`app/src/test/` and `app/src/androidTest/` don't exist) and CI only runs `assembleDebug`. Task 4 needs a real place for a real unit test to run — this task creates it.

**Files:**
- Modify: `app/build.gradle.kts`
- Modify: `.github/workflows/build.yml`
- Create: `app/src/test/java/com/guilherme/anotaplus/PlaceholderTest.kt` (proves the harness works before Task 4 depends on it)

**Interfaces:**
- Produces: a working `./gradlew test` task that CI runs and that any later `app/src/test/**` file participates in automatically (standard Gradle JVM test source set — no further wiring needed).

- [ ] **Step 1: Add the JUnit test dependency**

Edit `app/build.gradle.kts`, in the `dependencies { ... }` block, after line 118 (`implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")`):

```kotlin
    // Testes unitários JVM puros (sem Robolectric/instrumentação — este
    // projeto não tem infraestrutura de emulador em CI, então só entram
    // aqui testes que não dependem de Context/Android framework).
    testImplementation("junit:junit:4.13.2")
```

- [ ] **Step 2: Add the placeholder test**

Create `app/src/test/java/com/guilherme/anotaplus/PlaceholderTest.kt`:

```kotlin
package com.guilherme.anotaplus

import org.junit.Assert.assertEquals
import org.junit.Test

class PlaceholderTest {
    @Test
    fun `harness roda`() {
        assertEquals(2, 1 + 1)
    }
}
```

- [ ] **Step 3: Add the CI test step**

Edit `.github/workflows/build.yml`, insert a new step between "Configurar Gradle" (ends line 27) and "Compilar APK (debug)" (starts line 29):

```yaml
      - name: Rodar testes unitários
        run: gradle test --stacktrace

      - name: Compilar APK (debug)
        run: gradle assembleDebug --stacktrace
```

(This replaces the existing "Compilar APK (debug)" step header — the rest of that step, lines 30-36, stays exactly as-is below it.)

- [ ] **Step 4: Commit and push**

```bash
git add app/build.gradle.kts .github/workflows/build.yml app/src/test/java/com/guilherme/anotaplus/PlaceholderTest.kt
git commit -m "Adiciona infraestrutura de teste unitário (JUnit) e roda no CI"
git push
```

- [ ] **Step 5: Verify**

Open the GitHub Actions run for this push (repo → Actions tab). Confirm both "Rodar testes unitários" and "Compilar APK (debug)" steps show green. If "Rodar testes unitários" fails, read the Gradle output in the log — a failure here means the JUnit dependency didn't resolve, not a real code bug (there's no real logic yet).

---

### Task 2: Design tokens — spacing scale + accent color

**Files:**
- Modify: `app/src/main/res/values/dimens.xml`
- Modify: `app/src/main/res/values/colors.xml`

**Interfaces:**
- Produces: `@dimen/spacing_xs` (8dp), `@dimen/spacing_sm` (12dp), `@dimen/spacing_md` (16dp), `@dimen/spacing_lg` (20dp), `@color/verde_destaque` (#4CAF6D) — consumed by every later task in this plan.

- [ ] **Step 1: Add the spacing scale**

Edit `app/src/main/res/values/dimens.xml`, appending after the existing `radius_button` line (before `</resources>`):

```xml

    <!-- Escala de espaçamento (sessão de design 2026-07-23) — consolida os
         5 valores hoje espalhados (12/16/18/20/28dp) pro mesmo tipo de
         elemento visual. Ver docs/superpowers/specs/2026-07-23-refino-
         visual-fintech-mascote-design.md, item 2. -->
    <dimen name="spacing_xs">8dp</dimen>
    <dimen name="spacing_sm">12dp</dimen>
    <dimen name="spacing_md">16dp</dimen>
    <dimen name="spacing_lg">20dp</dimen>
```

- [ ] **Step 2: Add the accent color**

Edit `app/src/main/res/values/colors.xml`, appending after the existing `chart_teal_tint` line (before `</resources>`):

```xml

    <!-- Verde-claro de destaque (sessão de design 2026-07-23): só usado em
         detalhes pontuais (barra de progresso, números em destaque, borda
         de seleção, mascote) — NUNCA como preenchimento de botão ou tela.
         Tom de partida, recalibrar junto da arte final do mascote quando
         existir. -->
    <color name="verde_destaque">#4CAF6D</color>
```

- [ ] **Step 3: Commit and push**

```bash
git add app/src/main/res/values/dimens.xml app/src/main/res/values/colors.xml
git commit -m "Adiciona escala de espacamento e cor de destaque verde"
git push
```

- [ ] **Step 4: Verify**

No logic to unit-test (pure resource addition). Confirm the GitHub Actions build stays green — a typo in either XML file fails resource compilation immediately and shows up as a red `assembleDebug` step.

---

### Task 3: Placeholder mascot vector drawables

**Files:**
- Create: `app/src/main/res/drawable/ic_mascote_neutro.xml`
- Create: `app/src/main/res/drawable/ic_mascote_feliz.xml`
- Create: `app/src/main/res/drawable/ic_mascote_preocupado.xml`

**Interfaces:**
- Produces: 3 drawables with these exact names, referenced by every later task that displays the mascot. Swapping in real art later (same file names, or re-pointing these same names at imported PNGs) needs no code change anywhere else.

- [ ] **Step 1: Create the neutral mood**

Create `app/src/main/res/drawable/ic_mascote_neutro.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<!-- Placeholder do mascote (moeda de 1 Real) — humor neutro/padrão.
     Substituir pela arte real (3D render, fundo transparente) quando
     existir; manter este nome de arquivo pra não precisar mudar código. -->
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="48dp"
    android:height="48dp"
    android:viewportWidth="48"
    android:viewportHeight="48">
    <path android:fillColor="#C9962E" android:pathData="M24,4 A20,20 0 1,1 23.99,4 Z" />
    <path android:fillColor="#8FDA9B" android:pathData="M24,10 A14,14 0 1,1 23.99,10 Z" />
    <path android:fillColor="#1B1B1D" android:pathData="M18,21 a2,2 0 1,1 -0.01,0 Z" />
    <path android:fillColor="#1B1B1D" android:pathData="M30,21 a2,2 0 1,1 -0.01,0 Z" />
    <path
        android:strokeColor="#1B1B1D"
        android:strokeWidth="1.5"
        android:strokeLineCap="round"
        android:pathData="M19,28 Q24,31 29,28" />
</vector>
```

- [ ] **Step 2: Create the happy mood**

Create `app/src/main/res/drawable/ic_mascote_feliz.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<!-- Placeholder do mascote — humor feliz/comemorando (meta batida). -->
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="48dp"
    android:height="48dp"
    android:viewportWidth="48"
    android:viewportHeight="48">
    <path android:fillColor="#C9962E" android:pathData="M24,4 A20,20 0 1,1 23.99,4 Z" />
    <path android:fillColor="#8FDA9B" android:pathData="M24,10 A14,14 0 1,1 23.99,10 Z" />
    <path
        android:strokeColor="#1B1B1D"
        android:strokeWidth="1.5"
        android:strokeLineCap="round"
        android:pathData="M16,20 Q18,17 20,20" />
    <path
        android:strokeColor="#1B1B1D"
        android:strokeWidth="1.5"
        android:strokeLineCap="round"
        android:pathData="M28,20 Q30,17 32,20" />
    <path
        android:strokeColor="#1B1B1D"
        android:strokeWidth="2"
        android:strokeLineCap="round"
        android:pathData="M17,27 Q24,33 31,27" />
    <path android:fillColor="#E5C158" android:pathData="M8,10 l1.5,3 l3,0.5 l-2.2,2 l0.5,3 l-2.8,-1.5 l-2.8,1.5 l0.5,-3 l-2.2,-2 l3,-0.5 Z" />
</vector>
```

- [ ] **Step 3: Create the worried mood**

Create `app/src/main/res/drawable/ic_mascote_preocupado.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<!-- Placeholder do mascote — humor preocupado (alerta de orçamento
     estourado). Mantém a paleta âmbar/verde do personagem de propósito —
     não migra pra vermelho, pra não confundir com @color/color_gasto_vivo. -->
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="48dp"
    android:height="48dp"
    android:viewportWidth="48"
    android:viewportHeight="48">
    <path android:fillColor="#C9962E" android:pathData="M24,4 A20,20 0 1,1 23.99,4 Z" />
    <path android:fillColor="#8FDA9B" android:pathData="M24,10 A14,14 0 1,1 23.99,10 Z" />
    <path
        android:strokeColor="#1B1B1D"
        android:strokeWidth="1.5"
        android:strokeLineCap="round"
        android:pathData="M16,18 L20,20" />
    <path
        android:strokeColor="#1B1B1D"
        android:strokeWidth="1.5"
        android:strokeLineCap="round"
        android:pathData="M32,18 L28,20" />
    <path android:fillColor="#1B1B1D" android:pathData="M18,23 a2,2 0 1,1 -0.01,0 Z" />
    <path android:fillColor="#1B1B1D" android:pathData="M30,23 a2,2 0 1,1 -0.01,0 Z" />
    <path
        android:strokeColor="#1B1B1D"
        android:strokeWidth="1.5"
        android:strokeLineCap="round"
        android:pathData="M19,30 Q24,27 29,30" />
</vector>
```

- [ ] **Step 4: Commit and push**

```bash
git add app/src/main/res/drawable/ic_mascote_neutro.xml app/src/main/res/drawable/ic_mascote_feliz.xml app/src/main/res/drawable/ic_mascote_preocupado.xml
git commit -m "Adiciona drawables placeholder do mascote (3 humores)"
git push
```

- [ ] **Step 5: Verify**

Confirm GitHub Actions build is green (vector drawable XML errors fail resource compilation). No manual QA possible yet — nothing references these drawables until Task 9+.

---

### Task 4: `MetaCalculo` — pure prazo/valor-mensal math, with real unit tests

**Files:**
- Create: `app/src/main/java/com/guilherme/anotaplus/MetaCalculo.kt`
- Create: `app/src/test/java/com/guilherme/anotaplus/MetaCalculoTest.kt`

**Interfaces:**
- Produces: `MetaCalculo.mesesAte(prazoEpochMillis: Long, agoraEpochMillis: Long): Int`, `MetaCalculo.valorMensalNecessario(valorAlvo: Double, valorAtual: Double, prazoEpochMillis: Long?, agoraEpochMillis: Long): Double?`, `MetaCalculo.prazoEmMeses(agoraEpochMillis: Long, meses: Int): Long` — consumed by Task 6 (existing Meta dialog) and Task 11 (new Meta onboarding screen).

- [ ] **Step 1: Write the failing tests**

Create `app/src/test/java/com/guilherme/anotaplus/MetaCalculoTest.kt`:

```kotlin
package com.guilherme.anotaplus

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Calendar

class MetaCalculoTest {

    private fun epoch(ano: Int, mes: Int, dia: Int): Long {
        val cal = Calendar.getInstance()
        cal.set(ano, mes - 1, dia, 0, 0, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    @Test
    fun `mesesAte calcula meses inteiros entre duas datas`() {
        val agora = epoch(2026, 7, 23)
        val prazo = epoch(2027, 1, 23)
        assertEquals(6, MetaCalculo.mesesAte(prazo, agora))
    }

    @Test
    fun `mesesAte nunca retorna menos que 1`() {
        val agora = epoch(2026, 7, 23)
        val prazo = epoch(2026, 7, 25)
        assertEquals(1, MetaCalculo.mesesAte(prazo, agora))
    }

    @Test
    fun `valorMensalNecessario divide o valor restante pelos meses`() {
        val agora = epoch(2026, 7, 23)
        val prazo = epoch(2027, 1, 23)
        val resultado = MetaCalculo.valorMensalNecessario(1000.0, 0.0, prazo, agora)
        assertEquals(166.67, resultado!!, 0.01)
    }

    @Test
    fun `valorMensalNecessario desconta o valor ja guardado`() {
        val agora = epoch(2026, 7, 23)
        val prazo = epoch(2027, 1, 23)
        val resultado = MetaCalculo.valorMensalNecessario(1000.0, 400.0, prazo, agora)
        assertEquals(100.0, resultado!!, 0.01)
    }

    @Test
    fun `valorMensalNecessario retorna null sem prazo`() {
        val resultado = MetaCalculo.valorMensalNecessario(1000.0, 0.0, null, epoch(2026, 7, 23))
        assertNull(resultado)
    }

    @Test
    fun `prazoEmMeses soma meses a partir de agora`() {
        val agora = epoch(2026, 7, 23)
        val resultado = MetaCalculo.prazoEmMeses(agora, 6)
        assertEquals(epoch(2027, 1, 23), resultado)
    }
}
```

- [ ] **Step 2: Run to verify it fails**

Push this file alone would fail to compile (no `MetaCalculo` yet) — instead, verify locally-equivalent by inspection: `MetaCalculo` doesn't exist yet, so this test file does not compile. Do not push yet; proceed to Step 3 first (Gradle has no incremental "run just this test and see red" available without a local SDK, so the fail/pass cycle here is: write test, write implementation in the same commit, push once, confirm CI green — the "see it fail" step is a compile-time check you do by reading the code, not a captured red CI run).

- [ ] **Step 3: Write the implementation**

Create `app/src/main/java/com/guilherme/anotaplus/MetaCalculo.kt`:

```kotlin
package com.guilherme.anotaplus

import java.util.Calendar

// Matemática de prazo/valor-mensal de uma Meta, extraída como função pura
// (sem Context/Android) de propósito — este projeto não tem infraestrutura
// de teste instrumentado (sem emulador em CI), então qualquer lógica que
// precise de teste real tem que rodar em JVM puro via JUnit.
object MetaCalculo {

    // Meses inteiros entre `agora` e `prazo`, arredondado pra baixo e com
    // mínimo de 1 — mesmo um prazo "esse mês" ainda pede uma parcela única
    // em vez de dividir por zero.
    fun mesesAte(prazoEpochMillis: Long, agoraEpochMillis: Long): Int {
        val agora = Calendar.getInstance().apply { timeInMillis = agoraEpochMillis }
        val prazo = Calendar.getInstance().apply { timeInMillis = prazoEpochMillis }
        val meses = (prazo.get(Calendar.YEAR) - agora.get(Calendar.YEAR)) * 12 +
            (prazo.get(Calendar.MONTH) - agora.get(Calendar.MONTH))
        return meses.coerceAtLeast(1)
    }

    // Valor mensal necessário pra ir de valorAtual até valorAlvo dado um
    // prazo — null se a Meta não tem prazo definido (sem preview nesse
    // caso).
    fun valorMensalNecessario(
        valorAlvo: Double,
        valorAtual: Double,
        prazoEpochMillis: Long?,
        agoraEpochMillis: Long
    ): Double? {
        if (prazoEpochMillis == null) return null
        val restante = (valorAlvo - valorAtual).coerceAtLeast(0.0)
        val meses = mesesAte(prazoEpochMillis, agoraEpochMillis)
        return restante / meses
    }

    // Epoch millis de "daqui a `meses` meses", usado pelos atalhos de
    // prazo ("6 meses"/"1 ano").
    fun prazoEmMeses(agoraEpochMillis: Long, meses: Int): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = agoraEpochMillis
            add(Calendar.MONTH, meses)
        }
        return cal.timeInMillis
    }
}
```

- [ ] **Step 4: Commit and push**

```bash
git add app/src/main/java/com/guilherme/anotaplus/MetaCalculo.kt app/src/test/java/com/guilherme/anotaplus/MetaCalculoTest.kt
git commit -m "Adiciona MetaCalculo (prazo/valor mensal) com testes unitarios"
git push
```

- [ ] **Step 5: Verify it passes**

Open the GitHub Actions run. Confirm "Rodar testes unitários" is green — the log shows all 6 `MetaCalculoTest` cases passing. If red, read the failure: a wrong `coerceAtLeast`/month-diff sign is the likely culprit.

---

### Task 5: `Meta.prazo` field + Room migration 8→9

**Files:**
- Modify: `app/src/main/java/com/guilherme/anotaplus/data/Meta.kt`
- Modify: `app/src/main/java/com/guilherme/anotaplus/data/AppDatabase.kt`

**Interfaces:**
- Consumes: nothing new.
- Produces: `Meta.prazo: Long?` (nullable, default `null`) — consumed by Task 6 and Task 11's `Meta(...)` construction calls.

- [ ] **Step 1: Add the field to the entity**

Edit `app/src/main/java/com/guilherme/anotaplus/data/Meta.kt`:

```kotlin
package com.guilherme.anotaplus.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "metas")
data class Meta(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nome: String,
    val valorAlvo: Double,
    val valorAtual: Double = 0.0,
    val criadoEm: Long = System.currentTimeMillis(),
    // Data-alvo opcional (epoch millis) — null se o usuário não definiu
    // prazo. Vale pra qualquer Meta, criada no onboarding ou depois pela
    // aba Acompanhamento (migração 8->9).
    val prazo: Long? = null
)
```

- [ ] **Step 2: Add the migration**

Edit `app/src/main/java/com/guilherme/anotaplus/data/AppDatabase.kt`, insert after `MIGRATION_7_8` (after line 81, before the `@Database` annotation):

```kotlin

// v8 -> v9: adiciona prazo (Long?, epoch millis) em metas — data-alvo
// opcional pra qualquer Meta nomeada, definida no onboarding ou depois pela
// aba Acompanhamento.
val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE metas ADD COLUMN prazo INTEGER")
    }
}
```

Then update the `@Database` version (line 83) and the migration list (line 104):

```kotlin
@Database(entities = [Entry::class, Category::class, Carteira::class, Meta::class], version = 9, exportSchema = false)
```

```kotlin
                    .addMigrations(MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9)
```

- [ ] **Step 3: Commit and push**

```bash
git add app/src/main/java/com/guilherme/anotaplus/data/Meta.kt app/src/main/java/com/guilherme/anotaplus/data/AppDatabase.kt
git commit -m "Adiciona campo prazo em Meta (migracao Room 8 para 9)"
git push
```

- [ ] **Step 4: Verify**

Confirm GitHub Actions build is green (KSP regenerates the Room schema at compile time — a malformed migration/entity fails `assembleDebug`, not silently). No instrumented migration test exists in this repo (would need an emulator in CI, out of scope) — the ALTER TABLE is a single nullable-column addition, the lowest-risk Room migration shape, so manual review substitutes for an automated migration test here.

---

### Task 6: Prazo opcional na Meta existente (dialog do Acompanhamento)

**Files:**
- Modify: `app/src/main/res/layout/dialog_nova_meta.xml`
- Modify: `app/src/main/java/com/guilherme/anotaplus/ReportActivity.kt:314-328`
- Create: `app/src/main/res/drawable/bg_chip_prazo.xml`
- Create: `app/src/main/res/color/chip_prazo_text.xml`
- Modify: `app/src/main/res/values/strings.xml`

**Interfaces:**
- Consumes: `MetaCalculo.prazoEmMeses`, `MetaCalculo.valorMensalNecessario` (Task 4), `Meta.prazo` (Task 5).
- Produces: nothing new consumed elsewhere — this is the "Metas fora do onboarding" side of the spec's "vale pra qualquer Meta" requirement.

- [ ] **Step 1: Add the chip selector drawables**

Create `app/src/main/res/drawable/bg_chip_prazo.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<selector xmlns:android="http://schemas.android.com/apk/res/android">
    <item android:state_selected="true">
        <shape android:shape="rectangle">
            <solid android:color="@color/paper" />
            <stroke android:width="1.5dp" android:color="@color/verde_destaque" />
            <corners android:radius="@dimen/radius_pill" />
        </shape>
    </item>
    <item>
        <shape android:shape="rectangle">
            <solid android:color="@color/paper" />
            <stroke android:width="1dp" android:color="@color/paper_dim" />
            <corners android:radius="@dimen/radius_pill" />
        </shape>
    </item>
</selector>
```

Create `app/src/main/res/color/chip_prazo_text.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<selector xmlns:android="http://schemas.android.com/apk/res/android">
    <item android:state_selected="true" android:color="@color/verde_destaque" />
    <item android:color="@color/ink" />
</selector>
```

- [ ] **Step 2: Add prazo strings**

Edit `app/src/main/res/values/strings.xml`, add near the existing `hint_valor_alvo_meta`/`titulo_nova_meta` strings:

```xml
    <string name="label_prazo_meta">Prazo (opcional)</string>
    <string name="chip_prazo_6_meses">6 meses</string>
    <string name="chip_prazo_1_ano">1 ano</string>
    <string name="chip_prazo_escolher">Escolher</string>
    <string name="formato_valor_mensal_meta">≈ %s/mês pra chegar lá</string>
```

- [ ] **Step 3: Add the prazo UI to the dialog layout**

Edit `app/src/main/res/layout/dialog_nova_meta.xml`, insert between the `edit_valor_alvo_meta` `EditText` (ends line 37) and the buttons `LinearLayout` (starts line 39):

```xml

    <TextView
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_marginTop="20dp"
        android:text="@string/label_prazo_meta"
        android:textAppearance="@style/TextAppearance.AnotaPlus.Label" />

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_marginTop="8dp"
        android:orientation="horizontal">

        <TextView
            android:id="@+id/chip_prazo_6_meses"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_weight="1"
            android:layout_marginEnd="6dp"
            android:background="@drawable/bg_chip_prazo"
            android:textColor="@color/chip_prazo_text"
            android:gravity="center"
            android:padding="8dp"
            android:textSize="12sp"
            android:textStyle="bold"
            android:clickable="true"
            android:focusable="true"
            android:text="@string/chip_prazo_6_meses" />

        <TextView
            android:id="@+id/chip_prazo_1_ano"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_weight="1"
            android:layout_marginEnd="6dp"
            android:background="@drawable/bg_chip_prazo"
            android:textColor="@color/chip_prazo_text"
            android:gravity="center"
            android:padding="8dp"
            android:textSize="12sp"
            android:textStyle="bold"
            android:clickable="true"
            android:focusable="true"
            android:text="@string/chip_prazo_1_ano" />

        <TextView
            android:id="@+id/chip_prazo_escolher"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_weight="1"
            android:background="@drawable/bg_chip_prazo"
            android:textColor="@color/chip_prazo_text"
            android:gravity="center"
            android:padding="8dp"
            android:textSize="12sp"
            android:textStyle="bold"
            android:clickable="true"
            android:focusable="true"
            android:text="@string/chip_prazo_escolher" />

    </LinearLayout>

    <TextView
        android:id="@+id/text_preview_valor_mensal"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_marginTop="10dp"
        android:gravity="center"
        android:textColor="@color/verde_destaque"
        android:textStyle="bold"
        android:textSize="13sp"
        android:visibility="gone"
        tools:text="≈ R$ 166,67/mês pra chegar lá"
        tools:visibility="visible" />
```

Also add `xmlns:tools` to the file's root tag if not already present — check first: `dialog_nova_meta.xml`'s current root `<LinearLayout ...>` (line 2-6) has no `xmlns:tools`. Add it:

```xml
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:orientation="vertical"
    android:padding="24dp">
```

- [ ] **Step 4: Wire the prazo chips and preview in `ReportActivity.abrirDialogNovaMeta`**

Edit `app/src/main/java/com/guilherme/anotaplus/ReportActivity.kt`, replace the whole `abrirDialogNovaMeta` function (lines 314-328):

```kotlin
    private fun abrirDialogNovaMeta(metaDao: MetaDao) {
        val dialogBinding = DialogNovaMetaBinding.inflate(layoutInflater)
        val dialog = criarDialogoFlat(dialogBinding.root)
        var prazoSelecionado: Long? = null

        fun chips() = listOf(
            dialogBinding.chipPrazo6Meses,
            dialogBinding.chipPrazo1Ano,
            dialogBinding.chipPrazoEscolher
        )

        fun atualizarPreview() {
            val valorAlvo = dialogBinding.editValorAlvoMeta.text.toString()
                .replace(",", ".").toDoubleOrNull()
            val valorMensal = if (valorAlvo != null) {
                MetaCalculo.valorMensalNecessario(valorAlvo, 0.0, prazoSelecionado, System.currentTimeMillis())
            } else {
                null
            }
            if (valorMensal != null) {
                dialogBinding.textPreviewValorMensal.text = getString(
                    R.string.formato_valor_mensal_meta,
                    "R$ %.2f".format(locale, valorMensal)
                )
                dialogBinding.textPreviewValorMensal.visibility = View.VISIBLE
            } else {
                dialogBinding.textPreviewValorMensal.visibility = View.GONE
            }
        }

        fun selecionar(chipSelecionado: View) {
            chips().forEach { it.isSelected = it == chipSelecionado }
        }

        dialogBinding.chipPrazo6Meses.setOnClickListener {
            prazoSelecionado = MetaCalculo.prazoEmMeses(System.currentTimeMillis(), 6)
            selecionar(dialogBinding.chipPrazo6Meses)
            atualizarPreview()
        }
        dialogBinding.chipPrazo1Ano.setOnClickListener {
            prazoSelecionado = MetaCalculo.prazoEmMeses(System.currentTimeMillis(), 12)
            selecionar(dialogBinding.chipPrazo1Ano)
            atualizarPreview()
        }
        dialogBinding.chipPrazoEscolher.setOnClickListener {
            val agora = Calendar.getInstance()
            DatePickerDialog(
                this,
                { _, ano, mes, dia ->
                    val escolhido = Calendar.getInstance().apply { set(ano, mes, dia, 0, 0, 0) }
                    prazoSelecionado = escolhido.timeInMillis
                    selecionar(dialogBinding.chipPrazoEscolher)
                    atualizarPreview()
                },
                agora.get(Calendar.YEAR),
                agora.get(Calendar.MONTH),
                agora.get(Calendar.DAY_OF_MONTH)
            ).show()
        }
        dialogBinding.editValorAlvoMeta.addTextChangedListener(afterTextChanged = { atualizarPreview() })

        dialogBinding.btnCancelarMeta.setOnClickListener { dialog.dismiss() }
        dialogBinding.btnSalvarNovaMeta.setOnClickListener {
            val nome = dialogBinding.editNomeMeta.text.toString().trim()
            val valorAlvo = dialogBinding.editValorAlvoMeta.text.toString()
                .replace(",", ".").toDoubleOrNull()
            if (nome.isNotEmpty() && valorAlvo != null && valorAlvo > 0) {
                lifecycleScope.launch {
                    metaDao.insert(Meta(nome = nome, valorAlvo = valorAlvo, prazo = prazoSelecionado))
                }
                dialog.dismiss()
            }
        }
        dialog.show()
    }
```

This needs two new imports at the top of `ReportActivity.kt` (add alongside the existing imports, e.g. after line 9 `import androidx.lifecycle.lifecycleScope`):

```kotlin
import android.app.DatePickerDialog
import androidx.core.widget.addTextChangedListener
```

- [ ] **Step 5: Commit and push**

```bash
git add app/src/main/res/layout/dialog_nova_meta.xml app/src/main/res/drawable/bg_chip_prazo.xml app/src/main/res/color/chip_prazo_text.xml app/src/main/res/values/strings.xml app/src/main/java/com/guilherme/anotaplus/ReportActivity.kt
git commit -m "Adiciona prazo opcional na criacao de Meta pelo Acompanhamento"
git push
```

- [ ] **Step 6: Verify**

Confirm GitHub Actions build is green. Manual QA (sideload `app-debug.apk`): open Início → "+ Nova meta" → confirm the 3 prazo chips render, tapping "6 meses"/"1 ano" highlights the tapped chip in green and shows the "≈ R$X/mês" preview once a valor-alvo is typed, "Escolher" opens the native date picker, and saving without touching any prazo chip still creates a Meta successfully (prazo stays null, no preview shown, no crash).

---

### Task 7: Progress-bar accent color — swap `color_receita` fill to `verde_destaque`

Two literal "progress bar fill" spots, per the spec's rule that verde-claro (not the money-semantic green) is the app's progress-indicator color.

**Files:**
- Modify: `app/src/main/res/layout/activity_report.xml:202`
- Modify: `app/src/main/res/layout/item_meta.xml:59`

**Interfaces:** none (pure resource attribute change).

- [ ] **Step 1: Swap the "Meta de economia alcançada" summary bar**

Edit `app/src/main/res/layout/activity_report.xml` line 202, change:

```xml
                    android:progressTint="@color/color_receita"
```
to:
```xml
                    android:progressTint="@color/verde_destaque"
```

Leave the adjacent `text_meta_economia_percentual` (line 188, `android:textColor="@color/color_receita"`) **unchanged** — that label is semantic (it echoes the "receita/poupança" framing in prose), only the bar fill itself moves to the accent.

- [ ] **Step 2: Swap the named-Meta row bar**

Edit `app/src/main/res/layout/item_meta.xml` line 59, change:

```xml
            android:background="@color/color_receita"
```
to:
```xml
            android:background="@color/verde_destaque"
```

(This is the `bar_fill_meta` View's background — the weighted fill of each Meta's own progress bar.)

- [ ] **Step 3: Commit and push**

```bash
git add app/src/main/res/layout/activity_report.xml app/src/main/res/layout/item_meta.xml
git commit -m "Troca cor de preenchimento das barras de progresso pro verde de destaque"
git push
```

- [ ] **Step 4: Verify**

Confirm GitHub Actions build is green. Manual QA: open Início, confirm the "Meta de economia alcançada" bar and each Meta row's bar render in the lighter/more vivid `verde_destaque` (#4CAF6D), visually distinct from the darker olive `color_receita` still used for "Receitas: R$X" text elsewhere on the same screen.

---

### Task 8: `SessionPrefs.getName()` + `Prefs` nome de exibição

**Files:**
- Modify: `app/src/main/java/com/guilherme/anotaplus/data/SessionPrefs.kt`
- Modify: `app/src/main/java/com/guilherme/anotaplus/data/Prefs.kt`

**Interfaces:**
- Produces: `SessionPrefs.getName(context): String?`, `Prefs.getNomeExibicao(context): String?`, `Prefs.setNomeExibicao(context, nome: String)` — consumed by Task 10 (prefill + save) and Task 19 (Início greeting).

- [ ] **Step 1: Add the Google-name getter**

Edit `app/src/main/java/com/guilherme/anotaplus/data/SessionPrefs.kt`, add after `getEmail` (line 26):

```kotlin
    fun getName(context: Context): String? = prefs(context).getString(KEY_NAME, null)
```

- [ ] **Step 2: Add the editable display-name pref**

Edit `app/src/main/java/com/guilherme/anotaplus/data/Prefs.kt`, add a new key constant after `KEY_DIAS_POR_SEMANA` (line 15):

```kotlin
    private const val KEY_NOME_EXIBICAO = "nome_exibicao"
```

Add the getter/setter at the end of the object, after `getValorHora` (before the closing `}` on line 112):

```kotlin

    // Apelido escolhido no onboarding ("Como quer ser chamado") — distinto
    // do nome da conta Google (SessionPrefs.getName), que só serve de
    // sugestão inicial pra esse campo. Usado nas saudações do mascote.
    fun getNomeExibicao(context: Context): String? =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString(KEY_NOME_EXIBICAO, null)

    fun setNomeExibicao(context: Context, nome: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
            putString(KEY_NOME_EXIBICAO, nome)
        }
    }
```

- [ ] **Step 3: Commit and push**

```bash
git add app/src/main/java/com/guilherme/anotaplus/data/SessionPrefs.kt app/src/main/java/com/guilherme/anotaplus/data/Prefs.kt
git commit -m "Adiciona getter de nome do Google e pref de nome de exibicao"
git push
```

- [ ] **Step 4: Verify**

Confirm GitHub Actions build is green. No UI consumes these yet (Task 10/19 do) — nothing to manually QA in isolation.

---

### Task 9: Shared onboarding header (`<include>` + binder)

Builds the "mascote pequeno + balão de pergunta + voltar + barra de progresso" component used by all 6 onboarding screens (Tasks 10-16), so its markup exists exactly once.

**Files:**
- Create: `app/src/main/res/layout/onboarding_header.xml`
- Create: `app/src/main/res/drawable/bg_balao_pergunta.xml`
- Create: `app/src/main/java/com/guilherme/anotaplus/OnboardingHeaderUtil.kt`

**Interfaces:**
- Produces: `fun OnboardingHeaderBinding.bind(activity: Activity, progresso: Float, pergunta: String, mascote: Int = R.drawable.ic_mascote_neutro, mostrarVoltar: Boolean = true)` — every onboarding screen's `onCreate` calls this once via `binding.onboardingHeader.bind(...)`.

- [ ] **Step 1: Create the question-bubble background**

Create `app/src/main/res/drawable/bg_balao_pergunta.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android" android:shape="rectangle">
    <solid android:color="@color/paper" />
    <stroke android:width="1.5dp" android:color="@color/verde_destaque" />
    <corners android:radius="@dimen/radius_chip" />
</shape>
```

- [ ] **Step 2: Create the shared header layout**

Create `app/src/main/res/layout/onboarding_header.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    xmlns:tools="http://schemas.android.com/tools"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:orientation="vertical">

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="horizontal"
        android:gravity="center_vertical">

        <ImageButton
            android:id="@+id/btn_voltar_onboarding"
            android:layout_width="32dp"
            android:layout_height="32dp"
            android:background="?attr/selectableItemBackgroundBorderless"
            android:contentDescription="@string/btn_voltar"
            android:src="@drawable/ic_chevron_left"
            app:tint="@color/ink" />

        <ProgressBar
            android:id="@+id/progress_onboarding"
            style="?android:attr/progressBarStyleHorizontal"
            android:layout_width="0dp"
            android:layout_height="4dp"
            android:layout_weight="1"
            android:layout_marginStart="10dp"
            android:max="100"
            android:progress="0"
            android:progressTint="@color/verde_destaque"
            android:progressBackgroundTint="@color/paper_dim"
            tools:progress="40" />

    </LinearLayout>

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_marginTop="@dimen/spacing_lg"
        android:orientation="horizontal"
        android:gravity="center_vertical">

        <ImageView
            android:id="@+id/image_mascote_onboarding"
            android:layout_width="36dp"
            android:layout_height="36dp"
            android:src="@drawable/ic_mascote_neutro"
            android:contentDescription="@string/mascote_content_description" />

        <TextView
            android:id="@+id/text_pergunta_onboarding"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_weight="1"
            android:layout_marginStart="10dp"
            android:background="@drawable/bg_balao_pergunta"
            android:padding="@dimen/spacing_sm"
            android:textAppearance="@style/TextAppearance.AnotaPlus.RowTitle"
            tools:text="Como você quer ser chamado?" />

    </LinearLayout>

</LinearLayout>
```

Add the content-description string to `app/src/main/res/values/strings.xml`:

```xml
    <string name="mascote_content_description">Mascote do Anota+</string>
```

- [ ] **Step 3: Create the binder helper**

Create `app/src/main/java/com/guilherme/anotaplus/OnboardingHeaderUtil.kt`:

```kotlin
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
```

- [ ] **Step 4: Commit and push**

```bash
git add app/src/main/res/layout/onboarding_header.xml app/src/main/res/drawable/bg_balao_pergunta.xml app/src/main/java/com/guilherme/anotaplus/OnboardingHeaderUtil.kt app/src/main/res/values/strings.xml
git commit -m "Adiciona cabecalho compartilhado de onboarding (mascote + balao + progresso)"
git push
```

- [ ] **Step 5: Verify**

Confirm GitHub Actions build is green. Nothing `<include>`s this layout yet (Tasks 10-16 do) — no manual QA possible in isolation, but a compile error here (e.g. a typo'd id) would only surface once a consuming layout is added, so double-check the XML by eye before moving on.

---

### Task 10: `NomeOnboardingActivity` — "Como quer ser chamado" (new screen)

**Files:**
- Create: `app/src/main/res/layout/activity_nome_onboarding.xml`
- Create: `app/src/main/java/com/guilherme/anotaplus/NomeOnboardingActivity.kt`
- Modify: `app/src/main/AndroidManifest.xml`
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/java/com/guilherme/anotaplus/LoginActivity.kt:59-73`

**Interfaces:**
- Consumes: `OnboardingHeaderBinding.bind` (Task 9), `SessionPrefs.getName`/`Prefs.setNomeExibicao` (Task 8).
- Produces: chains to `SalarioOnboardingActivity`, forwarding `QuickAccessFlow.EXTRA_ONBOARDING`/`EXTRA_ABRIR_HISTORICO` exactly like `LoginActivity` did before this task.

- [ ] **Step 1: Add strings**

Edit `app/src/main/res/values/strings.xml`, add near the other onboarding strings:

```xml
    <string name="titulo_nome_onboarding">Como você quer ser chamado?</string>
    <string name="hint_nome_exibicao">Seu nome</string>
    <string name="desc_nome_sugerido">sugerido a partir da sua conta Google — edite se quiser</string>
    <string name="error_valor_obrigatorio">Preenche esse campo</string>
```

(`error_valor_obrigatorio` may already exist — `SalarioOnboardingActivity.kt:44` references `R.string.error_valor_obrigatorio`. Check first with a grep; only add it if missing.)

- [ ] **Step 2: Create the layout**

Create `app/src/main/res/layout/activity_nome_onboarding.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="@color/paper"
    android:orientation="vertical"
    android:padding="@dimen/spacing_lg">

    <include
        android:id="@+id/onboarding_header"
        layout="@layout/onboarding_header" />

    <EditText
        android:id="@+id/edit_nome_exibicao"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_marginTop="@dimen/spacing_lg"
        android:background="@drawable/bg_underline_field"
        android:hint="@string/hint_nome_exibicao"
        android:inputType="textPersonName|textCapWords"
        android:textColor="@color/ink"
        android:textColorHint="@color/ink_soft"
        android:textAppearance="@style/TextAppearance.AnotaPlus.AmountBold" />

    <TextView
        android:id="@+id/text_nome_sugerido"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_marginTop="6dp"
        android:text="@string/desc_nome_sugerido"
        android:textAppearance="@style/TextAppearance.AnotaPlus.Eyebrow"
        android:visibility="gone"
        tools:visibility="visible" />

    <View
        android:layout_width="0dp"
        android:layout_height="0dp"
        android:layout_weight="1" />

    <com.google.android.material.button.MaterialButton
        android:id="@+id/btn_continuar_nome"
        style="@style/Widget.AnotaPlus.Button.Primary"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text="@string/btn_continuar" />

</LinearLayout>
```

- [ ] **Step 3: Create the Activity**

Create `app/src/main/java/com/guilherme/anotaplus/NomeOnboardingActivity.kt`:

```kotlin
package com.guilherme.anotaplus

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.guilherme.anotaplus.data.Prefs
import com.guilherme.anotaplus.data.SessionPrefs
import com.guilherme.anotaplus.databinding.ActivityNomeOnboardingBinding

// Segunda tela do onboarding (logo após o Login): pergunta como o usuário
// quer ser chamado, pré-preenchido com o nome vindo da conta Google
// (SessionPrefs.getName) — campo separado, editável, guardado à parte
// (Prefs.setNomeExibicao) e usado depois nas saudações do mascote (ver
// ReportActivity). Repassa EXTRA_ONBOARDING/EXTRA_ABRIR_HISTORICO adiante
// pra SalarioOnboardingActivity, seguindo o mesmo padrão de QuickAccessFlow.
class NomeOnboardingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNomeOnboardingBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNomeOnboardingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.onboardingHeader.bind(
            activity = this,
            progresso = 0.25f,
            pergunta = getString(R.string.titulo_nome_onboarding)
        )

        val nomeGoogle = SessionPrefs.getName(this)
        if (!nomeGoogle.isNullOrBlank()) {
            binding.editNomeExibicao.setText(nomeGoogle)
            binding.textNomeSugerido.visibility = View.VISIBLE
        }

        binding.btnContinuarNome.setOnClickListener {
            val nome = binding.editNomeExibicao.text.toString().trim()
            if (nome.isEmpty()) {
                binding.editNomeExibicao.error = getString(R.string.error_valor_obrigatorio)
                return@setOnClickListener
            }
            Prefs.setNomeExibicao(this, nome)
            concluir()
        }
    }

    private fun concluir() {
        startActivity(
            Intent(this, SalarioOnboardingActivity::class.java)
                .putExtra(QuickAccessFlow.EXTRA_ONBOARDING, intent.getBooleanExtra(QuickAccessFlow.EXTRA_ONBOARDING, false))
                .putExtra(QuickAccessFlow.EXTRA_ABRIR_HISTORICO, intent.getBooleanExtra(QuickAccessFlow.EXTRA_ABRIR_HISTORICO, false))
        )
        finish()
    }
}
```

- [ ] **Step 4: Add the manifest entry**

Edit `app/src/main/AndroidManifest.xml`. Update the onboarding-sequence comment (lines 123-136) to mention the two new screens, and insert `NomeOnboardingActivity`'s block right after `LoginActivity`'s (after line 145):

```xml
        <!--
            Sequência de onboarding (só aparece na primeiríssima abertura do
            app, controlado por Prefs.isOnboardingConcluido):
            QuickCaptureActivity -> LoginActivity -> NomeOnboardingActivity
            (como quer ser chamado) -> SalarioOnboardingActivity (dados
            salariais, só na criação da conta) -> MetaOnboardingActivity
            (primeira Meta, pulável) -> QuickAccessChooserActivity ->
            tutorial de cada método escolhido (GestureGuideActivity e/ou
            WidgetSuggestionActivity, encadeados por QuickAccessFlow.kt) ->
            AnotacoesActivity (com o tour de tutorial "spotlight" ativo, ver
            TutorialTourManager.kt).
            Cada uma também pode ser aberta isoladamente depois (fora do
            onboarding) a partir de Configurações — SalarioOnboardingActivity
            e MetaOnboardingActivity são exceção: não são reabertas fora do
            onboarding (dados salariais reabrem via dialog em
            SettingsActivity; novas Metas depois se criam por
            ReportActivity.abrirDialogNovaMeta).
        -->
        <activity
            android:name=".GestureGuideActivity"
            android:exported="false"
            android:theme="@style/Theme.AnotaPlus" />

        <activity
            android:name=".LoginActivity"
            android:exported="false"
            android:theme="@style/Theme.AnotaPlus" />

        <activity
            android:name=".NomeOnboardingActivity"
            android:exported="false"
            android:theme="@style/Theme.AnotaPlus" />

        <activity
            android:name=".SalarioOnboardingActivity"
            android:exported="false"
            android:theme="@style/Theme.AnotaPlus" />
```

(The rest of the manifest — `QuickAccessChooserActivity`, `CalculadoraCompraActivity`, `WidgetSuggestionActivity` blocks at lines 152-166 — stays as-is; `MetaOnboardingActivity`'s own block is added in Task 11, right after `SalarioOnboardingActivity`'s.)

- [ ] **Step 5: Wire `LoginActivity` to start the new screen**

Edit `app/src/main/java/com/guilherme/anotaplus/LoginActivity.kt`, replace `concluir()` (lines 59-73):

```kotlin
    private fun concluir() {
        if (intent.getBooleanExtra(QuickAccessFlow.EXTRA_ONBOARDING, false)) {
            Prefs.marcarOnboardingConcluido(this)
            val abrirHistorico = intent.getBooleanExtra(QuickAccessFlow.EXTRA_ABRIR_HISTORICO, false)
            // Conta acabou de ser criada: pergunta como a pessoa quer ser
            // chamada antes de seguir pro resto do onboarding —
            // NomeOnboardingActivity encadeia SalarioOnboardingActivity na
            // sequência.
            startActivity(
                Intent(this, NomeOnboardingActivity::class.java)
                    .putExtra(QuickAccessFlow.EXTRA_ONBOARDING, true)
                    .putExtra(QuickAccessFlow.EXTRA_ABRIR_HISTORICO, abrirHistorico)
            )
        }
        finish()
    }
```

- [ ] **Step 6: Commit and push**

```bash
git add app/src/main/res/layout/activity_nome_onboarding.xml app/src/main/java/com/guilherme/anotaplus/NomeOnboardingActivity.kt app/src/main/AndroidManifest.xml app/src/main/res/values/strings.xml app/src/main/java/com/guilherme/anotaplus/LoginActivity.kt
git commit -m "Adiciona tela de onboarding 'Como quer ser chamado'"
git push
```

- [ ] **Step 7: Verify**

Confirm GitHub Actions build is green. Manual QA: uninstall/reinstall the sideloaded app (or clear its data) to re-trigger onboarding, log in with Google, confirm this new screen appears right after login with the Google account's given name pre-filled and the "sugerido a partir da sua conta Google" hint visible, typing a different name and continuing proceeds to the (still old-style, until Task 13) salary screen.

---

### Task 11: `MetaOnboardingActivity` — "Qual sua primeira meta?" (new screen, prazo opcional, pulável)

**Files:**
- Create: `app/src/main/res/layout/activity_meta_onboarding.xml`
- Create: `app/src/main/java/com/guilherme/anotaplus/MetaOnboardingActivity.kt`
- Modify: `app/src/main/AndroidManifest.xml`
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/java/com/guilherme/anotaplus/SalarioOnboardingActivity.kt:89-96`

**Interfaces:**
- Consumes: `OnboardingHeaderBinding.bind` (Task 9), `MetaCalculo` (Task 4), `Meta`/`MetaDao` (Task 5, existing), `bg_chip_prazo`/`chip_prazo_text` (Task 6 — reused, not duplicated).
- Produces: chains to `QuickAccessChooserActivity`, forwarding the same two extras `SalarioOnboardingActivity` forwarded before this task — whether the user creates a Meta or taps "Pular por agora".

- [ ] **Step 1: Add strings**

Edit `app/src/main/res/values/strings.xml`, add near the Meta-related strings from Task 6:

```xml
    <string name="titulo_meta_onboarding">Qual sua primeira meta?</string>
    <string name="btn_criar_meta_onboarding">Criar meta</string>
    <string name="btn_pular_meta_onboarding">Pular por agora</string>
```

(`hint_nome_meta` and `hint_valor_alvo_meta` already exist from `dialog_nova_meta.xml` — reuse them verbatim, don't duplicate.)

- [ ] **Step 2: Create the layout**

Create `app/src/main/res/layout/activity_meta_onboarding.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.core.widget.NestedScrollView xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="@color/paper"
    android:fillViewport="true">

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="vertical"
        android:padding="@dimen/spacing_lg">

        <include
            android:id="@+id/onboarding_header"
            layout="@layout/onboarding_header" />

        <EditText
            android:id="@+id/edit_nome_meta"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="@dimen/spacing_lg"
            android:background="@drawable/bg_underline_field"
            android:hint="@string/hint_nome_meta"
            android:inputType="text"
            android:textColor="@color/ink"
            android:textColorHint="@color/ink_soft"
            android:textAppearance="@style/TextAppearance.AnotaPlus.Body" />

        <EditText
            android:id="@+id/edit_valor_alvo_meta"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="@dimen/spacing_md"
            android:background="@drawable/bg_underline_field"
            android:hint="@string/hint_valor_alvo_meta"
            android:inputType="numberDecimal"
            android:textColor="@color/ink"
            android:textColorHint="@color/ink_soft"
            android:textAppearance="@style/TextAppearance.AnotaPlus.AmountBold" />

        <TextView
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="@dimen/spacing_lg"
            android:text="@string/label_prazo_meta"
            android:textAppearance="@style/TextAppearance.AnotaPlus.Label" />

        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="@dimen/spacing_xs"
            android:orientation="horizontal">

            <TextView
                android:id="@+id/chip_prazo_6_meses"
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_weight="1"
                android:layout_marginEnd="6dp"
                android:background="@drawable/bg_chip_prazo"
                android:textColor="@color/chip_prazo_text"
                android:gravity="center"
                android:padding="@dimen/spacing_xs"
                android:textSize="12sp"
                android:textStyle="bold"
                android:clickable="true"
                android:focusable="true"
                android:text="@string/chip_prazo_6_meses" />

            <TextView
                android:id="@+id/chip_prazo_1_ano"
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_weight="1"
                android:layout_marginEnd="6dp"
                android:background="@drawable/bg_chip_prazo"
                android:textColor="@color/chip_prazo_text"
                android:gravity="center"
                android:padding="@dimen/spacing_xs"
                android:textSize="12sp"
                android:textStyle="bold"
                android:clickable="true"
                android:focusable="true"
                android:text="@string/chip_prazo_1_ano" />

            <TextView
                android:id="@+id/chip_prazo_escolher"
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_weight="1"
                android:background="@drawable/bg_chip_prazo"
                android:textColor="@color/chip_prazo_text"
                android:gravity="center"
                android:padding="@dimen/spacing_xs"
                android:textSize="12sp"
                android:textStyle="bold"
                android:clickable="true"
                android:focusable="true"
                android:text="@string/chip_prazo_escolher" />

        </LinearLayout>

        <TextView
            android:id="@+id/text_preview_valor_mensal"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="@dimen/spacing_sm"
            android:gravity="center"
            android:textColor="@color/verde_destaque"
            android:textStyle="bold"
            android:textSize="13sp"
            android:visibility="gone"
            tools:text="≈ R$ 166,67/mês pra chegar lá"
            tools:visibility="visible" />

        <com.google.android.material.button.MaterialButton
            android:id="@+id/btn_criar_meta"
            style="@style/Widget.AnotaPlus.Button.Primary"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="@dimen/spacing_lg"
            android:text="@string/btn_criar_meta_onboarding" />

        <com.google.android.material.button.MaterialButton
            android:id="@+id/btn_pular_meta"
            style="@style/Widget.AnotaPlus.Button.Link"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="@dimen/spacing_xs"
            android:text="@string/btn_pular_meta_onboarding" />

    </LinearLayout>

</androidx.core.widget.NestedScrollView>
```

- [ ] **Step 3: Create the Activity**

Create `app/src/main/java/com/guilherme/anotaplus/MetaOnboardingActivity.kt`:

```kotlin
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
```

- [ ] **Step 4: Add the manifest entry**

Edit `app/src/main/AndroidManifest.xml`, insert right after `SalarioOnboardingActivity`'s block (added in Task 10 step 4) and before `QuickAccessChooserActivity`'s existing block:

```xml
        <activity
            android:name=".MetaOnboardingActivity"
            android:exported="false"
            android:theme="@style/Theme.AnotaPlus" />

        <activity
            android:name=".QuickAccessChooserActivity"
            android:exported="false"
            android:theme="@style/Theme.AnotaPlus" />
```

(Only the new `MetaOnboardingActivity` block is new here — `QuickAccessChooserActivity`'s block already exists, shown for anchoring.)

- [ ] **Step 5: Wire `SalarioOnboardingActivity` to start the new screen**

Edit `app/src/main/java/com/guilherme/anotaplus/SalarioOnboardingActivity.kt`, replace `concluir()` (lines 89-96):

```kotlin
    private fun concluir() {
        startActivity(
            Intent(this, MetaOnboardingActivity::class.java)
                .putExtra(QuickAccessFlow.EXTRA_ONBOARDING, onboarding)
                .putExtra(QuickAccessFlow.EXTRA_ABRIR_HISTORICO, abrirHistorico)
        )
        finish()
    }
```

- [ ] **Step 6: Commit and push**

```bash
git add app/src/main/res/layout/activity_meta_onboarding.xml app/src/main/java/com/guilherme/anotaplus/MetaOnboardingActivity.kt app/src/main/AndroidManifest.xml app/src/main/res/values/strings.xml app/src/main/java/com/guilherme/anotaplus/SalarioOnboardingActivity.kt
git commit -m "Adiciona tela de onboarding 'Qual sua primeira meta' com prazo opcional"
git push
```

- [ ] **Step 7: Verify**

Confirm GitHub Actions build is green. Manual QA: run onboarding through to this new screen, confirm filling nome+valor and tapping "6 meses" highlights the chip and shows the preview, "Criar meta" proceeds to the shortcut-choice screen and the new Meta shows up later in Início's Metas list with the right prazo; separately, re-run onboarding and tap "Pular por agora" with no fields filled — confirm it proceeds without creating a Meta and without crashing.

---

### Task 12: Migrate `LoginActivity` to the new onboarding visual pattern

**Files:**
- Modify: `app/src/main/res/layout/activity_login.xml`
- Modify: `app/src/main/java/com/guilherme/anotaplus/LoginActivity.kt`
- Modify: `app/src/main/res/values/strings.xml`

**Interfaces:**
- Consumes: `OnboardingHeaderBinding.bind` (Task 9).

- [ ] **Step 1: Add the bubble-question string**

Edit `app/src/main/res/values/strings.xml`:

```xml
    <string name="titulo_login_onboarding">Pronto pra começar?</string>
```

- [ ] **Step 2: Replace the layout**

Replace the entire contents of `app/src/main/res/layout/activity_login.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.core.widget.NestedScrollView xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="@color/paper"
    android:fillViewport="true">

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:orientation="vertical"
        android:padding="@dimen/spacing_lg"
        android:gravity="center_vertical">

        <include
            android:id="@+id/onboarding_header"
            layout="@layout/onboarding_header" />

        <TextView
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="@dimen/spacing_lg"
            android:gravity="center"
            android:text="@string/app_name"
            android:textAppearance="@style/TextAppearance.AnotaPlus.Heading"
            android:textSize="26sp" />

        <TextView
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="4dp"
            android:gravity="center"
            android:fontFamily="monospace"
            android:textColor="@color/ink_soft"
            android:letterSpacing="0.04"
            android:textSize="11sp"
            android:text="@string/login_tagline" />

        <TextView
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="18dp"
            android:gravity="center"
            android:text="@string/login_pitch"
            android:textAppearance="@style/TextAppearance.AnotaPlus.Body"
            android:textColor="@color/ink_soft" />

        <TextView
            android:id="@+id/text_status"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="14dp"
            android:gravity="center"
            android:textAppearance="@style/TextAppearance.AnotaPlus.Eyebrow"
            android:visibility="gone"
            tools:text="Entrando…"
            tools:visibility="visible" />

        <com.google.android.material.button.MaterialButton
            android:id="@+id/btn_entrar_google"
            style="@style/Widget.AnotaPlus.Button.Stamp"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="20dp"
            android:text="@string/btn_entrar_google" />

        <com.google.android.material.button.MaterialButton
            android:id="@+id/link_ver_planos"
            style="@style/Widget.AnotaPlus.Button.Link"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_gravity="center_horizontal"
            android:layout_marginTop="6dp"
            android:text="@string/link_ver_planos" />

    </LinearLayout>

</androidx.core.widget.NestedScrollView>
```

This drops `bg_card_paper`, the `scrim` background, and both `PerforationView`s — Login no longer looks like a "recibo emitido", matching the other 5 onboarding screens. The Google sign-in button keeps its current `Widget.AnotaPlus.Button.Stamp` style unchanged (brass accent) — per the spec, the sign-in action stays visually distinct rather than becoming another `ink` "Continuar" pill.

- [ ] **Step 3: Bind the header in `LoginActivity.onCreate`**

Edit `app/src/main/java/com/guilherme/anotaplus/LoginActivity.kt`, add one call right after `setContentView(binding.root)` (line 22):

```kotlin
        binding.onboardingHeader.bind(
            activity = this,
            progresso = 0.10f,
            pergunta = getString(R.string.titulo_login_onboarding),
            mostrarVoltar = false
        )
```

- [ ] **Step 4: Commit and push**

```bash
git add app/src/main/res/layout/activity_login.xml app/src/main/java/com/guilherme/anotaplus/LoginActivity.kt app/src/main/res/values/strings.xml
git commit -m "Migra LoginActivity pro padrao visual novo de onboarding"
git push
```

- [ ] **Step 5: Verify**

Confirm GitHub Actions build is green. Manual QA: re-trigger onboarding, confirm Login now shows the mascote+balão header at ~10% progress with no perforated-paper card, the rest of the screen (wordmark, tagline, pitch, Google button, ver planos link) still renders and functions exactly as before.

---

### Task 13: Migrate `SalarioOnboardingActivity` to the new onboarding visual pattern

**Files:**
- Modify: `app/src/main/res/layout/activity_salario_onboarding.xml`
- Modify: `app/src/main/java/com/guilherme/anotaplus/SalarioOnboardingActivity.kt`

**Interfaces:**
- Consumes: `OnboardingHeaderBinding.bind` (Task 9).

- [ ] **Step 1: Replace the layout**

Replace the entire contents of `app/src/main/res/layout/activity_salario_onboarding.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="@color/paper"
    android:orientation="vertical"
    android:padding="@dimen/spacing_lg">

    <include
        android:id="@+id/onboarding_header"
        layout="@layout/onboarding_header" />

    <FrameLayout
        android:layout_width="match_parent"
        android:layout_height="0dp"
        android:layout_marginTop="@dimen/spacing_lg"
        android:layout_weight="1">

        <!-- Passo 1: salário mensal -->
        <LinearLayout
            android:id="@+id/pagina_salario"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_gravity="center_vertical"
            android:orientation="vertical">

            <TextView
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:text="@string/desc_onboarding_salario"
                android:textAppearance="@style/TextAppearance.AnotaPlus.Body"
                android:textColor="@color/ink_soft" />

            <EditText
                android:id="@+id/edit_salario_mensal"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:layout_marginTop="@dimen/spacing_lg"
                android:background="@drawable/bg_underline_field"
                android:hint="@string/hint_salario_mensal"
                android:inputType="numberDecimal"
                android:textColor="@color/ink"
                android:textColorHint="@color/ink_soft"
                android:textAppearance="@style/TextAppearance.AnotaPlus.AmountBold" />

        </LinearLayout>

        <!-- Passo 2: horas por dia -->
        <LinearLayout
            android:id="@+id/pagina_horas"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_gravity="center_vertical"
            android:orientation="vertical"
            android:visibility="gone">

            <EditText
                android:id="@+id/edit_horas_por_dia"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:background="@drawable/bg_underline_field"
                android:hint="@string/hint_horas_por_dia"
                android:inputType="numberDecimal"
                android:textColor="@color/ink"
                android:textColorHint="@color/ink_soft"
                android:textAppearance="@style/TextAppearance.AnotaPlus.AmountBold" />

        </LinearLayout>

        <!-- Passo 3: dias por semana -->
        <LinearLayout
            android:id="@+id/pagina_dias"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_gravity="center_vertical"
            android:orientation="vertical"
            android:visibility="gone">

            <EditText
                android:id="@+id/edit_dias_por_semana"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:background="@drawable/bg_underline_field"
                android:hint="@string/hint_dias_por_semana"
                android:inputType="numberDecimal"
                android:textColor="@color/ink"
                android:textColorHint="@color/ink_soft"
                android:textAppearance="@style/TextAppearance.AnotaPlus.AmountBold" />

        </LinearLayout>

    </FrameLayout>

    <com.google.android.material.button.MaterialButton
        android:id="@+id/btn_proximo_onboarding"
        style="@style/Widget.AnotaPlus.Button.Primary"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text="@string/btn_proximo_onboarding"
        tools:text="Próximo" />

    <com.google.android.material.button.MaterialButton
        android:id="@+id/btn_pular_onboarding_salario"
        style="@style/Widget.AnotaPlus.Button.Link"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_marginTop="@dimen/spacing_xs"
        android:text="@string/btn_pular_onboarding_salario" />

</LinearLayout>
```

This drops the 3-dot indicator row (the shared header's progress bar replaces it) and each page's own repeated heading (the header's balão now carries that copy) — only the input fields and the one shared description line remain per page.

- [ ] **Step 2: Update `atualizarPasso()` to drive the shared header instead of the dots**

Edit `app/src/main/java/com/guilherme/anotaplus/SalarioOnboardingActivity.kt`, replace `atualizarPasso()` (lines 76-87):

```kotlin
    private fun atualizarPasso() {
        binding.paginaSalario.visibility = if (passo == 0) View.VISIBLE else View.GONE
        binding.paginaHoras.visibility = if (passo == 1) View.VISIBLE else View.GONE
        binding.paginaDias.visibility = if (passo == 2) View.VISIBLE else View.GONE

        val pergunta = when (passo) {
            0 -> getString(R.string.titulo_onboarding_salario_1)
            1 -> getString(R.string.titulo_onboarding_salario_2)
            else -> getString(R.string.titulo_onboarding_salario_3)
        }
        binding.onboardingHeader.bind(activity = this, progresso = 0.40f, pergunta = pergunta)

        binding.btnProximoOnboarding.text =
            if (passo == 2) getString(R.string.btn_concluir_onboarding) else getString(R.string.btn_proximo_onboarding)
    }
```

(The progress fraction stays at `0.40f` across all 3 sub-steps — this one macro-step of the onboarding sequence, not each internal page, gets its own band; see the comment in `OnboardingHeaderUtil.kt`.)

- [ ] **Step 3: Commit and push**

```bash
git add app/src/main/res/layout/activity_salario_onboarding.xml app/src/main/java/com/guilherme/anotaplus/SalarioOnboardingActivity.kt
git commit -m "Migra SalarioOnboardingActivity pro padrao visual novo de onboarding"
git push
```

- [ ] **Step 4: Verify**

Confirm GitHub Actions build is green. Manual QA: continue onboarding to this screen, confirm the header's question bubble text changes correctly across all 3 sub-steps ("Quanto você ganha por mês?" → "Quantas horas..." → "Quantos dias..."), the progress bar stays put at 40% throughout (doesn't jump per sub-step), and the "Pular por enquanto" full-width button still skips straight to the next screen (now `MetaOnboardingActivity`).

---

### Task 14: Migrate `GestureGuideActivity` to the new onboarding visual pattern

**Files:**
- Modify: `app/src/main/res/layout/activity_gesture_guide.xml`
- Modify: `app/src/main/java/com/guilherme/anotaplus/GestureGuideActivity.kt`

**Interfaces:**
- Consumes: `OnboardingHeaderBinding.bind` (Task 9).

- [ ] **Step 1: Replace the layout**

Replace the entire contents of `app/src/main/res/layout/activity_gesture_guide.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.core.widget.NestedScrollView xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="@color/paper"
    android:fillViewport="true">

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="vertical"
        android:padding="@dimen/spacing_lg">

        <include
            android:id="@+id/onboarding_header"
            layout="@layout/onboarding_header" />

        <TextView
            android:id="@+id/text_intro"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="@dimen/spacing_lg"
            android:textAppearance="@style/TextAppearance.AnotaPlus.Body"
            android:textColor="@color/ink_soft"
            tools:text="@string/guia_gesto_intro_moto" />

        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="@dimen/spacing_lg"
            android:orientation="vertical">

            <TextView
                android:id="@+id/text_passo_1"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:layout_marginBottom="12dp"
                android:textAppearance="@style/TextAppearance.AnotaPlus.Body"
                tools:text="@string/guia_gesto_passo_1_moto" />

            <TextView
                android:id="@+id/text_passo_2"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:layout_marginBottom="12dp"
                android:textAppearance="@style/TextAppearance.AnotaPlus.Body"
                tools:text="@string/guia_gesto_passo_2_moto" />

            <TextView
                android:id="@+id/text_passo_3"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:layout_marginBottom="12dp"
                android:textAppearance="@style/TextAppearance.AnotaPlus.Body"
                tools:text="@string/guia_gesto_passo_3_moto" />

            <TextView
                android:id="@+id/text_passo_4"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:textAppearance="@style/TextAppearance.AnotaPlus.Body"
                tools:text="@string/guia_gesto_passo_4_moto" />

        </LinearLayout>

        <com.google.android.material.button.MaterialButton
            android:id="@+id/btn_entendi"
            style="@style/Widget.AnotaPlus.Button.Primary"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="@dimen/spacing_lg"
            android:text="@string/btn_entendi" />

        <com.google.android.material.button.MaterialButton
            android:id="@+id/btn_abrir_config_moto"
            style="@style/Widget.AnotaPlus.Button.Link"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="@dimen/spacing_xs"
            android:text="@string/btn_abrir_config_moto" />

    </LinearLayout>

</androidx.core.widget.NestedScrollView>
```

Both `PerforationView`s and the `Wordmark`-styled title are gone (the header's balão carries the screen's "question"); `btn_entendi` is promoted from `Button.Link` to `Button.Primary` (it's the screen's real forward-progress action), `btn_abrir_config_moto` is demoted from `Button.Stamp` to a full-width `Button.Link` (it's an optional side action, not the main CTA).

- [ ] **Step 2: Bind the header and set its bubble text per branch**

Edit `app/src/main/java/com/guilherme/anotaplus/GestureGuideActivity.kt`, replace the body of `onCreate` (lines 27-54) — keep the existing per-manufacturer text branching, add one header-bind call, keep the two listener lines at the end:

```kotlin
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGestureGuideBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.onboardingHeader.bind(
            activity = this,
            progresso = 0.90f,
            pergunta = getString(R.string.title_guia_gesto)
        )

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
            binding.btnAbrirConfigMoto.visibility = android.view.View.GONE
        }

        binding.btnAbrirConfigMoto.setOnClickListener { abrirConfiguracoesGesto() }
        binding.btnEntendi.setOnClickListener { continuar() }
    }
```

(`abrirConfiguracoesGesto()` and `continuar()`, lines 56-91, are untouched.)

- [ ] **Step 3: Commit and push**

```bash
git add app/src/main/res/layout/activity_gesture_guide.xml app/src/main/java/com/guilherme/anotaplus/GestureGuideActivity.kt
git commit -m "Migra GestureGuideActivity pro padrao visual novo de onboarding"
git push
```

- [ ] **Step 4: Verify**

Confirm GitHub Actions build is green. Manual QA: on both a Motorola device (if available) and any other Android device, reach this screen via onboarding (choosing "Gestos" in the shortcut picker) and confirm text/buttons render correctly for both branches, "Entendi" is now the prominent black pill and correctly advances the `QuickAccessFlow` chain, and reopening this screen standalone from Perfil ("guia de gesto") still just closes on "Entendi" without navigating anywhere (per the existing `QuickAccessFlow.temFilaNoIntent` check).

---

### Task 15: Migrate `WidgetSuggestionActivity` to the new onboarding visual pattern

**Files:**
- Modify: `app/src/main/res/layout/activity_widget_suggestion.xml`
- Modify: `app/src/main/java/com/guilherme/anotaplus/WidgetSuggestionActivity.kt`

**Interfaces:**
- Consumes: `OnboardingHeaderBinding.bind` (Task 9).

- [ ] **Step 1: Replace the layout**

Replace the entire contents of `app/src/main/res/layout/activity_widget_suggestion.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.core.widget.NestedScrollView xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="@color/paper"
    android:fillViewport="true">

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="vertical"
        android:padding="@dimen/spacing_lg">

        <include
            android:id="@+id/onboarding_header"
            layout="@layout/onboarding_header" />

        <TextView
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="@dimen/spacing_lg"
            android:text="@string/texto_widget_sugestao_intro"
            android:textAppearance="@style/TextAppearance.AnotaPlus.Body"
            android:textColor="@color/ink_soft" />

        <TextView
            android:id="@+id/text_widget_fallback"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="@dimen/spacing_lg"
            android:text="@string/texto_widget_sugestao_fallback"
            android:textAppearance="@style/TextAppearance.AnotaPlus.Body"
            android:visibility="gone"
            tools:visibility="visible" />

        <com.google.android.material.button.MaterialButton
            android:id="@+id/btn_concluir_widget"
            style="@style/Widget.AnotaPlus.Button.Primary"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="@dimen/spacing_lg"
            android:text="@string/btn_concluir" />

        <com.google.android.material.button.MaterialButton
            android:id="@+id/btn_adicionar_widget"
            style="@style/Widget.AnotaPlus.Button.Link"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="@dimen/spacing_xs"
            android:text="@string/btn_adicionar_widget_tela" />

    </LinearLayout>

</androidx.core.widget.NestedScrollView>
```

Same rebalancing as Task 14: `btn_concluir_widget` promoted to `Button.Primary` (real forward action), `btn_adicionar_widget` demoted to full-width `Button.Link` (optional side action, still functional).

- [ ] **Step 2: Bind the header**

Edit `app/src/main/java/com/guilherme/anotaplus/WidgetSuggestionActivity.kt`, add the bind call right after `setContentView(binding.root)` (line 27):

```kotlin
        binding.onboardingHeader.bind(
            activity = this,
            progresso = 0.90f,
            pergunta = getString(R.string.title_widget_sugestao)
        )
```

- [ ] **Step 3: Commit and push**

```bash
git add app/src/main/res/layout/activity_widget_suggestion.xml app/src/main/java/com/guilherme/anotaplus/WidgetSuggestionActivity.kt
git commit -m "Migra WidgetSuggestionActivity pro padrao visual novo de onboarding"
git push
```

- [ ] **Step 4: Verify**

Confirm GitHub Actions build is green. Manual QA: reach this screen via onboarding (choosing "Widget"), confirm on a launcher that supports `requestPinAppWidget` the "Adicionar à tela inicial" button still triggers the system pin dialog, on one that doesn't the fallback text shows instead, and "Concluir" (now the prominent black pill) correctly advances the flow either way.

---

### Task 16: Migrate `QuickAccessChooserActivity` to the new onboarding visual pattern

**Files:**
- Modify: `app/src/main/res/layout/activity_quick_access_chooser.xml`
- Modify: `app/src/main/java/com/guilherme/anotaplus/QuickAccessChooserActivity.kt`

**Interfaces:**
- Consumes: `OnboardingHeaderBinding.bind` (Task 9).

- [ ] **Step 1: Replace the top of the layout**

Edit `app/src/main/res/layout/activity_quick_access_chooser.xml` — replace the outer `NestedScrollView`'s padding and the heading/subtitle block (lines 1-28) while leaving the three card sections (lines 30-153) and the button row (lines 149-170) untouched except for the `link_pular_atalho` button, changed in Step 2 below:

```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.core.widget.NestedScrollView xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    xmlns:tools="http://schemas.android.com/tools"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="@color/paper"
    android:fillViewport="true">

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="vertical"
        android:padding="@dimen/spacing_lg">

        <include
            android:id="@+id/onboarding_header"
            layout="@layout/onboarding_header" />

        <TextView
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="@dimen/spacing_lg"
            android:text="@string/subtitulo_atalho_rapido"
            android:textAppearance="@style/TextAppearance.AnotaPlus.Body"
            android:textColor="@color/ink_soft" />

        <!-- GESTOS -->
        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="@dimen/spacing_lg"
            android:orientation="vertical"
            android:background="@drawable/bg_card_flat"
            android:padding="@dimen/spacing_md">
```

(The removed heading `TextView` that showed `@string/title_atalho_rapido` is dropped — the header's balão now shows that same copy as the screen's "question". Everything from the `GESTOS` card's inner content onward — lines 39-153 in the old file — stays byte-for-byte identical, just re-indented one level to sit inside the same outer `LinearLayout`; don't retype it, only the opening tags above change.)

- [ ] **Step 2: Widen the skip button**

Still in `activity_quick_access_chooser.xml`, change `link_pular_atalho` (originally lines 163-170) from a small centered link to a full-width one:

```xml
        <com.google.android.material.button.MaterialButton
            android:id="@+id/link_pular_atalho"
            style="@style/Widget.AnotaPlus.Button.Link"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="@dimen/spacing_xs"
            android:text="@string/link_pular_atalho" />
```

(`btn_continuar` right above it, already `Widget.AnotaPlus.Button.Primary` full-width, is untouched.)

- [ ] **Step 3: Bind the header**

Edit `app/src/main/java/com/guilherme/anotaplus/QuickAccessChooserActivity.kt`, add the bind call right after `setContentView(binding.root)` (line 51):

```kotlin
        binding.onboardingHeader.bind(
            activity = this,
            progresso = 0.75f,
            pergunta = getString(R.string.title_atalho_rapido)
        )
```

- [ ] **Step 4: Commit and push**

```bash
git add app/src/main/res/layout/activity_quick_access_chooser.xml app/src/main/java/com/guilherme/anotaplus/QuickAccessChooserActivity.kt
git commit -m "Migra QuickAccessChooserActivity pro padrao visual novo de onboarding"
git push
```

- [ ] **Step 5: Verify**

Confirm GitHub Actions build is green. Manual QA: reach this screen via onboarding, confirm the header shows at 75% with the "Como você quer abrir o Anota+ rápido?" bubble, the 3 toggle cards and notification-permission flow still work exactly as before, "Continuar" proceeds into the `GestureGuideActivity`/`WidgetSuggestionActivity` chain (or straight to the tutorial if neither switch is on), and the now-full-width "Agora não" skip button still calls `QuickAccessFlow.avancar` with an empty list. This is the last of the 6 migrated/new onboarding screens — do a full end-to-end onboarding run here (uninstall/reinstall, log in, go through all 4 new/migrated screens plus whichever of Gestos/Widget you pick) to confirm the whole chain holds together before moving to the remaining, unrelated tasks below.

---

### Task 17: Perfil — realign the 4 lower blocks to match the Conta card

**Files:**
- Modify: `app/src/main/res/layout/activity_settings.xml`

**Interfaces:** none (pure resource attribute change).

- [ ] **Step 1: Widen the horizontal padding on the 4 blocks**

Edit `app/src/main/res/layout/activity_settings.xml` — the Conta card already uses `padding="16dp"` (all sides, unchanged). The 4 blocks below it currently use `paddingHorizontal="12dp"` only; change each to `paddingHorizontal="@dimen/spacing_md"` (still horizontal-only — their vertical rhythm comes from each `item_settings_row`'s own `paddingVertical="8dp"`, which stays as-is):

PROGRESSO block:
```xml
            <LinearLayout
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:layout_marginTop="10dp"
                android:orientation="vertical"
                android:background="@drawable/bg_card_flat"
                android:paddingHorizontal="@dimen/spacing_md">
```

PERSONALIZAÇÃO block: identical change (same `paddingHorizontal="12dp"` → `paddingHorizontal="@dimen/spacing_md"`).

LEGAL block: identical change.

SUPORTE block: identical change.

(All 4 occurrences are textually identical in the current file — `android:paddingHorizontal="12dp"` — so this is a single find/replace across the 4 matches, not 4 different edits.)

- [ ] **Step 2: Commit and push**

```bash
git add app/src/main/res/layout/activity_settings.xml
git commit -m "Realinha os 4 blocos inferiores do Perfil com o card Conta"
git push
```

- [ ] **Step 3: Verify**

Confirm GitHub Actions build is green. Manual QA: open Perfil, confirm the icon chips and row titles in Progresso/Personalização/Legal/Suporte now start at the same horizontal position as the content inside the Conta card above them (no more visible "jump" when scrolling past the first block).

---

### Task 18: Legal dialog — icon header, opt-in only (doesn't affect delete confirmations)

`dialog_confirmacao_flat.xml` is shared by 3 callers (`SettingsActivity` for the Legal stubs, but also `AnotacoesActivity` and `EditEntryActivity` for delete confirmations) — the icon header must default to hidden so only the Legal stubs opt into it.

**Files:**
- Create: `app/src/main/res/drawable/bg_icon_chip_accent.xml`
- Modify: `app/src/main/res/layout/dialog_confirmacao_flat.xml`
- Modify: `app/src/main/java/com/guilherme/anotaplus/SettingsActivity.kt:144-154,238-247`

**Interfaces:** none consumed from elsewhere in this plan; doesn't change `criarDialogoFlat`'s signature (Task 4/6/11's dialogs are unaffected).

- [ ] **Step 1: Create the accent icon-chip drawable**

Create `app/src/main/res/drawable/bg_icon_chip_accent.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android" android:shape="oval">
    <solid android:color="@color/verde_destaque" />
</shape>
```

- [ ] **Step 2: Add the (hidden-by-default) icon row to the dialog layout**

Edit `app/src/main/res/layout/dialog_confirmacao_flat.xml`, insert right after the root `<LinearLayout ...>` opening tag (after line 8, before the `text_titulo_confirmacao` `TextView`):

```xml

    <LinearLayout
        android:id="@+id/layout_icone_confirmacao"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_marginBottom="12dp"
        android:visibility="gone"
        tools:visibility="visible">

        <FrameLayout
            android:layout_width="40dp"
            android:layout_height="40dp"
            android:background="@drawable/bg_icon_chip_accent">

            <ImageView
                android:id="@+id/icon_confirmacao"
                android:layout_width="20dp"
                android:layout_height="20dp"
                android:layout_gravity="center"
                app:tint="@color/paper"
                tools:src="@drawable/ic_shield" />

        </FrameLayout>

    </LinearLayout>
```

(This layout has no `xmlns:app` declared today — check the root tag; if `app:tint` above doesn't resolve, add `xmlns:app="http://schemas.android.com/apk/res-auto"` to the root `<LinearLayout>` alongside the existing `xmlns:android`/`xmlns:tools`.)

Delete-confirmation callers (`AnotacoesActivity`, `EditEntryActivity`) never touch `layoutIconeConfirmacao`, so it stays `gone` for them — no visual change to those dialogs.

- [ ] **Step 3: Wire the icon into the Legal stub dialog only**

Edit `app/src/main/java/com/guilherme/anotaplus/SettingsActivity.kt`. Replace `mostrarDialogoStub` (lines 238-247) to accept an icon and set it:

```kotlin
    private fun mostrarDialogoStub(titulo: Int, conteudo: Int, icone: Int) {
        val dialogBinding = DialogConfirmacaoFlatBinding.inflate(layoutInflater)
        dialogBinding.layoutIconeConfirmacao.visibility = View.VISIBLE
        dialogBinding.iconConfirmacao.setImageResource(icone)
        dialogBinding.textTituloConfirmacao.text = getString(titulo)
        dialogBinding.textMensagemConfirmacao.text = getString(conteudo)
        dialogBinding.btnCancelarConfirmacao.visibility = View.GONE
        dialogBinding.btnConfirmarConfirmacao.text = getString(R.string.btn_fechar_dialog)
        val dialog = criarDialogoFlat(dialogBinding.root)
        dialogBinding.btnConfirmarConfirmacao.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }
```

And update its two call sites (lines 147 and 153) — reusing the same icons already shown on the corresponding Perfil rows (`ic_shield` for Privacidade, `ic_document` for Termos):

```kotlin
            mostrarDialogoStub(R.string.titulo_privacidade_stub, R.string.conteudo_privacidade_stub, R.drawable.ic_shield)
```

```kotlin
            mostrarDialogoStub(R.string.titulo_termos_stub, R.string.conteudo_termos_stub, R.drawable.ic_document)
```

- [ ] **Step 4: Commit and push**

```bash
git add app/src/main/res/drawable/bg_icon_chip_accent.xml app/src/main/res/layout/dialog_confirmacao_flat.xml app/src/main/java/com/guilherme/anotaplus/SettingsActivity.kt
git commit -m "Adiciona cabecalho com icone aos dialogos Legal, sem afetar confirmacoes de exclusao"
git push
```

- [ ] **Step 5: Verify**

Confirm GitHub Actions build is green. Manual QA: open Perfil → Legal → Política de Privacidade / Termos de Uso, confirm both now show a green circular icon chip above the title. Separately, trigger a delete confirmation (e.g. long-press/delete a note in Anotações) and confirm that dialog looks completely unchanged (no icon appears).

---

### Task 19: Início — saudação com mascote

**Files:**
- Modify: `app/src/main/res/layout/activity_report.xml`
- Modify: `app/src/main/java/com/guilherme/anotaplus/ReportActivity.kt:140-158`
- Modify: `app/src/main/res/values/strings.xml`

**Interfaces:**
- Consumes: `Prefs.getNomeExibicao` (Task 8), mascot drawables (Task 3).

- [ ] **Step 1: Add the greeting strings**

Edit `app/src/main/res/values/strings.xml`:

```xml
    <string name="saudacao_bom_dia">Bom dia</string>
    <string name="saudacao_boa_tarde">Boa tarde</string>
    <string name="saudacao_boa_noite">Boa noite</string>
    <string name="formato_saudacao_com_nome">%1$s, %2$s!</string>
    <string name="formato_saudacao_guardou">Você guardou %s esse mês 🎉</string>
    <string name="saudacao_neutra">Vamos economizar esse mês?</string>
```

- [ ] **Step 2: Add the greeting card to the layout**

Edit `app/src/main/res/layout/activity_report.xml`, insert immediately before the `<include android:id="@+id/row_mes" .../>` line (inside the scrollable content `LinearLayout`, as its first child):

```xml
            <LinearLayout
                android:id="@+id/card_saudacao"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:layout_marginBottom="@dimen/spacing_sm"
                android:orientation="horizontal"
                android:gravity="center_vertical"
                android:background="@drawable/bg_card_flat"
                android:padding="@dimen/spacing_md">

                <ImageView
                    android:id="@+id/image_mascote_saudacao"
                    android:layout_width="44dp"
                    android:layout_height="44dp"
                    android:src="@drawable/ic_mascote_neutro"
                    android:contentDescription="@string/mascote_content_description" />

                <LinearLayout
                    android:layout_width="0dp"
                    android:layout_height="wrap_content"
                    android:layout_weight="1"
                    android:layout_marginStart="@dimen/spacing_sm"
                    android:orientation="vertical">

                    <TextView
                        android:id="@+id/text_saudacao_titulo"
                        android:layout_width="match_parent"
                        android:layout_height="wrap_content"
                        android:textAppearance="@style/TextAppearance.AnotaPlus.RowTitle"
                        tools:text="Boa tarde, Guilherme!" />

                    <TextView
                        android:id="@+id/text_saudacao_sub"
                        android:layout_width="match_parent"
                        android:layout_height="wrap_content"
                        android:layout_marginTop="2dp"
                        android:textAppearance="@style/TextAppearance.AnotaPlus.Eyebrow"
                        android:textColor="@color/ink_soft"
                        tools:text="Você guardou R$ 300,00 esse mês" />

                </LinearLayout>

            </LinearLayout>
```

- [ ] **Step 3: Compute and bind the greeting from `renderResumo`**

Edit `app/src/main/java/com/guilherme/anotaplus/ReportActivity.kt`. First, add one call at the end of the existing `renderResumo` function (lines 140-158) — after the existing `binding.progressMetaEconomia.progress = percentual` line, insert:

```kotlin
        atualizarSaudacao(poupanca)
```

Then add the new private function anywhere else in the class (e.g. right after `renderResumo`):

```kotlin
    private fun atualizarSaudacao(poupanca: Double) {
        val nome = Prefs.getNomeExibicao(this)
        val hora = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val saudacao = when {
            hora < 12 -> getString(R.string.saudacao_bom_dia)
            hora < 18 -> getString(R.string.saudacao_boa_tarde)
            else -> getString(R.string.saudacao_boa_noite)
        }
        binding.textSaudacaoTitulo.text = if (nome.isNullOrBlank()) {
            saudacao
        } else {
            getString(R.string.formato_saudacao_com_nome, saudacao, nome)
        }
        if (poupanca > 0) {
            binding.imageMascoteSaudacao.setImageResource(R.drawable.ic_mascote_feliz)
            binding.textSaudacaoSub.text = getString(
                R.string.formato_saudacao_guardou,
                "R$ %.2f".format(locale, poupanca)
            )
        } else {
            binding.imageMascoteSaudacao.setImageResource(R.drawable.ic_mascote_neutro)
            binding.textSaudacaoSub.text = getString(R.string.saudacao_neutra)
        }
    }
```

- [ ] **Step 4: Commit and push**

```bash
git add app/src/main/res/layout/activity_report.xml app/src/main/java/com/guilherme/anotaplus/ReportActivity.kt app/src/main/res/values/strings.xml
git commit -m "Adiciona card de saudacao com mascote no Inicio"
git push
```

- [ ] **Step 5: Verify**

Confirm GitHub Actions build is green. Manual QA: open Início at different times of day (or change the device clock) and confirm the greeting text switches between "Bom dia"/"Boa tarde"/"Boa noite", the display name set during onboarding (Task 10) appears after the greeting, and the mascot + sub-text switch between the "guardou R$X" (feliz) and "vamos economizar" (neutro) framing depending on whether the selected month's poupança is positive.

---

### Task 20: Meta-row — celebração ao bater 100%

**Files:**
- Modify: `app/src/main/res/layout/item_meta.xml`
- Modify: `app/src/main/java/com/guilherme/anotaplus/ReportActivity.kt:282-312`
- Modify: `app/src/main/res/values/strings.xml`

**Interfaces:**
- Consumes: mascot drawables (Task 3).

- [ ] **Step 1: Add the string**

Edit `app/src/main/res/values/strings.xml`:

```xml
    <string name="texto_meta_batida">Meta batida! 🎉</string>
```

- [ ] **Step 2: Add the celebration row to the layout**

Edit `app/src/main/res/layout/item_meta.xml`, insert right after the progress-bar `LinearLayout` (ends at line 73 in the current file) and before the root `</LinearLayout>` (line 75):

```xml

    <LinearLayout
        android:id="@+id/layout_meta_celebracao"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_marginTop="8dp"
        android:orientation="horizontal"
        android:gravity="center_vertical"
        android:visibility="gone"
        tools:visibility="visible">

        <ImageView
            android:id="@+id/image_mascote_meta"
            android:layout_width="20dp"
            android:layout_height="20dp"
            android:src="@drawable/ic_mascote_feliz"
            android:contentDescription="@string/mascote_content_description" />

        <TextView
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_marginStart="6dp"
            android:text="@string/texto_meta_batida"
            android:textAppearance="@style/TextAppearance.AnotaPlus.Eyebrow"
            android:textColor="@color/verde_destaque" />

    </LinearLayout>
```

- [ ] **Step 3: Toggle it in `renderMetas`**

Edit `app/src/main/java/com/guilherme/anotaplus/ReportActivity.kt`, in `renderMetas` (lines 282-312), add one line right after the existing `trackParams` block (after line 306, before `row.btnExcluirMeta.setOnClickListener`):

```kotlin
            row.layoutMetaCelebracao.visibility = if (percentual >= 100) View.VISIBLE else View.GONE
```

- [ ] **Step 4: Commit and push**

```bash
git add app/src/main/res/layout/item_meta.xml app/src/main/java/com/guilherme/anotaplus/ReportActivity.kt app/src/main/res/values/strings.xml
git commit -m "Mostra o mascote feliz quando uma Meta bate 100%"
git push
```

- [ ] **Step 5: Verify**

Confirm GitHub Actions build is green. Manual QA: create a Meta with a small valorAlvo and mark enough progress (or directly bump `valorAtual` past `valorAlvo` via normal app flow) to push its percentual to 100 — confirm the "Meta batida! 🎉" row with the happy mascot appears under that Meta's bar and stays hidden for any Meta still under 100%.

---

### Task 21: Alerta de orçamento — ícone grande do mascote na notificação

**Files:**
- Modify: `app/src/main/java/com/guilherme/anotaplus/BudgetAlertNotifier.kt`

**Interfaces:**
- Consumes: mascot drawable (Task 3, `ic_mascote_preocupado`).

- [ ] **Step 1: Add the vector-to-bitmap helper and wire it into the notification**

Edit `app/src/main/java/com/guilherme/anotaplus/BudgetAlertNotifier.kt`. Add two imports after the existing `import java.util.Calendar` (line 11):

```kotlin
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.core.content.ContextCompat
```

Replace the `notificar` function (lines 39-63) to add `.setLargeIcon(...)`:

```kotlin
    private fun notificar(context: Context, categoria: String, total: Double, limite: Double) {
        criarCanalSeNecessario(context)

        val intent = Intent(context, CategoriasActivity::class.java)
            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val pendingIntent = PendingIntent.getActivity(
            context,
            categoria.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_wallet)
            .setLargeIcon(vetorParaBitmap(context, R.drawable.ic_mascote_preocupado))
            .setContentTitle(context.getString(R.string.alerta_orcamento_titulo, categoria))
            .setContentText(
                context.getString(
                    R.string.alerta_orcamento_texto,
                    "R$ %.2f".format(MesUtil.locale, total),
                    "R$ %.2f".format(MesUtil.locale, limite)
                )
            )
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .build()

        runCatching {
            NotificationManagerCompat.from(context).notify(categoria.hashCode(), notification)
        }
    }

    // A pequena silhueta branca de setSmallIcon() é a única exigida pelo
    // Android na barra de status — setLargeIcon() aceita um Bitmap de
    // verdade e é o que mostra o mascote colorido na notificação expandida.
    private fun vetorParaBitmap(context: Context, drawableRes: Int): Bitmap {
        val drawable = ContextCompat.getDrawable(context, drawableRes)!!
        val bitmap = Bitmap.createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }
```

- [ ] **Step 2: Commit and push**

```bash
git add app/src/main/java/com/guilherme/anotaplus/BudgetAlertNotifier.kt
git commit -m "Adiciona icone grande do mascote na notificacao de alerta de orcamento"
git push
```

- [ ] **Step 3: Verify**

Confirm GitHub Actions build is green. Manual QA: set a low limite on a category (Categorias), record a Gasto in that category past the limit, and confirm the resulting notification shows the small wallet icon in the status bar as before, and the mascot artwork as the large icon when the notification is expanded.

---

### Task 22: Navegação "voltar" real dentro da cadeia de onboarding

Adicionada após a revisão final de branch: o cabeçalho compartilhado de onboarding (Task 9) mostra uma seta de "voltar" em 6 telas, mas como cada tela chama `finish()` ao avançar, a pilha de activities nunca acumula — apertar "voltar" (ou o botão físico) sai do onboarding inteiro em vez de retornar à pergunta anterior. Esta tarefa corrige isso SOMENTE dentro do fluxo de onboarding (`onboarding == true`); a reabertura avulsa de `GestureGuideActivity`/`WidgetSuggestionActivity`/`QuickAccessChooserActivity` a partir do Perfil (`onboarding == false`) continua exatamente como está hoje.

**Files:**
- Modify: `app/src/main/java/com/guilherme/anotaplus/LoginActivity.kt`
- Modify: `app/src/main/java/com/guilherme/anotaplus/NomeOnboardingActivity.kt`
- Modify: `app/src/main/java/com/guilherme/anotaplus/SalarioOnboardingActivity.kt`
- Modify: `app/src/main/java/com/guilherme/anotaplus/MetaOnboardingActivity.kt`
- Modify: `app/src/main/java/com/guilherme/anotaplus/QuickAccessFlow.kt`
- Modify: `app/src/main/java/com/guilherme/anotaplus/GestureGuideActivity.kt`

**Interfaces:** nenhuma nova — só muda quando cada Activity chama `finish()`.

- [ ] **Step 1: Parar de finalizar as telas simples ao avançar (só durante onboarding)**

Edit `LoginActivity.kt`, substituir `concluir()`:

```kotlin
    private fun concluir() {
        if (intent.getBooleanExtra(QuickAccessFlow.EXTRA_ONBOARDING, false)) {
            Prefs.marcarOnboardingConcluido(this)
            val abrirHistorico = intent.getBooleanExtra(QuickAccessFlow.EXTRA_ABRIR_HISTORICO, false)
            startActivity(
                Intent(this, NomeOnboardingActivity::class.java)
                    .putExtra(QuickAccessFlow.EXTRA_ONBOARDING, true)
                    .putExtra(QuickAccessFlow.EXTRA_ABRIR_HISTORICO, abrirHistorico)
            )
        } else {
            finish()
        }
    }
```

Edit `NomeOnboardingActivity.kt`, substituir `concluir()` (remove só o `finish()` final):

```kotlin
    private fun concluir() {
        startActivity(
            Intent(this, SalarioOnboardingActivity::class.java)
                .putExtra(QuickAccessFlow.EXTRA_ONBOARDING, intent.getBooleanExtra(QuickAccessFlow.EXTRA_ONBOARDING, false))
                .putExtra(QuickAccessFlow.EXTRA_ABRIR_HISTORICO, intent.getBooleanExtra(QuickAccessFlow.EXTRA_ABRIR_HISTORICO, false))
        )
    }
```

Edit `MetaOnboardingActivity.kt`, substituir `concluir()` (remove só o `finish()` final):

```kotlin
    private fun concluir() {
        startActivity(
            Intent(this, QuickAccessChooserActivity::class.java)
                .putExtra(QuickAccessFlow.EXTRA_ONBOARDING, intent.getBooleanExtra(QuickAccessFlow.EXTRA_ONBOARDING, false))
                .putExtra(QuickAccessFlow.EXTRA_ABRIR_HISTORICO, intent.getBooleanExtra(QuickAccessFlow.EXTRA_ABRIR_HISTORICO, false))
        )
    }
```

- [ ] **Step 2: `SalarioOnboardingActivity` — voltar um passo internamente antes de sair da tela**

Edit `SalarioOnboardingActivity.kt`. Adicionar import:

```kotlin
import androidx.activity.OnBackPressedCallback
```

No `onCreate`, registrar o callback (logo após os dois `setOnClickListener`, antes de `atualizarPasso()`):

```kotlin
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (passo > 0) {
                    passo -= 1
                    atualizarPasso()
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
```

Substituir `concluir()` (remove só o `finish()` final):

```kotlin
    private fun concluir() {
        startActivity(
            Intent(this, MetaOnboardingActivity::class.java)
                .putExtra(QuickAccessFlow.EXTRA_ONBOARDING, onboarding)
                .putExtra(QuickAccessFlow.EXTRA_ABRIR_HISTORICO, abrirHistorico)
        )
    }
```

- [ ] **Step 3: `QuickAccessFlow` — só finalizar quando não for onboarding, e limpar a pilha inteira ao concluir de verdade**

Edit `QuickAccessFlow.kt`, em `avancar` trocar a linha final `activity.finish()` por:

```kotlin
        if (!onboarding) activity.finish()
```

E em `concluir` (privada), trocar o `Intent` de `AnotacoesActivity` pra limpar a task inteira quando for onboarding:

```kotlin
    private fun concluir(activity: Activity, onboarding: Boolean, abrirHistorico: Boolean) {
        if (onboarding) {
            TutorialTourManager.iniciar(onboarding = true, abrirHistorico = abrirHistorico)
            activity.startActivity(
                Intent(activity, AnotacoesActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
        activity.finish()
    }
```

(`CLEAR_TASK`/`NEW_TASK` colapsa toda a pilha de onboarding de uma vez ao concluir — depois disso não dá pra "voltar" pro onboarding nunca mais, o que é o comportamento certo. O `activity.finish()` no fim continua ali, redundante mas inofensivo, pra manter o diff mínimo.)

- [ ] **Step 4: `GestureGuideActivity` — só finalizar quando não for onboarding**

Edit `GestureGuideActivity.kt`, substituir `continuar()`:

```kotlin
    private fun continuar() {
        val onboarding = intent.getBooleanExtra(QuickAccessFlow.EXTRA_ONBOARDING, false)
        if (QuickAccessFlow.temFilaNoIntent(this)) {
            QuickAccessFlow.avancar(
                this,
                QuickAccessFlow.proximaFila(this),
                onboarding,
                intent.getBooleanExtra(QuickAccessFlow.EXTRA_ABRIR_HISTORICO, false)
            )
        }
        if (!onboarding) finish()
    }
```

`WidgetSuggestionActivity.kt` **não precisa mudar** — seu `continuar()` já delega inteiramente pro `finish()` condicional de dentro de `QuickAccessFlow.avancar()` (não tem um `finish()` próprio depois).

- [ ] **Step 5: Commit and push**

```bash
git add app/src/main/java/com/guilherme/anotaplus/LoginActivity.kt app/src/main/java/com/guilherme/anotaplus/NomeOnboardingActivity.kt app/src/main/java/com/guilherme/anotaplus/SalarioOnboardingActivity.kt app/src/main/java/com/guilherme/anotaplus/MetaOnboardingActivity.kt app/src/main/java/com/guilherme/anotaplus/QuickAccessFlow.kt app/src/main/java/com/guilherme/anotaplus/GestureGuideActivity.kt
git commit -m "Implementa navegacao voltar real na cadeia de onboarding"
git push
```

- [ ] **Step 6: Verify**

Confirm GitHub Actions build is green. Manual QA: re-trigger onboarding (limpar dados do app ou reinstalar), avançar até a tela de Meta, apertar "voltar" repetidamente — deve retornar Meta→Salário(passo 2)→Salário(passo 1)→Salário(passo 0)→Nome→Login, cada vez mostrando a tela anterior com o estado dela. Escolher só "Widget" na Escolha de atalho, avançar até o fim, confirmar que o tour de tutorial abre normalmente e que apertar voltar a partir de Anotações (pós-onboarding) NÃO volta pro onboarding (deve sair do app ou ir pra Início conforme o comportamento padrão do app). Separadamente, reabrir "guia de gesto" avulso a partir do Perfil e confirmar que "Entendi" ainda fecha a tela normalmente (comportamento avulso inalterado).

---

## Self-Review

**Spec coverage** (against `docs/superpowers/specs/2026-07-23-refino-visual-fintech-mascote-design.md`):
- §1 Cor de destaque → Task 2 (tokens) + Task 7 (the two literal progress-bar swaps) + the "no new green button" constraint enforced throughout.
- §2 Escala de espaçamento → Task 2 (tokens) + Task 17 (Perfil realignment) + `@dimen/spacing_*` used consistently in every new/migrated layout (Tasks 9-16, 19).
- §3 Onboarding (padrão visual, 2 telas novas, ordem completa) → Tasks 9-16 (pattern + 5 migrated screens) + Tasks 10-11 (2 new screens) + Task 5-6 (Meta prazo data model, shared with the non-onboarding Meta flow per the spec's explicit "vale pra qualquer Meta" requirement).
- §4 Diálogos Legal → Task 18.
- §5 Mascote (conceito, variantes, onde aparece) → Task 3 (placeholder art, 3 variantes) + Task 9 (onboarding placement) + Task 19 (saudação) + Task 20 (celebração de Meta) + Task 21 (alerta de orçamento). Empty states explicitly deferred (see "Scope note" above the task list) — not silently dropped.
- §6 Perfil (realinhar bloco Conta) → Task 17.
- Spec's own "Riscos e pontos em aberto": the `ReportActivity` Meta-creation site was found and updated (Task 6); `SessionPrefs` getter gap was closed (Task 8); empty-state mood choice remains genuinely deferred (out of this plan's scope, as stated).

**Placeholder scan:** no "TBD"/"TODO"/"similar to Task N" phrasing anywhere above — every step shows literal, complete code. The one spot that reads like a placeholder ("figure out the exact line" for `ReportActivity`'s Meta creation) was resolved during planning via research, not left as an open task.

**Type/name consistency check:** `MetaCalculo`'s 3 function signatures (Task 4) are used identically in Task 6 and Task 11. `Meta(nome, valorAlvo, prazo)` named-arg construction matches the entity defined in Task 5 in both call sites (Task 6, Task 11). `OnboardingHeaderBinding.bind(activity, progresso, pergunta, mascote, mostrarVoltar)` is called with matching parameter names/order in Tasks 10, 11, 12, 13, 14, 15, 16 — only `mostrarVoltar` is overridden (to `false`) in Task 10 and Task 12 (the two screens where "back" doesn't make sense), everywhere else the default `true` is used implicitly. `Prefs.getNomeExibicao`/`setNomeExibicao` (Task 8) are called with matching signatures in Task 10 (write) and Task 19 (read). `chip_prazo_6_meses`/`chip_prazo_1_ano`/`chip_prazo_escolher`/`text_preview_valor_mensal` ids are reused identically between Task 6's dialog and Task 11's screen (different generated Binding classes, no collision).

## Execution Handoff

Plan complete and saved to `docs/superpowers/plans/2026-07-23-refino-visual-fintech-mascote.md`. Two execution options:

**1. Subagent-Driven (recommended)** — I dispatch a fresh subagent per task, review between tasks, fast iteration.

**2. Inline Execution** — Execute tasks in this session using executing-plans, batch execution with checkpoints.

Which approach?


