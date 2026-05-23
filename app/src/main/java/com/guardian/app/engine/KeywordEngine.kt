package com.guardian.app.engine

import javax.inject.Inject
import javax.inject.Singleton

enum class ThreatLevel { NONE, KEYWORD, LINK, IMAGE }

data class KeywordResult(
    val level: ThreatLevel,
    val matchedTerms: List<String> = emptyList(),
    val detectedUrl: String? = null
)

@Singleton
class KeywordEngine @Inject constructor() {

    private val keywordSet: Set<String> = KeywordsData.KEYWORDS

    val blockedDomains: Set<String> = setOf(
        "pornhub.com", "xvideos.com", "xnxx.com", "xhamster.com", "redtube.com",
        "youporn.com", "tube8.com", "spankbang.com", "beeg.com", "brazzers.com",
        "bangbros.com", "realitykings.com", "mofos.com", "naughtyamerica.com",
        "onlyfans.com", "fansly.com", "chaturbate.com", "livejasmin.com",
        "stripchat.com", "camsoda.com", "bongacams.com", "myfreecams.com",
        "fapello.com", "rule34.xxx", "gelbooru.com", "nhentai.net", "e-hentai.org",
        "bet365.com", "1xbet.com", "betway.com"
    )

    private val urlRegex = Regex(
        """(https?://|www\.)[^\s<>"{}|\\^`\[\]]+""",
        RegexOption.IGNORE_CASE
    )

    fun analyse(text: String): KeywordResult {
        if (text.isBlank()) return KeywordResult(ThreatLevel.NONE)

        urlRegex.find(text)?.let { match ->
            if (isBlockedUrl(match.value)) {
                return KeywordResult(ThreatLevel.LINK, detectedUrl = match.value)
            }
        }

        val tokens = tokenise(text)
        val matched = tokens.filter { it in keywordSet }
        if (matched.isNotEmpty()) {
            return KeywordResult(ThreatLevel.KEYWORD, matchedTerms = matched)
        }
        return KeywordResult(ThreatLevel.NONE)
    }

    fun isBlockedUrl(url: String): Boolean {
        val lower = url.lowercase()
        return blockedDomains.any { lower.contains(it) } ||
            keywordSet.any { kw -> kw.length > 5 && lower.contains(kw) }
    }

    fun isBlockedPackage(packageName: String): Boolean {
        val lower = packageName.lowercase()
        return lower.contains("porn") || lower.contains("adult") ||
            lower.contains("xxx") || lower.contains("casino") || lower.contains("bet")
    }

    private fun tokenise(text: String): List<String> =
        text.lowercase()
            .split(Regex("[\\s,،.!?؟\"'()\\[\\]{}/\\\\|\\-_=+@#\$%^&*]+"))
            .filter { it.length >= 2 }
}
