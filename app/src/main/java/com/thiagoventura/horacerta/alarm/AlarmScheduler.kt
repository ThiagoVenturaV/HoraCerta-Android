package com.thiagoventura.horacerta.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import com.thiagoventura.horacerta.MainActivity
import com.thiagoventura.horacerta.data.DoseWithMedication
import com.thiagoventura.horacerta.data.MedicationRepository

class AlarmScheduler(private val context: Context) {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)
    private val protectedPreferences by lazy {
        context.createDeviceProtectedStorageContext()
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun canScheduleExactAlarms(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()

    fun rescheduleAll(repository: MedicationRepository, now: Long = System.currentTimeMillis()) {
        repository.ensureHorizon(now)
        repository.pendingForScheduling(now)
            .sortedBy { it.occurrence.scheduledAt }
            .mapNotNull { dose ->
                val stored = storedAlarm(dose.occurrence.id)
                val trigger = when {
                    stored != null && stored.triggerAtMillis > now -> stored.triggerAtMillis
                    dose.occurrence.scheduledAt >= now -> dose.occurrence.scheduledAt
                    stored != null && stored.triggerAtMillis > now - OVERDUE_RESTORE_WINDOW_MILLIS ->
                        now + OVERDUE_GRACE_MILLIS
                    else -> null
                }
                trigger?.let { dose to it }
            }
            .take(MAX_SCHEDULED_ALARMS)
            .forEach { (dose, trigger) ->
                schedule(dose, trigger)
            }
    }

    fun schedule(dose: DoseWithMedication, triggerAtMillis: Long = dose.occurrence.scheduledAt) {
        schedulePayload(
            payload = AlarmPayload(
                occurrenceId = dose.occurrence.id,
                medicationName = dose.medication.name,
                dosage = dose.medication.dosage,
                scheduledAt = dose.occurrence.scheduledAt,
                sound = dose.medication.sound,
                vibration = dose.medication.vibration,
            ),
            triggerAtMillis = triggerAtMillis,
            saveMirror = true,
        )
    }

    fun scheduleSnooze(payload: AlarmPayload) {
        val trigger = snoozeTriggerAt(payload)
        schedulePayload(payload, trigger, saveMirror = true)
    }

    fun cancel(occurrenceId: Long) {
        alarmManager.cancel(alarmPendingIntent(occurrenceId, null, PendingIntent.FLAG_UPDATE_CURRENT))
        protectedPreferences.edit().remove(keyFor(occurrenceId)).apply()
    }

    fun restoreFromDeviceProtectedMirror() {
        protectedPreferences.all
            .filterKeys { it.startsWith(KEY_PREFIX) }
            .values
            .mapNotNull { it as? String }
            .mapNotNull(::decodeRecord)
            .filter { it.triggerAtMillis > System.currentTimeMillis() - OVERDUE_RESTORE_WINDOW_MILLIS }
            .sortedBy(StoredAlarm::triggerAtMillis)
            .take(MAX_SCHEDULED_ALARMS)
            .forEach { stored ->
                schedulePayload(
                    stored.payload,
                    maxOf(stored.triggerAtMillis, System.currentTimeMillis() + OVERDUE_GRACE_MILLIS),
                    saveMirror = false,
                )
            }
    }

    private fun schedulePayload(payload: AlarmPayload, triggerAtMillis: Long, saveMirror: Boolean) {
        val operation = alarmPendingIntent(
            payload.occurrenceId,
            payload,
            PendingIntent.FLAG_UPDATE_CURRENT,
        )
        try {
            if (canScheduleExactAlarms()) {
                val showIntent = PendingIntent.getActivity(
                    context,
                    0,
                    Intent(context, MainActivity::class.java),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )
                alarmManager.setAlarmClock(
                    AlarmManager.AlarmClockInfo(triggerAtMillis, showIntent),
                    operation,
                )
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, operation)
            }
            if (saveMirror) {
                val stored = StoredAlarm(triggerAtMillis, payload)
                protectedPreferences.edit().putString(keyFor(payload.occurrenceId), encodeRecord(stored)).apply()
            }
        } catch (_: SecurityException) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, operation)
        }
    }

    private fun alarmPendingIntent(
        occurrenceId: Long,
        payload: AlarmPayload?,
        flags: Int,
    ): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            data = Uri.parse("horacerta://alarm/$occurrenceId")
            payload?.let {
                putExtra(AlarmContract.EXTRA_OCCURRENCE_ID, it.occurrenceId)
                putExtra(AlarmContract.EXTRA_MEDICATION_NAME, it.medicationName)
                putExtra(AlarmContract.EXTRA_DOSAGE, it.dosage)
                putExtra(AlarmContract.EXTRA_SCHEDULED_AT, it.scheduledAt)
                putExtra(AlarmContract.EXTRA_SOUND, it.sound)
                putExtra(AlarmContract.EXTRA_VIBRATION, it.vibration)
            }
        }
        return PendingIntent.getBroadcast(
            context,
            (occurrenceId xor (occurrenceId ushr 32)).toInt(),
            intent,
            flags or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun encodeRecord(stored: StoredAlarm): String = listOf(
        stored.triggerAtMillis,
        stored.payload.occurrenceId,
        Uri.encode(stored.payload.medicationName),
        Uri.encode(stored.payload.dosage),
        stored.payload.scheduledAt,
        if (stored.payload.sound) 1 else 0,
        if (stored.payload.vibration) 1 else 0,
    ).joinToString("|")

    private fun decodeRecord(value: String): StoredAlarm? {
        val parts = value.split('|')
        if (parts.size != 6 && parts.size != 7) return null
        return runCatching {
            val hasScheduledAt = parts.size == 7
            StoredAlarm(
                triggerAtMillis = parts[0].toLong(),
                payload = AlarmPayload(
                    occurrenceId = parts[1].toLong(),
                    medicationName = Uri.decode(parts[2]),
                    dosage = Uri.decode(parts[3]),
                    scheduledAt = if (hasScheduledAt) parts[4].toLong() else parts[0].toLong(),
                    sound = parts[if (hasScheduledAt) 5 else 4] == "1",
                    vibration = parts[if (hasScheduledAt) 6 else 5] == "1",
                ),
            )
        }.getOrNull()
    }

    private fun keyFor(id: Long) = "$KEY_PREFIX$id"

    private fun storedAlarm(id: Long): StoredAlarm? =
        protectedPreferences.getString(keyFor(id), null)?.let(::decodeRecord)

    private data class StoredAlarm(val triggerAtMillis: Long, val payload: AlarmPayload)

    companion object {
        private const val PREFS_NAME = "scheduled_alarms"
        private const val KEY_PREFIX = "alarm_"
        private const val MAX_SCHEDULED_ALARMS = 200
        private const val OVERDUE_GRACE_MILLIS = 30_000L
        private const val OVERDUE_RESTORE_WINDOW_MILLIS = 24L * 60L * 60L * 1000L

        fun snoozeTriggerAt(payload: AlarmPayload, now: Long = System.currentTimeMillis()): Long =
            maxOf(now, payload.scheduledAt) + AlarmContract.SNOOZE_MINUTES * 60_000L
    }
}
