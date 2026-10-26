package com.himii.guga.voice

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper

/**
 * 播放 App 内 raw 预置语音（叫醒 / 垫句），播完回调（主线程）。
 */
class LocalPhrasePlayer(context: Context) {

    private val applicationContext = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())
    private var mediaPlayer: MediaPlayer? = null

    fun playRawResource(
        rawResourceIdentifier: Int,
        onCompleted: () -> Unit,
        onFailed: () -> Unit,
        audioUsage: Int = AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE
    ) {
        stop()
        runCatching {
            val assetFileDescriptor =
                applicationContext.resources.openRawResourceFd(rawResourceIdentifier)
            val player = MediaPlayer()
            mediaPlayer = player
            player.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(audioUsage)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            player.setDataSource(
                assetFileDescriptor.fileDescriptor,
                assetFileDescriptor.startOffset,
                assetFileDescriptor.length
            )
            assetFileDescriptor.close()
            player.setOnCompletionListener {
                stop()
                mainHandler.post(onCompleted)
            }
            player.setOnErrorListener { _, _, _ ->
                stop()
                mainHandler.post(onFailed)
                true
            }
            player.prepare()
            player.start()
        }.onFailure {
            stop()
            mainHandler.post(onFailed)
        }
    }

    fun stop() {
        mediaPlayer?.runCatching {
            setOnCompletionListener(null)
            setOnErrorListener(null)
            if (isPlaying) {
                stop()
            }
            release()
        }
        mediaPlayer = null
    }

    fun isPlaying(): Boolean {
        return mediaPlayer?.isPlaying == true
    }
}
