package com.notivas.di

import android.content.Context
import androidx.room.Room
import com.notivas.data.local.CanvasDatabase
import com.notivas.data.local.dao.AssignmentDao
import com.notivas.data.local.dao.CourseDao
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
    fun provideDatabase(@ApplicationContext context: Context): CanvasDatabase {
        return Room.databaseBuilder(
            context,
            CanvasDatabase::class.java,
            "canvas_db"
        ).build()
    }

    @Provides
    fun provideCourseDao(db: CanvasDatabase): CourseDao = db.courseDao()

    @Provides
    fun provideAssignmentDao(db: CanvasDatabase): AssignmentDao = db.assignmentDao()
}
