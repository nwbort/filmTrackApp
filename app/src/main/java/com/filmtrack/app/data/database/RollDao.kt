package com.filmtrack.app.data.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.filmtrack.app.data.model.Roll
import kotlinx.coroutines.flow.Flow

@Dao
interface RollDao {
    @Query("SELECT * FROM rolls ORDER BY updatedAt DESC")
    fun getAllRolls(): Flow<List<Roll>>

    @Query("SELECT * FROM rolls WHERE id = :id")
    suspend fun getRollById(id: Long): Roll?

    @Query("SELECT * FROM rolls WHERE id = :id")
    fun getRollByIdFlow(id: Long): Flow<Roll?>

    @Query("SELECT * FROM rolls ORDER BY updatedAt DESC LIMIT 1")
    suspend fun getLastUsedRoll(): Roll?

    @Insert
    suspend fun insertRoll(roll: Roll): Long

    @Update
    suspend fun updateRoll(roll: Roll)

    @Delete
    suspend fun deleteRoll(roll: Roll)

    @Query("DELETE FROM rolls WHERE id = :id")
    suspend fun deleteRollById(id: Long)
}
