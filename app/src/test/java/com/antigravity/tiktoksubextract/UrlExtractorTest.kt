package com.antigravity.tiktoksubextract

import com.antigravity.tiktoksubextract.util.UrlExtractor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class UrlExtractorTest {

    @Test
    fun testExtractShortLink() {
        val input = "Xem video này hay lắm https://vt.tiktok.com/ZSqsonyxF/ #fyp"
        val extracted = UrlExtractor.extractTikTokUrl(input)
        assertNotNull(extracted)
        assertEquals("https://vt.tiktok.com/ZSqsonyxF/", extracted)
    }

    @Test
    fun testExtractFullWebLink() {
        val input = "https://www.tiktok.com/@username/video/7106594312292453678?is_from_webapp=1"
        val extracted = UrlExtractor.extractTikTokUrl(input)
        assertNotNull(extracted)
        assertEquals("https://www.tiktok.com/@username/video/7106594312292453678?is_from_webapp=1", extracted)
    }

    @Test
    fun testExtractWithPunctuation() {
        val input = "Check this out (https://vt.tiktok.com/ZSqsonyxF/)."
        val extracted = UrlExtractor.extractTikTokUrl(input)
        assertEquals("https://vt.tiktok.com/ZSqsonyxF/", extracted)
    }
}
