package com.guilherme.anotaplus.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface EntryDao {

    @Insert
    suspend fun insert(entry: Entry): Long

    @Query("SELECT * FROM entries ORDER BY timestamp DESC")
    fun getAll(): Flow<List<Entry>>

    @Query("DELETE FROM entries WHERE id = :id")
    suspend fun deleteById(id: Long)
}
