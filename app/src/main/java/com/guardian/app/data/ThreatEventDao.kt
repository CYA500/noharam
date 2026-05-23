package com.guardian.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface ThreatEventDao {
    @Insert
    suspend fun insert(event: ThreatEvent)

    @Query("SELECT COUNT(*) FROM threat_events WHERE level = :level AND timestampMs >= :sinceMs")
    suspend fun countSince(level: Int, sinceMs: Long): Int

    @Query(
        """
        SELECT level, COUNT(*) AS cnt FROM threat_events
        WHERE timestampMs >= :sinceMs
        GROUP BY level
        """
    )
    suspend fun countsByLevelSince(sinceMs: Long): List<LevelCount>

    @Query(
        """
        SELECT CAST(strftime('%w', datetime(timestampMs/1000, 'unixepoch')) AS INTEGER) AS dayOfWeek,
               COUNT(*) AS cnt
        FROM threat_events
        WHERE timestampMs >= :sinceMs
        GROUP BY dayOfWeek
        ORDER BY dayOfWeek
        """
    )
    suspend fun dailyCountsSince(sinceMs: Long): List<DayCount>

    @Query("SELECT COUNT(*) FROM threat_events WHERE timestampMs >= :startMs AND timestampMs < :endMs")
    suspend fun countBetween(startMs: Long, endMs: Long): Int
}

data class LevelCount(val level: Int, val cnt: Int)
data class DayCount(val dayOfWeek: Int, val cnt: Int)
