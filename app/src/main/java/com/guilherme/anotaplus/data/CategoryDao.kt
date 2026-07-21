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
}
