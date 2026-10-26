package com.himii.guga.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class LlmSettingsRepository(context: Context) {

    private val applicationContext = context.applicationContext

    private val encryptedPreferences: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(applicationContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            applicationContext,
            ENCRYPTED_PREFERENCE_FILE_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun loadSettings(): LlmConnectionSettings {
        return LlmConnectionSettings(
            baseUrl = encryptedPreferences.getString(KEY_BASE_URL, LlmConnectionSettings.DEFAULT_BASE_URL)
                ?: LlmConnectionSettings.DEFAULT_BASE_URL,
            apiKey = encryptedPreferences.getString(KEY_API_KEY, "") ?: "",
            modelName = encryptedPreferences.getString(
                KEY_MODEL_NAME,
                LlmConnectionSettings.DEFAULT_MODEL_NAME
            ) ?: LlmConnectionSettings.DEFAULT_MODEL_NAME
        )
    }

    fun saveSettings(settings: LlmConnectionSettings) {
        encryptedPreferences.edit()
            .putString(KEY_BASE_URL, settings.baseUrl.trim().trimEnd('/'))
            .putString(KEY_API_KEY, settings.apiKey.trim())
            .putString(KEY_MODEL_NAME, settings.modelName.trim())
            .apply()
    }

    companion object {
        private const val ENCRYPTED_PREFERENCE_FILE_NAME = "guga_llm_encrypted_store"
        private const val KEY_BASE_URL = "base_url"
        private const val KEY_API_KEY = "api_key"
        private const val KEY_MODEL_NAME = "model_name"
    }
}
