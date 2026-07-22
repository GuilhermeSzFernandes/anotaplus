package com.guilherme.anotaplus.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {

    @Insert
    suspend fun insert(category: Category): Long

    @Query("SELECT * FROM categories ORDER BY nome ASC")
    fun getAll(): Flow<List<Category>>

    @Query("SELECT nome FROM categories ORDER BY nome ASC")
    suspend fun getNomesOnce(): List<String>

    @Query("DELETE FROM categories WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT COUNT(*) FROM categories")
    suspend fun count(): Int

    @Query("SELECT * FROM categories WHERE remoteId IS NULL")
    suspend fun getPendentesDeSync(): List<Category>

    @Query("SELECT * FROM categories")
    suspend fun getAllOnce(): List<Category>

    @Query("UPDATE categories SET remoteId = :remoteId WHERE id = :id")
    suspend fun marcarSincronizada(id: Long, remoteId: String)

    @Query("UPDATE categories SET limite = :limite WHERE id = :id")
    suspend fun atualizarLimite(id: Long, limite: Double?)

    @Query("SELECT * FROM categories WHERE nome = :nome LIMIT 1")
    suspend fun getByNome(nome: String): Category?

    // Edição completa (CategoriasActivity): nome (renomear), cor, ícone e
    // limite de uma vez só — nenhum desses 4 campos sincroniza sozinho com
    // o backend (só limite tem endpoint próprio, ver SyncManager).
    @Query("UPDATE categories SET nome = :nome, cor = :cor, icone = :icone, limite = :limite WHERE id = :id")
    suspend fun atualizar(id: Long, nome: String, cor: String?, icone: String?, limite: Double?)
}
