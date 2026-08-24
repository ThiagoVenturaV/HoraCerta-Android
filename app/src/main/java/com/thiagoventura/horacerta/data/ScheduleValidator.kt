package com.thiagoventura.horacerta.data

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object ScheduleValidator {
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    fun newScheduleError(
        medication: Medication,
        now: Long = System.currentTimeMillis(),
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): String? {
        val today = Instant.ofEpochMilli(now).atZone(zoneId).toLocalDate()
        val startDate = LocalDate.ofEpochDay(medication.startEpochDay)

        if (startDate.isBefore(today)) {
            return "A data de início não pode estar no passado."
        }
        if (medication.endEpochDay?.let(LocalDate::ofEpochDay)?.isBefore(startDate) == true) {
            return "A data de término não pode ser anterior à data de início."
        }
        if (startDate.isAfter(today)) return null

        return when (medication.scheduleKind) {
            ScheduleKind.FIXED_TIMES -> {
                if (!medication.isScheduledOn(today.dayOfWeek)) return null
                val pastTime = medication.timesMinutes
                    .distinct()
                    .sorted()
                    .firstOrNull { minutes ->
                        val scheduledAt = today
                            .atTime(minutes / 60, minutes % 60)
                            .atZone(zoneId)
                            .toInstant()
                            .toEpochMilli()
                        scheduledAt <= now
                    }
                pastTime?.let {
                    val formatted = today.atTime(it / 60, it % 60).format(timeFormatter)
                    "O horário $formatted de hoje já passou. Escolha um horário futuro."
                }
            }

            ScheduleKind.INTERVAL -> {
                if (medication.firstDoseAt <= now) {
                    val formatted = Instant.ofEpochMilli(medication.firstDoseAt)
                        .atZone(zoneId)
                        .format(timeFormatter)
                    "O horário $formatted de hoje já passou. Escolha uma primeira dose futura."
                } else {
                    null
                }
            }
        }
    }
}
