package com.himii.guga.tts

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class TtsSettingsRepository(context: Context) {

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

    fun loadSettings(): TtsConnectionSettings {
        val providerKindName = encryptedPreferences.getString(
            KEY_PROVIDER_KIND,
            TtsProviderKind.DOUBAO_VOLCENGINE.name
        ) ?: TtsProviderKind.DOUBAO_VOLCENGINE.name
        val providerKind = runCatching { TtsProviderKind.valueOf(providerKindName) }
            .getOrDefault(TtsProviderKind.DOUBAO_VOLCENGINE)

        return TtsConnectionSettings(
            providerKind = providerKind,
            doubaoAppIdentifier = encryptedPreferences.getString(KEY_DOUBAO_APP_IDENTIFIER, "") ?: "",
            doubaoAccessToken = encryptedPreferences.getString(KEY_DOUBAO_ACCESS_TOKEN, "") ?: "",
            doubaoApiKey = encryptedPreferences.getString(KEY_DOUBAO_API_KEY, "") ?: "",
            doubaoResourceIdentifier = encryptedPreferences.getString(
                KEY_DOUBAO_RESOURCE_IDENTIFIER,
                TtsConnectionSettings.DEFAULT_DOUBAO_RESOURCE_IDENTIFIER
            ) ?: TtsConnectionSettings.DEFAULT_DOUBAO_RESOURCE_IDENTIFIER,
            doubaoSpeakerIdentifier = encryptedPreferences.getString(
                KEY_DOUBAO_SPEAKER_IDENTIFIER,
                TtsConnectionSettings.DEFAULT_DOUBAO_SPEAKER_IDENTIFIER
            ) ?: TtsConnectionSettings.DEFAULT_DOUBAO_SPEAKER_IDENTIFIER,
            openAiCompatibleBaseUrl = encryptedPreferences.getString(
                KEY_OPENAI_BASE_URL,
                TtsConnectionSettings.DEFAULT_OPENAI_COMPATIBLE_BASE_URL
            ) ?: TtsConnectionSettings.DEFAULT_OPENAI_COMPATIBLE_BASE_URL,
            openAiCompatibleApiKey = encryptedPreferences.getString(KEY_OPENAI_API_KEY, "") ?: "",
            openAiCompatibleModelName = encryptedPreferences.getString(
                KEY_OPENAI_MODEL_NAME,
                TtsConnectionSettings.DEFAULT_OPENAI_COMPATIBLE_MODEL_NAME
            ) ?: TtsConnectionSettings.DEFAULT_OPENAI_COMPATIBLE_MODEL_NAME,
            openAiCompatibleVoiceName = encryptedPreferences.getString(
                KEY_OPENAI_VOICE_NAME,
                TtsConnectionSettings.DEFAULT_OPENAI_COMPATIBLE_VOICE_NAME
            ) ?: TtsConnectionSettings.DEFAULT_OPENAI_COMPATIBLE_VOICE_NAME
        )
    }

    fun saveSettings(settings: TtsConnectionSettings) {
        encryptedPreferences.edit()
            .putString(KEY_PROVIDER_KIND, settings.providerKind.name)
            .putString(KEY_DOUBAO_APP_IDENTIFIER, settings.doubaoAppIdentifier.trim())
            .putString(KEY_DOUBAO_ACCESS_TOKEN, settings.doubaoAccessToken.trim())
            .putString(KEY_DOUBAO_API_KEY, settings.doubaoApiKey.trim())
            .putString(KEY_DOUBAO_RESOURCE_IDENTIFIER, settings.doubaoResourceIdentifier.trim())
            .putString(KEY_DOUBAO_SPEAKER_IDENTIFIER, settings.doubaoSpeakerIdentifier.trim())
            .putString(
                KEY_OPENAI_BASE_URL,
                settings.openAiCompatibleBaseUrl.trim().trimEnd('/')
            )
            .putString(KEY_OPENAI_API_KEY, settings.openAiCompatibleApiKey.trim())
            .putString(KEY_OPENAI_MODEL_NAME, settings.openAiCompatibleModelName.trim())
            .putString(KEY_OPENAI_VOICE_NAME, settings.openAiCompatibleVoiceName.trim())
            .apply()
    }

    companion object {
        private const val ENCRYPTED_PREFERENCE_FILE_NAME = "guga_tts_encrypted_store"
        private const val KEY_PROVIDER_KIND = "provider_kind"
        private const val KEY_DOUBAO_APP_IDENTIFIER = "doubao_app_identifier"
        private const val KEY_DOUBAO_ACCESS_TOKEN = "doubao_access_token"
        private const val KEY_DOUBAO_API_KEY = "doubao_api_key"
        private const val KEY_DOUBAO_RESOURCE_IDENTIFIER = "doubao_resource_identifier"
        private const val KEY_DOUBAO_SPEAKER_IDENTIFIER = "doubao_speaker_identifier"
        private const val KEY_OPENAI_BASE_URL = "openai_base_url"
        private const val KEY_OPENAI_API_KEY = "openai_api_key"
        private const val KEY_OPENAI_MODEL_NAME = "openai_model_name"
        private const val KEY_OPENAI_VOICE_NAME = "openai_voice_name"
    }
}
