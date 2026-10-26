package com.himii.guga.alarm

import android.app.ActivityOptions
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.himii.guga.R

/**
 * 用高优先级闹钟通知 + 全屏 Intent 拉起响铃页。
 * 新版安卓会拦截「后台直接 startActivity」，全屏闹钟通知是可靠路径。
 */
object AlarmRingingNotifier {

    const val NOTIFICATION_CHANNEL_IDENTIFIER = "guga_alarm_ringing_channel"
    const val NOTIFICATION_IDENTIFIER = 41001

    fun ensureNotificationChannel(context: Context) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_IDENTIFIER,
            context.getString(R.string.alarm_notification_channel_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(R.string.alarm_notification_channel_description)
            setBypassDnd(true)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        notificationManager.createNotificationChannel(channel)
    }

    fun showFullScreenRingingNotification(
        context: Context,
        alarmIdentifier: Long,
        displayLabel: String,
        enabledModuleIdentifiers: List<String>
    ) {
        ensureNotificationChannel(context)

        val ringingIntent = Intent(context, AlarmRingingActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(AlarmRingingActivity.EXTRA_ALARM_IDENTIFIER, alarmIdentifier)
            putExtra(AlarmRingingActivity.EXTRA_DISPLAY_LABEL, displayLabel)
            putStringArrayListExtra(
                AlarmRingingActivity.EXTRA_ENABLED_MODULE_IDENTIFIERS,
                ArrayList(enabledModuleIdentifiers)
            )
            putExtra(AlarmRingingActivity.EXTRA_LAUNCHED_FROM_NOTIFICATION, true)
        }

        val activityOptionsBundle = createBackgroundActivityStartAllowedOptionsBundle()
        val fullScreenPendingIntent = PendingIntent.getActivity(
            context,
            (alarmIdentifier % Int.MAX_VALUE).toInt() + FULL_SCREEN_REQUEST_CODE_OFFSET,
            ringingIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            activityOptionsBundle
        )

        val contentPendingIntent = PendingIntent.getActivity(
            context,
            (alarmIdentifier % Int.MAX_VALUE).toInt() + CONTENT_REQUEST_CODE_OFFSET,
            ringingIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            activityOptionsBundle
        )

        val notification = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_IDENTIFIER)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(context.getString(R.string.alarm_ringing_title))
            .setContentText(
                displayLabel.ifBlank { context.getString(R.string.alarm_default_label) }
            )
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .setContentIntent(contentPendingIntent)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .build()

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_IDENTIFIER, notification)
    }

    fun cancelRingingNotification(context: Context) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(NOTIFICATION_IDENTIFIER)
    }

    fun createBackgroundActivityStartAllowedOptionsBundle(): android.os.Bundle? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            return null
        }
        val activityOptions = ActivityOptions.makeBasic().apply {
            setPendingIntentCreatorBackgroundActivityStartMode(
                ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED
            )
        }
        return activityOptions.toBundle()
    }

    private const val FULL_SCREEN_REQUEST_CODE_OFFSET = 20_000
    private const val CONTENT_REQUEST_CODE_OFFSET = 30_000
}
