package com.guilherme.anotaplus

import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.graphics.ColorUtils
import androidx.recyclerview.widget.RecyclerView
import com.guilherme.anotaplus.data.Entry
import com.guilherme.anotaplus.data.EntryType
import com.guilherme.anotaplus.databinding.ItemEntryBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class EntryAdapter(private val onItemClick: (Entry) -> Unit) :
    RecyclerView.Adapter<EntryAdapter.EntryViewHolder>() {

    private var items: List<Entry> = emptyList()
    private val locale = Locale("pt", "BR")
    private val dateFormat = SimpleDateFormat("dd MMM · HH:mm", locale)

    fun submitList(newItems: List<Entry>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EntryViewHolder {
        val binding = ItemEntryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return EntryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: EntryViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class EntryViewHolder(private val binding: ItemEntryBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(entry: Entry) {
            val ehFinanceiro = entry.type == EntryType.GASTO || entry.type == EntryType.RECEBIMENTO
            val typeColor = binding.root.context.getColor(
                when (entry.type) {
                    EntryType.GASTO -> R.color.color_gasto_vivo
                    EntryType.RECEBIMENTO -> R.color.color_receita
                    EntryType.PENSAMENTO -> R.color.pensamento_color
                }
            )

            binding.textTipoValor.text = if (ehFinanceiro) {
                // Sinal de extrato: Recebimento soma, Gasto subtrai.
                val sinal = if (entry.type == EntryType.RECEBIMENTO) "+ " else "- "
                val valorTexto = entry.valor?.let { "$sinal" + "R$ %.2f".format(locale, it) } ?: "R$ --"
                val categoria = entry.categoria?.let { " · $it" } ?: ""
                "$valorTexto$categoria"
            } else {
                binding.root.context.getString(R.string.btn_pensamento)
            }
            binding.textTipoValor.setTextColor(typeColor)

            val iconeTipo = when (entry.type) {
                EntryType.GASTO -> R.drawable.ic_arrow_expense
                EntryType.RECEBIMENTO -> R.drawable.ic_arrow_income
                EntryType.PENSAMENTO -> R.drawable.ic_note
            }
            binding.iconTipo.setImageResource(iconeTipo)
            binding.iconTipo.setColorFilter(typeColor)
            binding.avatarTipo.background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(ColorUtils.setAlphaComponent(typeColor, 38))
            }

            binding.textTitulo.text = entry.titulo.orEmpty()
            binding.textTitulo.visibility =
                if (entry.titulo.isNullOrBlank()) android.view.View.GONE else android.view.View.VISIBLE

            // Ideias criadas no "bloco de notas" podem ter marcadores de
            // formatação (#, **, ☐ etc.) — na lista mostra só o texto
            // puro, sem sintaxe crua; a formatação de verdade só aparece
            // ao abrir a nota (RichTextEngine.instalarFormatacaoLive).
            val preview = if (ehFinanceiro) entry.texto else RichTextEngine.textoSemMarcadores(entry.texto)
            binding.textConteudo.text = preview
            binding.textConteudo.visibility =
                if (preview.isBlank()) android.view.View.GONE else android.view.View.VISIBLE

            binding.textNumber.text =
                binding.root.context.getString(R.string.entry_number_format, entry.id)
            binding.textData.text = dateFormat.format(Date(entry.timestamp)).uppercase(locale)

            binding.root.setOnClickListener { onItemClick(entry) }
        }
    }
}
