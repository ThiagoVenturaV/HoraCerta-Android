package com.thiagoventura.horacerta.data

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.time.LocalDate
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class InventoryDatabaseTest {
    private lateinit var context: Context
    private lateinit var databaseName: String
    private var database: HoraCertaDatabase? = null

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        databaseName = "inventory-${System.nanoTime()}.db"
    }

    @After
    fun tearDown() {
        database?.close()
        context.deleteDatabase(databaseName)
    }

    @Test
    fun takingAndUndoingDoseChangesStockExactlyOnce() {
        val db = HoraCertaDatabase(context, databaseName).also { database = it }
        val medicationId = db.insertMedication(trackedMedication(stock = 10, unitsPerDose = 2))
        db.insertOccurrences(medicationId, listOf(1_000L))
        val occurrenceId = db.getDoses(0, 2_000).single().occurrence.id

        db.setTaken(occurrenceId, true, now = 1_100L)

        assertEquals(8, db.getMedication(medicationId)?.stockQuantity)
        assertEquals(2, db.getDose(occurrenceId)?.occurrence?.inventoryConsumed)

        db.setTaken(occurrenceId, true, now = 1_200L)
        assertEquals(8, db.getMedication(medicationId)?.stockQuantity)

        db.setTaken(occurrenceId, false, now = 1_300L)
        assertEquals(10, db.getMedication(medicationId)?.stockQuantity)
        assertEquals(0, db.getDose(occurrenceId)?.occurrence?.inventoryConsumed)
    }

    @Test
    fun versionOneDatabaseMigratesWithInventoryDisabled() {
        context.openOrCreateDatabase(databaseName, Context.MODE_PRIVATE, null).use { db ->
            db.execSQL(
                """
                CREATE TABLE medications (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name TEXT NOT NULL,
                    dosage TEXT NOT NULL,
                    schedule_kind TEXT NOT NULL,
                    times_minutes TEXT NOT NULL,
                    interval_hours INTEGER NOT NULL,
                    first_dose_at INTEGER NOT NULL,
                    days_mask INTEGER NOT NULL,
                    start_epoch_day INTEGER NOT NULL,
                    end_epoch_day INTEGER,
                    sound INTEGER NOT NULL,
                    vibration INTEGER NOT NULL,
                    active INTEGER NOT NULL
                )
                """.trimIndent(),
            )
            db.execSQL(
                """
                CREATE TABLE occurrences (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    medication_id INTEGER NOT NULL,
                    scheduled_at INTEGER NOT NULL,
                    status TEXT NOT NULL DEFAULT 'PENDING',
                    taken_at INTEGER,
                    snooze_count INTEGER NOT NULL DEFAULT 0,
                    UNIQUE(medication_id, scheduled_at),
                    FOREIGN KEY(medication_id) REFERENCES medications(id) ON DELETE CASCADE
                )
                """.trimIndent(),
            )
            db.execSQL(
                """
                INSERT INTO medications (
                    name, dosage, schedule_kind, times_minutes, interval_hours,
                    first_dose_at, days_mask, start_epoch_day, end_epoch_day,
                    sound, vibration, active
                ) VALUES ('Losartana', '50 mg', 'FIXED_TIMES', '480', 8, 1000, 127, 1, NULL, 1, 1, 1)
                """.trimIndent(),
            )
            db.version = 1
        }

        val db = HoraCertaDatabase(context, databaseName).also { database = it }
        val migrated = db.getMedications().single()

        assertFalse(migrated.inventoryEnabled)
        assertEquals(0, migrated.stockQuantity)
        assertEquals(1, migrated.unitsPerDose)
        assertEquals(5, migrated.lowStockThreshold)
        assertEquals("unidades", migrated.stockUnit)
    }

    private fun trackedMedication(stock: Int, unitsPerDose: Int) = Medication(
        name = "Losartana",
        dosage = "50 mg",
        scheduleKind = ScheduleKind.FIXED_TIMES,
        timesMinutes = listOf(480),
        firstDoseAt = 1_000L,
        startEpochDay = LocalDate.now().toEpochDay(),
        inventoryEnabled = true,
        stockQuantity = stock,
        unitsPerDose = unitsPerDose,
        lowStockThreshold = 4,
        stockUnit = "comprimidos",
    )
}
