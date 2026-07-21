package com.guilherme.anotaplus.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class Category(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nome: String,
    // Id da categoria no backend depois do backup na nuvem; null enquanto
    // não sincronizada ainda.
    val remoteId: String? = null
)
