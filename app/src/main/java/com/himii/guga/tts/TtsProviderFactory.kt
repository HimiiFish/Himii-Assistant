package com.himii.guga.tts

object TtsProviderFactory {

    fun create(settings: TtsConnectionSettings): TtsProvider {
        if (!settings.isConfigured()) {
            throw IllegalStateException("请先在「语音设置」中配置 TTS")
        }
        return when (settings.providerKind) {
            TtsProviderKind.DOUBAO_VOLCENGINE -> DoubaoVolcengineTtsProvider(
                apiKey = settings.doubaoApiKey,
                resourceIdentifier = settings.doubaoResourceIdentifier,
                speakerIdentifier = settings.doubaoSpeakerIdentifier,
                appIdentifier = settings.doubaoAppIdentifier,
                accessToken = settings.doubaoAccessToken
            )

            TtsProviderKind.OPENAI_COMPATIBLE -> OpenAiCompatibleTtsProvider(
                baseUrl = settings.openAiCompatibleBaseUrl,
                apiKey = settings.openAiCompatibleApiKey,
                modelName = settings.openAiCompatibleModelName,
                voiceName = settings.openAiCompatibleVoiceName
            )
        }
    }
}
