package com.filmtrack.app.data.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.filmtrack.app.data.model.Frame
import kotlinx.coroutines.flow.Flow

@Dao
interface FrameDao {
    @Query("SELECT * FROM frames WHERE rollId = :rollId ORDER BY frameNumber ASC")
    fun getFramesForRoll(rollId: Long): Flow<List<Frame>>

    @Query("SELECT * FROM frames WHERE id = :id")
    suspend fun getFrameById(id: Long): Frame?

    @Query("SELECT COUNT(*) FROM frames WHERE rollId = :rollId")
    suspend fun getFrameCount(rollId: Long): Int

    @Query("SELECT MAX(frameNumber) FROM frames WHERE rollId = :rollId")
    suspend fun getMaxFrameNumber(rollId: Long): Int?

    @Query("SELECT photoUri FROM frames WHERE rollId = :rollId ORDER BY frameNumber ASC LIMIT :limit")
    suspend fun getFirstPhotoUris(rollId: Long, limit: Int): List<String>

    @Insert
    suspend fun insertFrame(frame: Frame): Long

    @Update
    suspend fun updateFrame(frame: Frame)

    @Delete
    suspend fun deleteFrame(frame: Frame)

    @Query("DELETE FROM frames WHERE id = :id")
    suspend fun deleteFrameById(id: Long)
}
