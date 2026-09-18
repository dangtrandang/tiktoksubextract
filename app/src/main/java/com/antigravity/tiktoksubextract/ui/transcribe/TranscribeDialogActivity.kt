package com.antigravity.tiktoksubextract.ui.transcribe

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.antigravity.tiktoksubextract.R
import com.antigravity.tiktoksubextract.data.api.GroqWhisperService
import com.antigravity.tiktoksubextract.data.api.TikWmService
import com.antigravity.tiktoksubextract.data.model.WhisperResponse
import com.antigravity.tiktoksubextract.data.pref.AppPreferences
import com.antigravity.tiktoksubextract.databinding.ActivityTranscribeDialogBinding
import com.antigravity.tiktoksubextract.ui.main.MainActivity
import com.antigravity.tiktoksubextract.util.TranscriptFormatter
import com.antigravity.tiktoksubextract.util.UrlExtractor
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.File

class TranscribeDialogActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTranscribeDialogBinding
    private lateinit var prefs: AppPreferences
    private val tikWmService = TikWmService()
    private val whisperService = GroqWhisperService()

    private var transcribeJob: Job? = null
    private var detectedUrl: String? = null
    private var whisperResult: WhisperResponse? = null
    private var currentTab: OutputTab = OutputTab.PLAIN_TEXT
    private var tempAudioFile: File? = null

    enum class OutputTab {
        PLAIN_TEXT, TIMESTAMP, SRT
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTranscribeDialogBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = AppPreferences(this)

        setupListeners()
        processIncomingIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        processIncomingIntent(intent)
    }

    private fun setupListeners() {
        binding.btnClose.setOnClickListener {
            cancelAndFinish()
        }

        binding.tabPlainText.setOnClickListener {
            selectTab(OutputTab.PLAIN_TEXT)
        }

        binding.tabTimestamp.setOnClickListener {
            selectTab(OutputTab.TIMESTAMP)
        }

        binding.tabSrt.setOnClickListener {
            selectTab(OutputTab.SRT)
        }

        binding.btnCopy.setOnClickListener {
            copyCurrentTextToClipboard()
        }

        binding.btnShare.setOnClickListener {
            shareCurrentText()
        }

        binding.btnRetry.setOnClickListener {
            detectedUrl?.let { startPipeline(it) }
        }

        binding.btnPasteFromClipboard.setOnClickListener {
            readAndProcessClipboard()
        }

        binding.btnOpenSettings.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        binding.btnStartTranscribe.setOnClickListener {
            detectedUrl?.let { startPipeline(it) }
        }

        binding.btnCancelManual.setOnClickListener {
            cancelAndFinish()
        }
    }

    private fun processIncomingIntent(intent: Intent?) {
        if (intent == null) {
            showError("Không nhận được dữ liệu từ ứng dụng khác.")
            return
        }

        val isFromClipboard = intent.getBooleanExtra(EXTRA_READ_CLIPBOARD, false)

        if (isFromClipboard) {
            readAndProcessClipboard()
            return
        }

        var textToExtract: String? = null
        if (intent.action == Intent.ACTION_SEND && intent.type?.startsWith("text/") == true) {
            textToExtract = intent.getStringExtra(Intent.EXTRA_TEXT)
        } else if (intent.hasExtra(EXTRA_URL)) {
            textToExtract = intent.getStringExtra(EXTRA_URL)
        }

        handleExtractedText(textToExtract, isFromClipboard = false)
    }

    private fun readAndProcessClipboard() {
        binding.layoutError.visibility = View.GONE
        binding.tvDetectedUrl.text = "Đang kiểm tra khay nhớ tạm..."

        lifecycleScope.launch {
            var foundUrl: String? = null
            // Retry up to 8 times (every 150ms) to allow window focus to settle after notification collapse
            for (i in 0 until 8) {
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = clipboard.primaryClip
                if (clip != null && clip.itemCount > 0) {
                    val rawText = clip.getItemAt(0).text?.toString()
                    val candidate = UrlExtractor.extractTikTokUrl(rawText)
                    if (candidate != null) {
                        foundUrl = candidate
                        break
                    }
                }
                kotlinx.coroutines.delay(150)
            }

            if (foundUrl != null) {
                handleExtractedText(foundUrl, isFromClipboard = true)
            } else {
                showError("Không tìm thấy link TikTok trong bộ nhớ tạm (Clipboard).\nHãy vào TikTok, bấm 'Sao chép liên kết' rồi chạm lại vào thông báo nhé!")
            }
        }
    }

    private fun handleExtractedText(rawText: String?, isFromClipboard: Boolean) {
        val url = UrlExtractor.extractTikTokUrl(rawText)
        if (url.isNullOrBlank()) {
            val msg = if (isFromClipboard) {
                "Không tìm thấy link TikTok trong bộ nhớ tạm (Clipboard).\nHãy vào TikTok, bấm 'Sao chép liên kết' rồi chạm lại vào thông báo nhé!"
            } else {
                "Không tìm thấy link TikTok hợp lệ trong nội dung chia sẻ:\n\"${rawText ?: ""}\""
            }
            showError(msg)
            return
        }

        detectedUrl = url
        binding.tvDetectedUrl.text = url

        // Check if Groq API Key is configured
        if (!prefs.isApiKeyConfigured) {
            showError("Bạn chưa nhập Groq API Key. Vui lòng vào Cài đặt để thêm key trước khi sử dụng.")
            binding.btnOpenSettings.visibility = View.VISIBLE
            return
        }

        if (prefs.autoStartOnShare || isFromClipboard) {
            startPipeline(url)
        } else {
            // Show confirmation buttons
            binding.layoutStartActions.visibility = View.VISIBLE
            setStep1Done()
        }
    }

    private fun startPipeline(videoUrl: String) {
        binding.layoutStartActions.visibility = View.GONE
        binding.layoutError.visibility = View.GONE
        binding.layoutResult.visibility = View.GONE

        transcribeJob?.cancel()
        transcribeJob = lifecycleScope.launch {
            try {
                // Step 1: Detect URL
                setStep1Done()

                // Step 2: Download Audio from TikWM
                setStep2Running()
                val tikWmResult = tikWmService.resolveTikTok(videoUrl)
                if (tikWmResult.isFailure) {
                    val ex = tikWmResult.exceptionOrNull()
                    setStep2Error()
                    showError("Lỗi TikWM: ${ex?.message ?: "Không lấy được audio từ link này"}")
                    return@launch
                }

                val mediaUrl = tikWmResult.getOrNull()?.getMediaUrl()
                if (mediaUrl.isNullOrBlank()) {
                    setStep2Error()
                    showError("Không tìm thấy link audio hoặc video từ TikWM.")
                    return@launch
                }

                val downloadResult = tikWmService.downloadAudio(this@TranscribeDialogActivity, mediaUrl)
                if (downloadResult.isFailure) {
                    val ex = downloadResult.exceptionOrNull()
                    setStep2Error()
                    showError("Lỗi tải âm thanh: ${ex?.message ?: "Không tải được file âm thanh"}")
                    return@launch
                }

                val audioFile = downloadResult.getOrThrow()
                tempAudioFile = audioFile
                setStep2Done()

                // Step 3: Transcribe via Groq Whisper
                setStep3Running()
                val whisperResultOutcome = whisperService.transcribeAudio(
                    apiKey = prefs.groqApiKey,
                    audioFile = audioFile,
                    model = prefs.whisperModel,
                    language = if (prefs.language == "auto") null else prefs.language
                )

                // Clean up temp audio file immediately
                try {
                    if (audioFile.exists()) audioFile.delete()
                } catch (_: Exception) {}

                if (whisperResultOutcome.isFailure) {
                    val ex = whisperResultOutcome.exceptionOrNull()
                    setStep3Error()
                    showError("Lỗi Groq Whisper AI: ${ex?.message ?: "Quá trình nhận diện thất bại"}")
                    return@launch
                }

                val response = whisperResultOutcome.getOrThrow()
                whisperResult = response

                // Step 4: Done!
                setStep3Done()
                setStep4Done()
                displayResult(response)

            } catch (e: Exception) {
                showError("Đã xảy ra lỗi: ${e.localizedMessage}")
            } finally {
                // Ensure audio file is cleaned up
                tempAudioFile?.let {
                    try { if (it.exists()) it.delete() } catch (_: Exception) {}
                }
            }
        }
    }

    private fun displayResult(response: WhisperResponse) {
        binding.layoutResult.visibility = View.VISIBLE
        selectTab(OutputTab.PLAIN_TEXT)
    }

    private fun selectTab(tab: OutputTab) {
        currentTab = tab
        val response = whisperResult ?: return

        // Update tab styles
        binding.tabPlainText.setBackgroundResource(
            if (tab == OutputTab.PLAIN_TEXT) R.drawable.bg_segment_selected else android.R.color.transparent
        )
        binding.tabPlainText.setTextColor(
            getColor(if (tab == OutputTab.PLAIN_TEXT) R.color.text_primary else R.color.text_secondary)
        )

        binding.tabTimestamp.setBackgroundResource(
            if (tab == OutputTab.TIMESTAMP) R.drawable.bg_segment_selected else android.R.color.transparent
        )
        binding.tabTimestamp.setTextColor(
            getColor(if (tab == OutputTab.TIMESTAMP) R.color.text_primary else R.color.text_secondary)
        )

        binding.tabSrt.setBackgroundResource(
            if (tab == OutputTab.SRT) R.drawable.bg_segment_selected else android.R.color.transparent
        )
        binding.tabSrt.setTextColor(
            getColor(if (tab == OutputTab.SRT) R.color.text_primary else R.color.text_secondary)
        )

        // Update text preview
        val text = when (tab) {
            OutputTab.PLAIN_TEXT -> TranscriptFormatter.toPlainText(response)
            OutputTab.TIMESTAMP -> TranscriptFormatter.toTimestampText(response)
            OutputTab.SRT -> TranscriptFormatter.toSrt(response)
        }
        binding.tvTranscriptResult.text = text
    }

    private fun getCurrentFormattedText(): String {
        val response = whisperResult ?: return ""
        return when (currentTab) {
            OutputTab.PLAIN_TEXT -> TranscriptFormatter.toPlainText(response)
            OutputTab.TIMESTAMP -> TranscriptFormatter.toTimestampText(response)
            OutputTab.SRT -> TranscriptFormatter.toSrt(response)
        }
    }

    private fun copyCurrentTextToClipboard() {
        val text = getCurrentFormattedText()
        if (text.isBlank()) return

        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("TikTok Transcript", text)
        clipboard.setPrimaryClip(clip)

        Toast.makeText(this, "✓ Đã sao chép văn bản vào bộ nhớ tạm!", Toast.LENGTH_SHORT).show()
    }

    private fun shareCurrentText() {
        val text = getCurrentFormattedText()
        if (text.isBlank()) return

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Lời thoại TikTok")
            putExtra(Intent.EXTRA_TEXT, text)
        }
        startActivity(Intent.createChooser(shareIntent, "Chia sẻ lời thoại sang AI / Ứng dụng"))
    }

    private fun showError(message: String) {
        binding.layoutError.visibility = View.VISIBLE
        binding.tvErrorMessage.text = message
    }

    private fun setStep1Done() {
        binding.ivStep1.setImageResource(R.drawable.ic_status_done)
        binding.tvStep1.setTextColor(getColor(R.color.text_primary))
    }

    private fun setStep2Running() {
        binding.ivStep2.setImageResource(R.drawable.ic_status_running)
        binding.tvStep2.setTextColor(getColor(R.color.ios_blue_light))
    }

    private fun setStep2Done() {
        binding.ivStep2.setImageResource(R.drawable.ic_status_done)
        binding.tvStep2.setTextColor(getColor(R.color.text_primary))
    }

    private fun setStep2Error() {
        binding.ivStep2.setImageResource(R.drawable.ic_status_error)
        binding.tvStep2.setTextColor(getColor(R.color.ios_red))
    }

    private fun setStep3Running() {
        binding.ivStep3.setImageResource(R.drawable.ic_status_running)
        binding.tvStep3.setTextColor(getColor(R.color.ios_blue_light))
    }

    private fun setStep3Done() {
        binding.ivStep3.setImageResource(R.drawable.ic_status_done)
        binding.tvStep3.setTextColor(getColor(R.color.text_primary))
    }

    private fun setStep3Error() {
        binding.ivStep3.setImageResource(R.drawable.ic_status_error)
        binding.tvStep3.setTextColor(getColor(R.color.ios_red))
    }

    private fun setStep4Done() {
        binding.ivStep4.setImageResource(R.drawable.ic_status_done)
        binding.tvStep4.setTextColor(getColor(R.color.ios_green))
    }

    private fun cancelAndFinish() {
        transcribeJob?.cancel()
        tempAudioFile?.let {
            try { if (it.exists()) it.delete() } catch (_: Exception) {}
        }
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        transcribeJob?.cancel()
        tempAudioFile?.let {
            try { if (it.exists()) it.delete() } catch (_: Exception) {}
        }
    }

    companion object {
        const val EXTRA_URL = "extra_tiktok_url"
        const val EXTRA_READ_CLIPBOARD = "extra_read_clipboard"
    }
}
