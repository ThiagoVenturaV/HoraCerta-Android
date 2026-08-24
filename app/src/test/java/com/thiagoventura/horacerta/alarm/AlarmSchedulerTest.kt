package com.thiagoventura.horacerta.alarm

import org.junit.Assert.assertEquals
import org.junit.Test

class AlarmSchedulerTest {
    @Test
    fun `snooze for overdue dose starts from now`() {
        val now = 2_000_000L
        val payload = payload(scheduledAt = 1_000_000L)

        assertEquals(now + 15 * 60_000L, AlarmScheduler.snoozeTriggerAt(payload, now))
    }

    @Test
    fun `snooze for future dose starts from scheduled time`() {
        val now = 1_000_000L
        val scheduledAt = 2_000_000L
        val payload = payload(scheduledAt)

        assertEquals(scheduledAt + 15 * 60_000L, AlarmScheduler.snoozeTriggerAt(payload, now))
    }

    private fun payload(scheduledAt: Long) = AlarmPayload(
        occurrenceId = 1L,
        medicationName = "Teste",
        dosage = "1 comprimido",
        scheduledAt = scheduledAt,
        sound = true,
        vibration = true,
    )
}
