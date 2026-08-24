package com.thiagoventura.horacerta.data

import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class ScheduleValidatorTest {
    private val zone = ZoneId.of("America/Fortaleza")
    private val today = LocalDate.of(2026, 8, 24)
    private val now = today.atTime(15, 30).atZone(zone).toInstant().toEpochMilli()

    @Test
    fun `rejects a start date in the past`() {
        val error = ScheduleValidator.newScheduleError(
            medication(times = listOf(16 * 60), start = today.minusDays(1)),
            now,
            zone,
        )

        assertTrue(error.orEmpty().contains("data de início"))
    }

    @Test
    fun `rejects a fixed time that already passed today`() {
        val error = ScheduleValidator.newScheduleError(
            medication(times = listOf(15 * 60)),
            now,
            zone,
        )

        assertTrue(error.orEmpty().contains("15:00"))
    }

    @Test
    fun `allows an earlier clock time when today is not selected`() {
        val tuesdayOnly = 1 shl 1
        val error = ScheduleValidator.newScheduleError(
            medication(times = listOf(8 * 60), daysMask = tuesdayOnly),
            now,
            zone,
        )

        assertNull(error)
    }

    @Test
    fun `rejects an interval first dose that already passed`() {
        val firstDose = today.atTime(14, 0).atZone(zone).toInstant().toEpochMilli()
        val error = ScheduleValidator.newScheduleError(
            medication(
                kind = ScheduleKind.INTERVAL,
                times = emptyList(),
                firstDoseAt = firstDose,
            ),
            now,
            zone,
        )

        assertTrue(error.orEmpty().contains("14:00"))
    }

    @Test
    fun `allows a future fixed time today`() {
        val error = ScheduleValidator.newScheduleError(
            medication(times = listOf(16 * 60)),
            now,
            zone,
        )

        assertNull(error)
    }

    private fun medication(
        kind: ScheduleKind = ScheduleKind.FIXED_TIMES,
        times: List<Int>,
        start: LocalDate = today,
        daysMask: Int = Medication.ALL_DAYS_MASK,
        firstDoseAt: Long = today.atTime(16, 0).atZone(zone).toInstant().toEpochMilli(),
    ) = Medication(
        name = "Teste",
        dosage = "1 comprimido",
        scheduleKind = kind,
        timesMinutes = times,
        firstDoseAt = firstDoseAt,
        daysMask = daysMask,
        startEpochDay = start.toEpochDay(),
    )
}
