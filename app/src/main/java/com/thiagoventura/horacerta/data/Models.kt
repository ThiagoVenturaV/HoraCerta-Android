package com.thiagoventura.horacerta.data

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

enum class ScheduleKind { FIXED_TIMES, INTERVAL }

enum class DoseStatus { PENDING, TAKEN }

data class Medication(
    val id: Long = 0,
    val name: String,
    val dosage: String,
    val scheduleKind: ScheduleKind,
    val timesMinutes: List<Int> = emptyList(),
    val intervalHours: Int = 8,
    val firstDoseAt: Long = System.currentTimeMillis(),
    val daysMask: Int = ALL_DAYS_MASK,
    val startEpochDay: Long = LocalDate.now().toEpochDay(),
    val endEpochDay: Long? = null,
    val sound: Boolean = true,
    val vibration: Boolean = true,
    val active: Boolean = true,
    val inventoryEnabled: Boolean = false,
    val stockQuantity: Int = 0,
    val unitsPerDose: Int = 1,
    val lowStockThreshold: Int = 5,
    val stockUnit: String = "unidades",
) {
    fun isScheduledOn(dayOfWeek: DayOfWeek): Boolean {
        val bit = 1 shl (dayOfWeek.value - 1)
        return daysMask and bit != 0
    }

    companion object {
        const val ALL_DAYS_MASK = 0b1111111
    }
}

data class DoseOccurrence(
    val id: Long,
    val medicationId: Long,
    val scheduledAt: Long,
    val status: DoseStatus,
    val takenAt: Long?,
    val snoozeCount: Int,
    val inventoryConsumed: Int = 0,
)

data class DoseWithMedication(
    val occurrence: DoseOccurrence,
    val medication: Medication,
)

data class ProgressSummary(
    val currentStreak: Int,
    val adherencePercent: Int,
    val takenCount: Int,
    val dueCount: Int,
    val bestStreak: Int = 0,
    val pendingCount: Int = 0,
    val missedCount: Int = 0,
    val lastSevenDays: List<DayProgress> = emptyList(),
)

enum class DayProgressStatus { COMPLETE, PENDING, MISSED, NO_DOSES }

data class DayProgress(
    val date: LocalDate,
    val status: DayProgressStatus,
    val takenCount: Int,
    val dueCount: Int,
)

fun Long.toLocalDate(zoneId: ZoneId = ZoneId.systemDefault()): LocalDate =
    Instant.ofEpochMilli(this).atZone(zoneId).toLocalDate()
