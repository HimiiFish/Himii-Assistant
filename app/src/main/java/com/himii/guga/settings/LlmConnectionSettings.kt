package com.himii.guga.settings

data class LlmConnectionSettings(
    val baseUrl: String,
    val apiKey: String,
    val modelName: String
) {
    fun isConfigured(): Boolean {
        return baseUrl.isNotBlank() && apiKey.isNotBlank() && modelName.isNotBlank()
    }

    companion object {
        const val DEFAULT_BASE_URL = "https://api.deepseek.com"
        const val DEFAULT_MODEL_NAME = "deepseek-chat"
    }
}
