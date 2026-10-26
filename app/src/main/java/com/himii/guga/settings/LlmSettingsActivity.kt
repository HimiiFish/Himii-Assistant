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
import com.himii.guga.ui.theme.GugaTheme

class LlmSettingsActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GugaTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    LlmSettingsScreen(onBackClicked = { finish() })
                }
            }
        }
    }

    companion object {
        fun createIntent(context: Context): Intent {
            return Intent(context, LlmSettingsActivity::class.java)
        }
    }
}

@Composable
private fun LlmSettingsScreen(onBackClicked: () -> Unit) {
    val context = LocalContext.current
    val llmSettingsRepository = remember { LlmSettingsRepository(context) }
    val userProfileRepository = remember { UserProfileRepository(context) }

    val initialLlm = remember { llmSettingsRepository.loadSettings() }
    val initialProfile = remember { userProfileRepository.loadSettings() }

    var baseUrlInput by remember { mutableStateOf(initialLlm.baseUrl) }
    var apiKeyInput by remember { mutableStateOf(initialLlm.apiKey) }
    var modelNameInput by remember { mutableStateOf(initialLlm.modelName) }
    var temperaturePreference by remember {
        mutableStateOf(initialProfile.temperaturePreference)
    }

    val preferenceOptions = listOf(
        UserProfileSettings.PREFERENCE_NORMAL,
        UserProfileSettings.PREFERENCE_FEELS_COLD_EASILY,
        UserProfileSettings.PREFERENCE_FEELS_HOT_EASILY
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.Top
    ) {
        Text(
            text = stringResource(R.string.llm_settings_title),
            style = MaterialTheme.typography.headlineMedium
        )
        Text(
            text = stringResource(R.string.llm_settings_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 8.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = baseUrlInput,
            onValueChange = { baseUrlInput = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text(text = stringResource(R.string.llm_settings_base_url_label)) }
        )
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = apiKeyInput,
            onValueChange = { apiKeyInput = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text(text = stringResource(R.string.llm_settings_api_key_label)) }
        )
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = modelNameInput,
            onValueChange = { modelNameInput = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text(text = stringResource(R.string.llm_settings_model_name_label)) }
        )

        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = stringResource(R.string.user_profile_temperature_preference_title),
            style = MaterialTheme.typography.titleMedium
        )
        preferenceOptions.forEach { option ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = temperaturePreference == option,
                        onClick = { temperaturePreference = option },
                        role = Role.RadioButton
                    )
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = temperaturePreference == option,
                    onClick = { temperaturePreference = option }
                )
                Text(text = option, modifier = Modifier.padding(start = 8.dp))
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
        Button(
            onClick = {
                llmSettingsRepository.saveSettings(
                    LlmConnectionSettings(
                        baseUrl = baseUrlInput,
                        apiKey = apiKeyInput,
                        modelName = modelNameInput
                    )
                )
                userProfileRepository.saveSettings(
                    UserProfileSettings(temperaturePreference = temperaturePreference)
                )
                Toast.makeText(context, R.string.llm_settings_saved, Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = stringResource(R.string.action_save))
        }

        Spacer(modifier = Modifier.height(12.dp))
        OutlinedButton(
            onClick = {
                baseUrlInput = LlmConnectionSettings.DEFAULT_BASE_URL
                modelNameInput = LlmConnectionSettings.DEFAULT_MODEL_NAME
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = stringResource(R.string.llm_settings_fill_deepseek_defaults))
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
