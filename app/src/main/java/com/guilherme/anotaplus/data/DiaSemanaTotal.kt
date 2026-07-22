package com.guilherme.anotaplus.data

// diaSemana segue o %w do SQLite: "0" = domingo .. "6" = sábado.
data class DiaSemanaTotal(
    val diaSemana: String,
    val total: Double
)
