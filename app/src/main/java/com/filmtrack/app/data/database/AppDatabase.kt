package com.filmtrack.app.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.filmtrack.app.data.model.Frame
import com.filmtrack.app.data.model.Roll

@Database(
    entities = [Roll::class, Frame::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun rollDao(): RollDao
    abstract fun frameDao(): FrameDao
}
