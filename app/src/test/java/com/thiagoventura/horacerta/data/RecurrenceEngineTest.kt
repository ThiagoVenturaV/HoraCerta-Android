package com.thiagoventura.horacerta.data

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class RecurrenceEngineTest {
    private val zone = ZoneId.of("UTC")

    @Test
    fun `fixed schedule creates every selected time`() {
        val startDay = LocalDate.of(2026, 8, 24)
        val medication = Medication(
            id = 1,
            name = "Teste",
            dosage = "1 comprimido",
            scheduleKind = ScheduleKind.FIXED_TIMES,
            timesMinutes = listOf(8 * 60, 20 * 60),
            startEpochDay = startDay.toEpochDay(),
        )
        val from = startDay.atStartOfDay(zone).toInstant().toEpochMilli()
        val to = startDay.plusDays(2).atStartOfDay(zone).toInstant().toEpochMilli()

        val generated = RecurrenceEngine.generate(medication, from, to, zone)

        assertEquals(4, generated.size)
        assertEquals(
            LocalDateTime.of(2026, 8, 24, 8, 0).atZone(zone).toInstant().toEpochMilli(),
            generated.first(),
        )
    }

    @Test
    fun `fixed schedule honors selected weekdays`() {
        val monday = LocalDate.of(2026, 8, 24)
        val mondayOnly = 1 shl 0
        val medication = Medication(
            id = 1,
            name = "Teste",
            dosage = "1 comprimido",
            scheduleKind = ScheduleKind.FIXED_TIMES,
            timesMinutes = listOf(9 * 60),
            daysMask = mondayOnly,
            startEpochDay = monday.toEpochDay(),
        )

        val generated = RecurrenceEngine.generate(
            medication,
            monday.atStartOfDay(zone).toInstant().toEpochMilli(),
            monday.plusDays(8).atStartOfDay(zone).toInstant().toEpochMilli(),
            zone,
        )

        assertEquals(2, generated.size)
    }

    @Test
    fun `interval schedule remains aligned with first dose`() {
        val day = LocalDate.of(2026, 8, 24)
        val first = day.atTime(6, 0).atZone(zone).toInstant().toEpochMilli()
        val medication = Medication(
            id = 1,
            name = "Teste",
            dosage = "10 ml",
            scheduleKind = ScheduleKind.INTERVAL,
            intervalHours = 8,
            firstDoseAt = first,
            startEpochDay = day.toEpochDay(),
        )

        val generated = RecurrenceEngine.generate(
            medication,
            day.atStartOfDay(zone).toInstant().toEpochMilli(),
            day.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli(),
            zone,
        )

        assertEquals(listOf(6, 14, 22), generated.map {
            java.time.Instant.ofEpochMilli(it).atZone(zone).hour
        })
    }

    @Test
    fun `streak counts consecutive complete days`() {
        val today = LocalDate.of(2026, 8, 24)
        val now = today.atTime(22, 0).atZone(zone).toInstant().toEpochMilli()
        val doses = listOf(
            occurrence(1, today, 8, DoseStatus.TAKEN),
            occurrence(2, today.minusDays(1), 8, DoseStatus.TAKEN),
            occurrence(3, today.minusDays(2), 8, DoseStatus.PENDING),
        )

        val summary = StreakCalculator.calculate(doses, now, zone)

        assertEquals(2, summary.currentStreak)
        assertEquals(2, summary.bestStreak)
        assertEquals(66, summary.adherencePercent)
        assertEquals(1, summary.missedCount)
        assertEquals(0, summary.pendingCount)
        assertEquals(DayProgressStatus.MISSED, summary.lastSevenDays[4].status)
        assertEquals(DayProgressStatus.COMPLETE, summary.lastSevenDays.last().status)
    }

    @Test
    fun `best streak remains historical after a missed day`() {
        val today = LocalDate.of(2026, 8, 24)
        val now = today.atTime(22, 0).atZone(zone).toInstant().toEpochMilli()
        val doses = listOf(
            occurrence(1, today.minusDays(4), 8, DoseStatus.TAKEN),
            occurrence(2, today.minusDays(3), 8, DoseStatus.TAKEN),
            occurrence(3, today.minusDays(2), 8, DoseStatus.TAKEN),
            occurrence(4, today.minusDays(1), 8, DoseStatus.PENDING),
            occurrence(5, today, 8, DoseStatus.TAKEN),
        )

        val summary = StreakCalculator.calculate(doses, now, zone)

        assertEquals(1, summary.currentStreak)
        assertEquals(3, summary.bestStreak)
        assertEquals(1, summary.missedCount)
    }

    @Test
    fun `pending dose today does not become a missed dose`() {
        val today = LocalDate.of(2026, 8, 24)
        val now = today.atTime(22, 0).atZone(zone).toInstant().toEpochMilli()
        val doses = listOf(
            occurrence(1, today.minusDays(1), 8, DoseStatus.TAKEN),
            occurrence(2, today, 8, DoseStatus.PENDING),
        )

        val summary = StreakCalculator.calculate(doses, now, zone)

        assertEquals(1, summary.currentStreak)
        assertEquals(1, summary.pendingCount)
        assertEquals(0, summary.missedCount)
        assertEquals(DayProgressStatus.PENDING, summary.lastSevenDays.last().status)
    }

    private fun occurrence(id: Long, day: LocalDate, hour: Int, status: DoseStatus) = DoseOccurrence(
        id = id,
        medicationId = 1,
        scheduledAt = day.atTime(hour, 0).atZone(zone).toInstant().toEpochMilli(),
        status = status,
        takenAt = null,
        snoozeCount = 0,
    )
}
