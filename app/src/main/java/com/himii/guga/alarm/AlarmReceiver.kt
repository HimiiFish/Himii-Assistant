package com.himii.guga.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent == null) {
            return
        }
        if (intent.action != ACTION_ALARM_FIRE) {
            return
        }

        val alarmIdentifier = intent.getLongExtra(
            EXTRA_ALARM_IDENTIFIER,
            AlarmScheduler.MORNING_ALARM_IDENTIFIER
        )
        val displayLabelFromIntent = intent.getStringExtra(EXTRA_DISPLAY_LABEL).orEmpty()
        val modulesFromIntent =
            intent.getStringArrayListExtra(EXTRA_ENABLED_MODULE_IDENTIFIERS).orEmpty()

        val storedAlarm = AlarmRepository(context).findAlarm(alarmIdentifier)
        val displayLabel = storedAlarm?.displayLabel?.takeIf { it.isNotBlank() }
            ?: displayLabelFromIntent
        val enabledModuleIdentifiers = storedAlarm?.enabledModuleIdentifiers
            ?: modulesFromIntent

        Log.i(
            LOG_TAG,
            "Alarm fired. alarmIdentifier=$alarmIdentifier label=$displayLabel " +
                "modules=$enabledModuleIdentifiers"
        )

        AlarmRingingNotifier.showFullScreenRingingNotification(
            context = context.applicationContext,
            alarmIdentifier = alarmIdentifier,
            displayLabel = displayLabel,
            enabledModuleIdentifiers = enabledModuleIdentifiers
        )

        val ringingIntent = Intent(context, AlarmRingingActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(AlarmRingingActivity.EXTRA_ALARM_IDENTIFIER, alarmIdentifier)
            putExtra(AlarmRingingActivity.EXTRA_DISPLAY_LABEL, displayLabel)
            putStringArrayListExtra(
                AlarmRingingActivity.EXTRA_ENABLED_MODULE_IDENTIFIERS,
                ArrayList(enabledModuleIdentifiers)
            )
        }
        runCatching {
            context.startActivity(
                ringingIntent,
                AlarmRingingNotifier.createBackgroundActivityStartAllowedOptionsBundle()
            )
        }.onFailure { error ->
            Log.w(LOG_TAG, "Direct start AlarmRingingActivity failed; rely on notification", error)
        }
    }

    companion object {
        const val ACTION_ALARM_FIRE = "com.himii.guga.action.ALARM_FIRE"
        const val EXTRA_ALARM_IDENTIFIER = "extra_alarm_identifier"
        const val EXTRA_DISPLAY_LABEL = "extra_display_label"
        const val EXTRA_ENABLED_MODULE_IDENTIFIERS = "extra_enabled_module_identifiers"
        private const val LOG_TAG = "GugaAlarmReceiver"
    }
}
