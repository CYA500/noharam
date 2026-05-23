package com.guardian.app.data

import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ThreatEventRepository @Inject constructor(
    private val dao: ThreatEventDao
) {
    suspend fun record(level: Int) {
        dao.insert(ThreatEvent(level = level))
    }

    suspend fun weeklyDailyCounts(): List<Int> {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -6)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val since = cal.timeInMillis
        val rows = dao.dailyCountsSince(since)
        val counts = IntArray(7)
        for (row in rows) {
            val idx = row.dayOfWeek.coerceIn(0, 6)
            counts[idx] = row.cnt
        }
        return counts.toList()
    }

    suspend fun totalThisWeek(): Int {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -7)
        val since = cal.timeInMillis
        val rows = dao.countsByLevelSince(since)
        return rows.sumOf { it.cnt }
    }

    suspend fun cleanDaysStreak(): Int {
        val cal = Calendar.getInstance()
        var streak = 0
        for (i in 0 until 30) {
            cal.timeInMillis = System.currentTimeMillis()
            cal.add(Calendar.DAY_OF_YEAR, -i)
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            val dayStart = cal.timeInMillis
            cal.add(Calendar.DAY_OF_YEAR, 1)
            val dayEnd = cal.timeInMillis
            if (dao.countBetween(dayStart, dayEnd) == 0) streak++ else break
        }
        return streak
    }
}
