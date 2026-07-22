package com.guilherme.anotaplus

import android.content.Context
import com.guilherme.anotaplus.data.AppDatabase
import com.guilherme.anotaplus.data.Carteira
import com.guilherme.anotaplus.data.Category
import com.guilherme.anotaplus.data.Entry
import com.guilherme.anotaplus.data.EntryType
import com.guilherme.anotaplus.data.SessionPrefs
import com.guilherme.anotaplus.network.ApiClient
import com.guilherme.anotaplus.network.dto.CarteiraSyncRequest
import com.guilherme.anotaplus.network.dto.CategoriaLimiteRequest
import com.guilherme.anotaplus.network.dto.CategoriaSyncRequest
import com.guilherme.anotaplus.network.dto.EntrySyncRequest
import com.guilherme.anotaplus.widget.WidgetUpdater
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
                val resposta = ApiClient.api.criarCategoria(
                    auth,
                    CategoriaSyncRequest(categoria.nome, categoria.limite)
                )
                db.categoryDao().marcarSincronizada(categoria.id, resposta.id)
            }.onSuccess { sincronizados++ }
        }

        db.carteiraDao().getPendentesDeSync().forEach { carteira ->
            runCatching {
                val resposta = ApiClient.api.criarCarteira(auth, CarteiraSyncRequest(carteira.nome))
                db.carteiraDao().marcarSincronizada(carteira.id, resposta.id)
            }.onSuccess { sincronizados++ }
        }

        db.entryDao().getPendentesDeSync().forEach { entry ->
            runCatching {
                val resposta = ApiClient.api.criarEntry(
                    auth,
                    EntrySyncRequest(
                        type = entry.type.name,
                        titulo = entry.titulo,
                        texto = entry.texto,
                        valor = entry.valor,
                        categoria = entry.categoria,
                        carteira = entry.carteira,
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
                    db.categoryDao().insert(
                        Category(nome = remota.nome, remoteId = remota.id, limite = remota.limite)
                    )
                    restaurados++
                }
                existente.remoteId == null -> {
                    db.categoryDao().marcarSincronizada(existente.id, remota.id)
                    if (existente.limite == null && remota.limite != null) {
                        db.categoryDao().atualizarLimite(existente.id, remota.limite)
                    }
                }
            }
        }

        val carteirasLocais = db.carteiraDao().getAllOnce()
        runCatching { ApiClient.api.listarCarteiras(auth) }.getOrNull()?.forEach { remota ->
            val existente = carteirasLocais.find { it.nome == remota.nome }
            when {
                existente == null -> {
                    db.carteiraDao().insert(Carteira(nome = remota.nome, remoteId = remota.id))
                    restaurados++
                }
                existente.remoteId == null -> {
                    db.carteiraDao().marcarSincronizada(existente.id, remota.id)
                }
            }
        }

        runCatching { ApiClient.api.listarEntries(auth) }.getOrNull()?.forEach { remota ->
            if (!db.entryDao().existsByRemoteId(remota.id)) {
                runCatching {
                    db.entryDao().insert(
                        Entry(
                            type = EntryType.valueOf(remota.type),
                            titulo = remota.titulo,
                            texto = remota.texto,
                            valor = remota.valor,
                            categoria = remota.categoria,
                            carteira = remota.carteira,
                            timestamp = Instant.parse(remota.timestamp).toEpochMilli(),
                            remoteId = remota.id
                        )
                    )
                }.onSuccess { restaurados++ }
            }
        }

        if (restaurados > 0) {
            WidgetUpdater.atualizarTodos(context)
        }

        return restaurados
    }

    /**
     * Propaga edição de limite de uma categoria já sincronizada antes (tem
     * remoteId) — diferente de sincronizarTudo(), que só empurra categorias
     * ainda sem remoteId. Usada pela SettingsActivity quando o usuário
     * edita o limite de uma categoria PRO já sincronizada. Categoria ainda
     * sem remoteId não precisa disso: o limite já viaja junto na primeira
     * sincronização (sincronizarTudo -> criarCategoria).
     */
    suspend fun enviarLimiteCategoria(context: Context, categoria: Category): Boolean {
        val remoteId = categoria.remoteId ?: return false
        val token = SessionPrefs.getAccessToken(context) ?: return false
        return runCatching {
            ApiClient.api.atualizarCategoria(
                "Bearer $token",
                remoteId,
                CategoriaLimiteRequest(categoria.limite)
            )
        }.isSuccess
    }
}
