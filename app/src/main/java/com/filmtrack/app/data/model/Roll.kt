package com.filmtrack.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "rolls")
data class Roll(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val filmStock: String = "",
    val camera: String = "",
    val iso: String = "",
    val exposureCount: Int = 36,
    val dateStarted: Long = System.currentTimeMillis(),
    val dateFinished: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
