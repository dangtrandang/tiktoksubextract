package com.antigravity.tiktoksubextract.data.api

import com.antigravity.tiktoksubextract.data.model.WhisperResponse
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

class GroqWhisperService(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .writeTimeout(90, TimeUnit.SECONDS)
        .build(),
    private val gson: Gson = Gson()
) {

    /**
     * Transcribes an audio file using Groq Whisper API with verbose_json output.
     */
    suspend fun transcribeAudio(
        apiKey: String,
        audioFile: File,
        model: String = "whisper-large-v3",
        language: String? = null
    ): Result<WhisperResponse> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext Result.failure(IOException("Groq API Key chưa được cài đặt. Vui lòng vào Cài đặt để thêm key."))
        }

        try {
            val mediaType = when {
                audioFile.name.endsWith(".mp4", ignoreCase = true) -> "video/mp4".toMediaTypeOrNull()
                audioFile.name.endsWith(".m4a", ignoreCase = true) -> "audio/m4a".toMediaTypeOrNull()
                audioFile.name.endsWith(".wav", ignoreCase = true) -> "audio/wav".toMediaTypeOrNull()
                else -> "audio/mpeg".toMediaTypeOrNull()
            }
            val fileBody = audioFile.asRequestBody(mediaType)

            val multipartBuilder = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", audioFile.name, fileBody)
                .addFormDataPart("model", model)
                .addFormDataPart("response_format", "verbose_json")
                .addFormDataPart("temperature", "0.0")
                .addFormDataPart("prompt", "Lời thoại tiếng Việt trong video ngắn.")

            if (!language.isNullOrBlank() && language != "auto") {
                multipartBuilder.addFormDataPart("language", language)
            }

            val request = Request.Builder()
                .url(GROQ_TRANSCRIPTION_URL)
                .header("Authorization", "Bearer $apiKey")
                .post(multipartBuilder.build())
                .build()

            client.newCall(request).execute().use { response ->
                val bodyString = response.body?.string() ?: ""

                if (!response.isSuccessful) {
                    val errorMsg = try {
                        val parsed = gson.fromJson(bodyString, WhisperResponse::class.java)
                        parsed.error?.message ?: "Lỗi Groq API (HTTP ${response.code})"
                    } catch (e: Exception) {
                        "Lỗi Groq API (HTTP ${response.code}): $bodyString"
                    }
                    return@withContext Result.failure(IOException(errorMsg))
                }

                val whisperResponse = gson.fromJson(bodyString, WhisperResponse::class.java)
                Result.success(whisperResponse)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Tests whether the provided Groq API key is valid.
     */
    suspend fun testApiKey(apiKey: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("https://api.groq.com/openai/v1/models")
                .header("Authorization", "Bearer $apiKey")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Result.success(true)
                } else {
                    Result.failure(IOException("API Key không hợp lệ hoặc đã hết hạn (HTTP ${response.code})"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Fetches and categorizes available models from Groq into (WhisperModels, LlmModels).
     */
    suspend fun fetchAvailableModels(apiKey: String): Result<Pair<List<String>, List<String>>> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("https://api.groq.com/openai/v1/models")
                .header("Authorization", "Bearer $apiKey")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(IOException("Không thể tải danh sách model (HTTP ${response.code})"))
                }

                val bodyString = response.body?.string() ?: ""
                val modelsResponse = gson.fromJson(bodyString, ModelsResponse::class.java)
                val allIds = modelsResponse.data?.map { it.id } ?: emptyList()

                val whisperModels = allIds.filter { it.contains("whisper", ignoreCase = true) }
                    .sorted()

                val llmModels = allIds.filter { id ->
                    !id.contains("whisper", ignoreCase = true) &&
                    !id.contains("guard", ignoreCase = true) &&
                    !id.contains("orpheus", ignoreCase = true)
                }.sorted()

                Result.success(Pair(whisperModels, llmModels))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private data class ModelsResponse(
        val data: List<ModelItem>? = null
    )
    private data class ModelItem(
        val id: String
    )

    companion object {
        private const val GROQ_TRANSCRIPTION_URL = "https://api.groq.com/openai/v1/audio/transcriptions"
    }
}
