package com.antigravity.tiktoksubextract.data.model

import com.google.gson.annotations.SerializedName

data class WhisperResponse(
    @SerializedName("task") val task: String? = null,
    @SerializedName("language") val language: String? = null,
    @SerializedName("duration") val duration: Double? = null,
    @SerializedName("text") val text: String = "",
    @SerializedName("segments") val segments: List<WhisperSegment>? = null,
    @SerializedName("error") val error: GroqError? = null
)

data class WhisperSegment(
    @SerializedName("id") val id: Int = 0,
    @SerializedName("seek") val seek: Int = 0,
    @SerializedName("start") val start: Double = 0.0,
    @SerializedName("end") val end: Double = 0.0,
    @SerializedName("text") val text: String = ""
)

data class GroqError(
    @SerializedName("message") val message: String? = null,
    @SerializedName("type") val type: String? = null,
    @SerializedName("code") val code: String? = null
)
