package com.guardian.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "threat_events")
data class ThreatEvent(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val level: Int,
    val timestampMs: Long = System.currentTimeMillis()
)
