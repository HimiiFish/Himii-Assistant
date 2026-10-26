package com.himii.guga.settings

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.himii.guga.R
import com.himii.guga.alarm.AlarmScheduler
import com.himii.guga.location.HomeLocationSettingsActivity
import com.himii.guga.ui.theme.GugaTheme

class SettingsHubActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GugaTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    SettingsHubScreen(onBackClicked = { finish() })
                }
            }
        }
    }

    companion object {
        fun createIntent(context: Context): Intent {
            return Intent(context, SettingsHubActivity::class.java)
        }
    }
}

@Composable
private fun SettingsHubScreen(onBackClicked: () -> Unit) {
    val context = LocalContext.current
    val alarmScheduler = AlarmScheduler(context)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        Text(
            text = stringResource(R.string.settings_hub_title),
            style = MaterialTheme.typography.headlineMedium
        )
        Text(
            text = stringResource(R.string.settings_hub_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        SettingsEntryCard(
            title = stringResource(R.string.settings_entry_location_title),
            description = stringResource(R.string.settings_entry_location_description),
            onClick = {
                context.startActivity(HomeLocationSettingsActivity.createIntent(context))
            }
        )
        Spacer(modifier = Modifier.height(12.dp))
        SettingsEntryCard(
            title = stringResource(R.string.settings_entry_model_title),
            description = stringResource(R.string.settings_entry_model_description),
            onClick = {
                context.startActivity(LlmSettingsActivity.createIntent(context))
            }
        )
        Spacer(modifier = Modifier.height(12.dp))
        SettingsEntryCard(
            title = stringResource(R.string.settings_entry_voice_title),
            description = stringResource(R.string.settings_entry_voice_description),
            onClick = {
                context.startActivity(TtsSettingsActivity.createIntent(context))
            }
        )
        Spacer(modifier = Modifier.height(12.dp))
        SettingsEntryCard(
            title = stringResource(R.string.settings_entry_alarm_permission_title),
            description = if (alarmScheduler.canScheduleExactAlarms()) {
                stringResource(R.string.settings_entry_alarm_permission_ready)
            } else {
                stringResource(R.string.settings_entry_alarm_permission_needed)
            },
            onClick = {
                context.startActivity(alarmScheduler.createExactAlarmPermissionSettingsIntent())
            }
        )

        Spacer(modifier = Modifier.height(24.dp))
        OutlinedButton(
            onClick = onBackClicked,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = stringResource(R.string.action_back))
        }
    }
}

@Composable
private fun SettingsEntryCard(
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}
