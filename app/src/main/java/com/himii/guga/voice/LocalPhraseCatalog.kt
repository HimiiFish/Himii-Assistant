package com.himii.guga.voice

import com.himii.guga.R
import kotlin.random.Random

/**
 * 预置本地语音资源目录：叫醒句、天气垫句等。可随时替换 raw 下的 MP3。
 */
object LocalPhraseCatalog {

    private val wakeUpPhraseResourceIdentifiers = listOf(
        R.raw.wake_up_01
    )

    private val weatherFillerPhraseResourceIdentifiers = listOf(
        R.raw.filler_weather_01
    )

    fun pickRandomWakeUpPhraseResourceIdentifier(
        random: Random = Random.Default
    ): Int {
        return wakeUpPhraseResourceIdentifiers.random(random)
    }

    fun pickRandomWeatherFillerPhraseResourceIdentifier(
        random: Random = Random.Default
    ): Int {
        return weatherFillerPhraseResourceIdentifiers.random(random)
    }
}
