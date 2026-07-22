package com.guilherme.anotaplus.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class Category(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nome: String,
    // Id da categoria no backend depois do backup na nuvem; null enquanto
    // não sincronizada ainda.
    val remoteId: String? = null,
    // Limite mensal de gasto pra essa categoria (feature de orçamento);
    // null enquanto o usuário não define um. Sincroniza com a nuvem só no
    // plano PRO (mesmo gate do backup) — ver SyncManager.
    val limite: Double? = null,
    // Cor/ícone de identificação visual — só local, não sincroniza com o
    // backend (o schema Prisma não tem essas colunas, e não faz sentido
    // adicionar só por isso). cor é um hex ("#A8823A"), icone é um emoji
    // (ver CategoriaEstilo.kt pra paleta fixa dos dois).
    val cor: String? = null,
    val icone: String? = null
)
