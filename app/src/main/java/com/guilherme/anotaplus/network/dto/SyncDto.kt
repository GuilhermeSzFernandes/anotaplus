package com.guilherme.anotaplus.network.dto

data class CategoriaSyncRequest(val nome: String, val limite: Double? = null)

data class CategoriaLimiteRequest(val limite: Double?)

data class CarteiraSyncRequest(val nome: String)

data class EntrySyncRequest(
    val type: String,
    val titulo: String?,
    val texto: String,
    val valor: Double?,
    val categoria: String?,
    val carteira: String?,
    val timestamp: String
)

// Usado tanto pra categoria quanto pra entry — só precisamos do id gerado
// no backend; o Gson ignora os outros campos do JSON de resposta (userId,
// email, etc.) que não existem nessa data class.
data class SyncResponse(val id: String)

// Resposta de GET /categories e GET /entries — usado pra restaurar backup
// (nuvem -> local), ex: depois de reinstalar o app.
data class CategoriaRemota(val id: String, val nome: String, val limite: Double? = null)

data class CarteiraRemota(val id: String, val nome: String)

data class EntryRemota(
    val id: String,
    val type: String,
    val titulo: String?,
    val texto: String,
    val valor: Double?,
    val categoria: String?,
    val carteira: String?,
    val timestamp: String
)
