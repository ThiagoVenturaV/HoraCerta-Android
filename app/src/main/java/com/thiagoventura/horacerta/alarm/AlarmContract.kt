package com.thiagoventura.horacerta.alarm

object AlarmContract {
    const val EXTRA_OCCURRENCE_ID = "occurrence_id"
    const val EXTRA_MEDICATION_NAME = "medication_name"
    const val EXTRA_DOSAGE = "dosage"
    const val EXTRA_SCHEDULED_AT = "scheduled_at"
    const val EXTRA_SOUND = "sound"
    const val EXTRA_VIBRATION = "vibration"
    const val ACTION_STOP_RINGING = "com.thiagoventura.horacerta.STOP_RINGING"
    const val SNOOZE_MINUTES = 15L
}

data class AlarmPayload(
    val occurrenceId: Long,
    val medicationName: String,
    val dosage: String,
    val scheduledAt: Long,
    val sound: Boolean,
    val vibration: Boolean,
)
