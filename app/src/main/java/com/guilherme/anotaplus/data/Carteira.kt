package com.guilherme.anotaplus.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "carteiras")
data class Carteira(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nome: String,
    val remoteId: String? = null
)
