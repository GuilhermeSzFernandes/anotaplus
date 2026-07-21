package com.guilherme.anotaplus

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.text.Editable
import android.text.Spannable
import android.text.TextWatcher
import android.text.style.BulletSpan
import android.text.style.RelativeSizeSpan
import android.text.style.ReplacementSpan
import android.text.style.StrikethroughSpan
import android.text.style.StyleSpan
import android.text.style.UnderlineSpan
import android.view.MotionEvent
import android.widget.EditText

/**
 * Formatação "viva" dentro do próprio EditText: o texto guardado continua
 * sendo puro texto simples com marcadores tipo Markdown — nada de HTML
 * nem Spannable serializado —, mas os marcadores (#, **, _, -) ficam
 * ocultos visualmente (span de largura zero) e só o efeito aparece:
 * título grande, negrito, itálico, marcador de lista/checklist. Escolhido
 * assim (em vez de um editor de texto rico "de verdade") porque é muito
 * mais simples e robusto de manter, e faz o checklist funcionar de forma
 * trivial no widget (é só procurar a linha e trocar o caractere ☐/☑).
 *
 * Marcadores reconhecidos, sempre no início da linha (exceto negrito e
 * itálico, que valem em qualquer posição):
 * - "# texto"      -> título (H1); "#" oculto
 * - "- texto"      -> item de lista; "-" oculto, ponto desenhado pelo BulletSpan
 * - "☐ texto"      -> item de checklist desmarcado; "☐" fica visível (é o checkbox)
 * - "☑ texto"      -> item de checklist marcado; "☑" fica visível, texto riscado
 * - "**texto**"    -> negrito; "**" ocultos
 * - "_texto_"      -> itálico; "_" ocultos
 *
 * Enter no fim de um item de lista/checklist continua a lista
 * automaticamente; Enter num item vazio sai da lista (quebra de linha
 * normal).
 */
object RichTextEngine {

    const val MARCA_CHECKLIST_VAZIA = "☐ "
    const val MARCA_CHECKLIST_MARCADA = "☑ "
    private const val MARCA_TITULO_1 = "# "
    private const val MARCA_TITULO_2 = "## "
    private const val MARCA_LISTA = "- "
    private val PREFIXOS_DE_LISTA = listOf(MARCA_LISTA, MARCA_CHECKLIST_VAZIA, MARCA_CHECKLIST_MARCADA)
    // "## " tem que vir antes de "# " aqui: como os dois começam com "#",
    // a ordem importa pra "firstOrNull { startsWith(it) }" não bater com
    // "# " primeiro numa linha de título 2.
    private val PREFIXOS_DE_LINHA = listOf(MARCA_TITULO_2, MARCA_TITULO_1) + PREFIXOS_DE_LISTA

    private val NEGRITO_REGEX = Regex("\\*\\*(.+?)\\*\\*")
    private val ITALICO_REGEX = Regex("_(.+?)_")
    private val SUBLINHADO_REGEX = Regex("~(.+?)~")

    /** Span de largura zero: o texto continua existindo no Editable (cursor/backspace normais), só não desenha nada. */
    private class MarcadorOcultoSpan : ReplacementSpan() {
        override fun getSize(paint: Paint, text: CharSequence, start: Int, end: Int, fm: Paint.FontMetricsInt?): Int = 0
        override fun draw(
            canvas: Canvas, text: CharSequence, start: Int, end: Int,
            x: Float, top: Int, y: Int, bottom: Int, paint: Paint
        ) {
            // Não desenha nada — é assim que o marcador "some".
        }
    }

    /** Recalcula os spans a cada tecla digitada, e trata Enter em listas. */
    fun instalarFormatacaoLive(editText: EditText) {
        editText.addTextChangedListener(object : TextWatcher {
            private var ajustando = false
            private var posicaoNovaLinha = -1

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                posicaoNovaLinha = if (!ajustando && count == 1 && before == 0 && s != null && s[start] == '\n') {
                    start
                } else {
                    -1
                }
            }

            override fun afterTextChanged(s: Editable?) {
                if (s == null || ajustando) return
                ajustando = true
                if (posicaoNovaLinha in 0..s.length) {
                    continuarOuSairDeLista(editText, s, posicaoNovaLinha)
                }
                aplicarFormatacaoVisual(s)
                ajustando = false
            }
        })
        editText.text?.let { aplicarFormatacaoVisual(it) }
    }

    private fun continuarOuSairDeLista(editText: EditText, editable: Editable, posicaoQuebra: Int) {
        val inicioLinhaAnterior = inicioDaLinha(editable, posicaoQuebra)
        if (inicioLinhaAnterior >= posicaoQuebra) return

        val linhaAnterior = editable.substring(inicioLinhaAnterior, posicaoQuebra)
        val marcador = PREFIXOS_DE_LISTA.firstOrNull { linhaAnterior.startsWith(it) } ?: return
        val conteudoSemMarcador = linhaAnterior.removePrefix(marcador)

        if (conteudoSemMarcador.isBlank()) {
            // Enter numa linha de lista vazia: sai da lista, quebra normal.
            editable.delete(inicioLinhaAnterior, posicaoQuebra)
        } else {
            // Novo item sempre começa desmarcado, mesmo continuando um item marcado.
            val novoMarcador = if (marcador == MARCA_CHECKLIST_MARCADA) MARCA_CHECKLIST_VAZIA else marcador
            val posInsercao = posicaoQuebra + 1
            editable.insert(posInsercao, novoMarcador)
            editText.setSelection((posInsercao + novoMarcador.length).coerceAtMost(editable.length))
        }
    }

    private fun aplicarFormatacaoVisual(editable: Editable) {
        // Remove só os spans que a gente mesmo aplica, senão acumula um
        // span novo em cima do outro a cada tecla digitada.
        editable.getSpans(0, editable.length, MarcadorOcultoSpan::class.java).forEach { editable.removeSpan(it) }
        editable.getSpans(0, editable.length, RelativeSizeSpan::class.java).forEach { editable.removeSpan(it) }
        editable.getSpans(0, editable.length, BulletSpan::class.java).forEach { editable.removeSpan(it) }
        editable.getSpans(0, editable.length, StrikethroughSpan::class.java).forEach { editable.removeSpan(it) }
        editable.getSpans(0, editable.length, StyleSpan::class.java).forEach { editable.removeSpan(it) }
        editable.getSpans(0, editable.length, UnderlineSpan::class.java).forEach { editable.removeSpan(it) }

        val texto = editable.toString()
        var inicioLinha = 0
        texto.lineSequence().forEach { linha ->
            val fimLinha = inicioLinha + linha.length
            when {
                linha.startsWith(MARCA_TITULO_2) -> {
                    ocultar(editable, inicioLinha, inicioLinha + MARCA_TITULO_2.length)
                    editable.setSpan(RelativeSizeSpan(1.15f), inicioLinha, fimLinha, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                    editable.setSpan(StyleSpan(Typeface.BOLD), inicioLinha, fimLinha, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
                linha.startsWith(MARCA_TITULO_1) -> {
                    ocultar(editable, inicioLinha, inicioLinha + MARCA_TITULO_1.length)
                    editable.setSpan(RelativeSizeSpan(1.35f), inicioLinha, fimLinha, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                    editable.setSpan(StyleSpan(Typeface.BOLD), inicioLinha, fimLinha, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
                linha.startsWith(MARCA_CHECKLIST_MARCADA) -> {
                    val fimMarca = inicioLinha + MARCA_CHECKLIST_MARCADA.length
                    editable.setSpan(StrikethroughSpan(), fimMarca, fimLinha, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
                linha.startsWith(MARCA_CHECKLIST_VAZIA) -> {
                    // "☐ " fica visível como está, sem estilo extra no resto da linha.
                }
                linha.startsWith(MARCA_LISTA) -> {
                    ocultar(editable, inicioLinha, inicioLinha + MARCA_LISTA.length)
                    editable.setSpan(BulletSpan(16), inicioLinha, fimLinha, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
            }

            NEGRITO_REGEX.findAll(linha).forEach { m ->
                val ini = inicioLinha + m.range.first
                val fim = inicioLinha + m.range.last + 1
                ocultar(editable, ini, ini + 2)
                ocultar(editable, fim - 2, fim)
                editable.setSpan(StyleSpan(Typeface.BOLD), ini, fim, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
            ITALICO_REGEX.findAll(linha).forEach { m ->
                val ini = inicioLinha + m.range.first
                val fim = inicioLinha + m.range.last + 1
                ocultar(editable, ini, ini + 1)
                ocultar(editable, fim - 1, fim)
                editable.setSpan(StyleSpan(Typeface.ITALIC), ini, fim, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
            SUBLINHADO_REGEX.findAll(linha).forEach { m ->
                val ini = inicioLinha + m.range.first
                val fim = inicioLinha + m.range.last + 1
                ocultar(editable, ini, ini + 1)
                ocultar(editable, fim - 1, fim)
                editable.setSpan(UnderlineSpan(), ini, fim, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            }

            inicioLinha = fimLinha + 1 // +1 pelo \n
        }
    }

    private fun ocultar(editable: Editable, inicio: Int, fim: Int) {
        if (inicio in 0 until fim && fim <= editable.length) {
            editable.setSpan(MarcadorOcultoSpan(), inicio, fim, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
    }

    // --- Ações da barra de formatação ---

    fun alternarTitulo1(editText: EditText) = alternarPrefixoDeLinha(editText, MARCA_TITULO_1)
    fun alternarTitulo2(editText: EditText) = alternarPrefixoDeLinha(editText, MARCA_TITULO_2)
    fun alternarLista(editText: EditText) = alternarPrefixoDeLinha(editText, MARCA_LISTA)
    fun alternarChecklist(editText: EditText) = alternarPrefixoDeLinha(editText, MARCA_CHECKLIST_VAZIA)
    fun alternarNegrito(editText: EditText) = envolverSelecao(editText, "**")
    fun alternarItalico(editText: EditText) = envolverSelecao(editText, "_")
    fun alternarSublinhado(editText: EditText) = envolverSelecao(editText, "~")

    /** Tira título/lista/checklist/negrito/itálico/sublinhado da linha atual, virando texto puro. */
    fun limparFormatacao(editText: EditText) {
        val editable = editText.text ?: return
        val cursor = editText.selectionStart.coerceAtLeast(0)
        val inicioLinha = inicioDaLinha(editable, cursor)
        val fimLinha = fimDaLinha(editable, cursor)

        var linha = editable.substring(inicioLinha, fimLinha)
        PREFIXOS_DE_LINHA.forEach { linha = linha.removePrefix(it) }
        linha = linha
            .replace(NEGRITO_REGEX, "$1")
            .replace(ITALICO_REGEX, "$1")
            .replace(SUBLINHADO_REGEX, "$1")

        editable.replace(inicioLinha, fimLinha, linha)
    }

    private fun alternarPrefixoDeLinha(editText: EditText, prefixo: String) {
        val editable = editText.text ?: return
        val cursor = editText.selectionStart.coerceAtLeast(0)
        val inicioLinha = inicioDaLinha(editable, cursor)
        val fimLinha = fimDaLinha(editable, cursor)
        val linha = editable.substring(inicioLinha, fimLinha)
        val prefixoAtual = PREFIXOS_DE_LINHA.firstOrNull { linha.startsWith(it) }

        when {
            prefixoAtual == prefixo -> editable.delete(inicioLinha, inicioLinha + prefixo.length)
            prefixoAtual != null -> {
                editable.delete(inicioLinha, inicioLinha + prefixoAtual.length)
                editable.insert(inicioLinha, prefixo)
            }
            else -> editable.insert(inicioLinha, prefixo)
        }
    }

    private fun envolverSelecao(editText: EditText, marcador: String) {
        val editable = editText.text ?: return
        val ini = editText.selectionStart.coerceAtLeast(0)
        val fim = editText.selectionEnd.coerceAtLeast(ini)

        val jaEnvolvido = ini >= marcador.length && fim + marcador.length <= editable.length &&
            editable.substring(ini - marcador.length, ini) == marcador &&
            editable.substring(fim, fim + marcador.length) == marcador

        if (jaEnvolvido) {
            editable.delete(fim, fim + marcador.length)
            editable.delete(ini - marcador.length, ini)
            editText.setSelection((ini - marcador.length).coerceAtLeast(0), (fim - marcador.length).coerceAtLeast(0))
        } else {
            editable.insert(fim, marcador)
            editable.insert(ini, marcador)
            editText.setSelection(ini + marcador.length, fim + marcador.length)
        }
    }

    /** Tocar no "☐"/"☑" no começo de uma linha de checklist marca/desmarca, sem abrir teclado nem mover cursor. */
    @Suppress("ClickableViewAccessibility")
    fun instalarToqueChecklist(editText: EditText) {
        editText.setOnTouchListener { view, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                val offset = editText.getOffsetForPosition(event.x, event.y)
                val editable = editText.text
                if (editable != null && offset <= editable.length) {
                    val inicioLinha = inicioDaLinha(editable, offset)
                    val fimLinha = fimDaLinha(editable, offset)
                    val linha = editable.substring(inicioLinha, fimLinha)
                    val tocouNaMarca = offset - inicioLinha <= 1

                    if (tocouNaMarca && linha.startsWith(MARCA_CHECKLIST_VAZIA)) {
                        editable.replace(inicioLinha, inicioLinha + MARCA_CHECKLIST_VAZIA.length, MARCA_CHECKLIST_MARCADA)
                        view.performClick()
                        return@setOnTouchListener true
                    }
                    if (tocouNaMarca && linha.startsWith(MARCA_CHECKLIST_MARCADA)) {
                        editable.replace(inicioLinha, inicioLinha + MARCA_CHECKLIST_MARCADA.length, MARCA_CHECKLIST_VAZIA)
                        view.performClick()
                        return@setOnTouchListener true
                    }
                }
            }
            false
        }
    }

    private fun inicioDaLinha(editable: CharSequence, offset: Int): Int {
        var i = offset.coerceIn(0, editable.length)
        while (i > 0 && editable[i - 1] != '\n') i--
        return i
    }

    private fun fimDaLinha(editable: CharSequence, offset: Int): Int {
        var i = offset.coerceIn(0, editable.length)
        while (i < editable.length && editable[i] != '\n') i++
        return i
    }

    /** Preview simples (Histórico): tira os marcadores, uma linha só. */
    fun textoSemMarcadores(texto: String): String {
        return texto.lineSequence().joinToString(" ") { linha ->
            linha
                .removePrefix(MARCA_TITULO_2)
                .removePrefix(MARCA_TITULO_1)
                .removePrefix(MARCA_LISTA)
                .removePrefix(MARCA_CHECKLIST_VAZIA)
                .removePrefix(MARCA_CHECKLIST_MARCADA)
        }
            .replace(NEGRITO_REGEX, "$1")
            .replace(ITALICO_REGEX, "$1")
            .replace(SUBLINHADO_REGEX, "$1")
            .trim()
    }
}
