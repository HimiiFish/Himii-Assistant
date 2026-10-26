package com.himii.guga.alarm

import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.himii.guga.R
import com.himii.guga.briefing.MorningBriefingPlaceholderActivity
import com.himii.guga.ui.theme.GugaTheme
import com.himii.guga.voice.LocalPhraseCatalog
import com.himii.guga.voice.LocalPhrasePlayer

/**
 * 三段起床：① 本地叫醒语音 → ② 铃声+震动 → ③ 用户关闭后按模块播报（无模块则结束）。
 */
class AlarmRingingActivity : ComponentActivity() {

    private var ringtoneMediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var localPhrasePlayer: LocalPhrasePlayer? = null
    private var alarmIdentifier: Long = AlarmScheduler.MORNING_ALARM_IDENTIFIER
    private var displayLabel: String = ""
    private var enabledModuleIdentifiers: List<String> = emptyList()
    private var hasEnteredRingtonePhase: Boolean = false
    private var ringingPhaseLabel by mutableStateOf("")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        turnScreenOnAndShowWhenLocked()

        alarmIdentifier = intent.getLongExtra(
            EXTRA_ALARM_IDENTIFIER,
            AlarmScheduler.MORNING_ALARM_IDENTIFIER
        )
        displayLabel = intent.getStringExtra(EXTRA_DISPLAY_LABEL).orEmpty()
        enabledModuleIdentifiers =
            intent.getStringArrayListExtra(EXTRA_ENABLED_MODULE_IDENTIFIERS).orEmpty()

        val storedAlarm = AlarmRepository(this).findAlarm(alarmIdentifier)
        if (storedAlarm != null) {
            if (displayLabel.isBlank()) {
                displayLabel = storedAlarm.displayLabel
            }
            enabledModuleIdentifiers = storedAlarm.enabledModuleIdentifiers
        }

        AlarmRingingNotifier.cancelRingingNotification(this)
        localPhrasePlayer = LocalPhrasePlayer(this)
        ringingPhaseLabel = getString(R.string.alarm_phase_wake_voice)

        enableEdgeToEdge()
        setContent {
            GugaTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AlarmRingingScreen(
                        displayLabel = displayLabel.ifBlank {
                            stringResource(R.string.alarm_default_label)
                        },
                        phaseLabel = ringingPhaseLabel,
                        hasWeatherModule = enabledModuleIdentifiers.contains(
                            AlarmModuleIdentifiers.WEATHER
                        ),
                        onDismissClicked = { dismissAlarm() },
                        onSnoozeClicked = { snoozeForFiveMinutes() }
                    )
                }
            }
        }

        startWakeUpPhraseThenRingtone()
    }

    override fun onDestroy() {
        stopAllFeedback()
        super.onDestroy()
    }

    private fun turnScreenOnAndShowWhenLocked() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    private fun startWakeUpPhraseThenRingtone() {
        val phrasePlayer = localPhrasePlayer ?: return
        val wakeResource = LocalPhraseCatalog.pickRandomWakeUpPhraseResourceIdentifier()
        phrasePlayer.playRawResource(
            rawResourceIdentifier = wakeResource,
            audioUsage = android.media.AudioAttributes.USAGE_ALARM,
            onCompleted = {
                runOnUiThread { enterRingtoneAndVibrationPhase() }
            },
            onFailed = {
                runOnUiThread { enterRingtoneAndVibrationPhase() }
            }
        )
    }

    private fun enterRingtoneAndVibrationPhase() {
        if (isFinishing || isDestroyed || hasEnteredRingtonePhase) {
            return
        }
        hasEnteredRingtonePhase = true
        ringingPhaseLabel = getString(R.string.alarm_phase_ringtone)
        startRingtoneAndVibration()
    }

    private fun startRingtoneAndVibration() {
        runCatching {
            val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            ringtoneMediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                setDataSource(this@AlarmRingingActivity, alarmUri)
                isLooping = true
                prepare()
                start()
            }
        }

        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(VibratorManager::class.java)
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(VIBRATOR_SERVICE) as Vibrator
        }

        val vibrationPattern = longArrayOf(0, 800, 400, 800)
        vibrator?.vibrate(
            VibrationEffect.createWaveform(vibrationPattern, 0)
        )
    }

    private fun stopAllFeedback() {
        localPhrasePlayer?.stop()
        localPhrasePlayer = null
        ringtoneMediaPlayer?.runCatching {
            stop()
            release()
        }
        ringtoneMediaPlayer = null
        vibrator?.cancel()
        vibrator = null
    }

    private fun dismissAlarm() {
        stopAllFeedback()
        AlarmRingingNotifier.cancelRingingNotification(this)
        val alarmRepository = AlarmRepository(this)
        val alarmScheduler = AlarmScheduler(this)
        val existingAlarm = alarmRepository.findAlarm(alarmIdentifier)
        if (existingAlarm != null && existingAlarm.repeatsEveryDay && existingAlarm.isEnabled) {
            alarmScheduler.scheduleAlarm(existingAlarm)
        } else {
            alarmScheduler.cancelAlarm(alarmIdentifier)
        }

        val shouldOpenBriefing = enabledModuleIdentifiers.contains(AlarmModuleIdentifiers.WEATHER)
        if (shouldOpenBriefing) {
            startActivity(
                MorningBriefingPlaceholderActivity.createIntent(
                    context = this,
                    triggerSource = MorningBriefingPlaceholderActivity.TRIGGER_SOURCE_ALARM_DISMISSED,
                    shouldPlayWeatherFillerPhrase = true,
                    enabledModuleIdentifiers = enabledModuleIdentifiers
                )
            )
        }
        finish()
    }

    private fun snoozeForFiveMinutes() {
        stopAllFeedback()
        AlarmRingingNotifier.cancelRingingNotification(this)
        val alarmScheduler = AlarmScheduler(this)
        alarmScheduler.scheduleOneShotInMillis(
            alarmIdentifier = alarmIdentifier,
            delayMillis = FIVE_MINUTES_IN_MILLIS,
            displayLabel = displayLabel.ifBlank { getString(R.string.alarm_snooze_label) },
            enabledModuleIdentifiers = enabledModuleIdentifiers
        )
        finish()
    }

    companion object {
        const val EXTRA_ALARM_IDENTIFIER = "extra_alarm_identifier"
        const val EXTRA_DISPLAY_LABEL = "extra_display_label"
        const val EXTRA_ENABLED_MODULE_IDENTIFIERS = "extra_enabled_module_identifiers"
        const val EXTRA_LAUNCHED_FROM_NOTIFICATION = "extra_launched_from_notification"
        private const val FIVE_MINUTES_IN_MILLIS = 5 * 60 * 1000L
    }
}

@Composable
private fun AlarmRingingScreen(
    displayLabel: String,
    phaseLabel: String,
    hasWeatherModule: Boolean,
    onDismissClicked: () -> Unit,
    onSnoozeClicked: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.alarm_ringing_title),
            style = MaterialTheme.typography.headlineLarge
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = displayLabel,
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = phaseLabel,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onDismissClicked,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = stringResource(
                    if (hasWeatherModule) {
                        R.string.alarm_dismiss_with_guide_button
                    } else {
                        R.string.alarm_dismiss_only_button
                    }
                )
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Button(
            onClick = onSnoozeClicked,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = stringResource(R.string.alarm_snooze_button))
        }
    }
}
