package com.thiagoventura.horacerta.data

import android.content.Context
import java.time.LocalDate
import java.time.ZoneId

class MedicationRepository(context: Context) {
    private val database = HoraCertaDatabase(context.applicationContext)
    private val preferences = context.getSharedPreferences("user_preferences", Context.MODE_PRIVATE)

    var userName: String
        get() = preferences.getString(KEY_USER_NAME, "") ?: ""
        set(value) { preferences.edit().putString(KEY_USER_NAME, value.trim()).apply() }

    var onboardingCompleted: Boolean
        get() = preferences.getBoolean(KEY_ONBOARDING_COMPLETED, false)
        set(value) { preferences.edit().putBoolean(KEY_ONBOARDING_COMPLETED, value).apply() }

    @Synchronized
    fun saveMedication(medication: Medication, now: Long = System.currentTimeMillis()): Medication {
        val saved = if (medication.id == 0L) {
            medication.copy(id = database.insertMedication(medication))
        } else {
            database.updateMedication(medication)
            medication
        }
        database.deleteFutureOccurrences(saved.id, now)
        generateFor(saved, now, horizonEnd(now))
        return saved
    }

    @Synchronized
    fun deleteMedication(id: Long) = database.deleteMedication(id)

    @Synchronized
    fun medications(): List<Medication> = database.getMedications()

    @Synchronized
    fun medication(id: Long): Medication? = database.getMedication(id)

    @Synchronized
    fun ensureHorizon(now: Long = System.currentTimeMillis()) {
        database.getMedications().filter(Medication::active).forEach { medication ->
            generateFor(medication, now - DAY_MILLIS, horizonEnd(now))
        }
    }

    @Synchronized
    fun regenerateFuture(now: Long = System.currentTimeMillis()) {
        database.getMedications().forEach { medication ->
            database.deleteFutureOccurrences(medication.id, now)
            if (medication.active) generateFor(medication, now, horizonEnd(now))
        }
    }

    @Synchronized
    fun dosesForDay(day: LocalDate, zoneId: ZoneId = ZoneId.systemDefault()): List<DoseWithMedication> {
        val start = day.atStartOfDay(zoneId).toInstant().toEpochMilli()
        val end = day.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
        return database.getDoses(start, end)
    }

    @Synchronized
    fun doses(from: Long, to: Long): List<DoseWithMedication> = database.getDoses(from, to)

    @Synchronized
    fun doseHistory(toInclusive: Long = System.currentTimeMillis()): List<DoseWithMedication> =
        database.getDoseHistory(toInclusive)

    @Synchronized
    fun pendingForScheduling(now: Long = System.currentTimeMillis()): List<DoseWithMedication> {
        return database.getPendingDoses(now - DAY_MILLIS, horizonEnd(now))
    }

    @Synchronized
    fun dose(id: Long): DoseWithMedication? = database.getDose(id)

    @Synchronized
    fun markTaken(id: Long, taken: Boolean) = database.setTaken(id, taken)

    @Synchronized
    fun incrementSnooze(id: Long) = database.incrementSnooze(id)

    @Synchronized
    fun progress(now: Long = System.currentTimeMillis()): ProgressSummary {
        val start = LocalDate.now().minusDays(180).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        return StreakCalculator.calculate(database.getDoses(start, now + 1).map { it.occurrence }, now)
    }

    private fun generateFor(medication: Medication, from: Long, to: Long) {
        val instants = RecurrenceEngine.generate(medication, from, to)
        database.insertOccurrences(medication.id, instants)
    }

    private fun horizonEnd(now: Long): Long = now + HORIZON_DAYS * DAY_MILLIS

    companion object {
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"
        private const val HORIZON_DAYS = 90L
        const val DAY_MILLIS = 24L * 60L * 60L * 1000L
    }
}
