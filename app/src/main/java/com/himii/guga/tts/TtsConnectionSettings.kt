package com.himii.guga.tts

enum class TtsProviderKind {
    DOUBAO_VOLCENGINE,
    OPENAI_COMPATIBLE
}

data class TtsConnectionSettings(
    val providerKind: TtsProviderKind,
    // 豆包 / 火山 —— 语音合成控制台常见：App Id + Access Token（V1）
    val doubaoAppIdentifier: String,
    val doubaoAccessToken: String,
    // 新版控制台：API Key（V3）；有 AppId/Token 时优先走 V1
    val doubaoApiKey: String,
    val doubaoResourceIdentifier: String,
    val doubaoSpeakerIdentifier: String,
    // OpenAI 兼容 TTS
    val openAiCompatibleBaseUrl: String,
    val openAiCompatibleApiKey: String,
    val openAiCompatibleModelName: String,
    val openAiCompatibleVoiceName: String
) {
    fun isConfigured(): Boolean {
        return when (providerKind) {
            TtsProviderKind.DOUBAO_VOLCENGINE -> {
                val hasV1 = doubaoAppIdentifier.isNotBlank() && doubaoAccessToken.isNotBlank()
                val hasV3 = doubaoApiKey.isNotBlank()
                (hasV1 || hasV3) && doubaoSpeakerIdentifier.isNotBlank()
            }

            TtsProviderKind.OPENAI_COMPATIBLE ->
                openAiCompatibleBaseUrl.isNotBlank() &&
                    openAiCompatibleApiKey.isNotBlank() &&
                    openAiCompatibleModelName.isNotBlank() &&
                    openAiCompatibleVoiceName.isNotBlank()
        }
    }

    companion object {
        const val DEFAULT_DOUBAO_RESOURCE_IDENTIFIER = "seed-tts-2.0"
        const val DEFAULT_DOUBAO_SPEAKER_IDENTIFIER = "zh_female_shuangkuaisisi_moon_bigtts"
        const val DEFAULT_OPENAI_COMPATIBLE_BASE_URL = "https://api.openai.com"
        const val DEFAULT_OPENAI_COMPATIBLE_MODEL_NAME = "tts-1"
        const val DEFAULT_OPENAI_COMPATIBLE_VOICE_NAME = "alloy"
    }
}
