package com.guilherme.anotaplus

import android.content.Context
import com.guilherme.anotaplus.data.AppDatabase
import com.guilherme.anotaplus.data.Category
import com.guilherme.anotaplus.data.Entry
import com.guilherme.anotaplus.data.EntryType
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

    /**
     * Restaura do backend pro Room local (nuvem -> local) — útil depois de
     * reinstalar o app ou trocar de aparelho. Categorias são casadas por
     * nome (evita duplicar as categorias padrão que já vêm semeadas em toda
     * instalação nova); entries são casadas pelo remoteId (uma entry só é
     * inserida se ainda não existir localmente nenhuma com esse remoteId),
     * então rodar isso várias vezes é seguro, não duplica nada.
     */
    suspend fun restaurarTudo(context: Context): Int {
        val token = SessionPrefs.getAccessToken(context) ?: return 0
        val auth = "Bearer $token"
        val db = AppDatabase.getInstance(context)
        var restaurados = 0

        val categoriasLocais = db.categoryDao().getAllOnce()
        runCatching { ApiClient.api.listarCategorias(auth) }.getOrNull()?.forEach { remota ->
            val existente = categoriasLocais.find { it.nome == remota.nome }
            when {
                existente == null -> {
                    db.categoryDao().insert(Category(nome = remota.nome, remoteId = remota.id))
                    restaurados++
                }
                existente.remoteId == null -> {
                    db.categoryDao().marcarSincronizada(existente.id, remota.id)
                }
            }
        }

        runCatching { ApiClient.api.listarEntries(auth) }.getOrNull()?.forEach { remota ->
            if (!db.entryDao().existsByRemoteId(remota.id)) {
                runCatching {
                    db.entryDao().insert(
                        Entry(
                            type = EntryType.valueOf(remota.type),
                            texto = remota.texto,
                            valor = remota.valor,
                            categoria = remota.categoria,
                            timestamp = Instant.parse(remota.timestamp).toEpochMilli(),
                            remoteId = remota.id
                        )
                    )
                }.onSuccess { restaurados++ }
            }
        }

        return restaurados
    }
}
