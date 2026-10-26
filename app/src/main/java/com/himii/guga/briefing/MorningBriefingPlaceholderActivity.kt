package com.himii.guga.briefing

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.material3.TextButton
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.himii.guga.R
import com.himii.guga.agent.MorningBriefingAgent
import com.himii.guga.alarm.AlarmModuleIdentifiers
import com.himii.guga.location.HomeLocationRepository
import com.himii.guga.location.HomeLocationSettingsActivity
import com.himii.guga.settings.LlmSettingsActivity
import com.himii.guga.settings.LlmSettingsRepository
import com.himii.guga.settings.SettingsHubActivity
import com.himii.guga.settings.UserProfileRepository
import com.himii.guga.tts.GuideSpeechPlayer
import com.himii.guga.tts.TtsProviderFactory
import com.himii.guga.tts.TtsSettingsRepository
import com.himii.guga.text.ReadablePlainTextFormatter
import com.himii.guga.ui.LightweightMarkdownText
import com.himii.guga.ui.theme.GugaTheme
import com.himii.guga.voice.LocalPhraseCatalog
import com.himii.guga.voice.LocalPhrasePlayer
import com.himii.guga.weather.OpenMeteoWeatherClient
import com.himii.guga.weather.WeatherSummaryFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 出门指南页：生成建议、播报；天气详情默认折叠。
 * 从闹钟关闭进入且挂了天气模块时：先播本地随机垫句，再播正文（不再说该起床）。
 */
class MorningBriefingPlaceholderActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val triggerSource = intent.getStringExtra(EXTRA_TRIGGER_SOURCE).orEmpty()
        val shouldPlayWeatherFillerPhrase =
            intent.getBooleanExtra(EXTRA_SHOULD_PLAY_WEATHER_FILLER_PHRASE, false)
        val enabledModuleIdentifiers =
            intent.getStringArrayListExtra(EXTRA_ENABLED_MODULE_IDENTIFIERS).orEmpty()

        enableEdgeToEdge()
        setContent {
            GugaTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MorningBriefingScreen(
                        triggerSource = triggerSource,
                        shouldAutoSpeakAfterAlarm =
                            triggerSource == TRIGGER_SOURCE_ALARM_DISMISSED &&
                                enabledModuleIdentifiers.contains(AlarmModuleIdentifiers.WEATHER),
                        shouldPlayWeatherFillerPhrase = shouldPlayWeatherFillerPhrase &&
                            enabledModuleIdentifiers.contains(AlarmModuleIdentifiers.WEATHER),
                        onBackClicked = { finish() },
                        onOpenLocationSettingsClicked = {
                            startActivity(HomeLocationSettingsActivity.createIntent(this))
                        },
                        onOpenSettingsClicked = {
                            startActivity(SettingsHubActivity.createIntent(this))
                        },
                        onOpenLlmSettingsClicked = {
                            startActivity(LlmSettingsActivity.createIntent(this))
                        }
                    )
                }
            }
        }
    }

    companion object {
        const val TRIGGER_SOURCE_ALARM_DISMISSED = "alarm_dismissed"
        private const val EXTRA_TRIGGER_SOURCE = "extra_trigger_source"
        private const val EXTRA_SHOULD_PLAY_WEATHER_FILLER_PHRASE =
            "extra_should_play_weather_filler_phrase"
        private const val EXTRA_ENABLED_MODULE_IDENTIFIERS =
            "extra_enabled_module_identifiers"

        fun createIntent(
            context: Context,
            triggerSource: String,
            shouldPlayWeatherFillerPhrase: Boolean = false,
            enabledModuleIdentifiers: List<String> = emptyList()
        ): Intent {
            return Intent(context, MorningBriefingPlaceholderActivity::class.java).apply {
                putExtra(EXTRA_TRIGGER_SOURCE, triggerSource)
                putExtra(EXTRA_SHOULD_PLAY_WEATHER_FILLER_PHRASE, shouldPlayWeatherFillerPhrase)
                putStringArrayListExtra(
                    EXTRA_ENABLED_MODULE_IDENTIFIERS,
                    ArrayList(enabledModuleIdentifiers)
                )
            }
        }
    }
}

private data class BriefingUiModel(
    val isLoading: Boolean = false,
    val agentGuideText: String? = null,
    val ruleBasedSummaryText: String? = null,
    val errorMessage: String? = null,
    val missingHomeLocation: Boolean = false,
    val missingLlmSettings: Boolean = false
)

@Composable
private fun MorningBriefingScreen(
    @Suppress("UNUSED_PARAMETER") triggerSource: String,
    shouldAutoSpeakAfterAlarm: Boolean,
    shouldPlayWeatherFillerPhrase: Boolean,
    onBackClicked: () -> Unit,
    onOpenLocationSettingsClicked: () -> Unit,
    onOpenSettingsClicked: () -> Unit,
    onOpenLlmSettingsClicked: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val homeLocationRepository = remember { HomeLocationRepository(context) }
    val llmSettingsRepository = remember { LlmSettingsRepository(context) }
    val userProfileRepository = remember { UserProfileRepository(context) }
    val ttsSettingsRepository = remember { TtsSettingsRepository(context) }
    val openMeteoWeatherClient = remember { OpenMeteoWeatherClient() }
    val guideSpeechPlayer = remember { GuideSpeechPlayer(context) }
    val localPhrasePlayer = remember { LocalPhrasePlayer(context) }

    var reloadToken by remember { mutableStateOf(0) }
    var uiModel by remember { mutableStateOf(BriefingUiModel(isLoading = true)) }
    var speechStatusMessage by remember { mutableStateOf<String?>(null) }
    var isSpeechBusy by remember { mutableStateOf(false) }
    var hasAutoSpoken by remember { mutableStateOf(false) }
    var isWeatherDetailExpanded by remember { mutableStateOf(false) }
    var isWeatherFillerFinished by remember {
        mutableStateOf(!shouldPlayWeatherFillerPhrase)
    }

    DisposableEffect(Unit) {
        onDispose {
            guideSpeechPlayer.stop()
            localPhrasePlayer.stop()
        }
    }

    fun speakGuideText(guideText: String) {
        coroutineScope.launch {
            isSpeechBusy = true
            localPhrasePlayer.stop()
            guideSpeechPlayer.stop()
            speechStatusMessage = context.getString(R.string.tts_status_synthesizing)
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val settings = ttsSettingsRepository.loadSettings()
                    if (!settings.isConfigured()) {
                        throw IllegalStateException(
                            context.getString(R.string.tts_missing_settings)
                        )
                    }
                    val provider = TtsProviderFactory.create(settings)
                    val speechText = ReadablePlainTextFormatter.toSpeechPlainText(guideText)
                    val audioBytes = provider.synthesizeToAudioBytes(speechText)
                    // 播放也放在后台：主线程 prepare 长音频容易造成听感卡顿
                    guideSpeechPlayer.playMp3Bytes(audioBytes)
                    audioBytes.size
                }
            }
            result.onSuccess {
                speechStatusMessage = context.getString(R.string.tts_status_playing)
            }.onFailure { error ->
                speechStatusMessage = error.message
                    ?: context.getString(R.string.tts_status_synthesize_failed)
            }
            isSpeechBusy = false
        }
    }

    LaunchedEffect(shouldPlayWeatherFillerPhrase) {
        if (!shouldPlayWeatherFillerPhrase) {
            isWeatherFillerFinished = true
            return@LaunchedEffect
        }
        isWeatherFillerFinished = false
        speechStatusMessage = context.getString(R.string.morning_briefing_filler_playing)
        val fillerResource =
            LocalPhraseCatalog.pickRandomWeatherFillerPhraseResourceIdentifier()
        localPhrasePlayer.playRawResource(
            rawResourceIdentifier = fillerResource,
            onCompleted = { isWeatherFillerFinished = true },
            onFailed = { isWeatherFillerFinished = true }
        )
    }

    LaunchedEffect(reloadToken) {
        hasAutoSpoken = false
        uiModel = BriefingUiModel(isLoading = true)

        val homeLocation = homeLocationRepository.loadHomeLocation()
        if (homeLocation == null) {
            uiModel = BriefingUiModel(
                missingHomeLocation = true,
                errorMessage = context.getString(R.string.morning_briefing_missing_home_location)
            )
            return@LaunchedEffect
        }

        val ruleBasedSummary = withContext(Dispatchers.IO) {
            runCatching {
                val snapshot = openMeteoWeatherClient.fetchWeatherSnapshot(homeLocation)
                WeatherSummaryFormatter.formatReadableSummary(snapshot)
            }.getOrNull()
        }

        val llmSettings = llmSettingsRepository.loadSettings()
        if (!llmSettings.isConfigured()) {
            uiModel = BriefingUiModel(
                missingLlmSettings = true,
                ruleBasedSummaryText = ruleBasedSummary,
                errorMessage = context.getString(R.string.morning_briefing_missing_llm_settings)
            )
            return@LaunchedEffect
        }

        val agentResult = runCatching {
            MorningBriefingAgent(
                connectionSettings = llmSettings,
                homeLocationRepository = homeLocationRepository,
                userProfileRepository = userProfileRepository,
                openMeteoWeatherClient = openMeteoWeatherClient
            ).generateMorningBriefing()
        }

        uiModel = agentResult.fold(
            onSuccess = { result ->
                BriefingUiModel(
                    agentGuideText = result.guideText,
                    ruleBasedSummaryText = ruleBasedSummary
                )
            },
            onFailure = { error ->
                BriefingUiModel(
                    errorMessage = error.message
                        ?: context.getString(R.string.morning_briefing_agent_failed),
                    ruleBasedSummaryText = ruleBasedSummary
                )
            }
        )
    }

    LaunchedEffect(
        uiModel.agentGuideText,
        shouldAutoSpeakAfterAlarm,
        isWeatherFillerFinished,
        uiModel.isLoading
    ) {
        val guideText = uiModel.agentGuideText
        if (shouldAutoSpeakAfterAlarm &&
            isWeatherFillerFinished &&
            !hasAutoSpoken &&
            !guideText.isNullOrBlank() &&
            !uiModel.isLoading
        ) {
            hasAutoSpoken = true
            localPhrasePlayer.stop()
            // 等垫句通道释放后再播正文，减轻衔接处卡顿
            delay(250)
            speakGuideText(guideText)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = stringResource(R.string.morning_briefing_title),
            style = MaterialTheme.typography.headlineMedium
        )

        if (uiModel.isLoading) {
            CircularProgressIndicator(modifier = Modifier.padding(top = 24.dp))
            Text(
                text = stringResource(R.string.morning_briefing_agent_loading),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 12.dp)
            )
        }

        uiModel.errorMessage?.let { message ->
            Text(
                text = message,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 16.dp)
            )
        }

        uiModel.agentGuideText?.let { guideText ->
            Text(
                text = stringResource(R.string.morning_briefing_guide_section_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 20.dp)
            )
            LightweightMarkdownText(
                markdownText = guideText,
                modifier = Modifier.padding(top = 8.dp)
            )

            Button(
                onClick = { speakGuideText(guideText) },
                enabled = !isSpeechBusy,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
            ) {
                Text(text = stringResource(R.string.tts_button_speak_guide))
            }

            OutlinedButton(
                onClick = {
                    localPhrasePlayer.stop()
                    guideSpeechPlayer.stop()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                Text(text = stringResource(R.string.tts_button_stop))
            }
        }

        speechStatusMessage?.let { message ->
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        uiModel.ruleBasedSummaryText?.let { summaryText ->
            TextButton(
                onClick = { isWeatherDetailExpanded = !isWeatherDetailExpanded },
                modifier = Modifier.padding(top = 12.dp)
            ) {
                Text(
                    text = stringResource(
                        if (isWeatherDetailExpanded) {
                            R.string.morning_briefing_collapse_weather
                        } else {
                            R.string.morning_briefing_expand_weather
                        }
                    )
                )
            }
            if (isWeatherDetailExpanded) {
                Text(
                    text = stringResource(R.string.morning_briefing_weather_section_title),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(top = 4.dp)
                )
                Text(
                    text = summaryText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }

        if (uiModel.missingHomeLocation) {
            Button(
                onClick = onOpenLocationSettingsClicked,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
            ) {
                Text(text = stringResource(R.string.home_location_open_settings))
            }
        }

        if (uiModel.missingLlmSettings) {
            Button(
                onClick = onOpenLlmSettingsClicked,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
            ) {
                Text(text = stringResource(R.string.llm_settings_open))
            }
        }

        OutlinedButton(
            onClick = { reloadToken += 1 },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
        ) {
            Text(text = stringResource(R.string.action_regenerate_guide))
        }

        OutlinedButton(
            onClick = onOpenSettingsClicked,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        ) {
            Text(text = stringResource(R.string.home_button_settings))
        }

        OutlinedButton(
            onClick = onBackClicked,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 24.dp)
        ) {
            Text(text = stringResource(R.string.action_back))
        }
    }
}
