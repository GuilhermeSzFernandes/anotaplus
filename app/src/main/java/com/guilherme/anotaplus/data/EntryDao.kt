package com.guilherme.anotaplus.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface EntryDao {

    @Insert
    suspend fun insert(entry: Entry): Long

    @Update
    suspend fun update(entry: Entry)

    @Query("SELECT * FROM entries WHERE id = :id")
    suspend fun getById(id: Long): Entry?

    @Query("SELECT * FROM entries ORDER BY timestamp DESC")
    fun getAll(): Flow<List<Entry>>

    @Query("SELECT * FROM entries WHERE type = :type ORDER BY timestamp DESC")
    fun getByType(type: EntryType): Flow<List<Entry>>

    @Query(
        "SELECT * FROM entries WHERE type = :type AND timestamp BETWEEN :inicio AND :fim ORDER BY timestamp DESC"
    )
    fun getByTypeAndRange(type: EntryType, inicio: Long, fim: Long): Flow<List<Entry>>

    @Query("DELETE FROM entries WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM entries WHERE remoteId IS NULL")
    suspend fun getPendentesDeSync(): List<Entry>

    @Query("SELECT EXISTS(SELECT 1 FROM entries WHERE remoteId = :remoteId)")
    suspend fun existsByRemoteId(remoteId: String): Boolean

    @Query("UPDATE entries SET remoteId = :remoteId WHERE id = :id")
    suspend fun marcarSincronizada(id: Long, remoteId: String)

    @Query(
        """
        SELECT categoria, SUM(valor) AS total FROM entries
        WHERE type = 'GASTO' AND timestamp BETWEEN :inicio AND :fim AND valor IS NOT NULL
        GROUP BY categoria
        ORDER BY total DESC
        """
    )
    fun getGastoPorCategoria(inicio: Long, fim: Long): Flow<List<CategoriaTotal>>

    @Query(
        "SELECT COALESCE(SUM(valor), 0) FROM entries WHERE type = 'GASTO' AND timestamp BETWEEN :inicio AND :fim"
    )
    fun getTotalGasto(inicio: Long, fim: Long): Flow<Double>

    // Versão "de uma vez só" (sem Flow), usada pelo widget da tela inicial:
    // ele não fica escutando o banco, só relê o total quando é atualizado.
    @Query(
        "SELECT COALESCE(SUM(valor), 0) FROM entries WHERE type = 'GASTO' AND timestamp BETWEEN :inicio AND :fim"
    )
    suspend fun getTotalGastoOnce(inicio: Long, fim: Long): Double

    @Query(
        "SELECT COUNT(*) FROM entries WHERE type = 'PENSAMENTO' AND timestamp BETWEEN :inicio AND :fim"
    )
    fun getTotalIdeias(inicio: Long, fim: Long): Flow<Int>
}
