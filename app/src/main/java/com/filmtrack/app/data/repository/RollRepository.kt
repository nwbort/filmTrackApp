package com.filmtrack.app.data.repository

import com.filmtrack.app.data.database.FrameDao
import com.filmtrack.app.data.database.RollDao
import com.filmtrack.app.data.model.Frame
import com.filmtrack.app.data.model.Roll
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RollRepository @Inject constructor(
    private val rollDao: RollDao,
    private val frameDao: FrameDao
) {
    fun getAllRolls(): Flow<List<Roll>> = rollDao.getAllRolls()

    fun getRollByIdFlow(id: Long): Flow<Roll?> = rollDao.getRollByIdFlow(id)

    suspend fun getRollById(id: Long): Roll? = rollDao.getRollById(id)

    suspend fun getLastUsedRoll(): Roll? = rollDao.getLastUsedRoll()

    suspend fun createRoll(roll: Roll): Long = rollDao.insertRoll(roll)

    suspend fun updateRoll(roll: Roll) = rollDao.updateRoll(
        roll.copy(updatedAt = System.currentTimeMillis())
    )

    suspend fun deleteRoll(id: Long) = rollDao.deleteRollById(id)

    fun getFramesForRoll(rollId: Long): Flow<List<Frame>> = frameDao.getFramesForRoll(rollId)

    suspend fun getFrameCount(rollId: Long): Int = frameDao.getFrameCount(rollId)

    suspend fun getFirstPhotoUris(rollId: Long, limit: Int = 6): List<String> =
        frameDao.getFirstPhotoUris(rollId, limit)

    suspend fun getNextFrameNumber(rollId: Long): Int {
        val max = frameDao.getMaxFrameNumber(rollId)
        return (max ?: 0) + 1
    }

    suspend fun addFrame(frame: Frame): Long {
        val id = frameDao.insertFrame(frame)
        // Update roll's updatedAt timestamp
        rollDao.getRollById(frame.rollId)?.let { roll ->
            rollDao.updateRoll(roll.copy(updatedAt = System.currentTimeMillis()))
        }
        return id
    }

    suspend fun updateFrame(frame: Frame) = frameDao.updateFrame(frame)

    suspend fun deleteFrame(id: Long) = frameDao.deleteFrameById(id)

    suspend fun getFrameById(id: Long): Frame? = frameDao.getFrameById(id)
}
