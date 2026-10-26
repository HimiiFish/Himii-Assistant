package com.himii.guga

import android.Manifest
import android.app.TimePickerDialog
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.himii.guga.alarm.AlarmModuleIdentifiers
import com.himii.guga.alarm.AlarmRecord
import com.himii.guga.alarm.AlarmRepository
import com.himii.guga.alarm.AlarmScheduler
import com.himii.guga.alarm.AlarmUpcomingFinder
import com.himii.guga.briefing.MorningBriefingPlaceholderActivity
import com.himii.guga.settings.SettingsHubActivity
import com.himii.guga.ui.theme.GugaTheme
import java.util.Locale

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GugaTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    HomeScreen()
                }
            }
        }
    }
}

@Composable
fun HomeScreen() {
    val context = LocalContext.current
    val alarmRepository = remember { AlarmRepository(context) }
    val alarmScheduler = remember { AlarmScheduler(context) }

    var alarmRecords by remember {
        mutableStateOf(
            alarmRepository.loadAllAlarms().sortedWith(
                compareBy({ it.hourOfDay }, { it.minuteOfHour })
            )
        )
    }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var editorTarget by remember { mutableStateOf<AlarmEditorTarget?>(null) }

    val upcoming = remember(alarmRecords) {
        AlarmUpcomingFinder.findNextUpcomingEnabledAlarm(alarmRecords)
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { /* 用户拒绝时仍可继续，响铃页可工作 */ }

    val exactAlarmSettingsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        statusMessage = if (alarmScheduler.canScheduleExactAlarms()) {
            context.getString(R.string.permission_exact_alarm_granted)
        } else {
            context.getString(R.string.permission_exact_alarm_denied)
        }
    }

    fun reloadAlarmsFromStore() {
        alarmRecords = alarmRepository.loadAllAlarms().sortedWith(
            compareBy({ it.hourOfDay }, { it.minuteOfHour })
        )
    }

    fun ensureNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return
        }
        val permissionState = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        )
        if (permissionState != PackageManager.PERMISSION_GRANTED) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    fun ensureExactAlarmPermissionOrOpenSettings(): Boolean {
        if (alarmScheduler.canScheduleExactAlarms()) {
            return true
        }
        statusMessage = context.getString(R.string.permission_exact_alarm_required)
        exactAlarmSettingsLauncher.launch(
            alarmScheduler.createExactAlarmPermissionSettingsIntent()
        )
        return false
    }

    fun persistAlarm(alarmRecord: AlarmRecord) {
        ensureNotificationPermission()
        if (alarmRecord.isEnabled && !ensureExactAlarmPermissionOrOpenSettings()) {
            return
        }
        alarmRepository.upsertAlarm(alarmRecord)
        if (alarmRecord.isEnabled) {
            alarmScheduler.scheduleAlarm(alarmRecord)
        } else {
            alarmScheduler.cancelAlarm(alarmRecord.alarmIdentifier)
        }
        reloadAlarmsFromStore()
        statusMessage = if (alarmRecord.isEnabled) {
            context.getString(
                R.string.home_status_alarm_enabled_format,
                formatClockTime(alarmRecord.hourOfDay, alarmRecord.minuteOfHour)
            )
        } else {
            context.getString(R.string.home_status_alarm_disabled)
        }
        Toast.makeText(context, R.string.home_toast_alarm_saved, Toast.LENGTH_SHORT).show()
    }

    fun deleteAlarm(alarmRecord: AlarmRecord) {
        alarmScheduler.cancelAlarm(alarmRecord.alarmIdentifier)
        alarmRepository.deleteAlarm(alarmRecord.alarmIdentifier)
        reloadAlarmsFromStore()
        statusMessage = context.getString(R.string.home_status_alarm_deleted)
        Toast.makeText(context, R.string.home_toast_alarm_deleted, Toast.LENGTH_SHORT).show()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.application_name),
            style = MaterialTheme.typography.headlineLarge
        )
        Text(
            text = stringResource(R.string.home_tagline),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp)
        )

        Spacer(modifier = Modifier.height(28.dp))

        Text(
            text = stringResource(R.string.home_next_alarm_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = upcoming?.let {
                formatClockTime(it.alarmRecord.hourOfDay, it.alarmRecord.minuteOfHour)
            } ?: stringResource(R.string.home_next_alarm_none),
            style = MaterialTheme.typography.displayMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp)
        )
        upcoming?.let {
            Text(
                text = it.alarmRecord.displayLabel.ifBlank {
                    stringResource(R.string.alarm_default_label)
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        statusMessage?.let { message ->
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 12.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.home_alarm_list_title),
                style = MaterialTheme.typography.titleMedium
            )
            TextButton(onClick = {
                editorTarget = AlarmEditorTarget.Create
            }) {
                Text(text = stringResource(R.string.home_button_add_alarm))
            }
        }

        if (alarmRecords.isEmpty()) {
            Text(
                text = stringResource(R.string.home_alarm_list_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
            )
        }

        alarmRecords.forEach { alarmRecord ->
            AlarmListRow(
                alarmRecord = alarmRecord,
                onToggleEnabled = { enabled ->
                    persistAlarm(alarmRecord.copy(isEnabled = enabled))
                },
                onClick = {
                    editorTarget = AlarmEditorTarget.Edit(alarmRecord)
                }
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = {
                context.startActivity(
                    MorningBriefingPlaceholderActivity.createIntent(
                        context = context,
                        triggerSource = "home",
                        shouldPlayWeatherFillerPhrase = false,
                        enabledModuleIdentifiers = listOf(AlarmModuleIdentifiers.WEATHER)
                    )
                )
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = stringResource(R.string.home_button_today_guide))
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = {
                context.startActivity(SettingsHubActivity.createIntent(context))
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = stringResource(R.string.home_button_settings))
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(onClick = {
            ensureNotificationPermission()
            if (!alarmScheduler.canScheduleExactAlarms()) {
                exactAlarmSettingsLauncher.launch(
                    alarmScheduler.createExactAlarmPermissionSettingsIntent()
                )
            } else {
                statusMessage = context.getString(R.string.permission_exact_alarm_granted)
            }
        }) {
            Text(text = stringResource(R.string.home_button_check_permissions))
        }
    }

    editorTarget?.let { target ->
        AlarmEditorDialog(
            target = target,
            onDismiss = { editorTarget = null },
            onSave = { hourOfDay, minuteOfHour, weatherModuleEnabled, isEnabled ->
                val modules = buildList {
                    if (weatherModuleEnabled) {
                        add(AlarmModuleIdentifiers.WEATHER)
                    }
                }
                val alarmRecord = when (target) {
                    is AlarmEditorTarget.Create -> AlarmRecord(
                        alarmIdentifier = alarmRepository.allocateNextAlarmIdentifier(),
                        hourOfDay = hourOfDay,
                        minuteOfHour = minuteOfHour,
                        isEnabled = isEnabled,
                        repeatsEveryDay = true,
                        displayLabel = context.getString(R.string.alarm_default_label),
                        enabledModuleIdentifiers = modules
                    )
                    is AlarmEditorTarget.Edit -> target.alarmRecord.copy(
                        hourOfDay = hourOfDay,
                        minuteOfHour = minuteOfHour,
                        isEnabled = isEnabled,
                        enabledModuleIdentifiers = modules
                    )
                }
                persistAlarm(alarmRecord)
                editorTarget = null
            },
            onDelete = { alarmRecord ->
                deleteAlarm(alarmRecord)
                editorTarget = null
            }
        )
    }
}

private sealed class AlarmEditorTarget {
    data object Create : AlarmEditorTarget()
    data class Edit(val alarmRecord: AlarmRecord) : AlarmEditorTarget()
}

@Composable
private fun AlarmListRow(
    alarmRecord: AlarmRecord,
    onToggleEnabled: (Boolean) -> Unit,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = formatClockTime(alarmRecord.hourOfDay, alarmRecord.minuteOfHour),
                style = MaterialTheme.typography.headlineSmall
            )
            Text(
                text = buildAlarmSubtitle(alarmRecord),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = alarmRecord.isEnabled,
            onCheckedChange = onToggleEnabled
        )
    }
}

@Composable
private fun buildAlarmSubtitle(alarmRecord: AlarmRecord): String {
    val modulePart = if (alarmRecord.hasWeatherModule()) {
        stringResource(R.string.home_alarm_module_weather_on)
    } else {
        stringResource(R.string.home_alarm_module_none)
    }
    val repeatPart = stringResource(R.string.home_alarm_repeat_daily)
    return "$repeatPart · $modulePart"
}

@Composable
private fun AlarmEditorDialog(
    target: AlarmEditorTarget,
    onDismiss: () -> Unit,
    onSave: (
        hourOfDay: Int,
        minuteOfHour: Int,
        weatherModuleEnabled: Boolean,
        isEnabled: Boolean
    ) -> Unit,
    onDelete: (AlarmRecord) -> Unit
) {
    val context = LocalContext.current
    val existing = (target as? AlarmEditorTarget.Edit)?.alarmRecord
    var hourOfDay by remember {
        mutableStateOf(existing?.hourOfDay ?: 7)
    }
    var minuteOfHour by remember {
        mutableStateOf(existing?.minuteOfHour ?: 0)
    }
    var weatherModuleEnabled by remember {
        mutableStateOf(existing?.hasWeatherModule() ?: true)
    }
    var isEnabled by remember {
        mutableStateOf(existing?.isEnabled ?: true)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(
                    if (target is AlarmEditorTarget.Create) {
                        R.string.home_alarm_editor_title_create
                    } else {
                        R.string.home_alarm_editor_title_edit
                    }
                )
            )
        },
        text = {
            Column {
                TextButton(
                    onClick = {
                        TimePickerDialog(
                            context,
                            { _, selectedHour, selectedMinute ->
                                hourOfDay = selectedHour
                                minuteOfHour = selectedMinute
                            },
                            hourOfDay,
                            minuteOfHour,
                            true
                        ).show()
                    }
                ) {
                    Text(
                        text = stringResource(
                            R.string.home_alarm_editor_time_format,
                            formatClockTime(hourOfDay, minuteOfHour)
                        )
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = weatherModuleEnabled,
                        onCheckedChange = { weatherModuleEnabled = it }
                    )
                    Text(text = stringResource(R.string.home_alarm_editor_module_weather))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = isEnabled,
                        onCheckedChange = { isEnabled = it }
                    )
                    Text(text = stringResource(R.string.home_alarm_enable_label))
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(hourOfDay, minuteOfHour, weatherModuleEnabled, isEnabled)
                }
            ) {
                Text(text = stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            Row {
                if (existing != null) {
                    TextButton(onClick = { onDelete(existing) }) {
                        Text(text = stringResource(R.string.home_alarm_editor_delete))
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text(text = stringResource(R.string.action_back))
                }
            }
        }
    )
}

private fun formatClockTime(hourOfDay: Int, minuteOfHour: Int): String {
    return String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minuteOfHour)
}
