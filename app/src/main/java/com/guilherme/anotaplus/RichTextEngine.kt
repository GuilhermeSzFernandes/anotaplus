package com.guilherme.anotaplus

import android.graphics.Typeface
import android.text.Editable
import android.text.Spannable
import android.text.TextWatcher
import android.text.style.BulletSpan
import android.text.style.RelativeSizeSpan
import android.text.style.StrikethroughSpan
import android.text.style.StyleSpan
import android.view.MotionEvent
import android.widget.EditText

/**
 * Formatação "viva" dentro do próprio EditText: o texto guardado continua
 * sendo puro texto simples com marcadores tipo Markdown — nada de HTML
 * nem Spannable serializado —, mas os spans visuais são recalculados a
 * cada mudança pra o efeito aparecer na hora (título grande, negrito,
 * itálico, marcador de lista/checklist riscado quando marcado). Escolhido
 * assim (em vez de um editor de texto rico "de verdade") porque é muito
 * mais simples e robusto de manter, e faz o checklist funcionar de forma
 * trivial no widget (é só procurar a linha e trocar o caractere).
 *
 * Marcadores reconhecidos, sempre no início da linha (exceto negrito e
 * itálico, que valem em qualquer posição):
 * - "# texto"      -> título (H1)
 * - "- texto"      -> item de lista
 * - "☐ texto"      -> item de checklist desmarcado
 * - "☑ texto"      -> item de checklist marcado
 * - "**texto**"    -> negrito (em qualquer lugar da linha)
 * - "_texto_"      -> itálico (em qualquer lugar da linha)
 */
object RichTextEngine {

    const val MARCA_CHECKLIST_VAZIA = "☐ "
    const val MARCA_CHECKLIST_MARCADA = "☑ "
    private const val MARCA_TITULO = "# "
    private const val MARCA_LISTA = "- "

    private val NEGRITO_REGEX = Regex("\\*\\*(.+?)\\*\\*")
    private val ITALICO_REGEX = Regex("_(.+?)_")

    /** Recalcula os spans visuais a cada tecla digitada. */
    fun instalarFormatacaoLive(editText: EditText) {
        editText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (s != null) aplicarFormatacao(s)
            }
        })
        editText.text?.let { aplicarFormatacao(it) }
    }

    private fun aplicarFormatacao(editable: Editable) {
        // Remove só os spans que a gente mesmo aplica, senão acumula um
        // span novo em cima do outro a cada tecla digitada.
        editable.getSpans(0, editable.length, RelativeSizeSpan::class.java).forEach { editable.removeSpan(it) }
        editable.getSpans(0, editable.length, BulletSpan::class.java).forEach { editable.removeSpan(it) }
        editable.getSpans(0, editable.length, StrikethroughSpan::class.java).forEach { editable.removeSpan(it) }
        editable.getSpans(0, editable.length, StyleSpan::class.java).forEach { editable.removeSpan(it) }

        val texto = editable.toString()
        var inicioLinha = 0
        texto.lineSequence().forEach { linha ->
            val fimLinha = inicioLinha + linha.length
            when {
                linha.startsWith(MARCA_TITULO) -> {
                    editable.setSpan(RelativeSizeSpan(1.3f), inicioLinha, fimLinha, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                    editable.setSpan(StyleSpan(Typeface.BOLD), inicioLinha, fimLinha, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
                linha.startsWith(MARCA_CHECKLIST_MARCADA) ->
                    editable.setSpan(StrikethroughSpan(), inicioLinha, fimLinha, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                linha.startsWith(MARCA_LISTA) ->
                    editable.setSpan(BulletSpan(16), inicioLinha, fimLinha, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            }

            NEGRITO_REGEX.findAll(linha).forEach { m ->
                editable.setSpan(
                    StyleSpan(Typeface.BOLD),
                    inicioLinha + m.range.first,
                    inicioLinha + m.range.last + 1,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
            ITALICO_REGEX.findAll(linha).forEach { m ->
                editable.setSpan(
                    StyleSpan(Typeface.ITALIC),
                    inicioLinha + m.range.first,
                    inicioLinha + m.range.last + 1,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }

            inicioLinha = fimLinha + 1 // +1 pelo \n
        }
    }

    // --- Ações da barra de formatação ---

    fun alternarTitulo(editText: EditText) = alternarPrefixoDeLinha(editText, MARCA_TITULO)
    fun alternarLista(editText: EditText) = alternarPrefixoDeLinha(editText, MARCA_LISTA)
    fun alternarChecklist(editText: EditText) = alternarPrefixoDeLinha(editText, MARCA_CHECKLIST_VAZIA)
    fun alternarNegrito(editText: EditText) = envolverSelecao(editText, "**")
    fun alternarItalico(editText: EditText) = envolverSelecao(editText, "_")

    private val PREFIXOS_DE_LINHA = listOf(MARCA_TITULO, MARCA_LISTA, MARCA_CHECKLIST_VAZIA, MARCA_CHECKLIST_MARCADA)

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
                .removePrefix(MARCA_TITULO)
                .removePrefix(MARCA_LISTA)
                .removePrefix(MARCA_CHECKLIST_VAZIA)
                .removePrefix(MARCA_CHECKLIST_MARCADA)
        }
            .replace(NEGRITO_REGEX, "$1")
            .replace(ITALICO_REGEX, "$1")
            .trim()
    }
}
