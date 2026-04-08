package com.filmtrack.app.di

import android.content.Context
import androidx.room.Room
import com.filmtrack.app.data.database.AppDatabase
import com.filmtrack.app.data.database.FrameDao
import com.filmtrack.app.data.database.RollDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "filmtrack.db"
        ).build()
    }

    @Provides
    fun provideRollDao(database: AppDatabase): RollDao = database.rollDao()

    @Provides
    fun provideFrameDao(database: AppDatabase): FrameDao = database.frameDao()
}
