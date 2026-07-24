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
