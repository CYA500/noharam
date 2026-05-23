package com.guardian.app.engine

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GuardianAI @Inject constructor() {

    data class AnalysisReport(
        val approved: Boolean,
        val isSpam: Boolean,
        val isCoherent: Boolean,
        val responseMessage: String,
        val remainingMs: Long
    )

    fun analyzeUnlockRequest(
        text: String,
        wordTimestampsMs: List<Long>,
        remainingMs: Long
    ): Boolean {
        analyzeUnlockRequestDetailed(text, wordTimestampsMs, remainingMs)
        return false
    }

    fun analyzeUnlockRequestDetailed(
        text: String,
        wordTimestampsMs: List<Long>,
        remainingMs: Long
    ): AnalysisReport {
        val isSpam = detectSpam(text, wordTimestampsMs)
        val isCoherent = !isSpam && detectCoherence(text)
        val remaining = formatDuration(remainingMs)
        val response = buildString {
            append("فهمنا ما كتبت.\n")
            append("لكن أنت من اختار هذا القرار بنفسك.\n")
            append("الوقت المتبقي: $remaining\n\n")
            append("﴿وَاصْبِرْ وَمَا صَبْرُكَ إِلَّا بِاللَّهِ﴾")
        }
        return AnalysisReport(
            approved = false,
            isSpam = isSpam,
            isCoherent = isCoherent,
            responseMessage = response,
            remainingMs = remainingMs
        )
    }

    private fun detectSpam(text: String, wordTimestampsMs: List<Long>): Boolean {
        if (text.isBlank()) return true
        val words = text.trim().split(Regex("\\s+"))
        if (words.size < 2) return true

        if (wordTimestampsMs.size >= 2) {
            var totalGap = 0L
            for (i in 1 until wordTimestampsMs.size) {
                totalGap += wordTimestampsMs[i] - wordTimestampsMs[i - 1]
            }
            val avgMs = totalGap.toDouble() / (wordTimestampsMs.size - 1)
            if (avgMs < 300) return true
        }

        val avgLen = text.replace(" ", "").length.toDouble() / words.size
        if (avgLen < 2.5) return true

        val distinct = words.map { it.lowercase() }.toSet()
        if (distinct.size.toDouble() / words.size < 0.4) return true
        return false
    }

    private fun detectCoherence(text: String): Boolean {
        val lower = text.lowercase()
        if (lower.length < 20) return false
        val connectors = listOf(
            "لأن", "بسبب", "أريد", "أحتاج", "ضروري", "مهم", "عاجل",
            "because", "need", "urgent", "important", "must", "have to",
            "emergency", "طوارئ", "ضرورة"
        )
        return connectors.any { lower.contains(it) }
    }

    private fun formatDuration(ms: Long): String {
        if (ms <= 0) return "00:00:00"
        val totalSeconds = ms / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return "%02d:%02d:%02d".format(hours, minutes, seconds)
    }
}
