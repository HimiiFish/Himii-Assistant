package com.himii.guga.tts

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import java.io.File
import java.util.concurrent.atomic.AtomicInteger

/**
 * 将合成出的 MP3 写入缓存并用 MediaPlayer 播放。
 * 注意：prepare / start 应在后台线程调用，避免主线程卡顿导致听感「一卡一卡」。
 */
class GuideSpeechPlayer(context: Context) {

    private val applicationContext = context.applicationContext
    private val audioManager =
        applicationContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val temporaryFileSequence = AtomicInteger(0)

    private var mediaPlayer: MediaPlayer? = null
    private var temporaryAudioFile: File? = null
    private var audioFocusRequest: AudioFocusRequest? = null

    /**
     * 同步准备并播放。调用方请放到 IO / Default 线程。
     */
    fun playMp3Bytes(audioBytes: ByteArray) {
        stopInternal(abandonAudioFocus = true)
        if (audioBytes.isEmpty()) {
            throw IllegalStateException("音频数据为空")
        }

        val audioFile = File(
            applicationContext.cacheDir,
            "guga_guide_speech_${temporaryFileSequence.incrementAndGet()}.mp3"
        )
        audioFile.outputStream().use { outputStream ->
            outputStream.write(audioBytes)
            outputStream.flush()
            outputStream.fd.sync()
        }
        temporaryAudioFile = audioFile

        if (!requestPlaybackAudioFocus()) {
            // 拿不到焦点也继续播，但优先保证尽量独占
        }

        val player = MediaPlayer()
        mediaPlayer = player
        try {
            player.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            player.setDataSource(audioFile.absolutePath)
            player.setOnCompletionListener {
                stopInternal(abandonAudioFocus = true)
            }
            player.setOnErrorListener { _, _, _ ->
                stopInternal(abandonAudioFocus = true)
                true
            }
            // 给解码器一点缓冲余量，减轻中低端机顿挫
            runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    // 无公开 setBuffer 对文件源；保持 prepare 完整即可
                }
            }
            player.prepare()
            player.start()
        } catch (error: Exception) {
            stopInternal(abandonAudioFocus = true)
            throw error
        }
    }

    fun stop() {
        stopInternal(abandonAudioFocus = true)
    }

    fun isPlaying(): Boolean {
        return mediaPlayer?.isPlaying == true
    }

    private fun stopInternal(abandonAudioFocus: Boolean) {
        mediaPlayer?.runCatching {
            setOnCompletionListener(null)
            setOnErrorListener(null)
            if (isPlaying) {
                stop()
            }
            reset()
            release()
        }
        mediaPlayer = null
        temporaryAudioFile?.runCatching { delete() }
        temporaryAudioFile = null
        if (abandonAudioFocus) {
            abandonPlaybackAudioFocus()
        }
    }

    private fun requestPlaybackAudioFocus(): Boolean {
        abandonPlaybackAudioFocus()
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                .setOnAudioFocusChangeListener { focusChange ->
                    if (focusChange == AudioManager.AUDIOFOCUS_LOSS ||
                        focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT
                    ) {
                        // 被电话等抢走时停掉，避免半截乱响
                        stopInternal(abandonAudioFocus = true)
                    }
                }
                .build()
            audioFocusRequest = focusRequest
            audioManager.requestAudioFocus(focusRequest) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(
                null,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
            ) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        }
    }

    private fun abandonPlaybackAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let { request ->
                audioManager.abandonAudioFocusRequest(request)
            }
            audioFocusRequest = null
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(null)
        }
    }
}
