package com.antigravity.tiktoksubextract.data.api

import android.content.Context
import com.antigravity.tiktoksubextract.data.model.TikWmResponse
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit

class TikWmService(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build(),
    private val gson: Gson = Gson()
) {

    /**
     * Resolves TikTok video metadata and returns the downloadable media/audio URL.
     */
    suspend fun resolveTikTok(videoUrl: String): Result<TikWmResponse> = withContext(Dispatchers.IO) {
        try {
            val formBody = FormBody.Builder()
                .add("url", videoUrl)
                .build()

            val request = Request.Builder()
                .url(API_URL)
                .header("User-Agent", "Mozilla/5.0 (Android; Mobile; rv:120.0)")
                .post(formBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(
                        IOException("TikWM server error (HTTP ${response.code})")
                    )
                }

                val bodyString = response.body?.string()
                    ?: return@withContext Result.failure(IOException("TikWM trả về dữ liệu rỗng"))

                val tikWmResponse = gson.fromJson(bodyString, TikWmResponse::class.java)
                if (tikWmResponse.isSuccess && tikWmResponse.getMediaUrl() != null) {
                    Result.success(tikWmResponse)
                } else {
                    val errorMsg = tikWmResponse.msg ?: "Không tìm thấy link âm thanh từ video TikTok này"
                    Result.failure(IOException(errorMsg))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Downloads the audio file from URL to app cache directory.
     */
    suspend fun downloadAudio(context: Context, audioUrl: String): Result<File> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(audioUrl)
                .header("User-Agent", "Mozilla/5.0 (Android; Mobile; rv:120.0)")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(
                        IOException("Lỗi tải file âm thanh (HTTP ${response.code})")
                    )
                }

                val body = response.body
                    ?: return@withContext Result.failure(IOException("Không nhận được dữ liệu âm thanh"))

                val contentType = response.header("Content-Type") ?: ""
                val isVideo = audioUrl.contains(".mp4", ignoreCase = true) || contentType.contains("video", ignoreCase = true)
                val ext = if (isVideo) ".mp4" else ".mp3"

                val tempFile = File(context.cacheDir, "tiktok_media_${System.currentTimeMillis()}$ext")
                body.byteStream().use { input ->
                    FileOutputStream(tempFile).use { output ->
                        input.copyTo(output)
                    }
                }

                if (tempFile.length() == 0L) {
                    tempFile.delete()
                    return@withContext Result.failure(IOException("File âm thanh tải về có dung lượng 0 byte"))
                }

                // If downloaded file is a video, strip video track and extract only audio to M4A
                // This reduces file size by 90-95% (e.g. 30MB down to 1-2MB) and avoids Groq 413 error!
                if (isVideo) {
                    val audioOutputFile = File(context.cacheDir, "tiktok_audio_${System.currentTimeMillis()}.m4a")
                    val extractSuccess = com.antigravity.tiktoksubextract.util.AudioExtractor.extractAudioFromVideo(tempFile, audioOutputFile)
                    // Delete original video file
                    try { tempFile.delete() } catch (_: Exception) {}

                    if (extractSuccess && audioOutputFile.exists() && audioOutputFile.length() > 0L) {
                        Result.success(audioOutputFile)
                    } else {
                        // Fallback if demux failed
                        Result.success(tempFile)
                    }
                } else {
                    Result.success(tempFile)
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    companion object {
        private const val API_URL = "https://www.tikwm.com/api/"
    }
}
