package com.guilherme.anotaplus.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class EntryType { GASTO, PENSAMENTO }

@Entity(tableName = "entries")
data class Entry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: EntryType,
    // Só usado pela tela "bloco de notas" (ManualIdeiaActivity) — Gasto e
    // Ideia via Captura Rápida continuam sem título, null.
    val titulo: String? = null,
    val texto: String,
    val valor: Double? = null,
    val categoria: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    // Id do registro no backend depois que foi feito o backup na nuvem;
    // null enquanto não sincronizado ainda.
    val remoteId: String? = null
)
