package com.thiagoventura.horacerta.data

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

object RecurrenceEngine {
    fun generate(
        medication: Medication,
        fromInclusive: Long,
        toExclusive: Long,
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): List<Long> {
        if (!medication.active || toExclusive <= fromInclusive) return emptyList()
        return when (medication.scheduleKind) {
            ScheduleKind.FIXED_TIMES -> generateFixed(medication, fromInclusive, toExclusive, zoneId)
            ScheduleKind.INTERVAL -> generateInterval(medication, fromInclusive, toExclusive, zoneId)
        }
    }

    private fun generateFixed(
        medication: Medication,
        fromInclusive: Long,
        toExclusive: Long,
        zoneId: ZoneId,
    ): List<Long> {
        val fromDay = Instant.ofEpochMilli(fromInclusive).atZone(zoneId).toLocalDate()
        val toDay = Instant.ofEpochMilli(toExclusive - 1).atZone(zoneId).toLocalDate()
        var day = maxOf(fromDay, LocalDate.ofEpochDay(medication.startEpochDay))
        val medicationEnd = medication.endEpochDay?.let(LocalDate::ofEpochDay)
        val lastDay = if (medicationEnd == null) toDay else minOf(toDay, medicationEnd)
        if (day > lastDay) return emptyList()

        val result = mutableListOf<Long>()
        val times = medication.timesMinutes.distinct().sorted()
        while (!day.isAfter(lastDay)) {
            if (medication.isScheduledOn(day.dayOfWeek)) {
                times.forEach { minutes ->
                    val hour = (minutes / 60).coerceIn(0, 23)
                    val minute = (minutes % 60).coerceIn(0, 59)
                    val instant = day.atTime(hour, minute).atZone(zoneId).toInstant().toEpochMilli()
                    if (instant in fromInclusive until toExclusive) result += instant
                }
            }
            day = day.plusDays(1)
        }
        return result
    }

    private fun generateInterval(
        medication: Medication,
        fromInclusive: Long,
        toExclusive: Long,
        zoneId: ZoneId,
    ): List<Long> {
        val intervalMillis = medication.intervalHours.coerceAtLeast(1) * 60L * 60L * 1000L
        val startBoundary = LocalDate.ofEpochDay(medication.startEpochDay)
            .atStartOfDay(zoneId).toInstant().toEpochMilli()
        val endBoundary = medication.endEpochDay?.let {
            LocalDate.ofEpochDay(it).plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
        } ?: Long.MAX_VALUE
        val base = maxOf(medication.firstDoseAt, startBoundary)
        val firstIndex = if (fromInclusive <= base) 0L else (fromInclusive - base + intervalMillis - 1) / intervalMillis
        var instant = base + firstIndex * intervalMillis
        val result = mutableListOf<Long>()
        while (instant < toExclusive && instant < endBoundary) {
            if (instant >= fromInclusive) result += instant
            instant += intervalMillis
        }
        return result
    }
}

object StreakCalculator {
    fun calculate(
        doses: List<DoseOccurrence>,
        now: Long = System.currentTimeMillis(),
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): ProgressSummary {
        val due = doses.filter { it.scheduledAt <= now }
        val taken = due.count { it.status == DoseStatus.TAKEN }
        val adherence = if (due.isEmpty()) 100 else ((taken * 100.0) / due.size).toInt()
        val byDay = due.groupBy { it.scheduledAt.toLocalDate(zoneId) }
        val today = Instant.ofEpochMilli(now).atZone(zoneId).toLocalDate()
        val scheduledDays = byDay.keys.sorted()

        var streak = 0
        for (date in scheduledDays.asReversed()) {
            val dayDoses = byDay.getValue(date)
            if (date == today && dayDoses.any { it.status == DoseStatus.PENDING }) {
                continue
            }
            if (dayDoses.all { it.status == DoseStatus.TAKEN }) {
                streak++
            } else {
                break
            }
        }

        var running = 0
        var best = 0
        scheduledDays.forEach { date ->
            val complete = byDay.getValue(date).all { it.status == DoseStatus.TAKEN }
            if (complete) {
                running++
                best = maxOf(best, running)
            } else if (date.isBefore(today)) {
                running = 0
            }
        }

        val pending = due.count {
            it.status == DoseStatus.PENDING && it.scheduledAt.toLocalDate(zoneId) == today
        }
        val missed = due.count {
            it.status == DoseStatus.PENDING && it.scheduledAt.toLocalDate(zoneId).isBefore(today)
        }
        val lastSevenDays = (6L downTo 0L).map { daysAgo ->
            val date = today.minusDays(daysAgo)
            val dayDoses = byDay[date].orEmpty()
            val dayTaken = dayDoses.count { it.status == DoseStatus.TAKEN }
            val status = when {
                dayDoses.isEmpty() -> DayProgressStatus.NO_DOSES
                dayDoses.all { it.status == DoseStatus.TAKEN } -> DayProgressStatus.COMPLETE
                date == today -> DayProgressStatus.PENDING
                else -> DayProgressStatus.MISSED
            }
            DayProgress(date, status, dayTaken, dayDoses.size)
        }

        return ProgressSummary(
            currentStreak = streak,
            adherencePercent = adherence,
            takenCount = taken,
            dueCount = due.size,
            bestStreak = best,
            pendingCount = pending,
            missedCount = missed,
            lastSevenDays = lastSevenDays,
        )
    }
}
