package com.himii.guga.alarm

import java.util.Calendar

/**
 * 在全部平等的多闹钟中，找出时间上最近一次将响的已开启闹钟。
 */
object AlarmUpcomingFinder {

    data class UpcomingAlarm(
        val alarmRecord: AlarmRecord,
        val triggerAtMillis: Long
    )

    fun findNextUpcomingEnabledAlarm(
        alarmRecords: List<AlarmRecord>,
        nowMillis: Long = System.currentTimeMillis()
    ): UpcomingAlarm? {
        return alarmRecords
            .filter { it.isEnabled }
            .map { alarmRecord ->
                UpcomingAlarm(
                    alarmRecord = alarmRecord,
                    triggerAtMillis = computeNextTriggerAtMillis(
                        hourOfDay = alarmRecord.hourOfDay,
                        minuteOfHour = alarmRecord.minuteOfHour,
                        nowMillis = nowMillis
                    )
                )
            }
            .minByOrNull { it.triggerAtMillis }
    }

    fun computeNextTriggerAtMillis(
        hourOfDay: Int,
        minuteOfHour: Int,
        nowMillis: Long = System.currentTimeMillis()
    ): Long {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = nowMillis
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            set(Calendar.HOUR_OF_DAY, hourOfDay)
            set(Calendar.MINUTE, minuteOfHour)
            if (timeInMillis <= nowMillis) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }
        return calendar.timeInMillis
    }
}
