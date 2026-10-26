package com.himii.guga.tts

/**
 * 文本转语音提供者。第一版默认豆包；可替换为 OpenAI 兼容 /audio/speech。
 */
interface TtsProvider {
    fun synthesizeToAudioBytes(text: String): ByteArray
}
