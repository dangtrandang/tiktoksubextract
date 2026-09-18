package com.antigravity.tiktoksubextract.data.api

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

class GroqLlamaService(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build(),
    private val gson: Gson = Gson()
) {

    /**
     * Contextually corrects homophone errors, typos, and terms in Vietnamese transcript
     * using Groq's ultra-fast Llama 3.3 70B model.
     */
    suspend fun polishTranscript(apiKey: String, rawText: String): Result<String> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext Result.failure(IOException("Chưa cấu hình Groq API Key"))
        }

        if (rawText.isBlank()) {
            return@withContext Result.success(rawText)
        }

        try {
            val systemPrompt = """
Bạn là chuyên gia hiệu đính văn bản bóc băng giọng nói tiếng Việt từ video.
Nhiệm vụ:
1. Đọc kỹ văn bản bóc băng (Speech-to-Text) dưới đây.
2. Dựa vào ngữ cảnh toàn bài, sửa lại các từ đồng âm/gần âm hoặc từ địa phương bị nhận diện sai chính tả tiếng Việt.
3. Chuẩn hóa đúng các thuật ngữ tiếng Anh, công nghệ, mạng xã hội, tên riêng (ví dụ: TikTok, AI, ChatGPT, Marketing, Prompt, View, Follow, Affiliate, Gen Z, Viral...).
4. Thêm dấu câu (chấm, phẩy, hỏi, than) và viết hoa đúng vị trí để văn bản mạch lạc, tự nhiên, dễ đọc.
QUY TẮC BẮT BUỘC:
- Giữ nguyên 100% nội dung, lời nói và văn phong của người nói.
- KHÔNG tóm tắt, KHÔNG thêm bớt ý, KHÔNG viết thêm bất kỳ lời bình luận hay giải thích nào.
- Chỉ trả về duy nhất đoạn văn bản tiếng Việt đã được sửa lỗi, không thêm bất kỳ văn bản nào khác.
""".trimIndent()

            val requestPayload = ChatCompletionRequest(
                model = "llama-3.3-70b-versatile",
                temperature = 0.1,
                messages = listOf(
                    ChatMessage(role = "system", content = systemPrompt),
                    ChatMessage(role = "user", content = rawText)
                )
            )

            val jsonBody = gson.toJson(requestPayload)
            val requestBody = jsonBody.toRequestBody("application/json; charset=utf-8".toMediaType())

            val request = Request.Builder()
                .url(GROQ_CHAT_URL)
                .header("Authorization", "Bearer $apiKey")
                .header("Content-Type", "application/json")
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                val bodyString = response.body?.string() ?: ""

                if (!response.isSuccessful) {
                    return@withContext Result.failure(
                        IOException("Lỗi Groq Llama API (HTTP ${response.code}): $bodyString")
                    )
                }

                val chatResponse = gson.fromJson(bodyString, ChatCompletionResponse::class.java)
                val correctedText = chatResponse.choices?.firstOrNull()?.message?.content?.trim()

                if (!correctedText.isNullOrBlank()) {
                    Result.success(correctedText)
                } else {
                    Result.success(rawText)
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    companion object {
        private const val GROQ_CHAT_URL = "https://api.groq.com/openai/v1/chat/completions"
    }
}

data class ChatCompletionRequest(
    @SerializedName("model") val model: String,
    @SerializedName("temperature") val temperature: Double,
    @SerializedName("messages") val messages: List<ChatMessage>
)

data class ChatMessage(
    @SerializedName("role") val role: String,
    @SerializedName("content") val content: String
)

data class ChatCompletionResponse(
    @SerializedName("id") val id: String? = null,
    @SerializedName("choices") val choices: List<ChatChoice>? = null
)

data class ChatChoice(
    @SerializedName("index") val index: Int = 0,
    @SerializedName("message") val message: ChatMessage? = null
)
