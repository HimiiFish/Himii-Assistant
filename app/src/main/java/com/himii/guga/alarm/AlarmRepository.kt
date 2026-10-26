package com.himii.guga.alarm

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class AlarmRepository(context: Context) {

    private val sharedPreferences = context.applicationContext.getSharedPreferences(
        PREFERENCE_FILE_NAME,
        Context.MODE_PRIVATE
    )

    fun loadAllAlarms(): List<AlarmRecord> {
        val rawJson = sharedPreferences.getString(KEY_ALARM_LIST_JSON, null) ?: return emptyList()
        return runCatching { parseAlarmList(rawJson) }.getOrDefault(emptyList())
    }

    fun saveAllAlarms(alarmRecords: List<AlarmRecord>) {
        val jsonArray = JSONArray()
        alarmRecords.forEach { alarmRecord ->
            val moduleArray = JSONArray()
            alarmRecord.enabledModuleIdentifiers.forEach { moduleIdentifier ->
                moduleArray.put(moduleIdentifier)
            }
            jsonArray.put(
                JSONObject()
                    .put(JSON_ALARM_IDENTIFIER, alarmRecord.alarmIdentifier)
                    .put(JSON_HOUR_OF_DAY, alarmRecord.hourOfDay)
                    .put(JSON_MINUTE_OF_HOUR, alarmRecord.minuteOfHour)
                    .put(JSON_IS_ENABLED, alarmRecord.isEnabled)
                    .put(JSON_REPEATS_EVERY_DAY, alarmRecord.repeatsEveryDay)
                    .put(JSON_DISPLAY_LABEL, alarmRecord.displayLabel)
                    .put(JSON_ENABLED_MODULE_IDENTIFIERS, moduleArray)
            )
        }
        sharedPreferences.edit()
            .putString(KEY_ALARM_LIST_JSON, jsonArray.toString())
            .apply()
    }

    fun upsertAlarm(alarmRecord: AlarmRecord) {
        val updatedList = loadAllAlarms()
            .filterNot { it.alarmIdentifier == alarmRecord.alarmIdentifier }
            .toMutableList()
        updatedList.add(alarmRecord)
        saveAllAlarms(updatedList)
    }

    fun deleteAlarm(alarmIdentifier: Long) {
        val updatedList = loadAllAlarms().filterNot { it.alarmIdentifier == alarmIdentifier }
        saveAllAlarms(updatedList)
    }

    fun findAlarm(alarmIdentifier: Long): AlarmRecord? {
        return loadAllAlarms().firstOrNull { it.alarmIdentifier == alarmIdentifier }
    }

    fun allocateNextAlarmIdentifier(): Long {
        val existingIdentifiers = loadAllAlarms().map { it.alarmIdentifier }.toSet()
        var candidate = 1L
        while (existingIdentifiers.contains(candidate) ||
            candidate == AlarmScheduler.TEST_ALARM_IDENTIFIER
        ) {
            candidate += 1
        }
        return candidate
    }

    private fun parseAlarmList(rawJson: String): List<AlarmRecord> {
        val jsonArray = JSONArray(rawJson)
        val alarmRecords = mutableListOf<AlarmRecord>()
        for (index in 0 until jsonArray.length()) {
            val jsonObject = jsonArray.getJSONObject(index)
            alarmRecords.add(
                AlarmRecord(
                    alarmIdentifier = jsonObject.getLong(JSON_ALARM_IDENTIFIER),
                    hourOfDay = jsonObject.getInt(JSON_HOUR_OF_DAY),
                    minuteOfHour = jsonObject.getInt(JSON_MINUTE_OF_HOUR),
                    isEnabled = jsonObject.getBoolean(JSON_IS_ENABLED),
                    repeatsEveryDay = jsonObject.getBoolean(JSON_REPEATS_EVERY_DAY),
                    displayLabel = jsonObject.getString(JSON_DISPLAY_LABEL),
                    enabledModuleIdentifiers = parseModuleIdentifiers(jsonObject)
                )
            )
        }
        return alarmRecords
    }

    /**
     * 旧数据没有模块字段时，默认挂上天气，避免已有用户失去关闭后指南。
     */
    private fun parseModuleIdentifiers(jsonObject: JSONObject): List<String> {
        if (!jsonObject.has(JSON_ENABLED_MODULE_IDENTIFIERS)) {
            return listOf(AlarmModuleIdentifiers.WEATHER)
        }
        val moduleArray = jsonObject.optJSONArray(JSON_ENABLED_MODULE_IDENTIFIERS)
            ?: return emptyList()
        val moduleIdentifiers = mutableListOf<String>()
        for (index in 0 until moduleArray.length()) {
            val moduleIdentifier = moduleArray.optString(index)
            if (moduleIdentifier.isNotBlank() &&
                AlarmModuleIdentifiers.isKnownModuleIdentifier(moduleIdentifier)
            ) {
                moduleIdentifiers.add(moduleIdentifier)
            }
        }
        return moduleIdentifiers.distinct()
    }

    companion object {
        private const val PREFERENCE_FILE_NAME = "guga_alarm_store"
        private const val KEY_ALARM_LIST_JSON = "alarm_list_json"
        private const val JSON_ALARM_IDENTIFIER = "alarmIdentifier"
        private const val JSON_HOUR_OF_DAY = "hourOfDay"
        private const val JSON_MINUTE_OF_HOUR = "minuteOfHour"
        private const val JSON_IS_ENABLED = "isEnabled"
        private const val JSON_REPEATS_EVERY_DAY = "repeatsEveryDay"
        private const val JSON_DISPLAY_LABEL = "displayLabel"
        private const val JSON_ENABLED_MODULE_IDENTIFIERS = "enabledModuleIdentifiers"
    }
}
