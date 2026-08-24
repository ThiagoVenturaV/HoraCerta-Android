package com.thiagoventura.horacerta.alarm

import org.junit.Assert.assertEquals
import org.junit.Test

class ActiveAlarmStoreTest {
    @Test
    fun `normalizes simultaneous doses without losing an occurrence`() {
        val later = payload(2, "Losartana", 1_000)
        val earlier = payload(1, "Metformina", 900)

        val result = normalizeAlarmPayloads(listOf(later, earlier, later))

        assertEquals(listOf(1L, 2L), result.map(AlarmPayload::occurrenceId))
    }

    @Test
    fun `confirmation plan snoozes only unchecked doses`() {
        val first = payload(1, "Losartana", 1_000)
        val second = payload(2, "Metformina", 1_000)

        val plan = AlarmConfirmationPlan.create(listOf(first, second), setOf(second.occurrenceId))

        assertEquals(listOf(2L), plan.confirmed.map(AlarmPayload::occurrenceId))
        assertEquals(listOf(1L), plan.snoozed.map(AlarmPayload::occurrenceId))
    }

    private fun payload(id: Long, name: String, scheduledAt: Long) = AlarmPayload(
        occurrenceId = id,
        medicationName = name,
        dosage = "1 comprimido",
        scheduledAt = scheduledAt,
        sound = true,
        vibration = true,
    )
}
