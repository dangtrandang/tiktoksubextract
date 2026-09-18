package com.antigravity.tiktoksubextract.data.model

import com.google.gson.annotations.SerializedName

data class TikWmResponse(
    @SerializedName("code") val code: Int = -1,
    @SerializedName("msg") val msg: String? = null,
    @SerializedName("data") val data: TikWmData? = null
) {
    val isSuccess: Boolean get() = code == 0 && data != null

    /**
     * Resolves the media URL containing the actual video audio (creator voice).
     * IMPORTANT: We MUST prioritize the video stream (data.play) over music_info!
     * In TikTok, music_info.play is only the background music track from the TikTok sound library,
     * which does NOT contain the creator's voice and causes Whisper to hallucinate ('Ghiền Mì Gõ').
     * data.play is the actual video file which contains the mixed voice + audio.
     */
    fun getMediaUrl(): String? {
        val d = data ?: return null

        // 1. Prefer video stream which contains the real mixed voice track
        d.play?.let { if (it.isNotBlank()) return formatUrl(it) }
        d.wmPlay?.let { if (it.isNotBlank()) return formatUrl(it) }

        // 2. Fallback to music track if video stream is absent
        d.music?.let { if (it.isNotBlank()) return formatUrl(it) }
        d.musicInfo?.play?.let { if (it.isNotBlank()) return formatUrl(it) }

        return null
    }

    private fun formatUrl(url: String): String {
        return if (url.startsWith("http://") || url.startsWith("https://")) {
            url
        } else {
            "https://www.tikwm.com$url"
        }
    }
}

data class TikWmData(
    @SerializedName("id") val id: String? = null,
    @SerializedName("title") val title: String? = null,
    @SerializedName("duration") val duration: Int = 0,
    @SerializedName("play") val play: String? = null,
    @SerializedName("wmplay") val wmPlay: String? = null,
    @SerializedName("music") val music: String? = null,
    @SerializedName("music_info") val musicInfo: TikWmMusicInfo? = null
)

data class TikWmMusicInfo(
    @SerializedName("id") val id: String? = null,
    @SerializedName("title") val title: String? = null,
    @SerializedName("play") val play: String? = null
)
