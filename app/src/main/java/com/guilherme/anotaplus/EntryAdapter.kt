package com.guilherme.anotaplus

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.guilherme.anotaplus.data.Entry
import com.guilherme.anotaplus.data.EntryType
import com.guilherme.anotaplus.databinding.ItemEntryBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class EntryAdapter : RecyclerView.Adapter<EntryAdapter.EntryViewHolder>() {

    private var items: List<Entry> = emptyList()
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("pt", "BR"))

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
            val isGasto = entry.type == EntryType.GASTO
            binding.textTipoValor.text = if (isGasto) {
                val valorTexto = entry.valor?.let { "R$ %.2f".format(it) } ?: "R$ --"
                val categoria = entry.categoria?.let { " · $it" } ?: ""
                "💸 $valorTexto$categoria"
            } else {
                "💭 Pensamento"
            }
            binding.textTipoValor.setTextColor(
                binding.root.context.getColor(
                    if (isGasto) R.color.gasto_color else R.color.pensamento_color
                )
            )
            binding.textConteudo.text = entry.texto
            binding.textConteudo.visibility =
                if (entry.texto.isBlank()) android.view.View.GONE else android.view.View.VISIBLE
            binding.textData.text = dateFormat.format(Date(entry.timestamp))
        }
    }
}
