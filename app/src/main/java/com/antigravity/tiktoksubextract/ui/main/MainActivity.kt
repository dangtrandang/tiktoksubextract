package com.antigravity.tiktoksubextract.ui.main

import android.Manifest
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.antigravity.tiktoksubextract.R
import com.antigravity.tiktoksubextract.data.api.GroqWhisperService
import com.antigravity.tiktoksubextract.data.pref.AppPreferences
import com.antigravity.tiktoksubextract.databinding.ActivityMainBinding
import com.antigravity.tiktoksubextract.service.QuickNotificationService
import com.antigravity.tiktoksubextract.ui.transcribe.TranscribeDialogActivity
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var prefs: AppPreferences
    private val whisperService = GroqWhisperService()

    private var selectedWhisperModel = AppPreferences.DEFAULT_MODEL
    private var selectedLlmModel = AppPreferences.DEFAULT_LLM_MODEL
    private var selectedLanguage = "auto"

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            prefs.quickNotificationEnabled = true
            QuickNotificationService.start(this)
            Toast.makeText(this, "✓ Đã bật thông báo thao tác nhanh!", Toast.LENGTH_SHORT).show()
        } else {
            binding.switchQuickNotification.isChecked = false
            prefs.quickNotificationEnabled = false
            Toast.makeText(this, "Cần cấp quyền thông báo để hiển thị thao tác nhanh", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = AppPreferences(this)

        loadPreferences()
        setupListeners()
    }

    private fun loadPreferences() {
        binding.etApiKey.setText(prefs.groqApiKey)
        binding.switchAutoStart.isChecked = prefs.autoStartOnShare
        binding.switchQuickNotification.isChecked = prefs.quickNotificationEnabled

        if (prefs.quickNotificationEnabled) {
            QuickNotificationService.start(this)
        }

        selectedWhisperModel = prefs.whisperModel
        binding.tvSelectedWhisperModel.text = selectedWhisperModel

        selectedLlmModel = prefs.llmModel
        binding.tvSelectedLlmModel.text = selectedLlmModel

        selectedLanguage = prefs.language
        updateLanguageUi(selectedLanguage)
    }

    private fun setupListeners() {
        // Whisper Model Picker Dialog
        binding.layoutSelectWhisperModel.setOnClickListener {
            val cachedModels = prefs.cachedWhisperModels.toList().sorted().ifEmpty {
                AppPreferences.DEFAULT_WHISPER_MODELS.toList().sorted()
            }
            showModelSelectionDialog(
                title = "Chọn mô hình Whisper (Bóc băng)",
                items = cachedModels,
                currentSelection = selectedWhisperModel
            ) { selected ->
                selectedWhisperModel = selected
                binding.tvSelectedWhisperModel.text = selected
            }
        }

        // LLM Model Picker Dialog
        binding.layoutSelectLlmModel.setOnClickListener {
            val cachedModels = prefs.cachedLlmModels.toList().sorted().ifEmpty {
                AppPreferences.DEFAULT_LLM_MODELS.toList().sorted()
            }
            showModelSelectionDialog(
                title = "Chọn mô hình AI (Sửa lỗi chính tả)",
                items = cachedModels,
                currentSelection = selectedLlmModel
            ) { selected ->
                selectedLlmModel = selected
                binding.tvSelectedLlmModel.text = selected
            }
        }

        // Reload Models from Groq API
        binding.btnReloadModels.setOnClickListener {
            val key = binding.etApiKey.text?.toString()?.trim() ?: ""
            if (key.isBlank()) {
                Toast.makeText(this, "Vui lòng nhập Groq API Key trước", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            binding.btnReloadModels.isEnabled = false
            binding.btnReloadModels.text = "Đang tải..."

            lifecycleScope.launch {
                val result = whisperService.fetchAvailableModels(key)
                binding.btnReloadModels.isEnabled = true
                binding.btnReloadModels.text = "🔄 Tải model mới"

                result.onSuccess { (whisperModels, llmModels) ->
                    if (whisperModels.isNotEmpty()) {
                        prefs.cachedWhisperModels = whisperModels.toSet()
                        if (!whisperModels.contains(selectedWhisperModel)) {
                            selectedWhisperModel = whisperModels.first()
                            binding.tvSelectedWhisperModel.text = selectedWhisperModel
                            prefs.whisperModel = selectedWhisperModel
                        }
                    }

                    if (llmModels.isNotEmpty()) {
                        prefs.cachedLlmModels = llmModels.toSet()
                        if (!llmModels.contains(selectedLlmModel)) {
                            // Ưu tiên chọn compound-mini nếu có, không thì model đầu tiên
                            selectedLlmModel = llmModels.firstOrNull { it.contains("compound-mini") }
                                ?: llmModels.first()
                            binding.tvSelectedLlmModel.text = selectedLlmModel
                            prefs.llmModel = selectedLlmModel
                        }
                    }

                    Toast.makeText(
                        this@MainActivity,
                        "✓ Đã đồng bộ ${whisperModels.size} Whisper & ${llmModels.size} LLM models!",
                        Toast.LENGTH_LONG
                    ).show()
                }.onFailure { ex ->
                    Toast.makeText(
                        this@MainActivity,
                        "✕ Lỗi khi tải models: ${ex.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }

        // Language Selection
        binding.langAuto.setOnClickListener {
            selectedLanguage = "auto"
            updateLanguageUi(selectedLanguage)
        }

        binding.langVi.setOnClickListener {
            selectedLanguage = "vi"
            updateLanguageUi(selectedLanguage)
        }

        binding.langEn.setOnClickListener {
            selectedLanguage = "en"
            updateLanguageUi(selectedLanguage)
        }

        // Test API Key
        binding.btnTestKey.setOnClickListener {
            val key = binding.etApiKey.text?.toString()?.trim() ?: ""
            if (key.isBlank()) {
                binding.tvTestStatus.text = "Vui lòng nhập API Key trước khi test"
                binding.tvTestStatus.setTextColor(getColor(R.color.ios_red))
                return@setOnClickListener
            }

            binding.tvTestStatus.text = "Đang kiểm tra kết nối..."
            binding.tvTestStatus.setTextColor(getColor(R.color.ios_blue_light))

            lifecycleScope.launch {
                val result = whisperService.testApiKey(key)
                if (result.isSuccess) {
                    binding.tvTestStatus.text = "✓ Kết nối Groq thành công!"
                    binding.tvTestStatus.setTextColor(getColor(R.color.ios_green))
                } else {
                    val ex = result.exceptionOrNull()
                    binding.tvTestStatus.text = "✕ ${ex?.message ?: "Lỗi kết nối"}"
                    binding.tvTestStatus.setTextColor(getColor(R.color.ios_red))
                }
            }
        }

        // Quick Notification Switch
        binding.switchQuickNotification.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                        prefs.quickNotificationEnabled = true
                        QuickNotificationService.start(this)
                        Toast.makeText(this, "✓ Đã bật thông báo thao tác nhanh!", Toast.LENGTH_SHORT).show()
                    } else {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                } else {
                    prefs.quickNotificationEnabled = true
                    QuickNotificationService.start(this)
                    Toast.makeText(this, "✓ Đã bật thông báo thao tác nhanh!", Toast.LENGTH_SHORT).show()
                }
            } else {
                prefs.quickNotificationEnabled = false
                QuickNotificationService.stop(this)
                Toast.makeText(this, "Đã tắt thông báo thao tác nhanh", Toast.LENGTH_SHORT).show()
            }
        }

        // Save Settings
        binding.btnSaveSettings.setOnClickListener {
            val key = binding.etApiKey.text?.toString()?.trim() ?: ""
            prefs.groqApiKey = key
            prefs.whisperModel = selectedWhisperModel
            prefs.llmModel = selectedLlmModel
            prefs.language = selectedLanguage
            prefs.autoStartOnShare = binding.switchAutoStart.isChecked
            prefs.quickNotificationEnabled = binding.switchQuickNotification.isChecked

            if (binding.switchQuickNotification.isChecked) {
                QuickNotificationService.start(this)
            } else {
                QuickNotificationService.stop(this)
            }

            Toast.makeText(this, "✓ Đã lưu cài đặt!", Toast.LENGTH_SHORT).show()
        }

        // Paste URL Button
        binding.btnPasteUrl.setOnClickListener {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clipData = clipboard.primaryClip
            if (clipData != null && clipData.itemCount > 0) {
                val pasteText = clipData.getItemAt(0).text?.toString() ?: ""
                binding.etManualUrl.setText(pasteText)
            } else {
                Toast.makeText(this, "Bộ nhớ tạm rỗng", Toast.LENGTH_SHORT).show()
            }
        }

        // Manual Transcribe Start
        binding.btnStartManual.setOnClickListener {
            val url = binding.etManualUrl.text?.toString()?.trim() ?: ""
            if (url.isBlank()) {
                Toast.makeText(this, "Vui lòng dán link TikTok cần bóc tách", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Save settings first if modified
            prefs.groqApiKey = binding.etApiKey.text?.toString()?.trim() ?: ""
            prefs.whisperModel = selectedWhisperModel
            prefs.llmModel = selectedLlmModel
            prefs.language = selectedLanguage
            prefs.autoStartOnShare = binding.switchAutoStart.isChecked

            val intent = Intent(this, TranscribeDialogActivity::class.java).apply {
                putExtra(TranscribeDialogActivity.EXTRA_URL, url)
            }
            startActivity(intent)
        }
    }

    private fun showModelSelectionDialog(
        title: String,
        items: List<String>,
        currentSelection: String,
        onSelected: (String) -> Unit
    ) {
        if (items.isEmpty()) {
            Toast.makeText(this, "Không có model nào. Vui lòng bấm 'Tải model mới'", Toast.LENGTH_SHORT).show()
            return
        }

        val currentIndex = items.indexOf(currentSelection).let { if (it >= 0) it else 0 }
        var tempSelectedIndex = currentIndex

        com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
            .setTitle(title)
            .setSingleChoiceItems(items.toTypedArray(), currentIndex) { _, which ->
                tempSelectedIndex = which
            }
            .setPositiveButton("Chọn") { dialog, _ ->
                if (tempSelectedIndex in items.indices) {
                    onSelected(items[tempSelectedIndex])
                }
                dialog.dismiss()
            }
            .setNegativeButton("Hủy") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun updateLanguageUi(lang: String) {
        binding.langAuto.setBackgroundResource(
            if (lang == "auto") R.drawable.bg_segment_selected else android.R.color.transparent
        )
        binding.langAuto.setTextColor(
            getColor(if (lang == "auto") R.color.text_primary else R.color.text_secondary)
        )

        binding.langVi.setBackgroundResource(
            if (lang == "vi") R.drawable.bg_segment_selected else android.R.color.transparent
        )
        binding.langVi.setTextColor(
            getColor(if (lang == "vi") R.color.text_primary else R.color.text_secondary)
        )

        binding.langEn.setBackgroundResource(
            if (lang == "en") R.drawable.bg_segment_selected else android.R.color.transparent
        )
        binding.langEn.setTextColor(
            getColor(if (lang == "en") R.color.text_primary else R.color.text_secondary)
        )
    }
}
