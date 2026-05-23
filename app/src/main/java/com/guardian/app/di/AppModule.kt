package com.guardian.app.di

import android.content.Context
import androidx.room.Room
import com.guardian.app.data.GuardianDatabase
import com.guardian.app.data.ThreatEventDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): GuardianDatabase =
        Room.databaseBuilder(context, GuardianDatabase::class.java, "guardian.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideThreatEventDao(db: GuardianDatabase): ThreatEventDao = db.threatEventDao()
}
