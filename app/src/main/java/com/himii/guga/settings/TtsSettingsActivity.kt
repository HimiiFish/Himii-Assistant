package com.himii.guga.settings

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.himii.guga.R
import com.himii.guga.tts.TtsConnectionSettings
import com.himii.guga.tts.TtsProviderKind
import com.himii.guga.tts.TtsSettingsRepository
import com.himii.guga.ui.theme.GugaTheme

class TtsSettingsActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GugaTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    TtsSettingsScreen(onBackClicked = { finish() })
                }
            }
        }
    }

    companion object {
        fun createIntent(context: Context): Intent {
            return Intent(context, TtsSettingsActivity::class.java)
        }
    }
}

@Composable
private fun TtsSettingsScreen(onBackClicked: () -> Unit) {
    val context = LocalContext.current
    val ttsSettingsRepository = remember { TtsSettingsRepository(context) }
    val initial = remember { ttsSettingsRepository.loadSettings() }

    var providerKind by remember { mutableStateOf(initial.providerKind) }
    var doubaoAppIdentifier by remember { mutableStateOf(initial.doubaoAppIdentifier) }
    var doubaoAccessToken by remember { mutableStateOf(initial.doubaoAccessToken) }
    var doubaoApiKey by remember { mutableStateOf(initial.doubaoApiKey) }
    var doubaoResourceIdentifier by remember { mutableStateOf(initial.doubaoResourceIdentifier) }
    var doubaoSpeakerIdentifier by remember { mutableStateOf(initial.doubaoSpeakerIdentifier) }
    var openAiBaseUrl by remember { mutableStateOf(initial.openAiCompatibleBaseUrl) }
    var openAiApiKey by remember { mutableStateOf(initial.openAiCompatibleApiKey) }
    var openAiModelName by remember { mutableStateOf(initial.openAiCompatibleModelName) }
    var openAiVoiceName by remember { mutableStateOf(initial.openAiCompatibleVoiceName) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.Top
    ) {
        Text(
            text = stringResource(R.string.tts_settings_title),
            style = MaterialTheme.typography.headlineMedium
        )
        Text(
            text = stringResource(R.string.tts_settings_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 8.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.tts_settings_provider_title),
            style = MaterialTheme.typography.titleMedium
        )

        ProviderRadioRow(
            label = stringResource(R.string.tts_provider_doubao),
            selected = providerKind == TtsProviderKind.DOUBAO_VOLCENGINE,
            onClick = { providerKind = TtsProviderKind.DOUBAO_VOLCENGINE }
        )
        ProviderRadioRow(
            label = stringResource(R.string.tts_provider_openai_compatible),
            selected = providerKind == TtsProviderKind.OPENAI_COMPATIBLE,
            onClick = { providerKind = TtsProviderKind.OPENAI_COMPATIBLE }
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (providerKind == TtsProviderKind.DOUBAO_VOLCENGINE) {
            Text(
                text = stringResource(R.string.tts_doubao_v1_section),
                style = MaterialTheme.typography.titleSmall
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = doubaoAppIdentifier,
                onValueChange = { doubaoAppIdentifier = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text(text = stringResource(R.string.tts_doubao_app_id_label)) }
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = doubaoAccessToken,
                onValueChange = { doubaoAccessToken = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text(text = stringResource(R.string.tts_doubao_access_token_label)) }
            )

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.tts_doubao_v3_section),
                style = MaterialTheme.typography.titleSmall
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = doubaoApiKey,
                onValueChange = { doubaoApiKey = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text(text = stringResource(R.string.tts_doubao_api_key_label)) }
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = doubaoResourceIdentifier,
                onValueChange = { doubaoResourceIdentifier = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text(text = stringResource(R.string.tts_doubao_resource_label)) }
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = doubaoSpeakerIdentifier,
                onValueChange = { doubaoSpeakerIdentifier = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text(text = stringResource(R.string.tts_doubao_speaker_label)) }
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.tts_doubao_hint),
                style = MaterialTheme.typography.bodySmall
            )
        } else {
            OutlinedTextField(
                value = openAiBaseUrl,
                onValueChange = { openAiBaseUrl = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text(text = stringResource(R.string.tts_openai_base_url_label)) }
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = openAiApiKey,
                onValueChange = { openAiApiKey = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text(text = stringResource(R.string.tts_openai_api_key_label)) }
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = openAiModelName,
                onValueChange = { openAiModelName = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text(text = stringResource(R.string.tts_openai_model_label)) }
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = openAiVoiceName,
                onValueChange = { openAiVoiceName = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text(text = stringResource(R.string.tts_openai_voice_label)) }
            )
        }

        Spacer(modifier = Modifier.height(20.dp))
        Button(
            onClick = {
                ttsSettingsRepository.saveSettings(
                    TtsConnectionSettings(
                        providerKind = providerKind,
                        doubaoAppIdentifier = doubaoAppIdentifier,
                        doubaoAccessToken = doubaoAccessToken,
                        doubaoApiKey = doubaoApiKey,
                        doubaoResourceIdentifier = doubaoResourceIdentifier,
                        doubaoSpeakerIdentifier = doubaoSpeakerIdentifier,
                        openAiCompatibleBaseUrl = openAiBaseUrl,
                        openAiCompatibleApiKey = openAiApiKey,
                        openAiCompatibleModelName = openAiModelName,
                        openAiCompatibleVoiceName = openAiVoiceName
                    )
                )
                Toast.makeText(context, R.string.tts_settings_saved, Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = stringResource(R.string.action_save))
        }

        Spacer(modifier = Modifier.height(12.dp))
        OutlinedButton(
            onClick = onBackClicked,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = stringResource(R.string.action_back))
        }
    }
}

@Composable
private fun ProviderRadioRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.RadioButton
            )
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Text(text = label, modifier = Modifier.padding(start = 8.dp))
    }
}
