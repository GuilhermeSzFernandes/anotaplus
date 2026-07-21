package com.guilherme.anotaplus

import android.content.Context
import com.guilherme.anotaplus.data.AppDatabase
import com.guilherme.anotaplus.data.SessionPrefs
import com.guilherme.anotaplus.network.ApiClient
import com.guilherme.anotaplus.network.dto.CategoriaSyncRequest
import com.guilherme.anotaplus.network.dto.EntrySyncRequest
import java.time.Instant

/**
 * Backup na nuvem: empurra pro backend tudo que ainda não tem remoteId
 * (categorias e entries locais). Cada item falha isoladamente (runCatching)
 * pra um erro de rede num item não travar o resto do lote.
 */
object SyncManager {

    suspend fun sincronizarTudo(context: Context): Int {
        val token = SessionPrefs.getAccessToken(context) ?: return 0
        val auth = "Bearer $token"
        val db = AppDatabase.getInstance(context)
        var sincronizados = 0

        db.categoryDao().getPendentesDeSync().forEach { categoria ->
            runCatching {
                val resposta = ApiClient.api.criarCategoria(auth, CategoriaSyncRequest(categoria.nome))
                db.categoryDao().marcarSincronizada(categoria.id, resposta.id)
            }.onSuccess { sincronizados++ }
        }

        db.entryDao().getPendentesDeSync().forEach { entry ->
            runCatching {
                val resposta = ApiClient.api.criarEntry(
                    auth,
                    EntrySyncRequest(
                        type = entry.type.name,
                        texto = entry.texto,
                        valor = entry.valor,
                        categoria = entry.categoria,
                        timestamp = Instant.ofEpochMilli(entry.timestamp).toString()
                    )
                )
                db.entryDao().marcarSincronizada(entry.id, resposta.id)
            }.onSuccess { sincronizados++ }
        }

        return sincronizados
    }
}
