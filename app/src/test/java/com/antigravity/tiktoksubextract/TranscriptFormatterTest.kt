package com.antigravity.tiktoksubextract

import com.antigravity.tiktoksubextract.data.model.WhisperResponse
import com.antigravity.tiktoksubextract.data.model.WhisperSegment
import com.antigravity.tiktoksubextract.util.TranscriptFormatter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TranscriptFormatterTest {

    @Test
    fun testPlainTextFormatting() {
        val segments = listOf(
            WhisperSegment(id = 0, start = 0.0, end = 2.5, text = "Xin chào các bạn"),
            WhisperSegment(id = 1, start = 2.5, end = 5.0, text = "Hôm nay tôi sẽ chia sẻ")
        )
        val response = WhisperResponse(text = "Xin chào các bạn Hôm nay tôi sẽ chia sẻ", segments = segments)

        val plainText = TranscriptFormatter.toPlainText(response)
        assertEquals("Xin chào các bạn Hôm nay tôi sẽ chia sẻ", plainText)
    }

    @Test
    fun testTimestampFormatting() {
        val segments = listOf(
            WhisperSegment(id = 0, start = 4.2, end = 8.0, text = "Đoạn thứ nhất"),
            WhisperSegment(id = 1, start = 65.0, end = 70.0, text = "Đoạn thứ hai")
        )
        val response = WhisperResponse(segments = segments)

        val timestampText = TranscriptFormatter.toTimestampText(response)
        val lines = timestampText.lines()
        assertEquals("[00:04] Đoạn thứ nhất", lines[0])
        assertEquals("[01:05] Đoạn thứ hai", lines[1])
    }

    @Test
    fun testSrtFormatting() {
        val segments = listOf(
            WhisperSegment(id = 0, start = 1.5, end = 4.25, text = "Hello world")
        )
        val response = WhisperResponse(segments = segments)

        val srt = TranscriptFormatter.toSrt(response)
        assertTrue(srt.contains("00:00:01,500 --> 00:00:04,250"))
        assertTrue(srt.contains("Hello world"))
    }
}
