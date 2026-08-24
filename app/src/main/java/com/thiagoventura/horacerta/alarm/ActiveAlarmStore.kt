package com.thiagoventura.horacerta.alarm

import android.content.Context
import android.net.Uri

/**
 * Device-protected mirror of the doses participating in the current alarm
 * session. It is available before the first unlock after a reboot and lets
 * multiple broadcasts share one screen without merging their individual state.
 */
class ActiveAlarmStore(context: Context) {
    private val preferences = context.createDeviceProtectedStorageContext()
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun add(payload: AlarmPayload) {
        preferences.edit().putString(keyFor(payload.occurrenceId), encode(payload)).commit()
    }

    fun payloads(): List<AlarmPayload> = normalizeAlarmPayloads(
        preferences.all
            .filterKeys { it.startsWith(KEY_PREFIX) }
            .values
            .mapNotNull { it as? String }
            .mapNotNull(::decode),
    )

    fun remove(occurrenceId: Long) {
        preferences.edit().remove(keyFor(occurrenceId)).commit()
    }

    fun removeAll(occurrenceIds: Collection<Long>) {
        if (occurrenceIds.isEmpty()) return
        preferences.edit().apply {
            occurrenceIds.forEach { remove(keyFor(it)) }
        }.commit()
    }

    private fun keyFor(id: Long) = "$KEY_PREFIX$id"

    private fun encode(payload: AlarmPayload): String = listOf(
        payload.occurrenceId,
        Uri.encode(payload.medicationName),
        Uri.encode(payload.dosage),
        payload.scheduledAt,
        if (payload.sound) 1 else 0,
        if (payload.vibration) 1 else 0,
    ).joinToString("|")

    private fun decode(value: String): AlarmPayload? {
        val parts = value.split('|')
        if (parts.size != 6) return null
        return runCatching {
            AlarmPayload(
                occurrenceId = parts[0].toLong(),
                medicationName = Uri.decode(parts[1]),
                dosage = Uri.decode(parts[2]),
                scheduledAt = parts[3].toLong(),
                sound = parts[4] == "1",
                vibration = parts[5] == "1",
            )
        }.getOrNull()
    }

    companion object {
        private const val PREFS_NAME = "active_alarm_session"
        private const val KEY_PREFIX = "dose_"
    }
}

internal fun normalizeAlarmPayloads(payloads: List<AlarmPayload>): List<AlarmPayload> = payloads
    .distinctBy(AlarmPayload::occurrenceId)
    .sortedWith(compareBy(AlarmPayload::scheduledAt, AlarmPayload::medicationName))

internal data class AlarmConfirmationPlan(
    val confirmed: List<AlarmPayload>,
    val snoozed: List<AlarmPayload>,
) {
    companion object {
        fun create(payloads: List<AlarmPayload>, confirmedIds: Set<Long>): AlarmConfirmationPlan {
            val normalized = normalizeAlarmPayloads(payloads)
            return AlarmConfirmationPlan(
                confirmed = normalized.filter { it.occurrenceId in confirmedIds },
                snoozed = normalized.filterNot { it.occurrenceId in confirmedIds },
            )
        }
    }
}
