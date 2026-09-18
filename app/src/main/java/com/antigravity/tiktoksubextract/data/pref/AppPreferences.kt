package com.antigravity.tiktoksubextract.data.pref

import android.content.Context
import android.content.SharedPreferences

class AppPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    var groqApiKey: String
        get() = prefs.getString(KEY_GROQ_API_KEY, "") ?: ""
        set(value) = prefs.edit().putString(KEY_GROQ_API_KEY, value.trim()).apply()

    var whisperModel: String
        get() = prefs.getString(KEY_WHISPER_MODEL, DEFAULT_MODEL) ?: DEFAULT_MODEL
        set(value) = prefs.edit().putString(KEY_WHISPER_MODEL, value).apply()

    var language: String
        get() = prefs.getString(KEY_LANGUAGE, "auto") ?: "auto"
        set(value) = prefs.edit().putString(KEY_LANGUAGE, value).apply()

    var autoStartOnShare: Boolean
        get() = prefs.getBoolean(KEY_AUTO_START, true)
        set(value) = prefs.edit().putBoolean(KEY_AUTO_START, value).apply()

    var defaultOutputFormat: String
        get() = prefs.getString(KEY_OUTPUT_FORMAT, "plain") ?: "plain"
        set(value) = prefs.edit().putString(KEY_OUTPUT_FORMAT, value).apply()

    var quickNotificationEnabled: Boolean
        get() = prefs.getBoolean(KEY_QUICK_NOTIFICATION, false)
        set(value) = prefs.edit().putBoolean(KEY_QUICK_NOTIFICATION, value).apply()

    var llmModel: String
        get() = prefs.getString(KEY_LLM_MODEL, DEFAULT_LLM_MODEL) ?: DEFAULT_LLM_MODEL
        set(value) = prefs.edit().putString(KEY_LLM_MODEL, value).apply()

    var cachedWhisperModels: Set<String>
        get() = prefs.getStringSet(KEY_CACHED_WHISPER_MODELS, DEFAULT_WHISPER_MODELS) ?: DEFAULT_WHISPER_MODELS
        set(value) = prefs.edit().putStringSet(KEY_CACHED_WHISPER_MODELS, value).apply()

    var cachedLlmModels: Set<String>
        get() = prefs.getStringSet(KEY_CACHED_LLM_MODELS, DEFAULT_LLM_MODELS) ?: DEFAULT_LLM_MODELS
        set(value) = prefs.edit().putStringSet(KEY_CACHED_LLM_MODELS, value).apply()

    val isApiKeyConfigured: Boolean
        get() = groqApiKey.isNotBlank()

    companion object {
        private const val PREF_NAME = "tiktok_sub_extract_prefs"
        private const val KEY_GROQ_API_KEY = "groq_api_key"
        private const val KEY_WHISPER_MODEL = "whisper_model"
        private const val KEY_LANGUAGE = "language"
        private const val KEY_AUTO_START = "auto_start_on_share"
        private const val KEY_OUTPUT_FORMAT = "default_output_format"
        private const val KEY_QUICK_NOTIFICATION = "quick_notification_enabled"
        private const val KEY_LLM_MODEL = "llm_model"
        private const val KEY_CACHED_WHISPER_MODELS = "cached_whisper_models"
        private const val KEY_CACHED_LLM_MODELS = "cached_llm_models"

        const val DEFAULT_MODEL = "whisper-large-v3"
        const val TURBO_MODEL = "whisper-large-v3-turbo"
        const val DEFAULT_LLM_MODEL = "groq/compound-mini"

        val DEFAULT_WHISPER_MODELS = setOf("whisper-large-v3", "whisper-large-v3-turbo")
        val DEFAULT_LLM_MODELS = setOf(
            "groq/compound-mini",
            "groq/compound",
            "openai/gpt-oss-120b",
            "openai/gpt-oss-20b",
            "qwen/qwen3.8-27b"
        )
    }
}
