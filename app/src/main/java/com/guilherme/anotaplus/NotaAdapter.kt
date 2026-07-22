package com.guilherme.anotaplus

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.guilherme.anotaplus.data.Entry
import com.guilherme.anotaplus.databinding.ItemNotaCardBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Grid de cards das Anotações — adapter próprio (não reaproveita o
// EntryAdapter usado no Financeiro, que é uma linha de extrato, não um
// card): tem estado de seleção múltipla, cor por card e layout diferente.
class NotaAdapter(
    private val onAbrir: (Entry) -> Unit,
    private val onSelecaoMudou: () -> Unit
) : RecyclerView.Adapter<NotaAdapter.NotaViewHolder>() {

    private var items: List<Entry> = emptyList()
    private val selecionados = mutableSetOf<Long>()
    private val locale = Locale("pt", "BR")
    private val dateFormat = SimpleDateFormat("dd MMM", locale)

    private val cores = intArrayOf(
        R.color.brass,
        R.color.gasto_color,
        R.color.color_positivo,
        R.color.pensamento_color,
        R.color.chart_teal,
        R.color.chart_dusty_rose
    )

    val modoSelecao: Boolean get() = selecionados.isNotEmpty()

    fun submitList(newItems: List<Entry>) {
        items = newItems
        notifyDataSetChanged()
    }

    fun idsSelecionados(): List<Long> = selecionados.toList()

    fun qtdSelecionados(): Int = selecionados.size

    fun limparSelecao() {
        if (selecionados.isEmpty()) return
        selecionados.clear()
        notifyDataSetChanged()
        onSelecaoMudou()
    }

    fun entryNaPosicao(position: Int): Entry = items[position]

    private fun alternarSelecao(id: Long) {
        if (!selecionados.remove(id)) selecionados.add(id)
        notifyDataSetChanged()
        onSelecaoMudou()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotaViewHolder {
        val binding = ItemNotaCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return NotaViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NotaViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class NotaViewHolder(private val binding: ItemNotaCardBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(entry: Entry) {
            val corpo = RichTextEngine.textoSemMarcadores(entry.texto)
            val temTitulo = !entry.titulo.isNullOrBlank()

            binding.textTituloNota.text = if (temTitulo) entry.titulo else corpo
            binding.textPreviewNota.text = corpo
            binding.textPreviewNota.visibility =
                if (temTitulo && corpo.isNotBlank()) View.VISIBLE else View.GONE

            binding.textDataNota.text = dateFormat.format(Date(entry.timestamp)).uppercase(locale)

            val corIndex = (entry.id % cores.size).toInt()
            binding.viewStripeCor.setBackgroundColor(
                ContextCompat.getColor(binding.root.context, cores[corIndex])
            )

            val tag = entry.categoria?.takeIf { it.isNotBlank() }
            binding.textTagNota.visibility = if (tag != null) View.VISIBLE else View.GONE
            binding.textTagNota.text = tag.orEmpty()

            binding.overlaySelecionado.visibility =
                if (selecionados.contains(entry.id)) View.VISIBLE else View.GONE

            binding.root.setOnClickListener {
                if (modoSelecao) alternarSelecao(entry.id) else onAbrir(entry)
            }
            binding.root.setOnLongClickListener {
                alternarSelecao(entry.id)
                true
            }
        }
    }
}
