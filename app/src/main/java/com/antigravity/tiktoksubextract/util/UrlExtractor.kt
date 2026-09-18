package com.antigravity.tiktoksubextract.util

import java.util.regex.Pattern

object UrlExtractor {
    // Regex matching TikTok web links, shortlinks (vt.tiktok.com, vm.tiktok.com), mobile links
    private val TIKTOK_URL_REGEX = Pattern.compile(
        "https?://(?:(?:www|vm|vt|m)\\.)?tiktok\\.com/(?:@[^/]+/video/\\d+|\\w+/[^\\s?#]+|[\\w/]+)[^\\s]*",
        Pattern.CASE_INSENSITIVE
    )

    // Generic URL regex fallback if TikTok domain varies
    private val GENERIC_URL_REGEX = Pattern.compile(
        "https?://[^\\s]+",
        Pattern.CASE_INSENSITIVE
    )

    /**
     * Extracts a TikTok URL from incoming shared text.
     */
    fun extractTikTokUrl(text: String?): String? {
        if (text.isNullOrBlank()) return null

        // 1. Try TikTok specific pattern
        val matcher = TIKTOK_URL_REGEX.matcher(text)
        if (matcher.find()) {
            return cleanUrl(matcher.group())
        }

        // 2. Try generic URL matcher containing 'tiktok'
        val genericMatcher = GENERIC_URL_REGEX.matcher(text)
        while (genericMatcher.find()) {
            val candidate = genericMatcher.group()
            if (candidate.contains("tiktok", ignoreCase = true)) {
                return cleanUrl(candidate)
            }
        }

        return null
    }

    private fun cleanUrl(raw: String): String {
        // Strip trailing punctuation like ., ), ], etc. that might be attached from text
        var cleaned = raw.trim()
        while (cleaned.endsWith(".") || cleaned.endsWith(",") || cleaned.endsWith(")") || cleaned.endsWith("]")) {
            cleaned = cleaned.substring(0, cleaned.length - 1)
        }
        return cleaned
    }
}
