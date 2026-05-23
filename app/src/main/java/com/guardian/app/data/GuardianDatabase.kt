package com.guardian.app.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [ThreatEvent::class], version = 1, exportSchema = false)
abstract class GuardianDatabase : RoomDatabase() {
    abstract fun threatEventDao(): ThreatEventDao
}
