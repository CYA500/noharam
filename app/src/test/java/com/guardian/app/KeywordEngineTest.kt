package com.guardian.app

import com.guardian.app.engine.GuardianAI
import com.guardian.app.engine.KeywordEngine
import com.guardian.app.engine.KeywordsData
import com.guardian.app.engine.ThreatLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class KeywordEngineTest {

    @Test
    fun keywordSet_hasExactly300Terms() {
        assertEquals(300, KeywordsData.KEYWORDS.size)
    }

    @Test
    fun analyse_detectsKeyword() {
        val engine = KeywordEngine()
        val result = engine.analyse("هذا نص يحتوي porn")
        assertEquals(ThreatLevel.KEYWORD, result.level)
    }

    @Test
    fun analyse_detectsBlockedUrl() {
        val engine = KeywordEngine()
        val result = engine.analyse("visit https://pornhub.com/video")
        assertEquals(ThreatLevel.LINK, result.level)
    }

    @Test
    fun guardianAI_alwaysRejectsUnlock() {
        val ai = GuardianAI()
        val approved = ai.analyzeUnlockRequest(
            text = "أريد الفتح لأن عندي عمل مهم جداً اليوم",
            wordTimestampsMs = listOf(0L, 500L, 1200L, 2000L),
            remainingMs = 3_600_000L
        )
        assertFalse(approved)
    }
}
