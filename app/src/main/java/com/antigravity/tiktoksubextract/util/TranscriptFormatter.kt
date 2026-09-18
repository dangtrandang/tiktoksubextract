package com.antigravity.tiktoksubextract.util

import com.antigravity.tiktoksubextract.data.model.WhisperResponse
import com.antigravity.tiktoksubextract.data.model.WhisperSegment
import java.util.Locale

object TranscriptFormatter {

    /**
     * Converts response to continuous plain text for ChatGPT reading.
     */
    fun toPlainText(response: WhisperResponse): String {
        val segments = response.segments
        return if (!segments.isNullOrEmpty()) {
            segments.joinToString(" ") { it.text.trim() }
        } else {
            response.text.trim()
        }
    }

    /**
     * Formats segments with timestamp: [mm:ss] Text
     */
    fun toTimestampText(response: WhisperResponse): String {
        val segments = response.segments
        if (segments.isNullOrEmpty()) {
            return response.text.trim()
        }

        return segments.joinToString("\n") { segment ->
            val timestamp = formatMmSs(segment.start)
            "[$timestamp] ${segment.text.trim()}"
        }
    }

    /**
     * Formats segments into standard SubRip (.SRT) format.
     */
    fun toSrt(response: WhisperResponse): String {
        val segments = response.segments
        if (segments.isNullOrEmpty()) {
            return "1\n00:00:00,000 --> 00:00:05,000\n${response.text.trim()}"
        }

        val sb = StringBuilder()
        segments.forEachIndexed { index, segment ->
            sb.append(index + 1).append("\n")
            sb.append(formatSrtTime(segment.start))
                .append(" --> ")
                .append(formatSrtTime(segment.end))
                .append("\n")
            sb.append(segment.text.trim()).append("\n\n")
        }
        return sb.toString().trim()
    }

    private fun formatMmSs(secondsTotal: Double): String {
        val totalSec = secondsTotal.toLong()
        val hours = totalSec / 3600
        val minutes = (totalSec % 3600) / 60
        val seconds = totalSec % 60

        return if (hours > 0) {
            String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.US, "%02d:%02d", minutes, seconds)
        }
    }

    private fun formatSrtTime(secondsTotal: Double): String {
        val totalMillis = (secondsTotal * 1000).toLong()
        val hours = totalMillis / 3600000
        val minutes = (totalMillis % 3600000) / 60000
        val seconds = (totalMillis % 60000) / 1000
        val millis = totalMillis % 1000

        return String.format(Locale.US, "%02d:%02d:%02d,%03d", hours, minutes, seconds, millis)
    }
}
