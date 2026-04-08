package com.filmtrack.app.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "frames",
    foreignKeys = [
        ForeignKey(
            entity = Roll::class,
            parentColumns = ["id"],
            childColumns = ["rollId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("rollId")]
)
data class Frame(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val rollId: Long,
    val frameNumber: Int,
    val photoUri: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val capturedAt: Long = System.currentTimeMillis(),
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
