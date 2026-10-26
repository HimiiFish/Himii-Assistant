package com.himii.guga.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings

class AlarmScheduler(private val context: Context) {

    private val alarmManager =
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun canScheduleExactAlarms(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }

    fun createExactAlarmPermissionSettingsIntent(): Intent {
        return Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
            data = android.net.Uri.parse("package:${context.packageName}")
        }
    }

    fun scheduleAlarm(alarmRecord: AlarmRecord) {
        if (!alarmRecord.isEnabled) {
            cancelAlarm(alarmRecord.alarmIdentifier)
            return
        }
        val triggerAtMillis = AlarmUpcomingFinder.computeNextTriggerAtMillis(
            hourOfDay = alarmRecord.hourOfDay,
            minuteOfHour = alarmRecord.minuteOfHour
        )
        scheduleAtMillis(
            alarmIdentifier = alarmRecord.alarmIdentifier,
            triggerAtMillis = triggerAtMillis,
            displayLabel = alarmRecord.displayLabel,
            enabledModuleIdentifiers = alarmRecord.enabledModuleIdentifiers
        )
    }

    fun scheduleOneShotInMillis(
        alarmIdentifier: Long,
        delayMillis: Long,
        displayLabel: String,
        enabledModuleIdentifiers: List<String>
    ) {
        scheduleAtMillis(
            alarmIdentifier = alarmIdentifier,
            triggerAtMillis = System.currentTimeMillis() + delayMillis,
            displayLabel = displayLabel,
            enabledModuleIdentifiers = enabledModuleIdentifiers
        )
    }

    fun cancelAlarm(alarmIdentifier: Long) {
        alarmManager.cancel(
            createFireBroadcastPendingIntent(
                alarmIdentifier = alarmIdentifier,
                displayLabel = "",
                enabledModuleIdentifiers = emptyList()
            )
        )
    }

    fun rescheduleAllEnabledAlarms(alarmRecords: List<AlarmRecord>) {
        alarmRecords.forEach { alarmRecord ->
            if (alarmRecord.isEnabled) {
                scheduleAlarm(alarmRecord)
            } else {
                cancelAlarm(alarmRecord.alarmIdentifier)
            }
        }
    }

    private fun scheduleAtMillis(
        alarmIdentifier: Long,
        triggerAtMillis: Long,
        displayLabel: String,
        enabledModuleIdentifiers: List<String>
    ) {
        AlarmRingingNotifier.ensureNotificationChannel(context)

        val firePendingIntent = createFireBroadcastPendingIntent(
            alarmIdentifier = alarmIdentifier,
            displayLabel = displayLabel,
            enabledModuleIdentifiers = enabledModuleIdentifiers
        )
        val showIntent = Intent(context, com.himii.guga.MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val showPendingIntent = PendingIntent.getActivity(
            context,
            (alarmIdentifier % Int.MAX_VALUE).toInt(),
            showIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            AlarmRingingNotifier.createBackgroundActivityStartAllowedOptionsBundle()
        )
        val alarmClockInfo = AlarmManager.AlarmClockInfo(triggerAtMillis, showPendingIntent)
        alarmManager.setAlarmClock(alarmClockInfo, firePendingIntent)
    }

    private fun createFireBroadcastPendingIntent(
        alarmIdentifier: Long,
        displayLabel: String,
        enabledModuleIdentifiers: List<String>
    ): PendingIntent {
        val fireIntent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_ALARM_FIRE
            putExtra(AlarmReceiver.EXTRA_ALARM_IDENTIFIER, alarmIdentifier)
            putExtra(AlarmReceiver.EXTRA_DISPLAY_LABEL, displayLabel)
            putStringArrayListExtra(
                AlarmReceiver.EXTRA_ENABLED_MODULE_IDENTIFIERS,
                ArrayList(enabledModuleIdentifiers)
            )
        }
        return PendingIntent.getBroadcast(
            context,
            (alarmIdentifier % Int.MAX_VALUE).toInt() + REQUEST_CODE_OFFSET,
            fireIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    companion object {
        private const val REQUEST_CODE_OFFSET = 10_000
        const val TEST_ALARM_IDENTIFIER = 900_001L
        /** 兼容旧版单闹钟标识；新闹钟用仓库自增。 */
        const val MORNING_ALARM_IDENTIFIER = 1L
    }
}
