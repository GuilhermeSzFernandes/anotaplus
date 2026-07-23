package com.guilherme.anotaplus.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MetaDao {

    @Insert
    suspend fun insert(meta: Meta): Long

    @Query("SELECT * FROM metas ORDER BY criadoEm ASC")
    fun getAll(): Flow<List<Meta>>

    @Query("UPDATE metas SET valorAtual = :valorAtual WHERE id = :id")
    suspend fun atualizarValorAtual(id: Long, valorAtual: Double)

    @Query("DELETE FROM metas WHERE id = :id")
    suspend fun deleteById(id: Long)
}
