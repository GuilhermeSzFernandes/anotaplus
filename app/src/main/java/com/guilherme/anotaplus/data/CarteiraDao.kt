package com.guilherme.anotaplus.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CarteiraDao {

    @Insert
    suspend fun insert(carteira: Carteira): Long

    @Query("SELECT * FROM carteiras ORDER BY nome ASC")
    fun getAll(): Flow<List<Carteira>>

    @Query("SELECT nome FROM carteiras ORDER BY nome ASC")
    suspend fun getNomesOnce(): List<String>

    @Query("DELETE FROM carteiras WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM carteiras WHERE remoteId IS NULL")
    suspend fun getPendentesDeSync(): List<Carteira>

    @Query("SELECT * FROM carteiras")
    suspend fun getAllOnce(): List<Carteira>

    @Query("UPDATE carteiras SET remoteId = :remoteId WHERE id = :id")
    suspend fun marcarSincronizada(id: Long, remoteId: String)
}
