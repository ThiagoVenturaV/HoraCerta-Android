package com.thiagoventura.horacerta.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class HoraCertaDatabase(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.setForeignKeyConstraintsEnabled(true)
        db.enableWriteAheadLogging()
    }

    override fun onCreate(db: SQLiteDatabase) {
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
            """.trimIndent()
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
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX idx_occurrences_scheduled ON occurrences(scheduled_at)")
        db.execSQL("CREATE INDEX idx_occurrences_status ON occurrences(status)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit

    fun insertMedication(medication: Medication): Long {
        return writableDatabase.insertOrThrow("medications", null, medication.toValues())
    }

    fun updateMedication(medication: Medication) {
        writableDatabase.update(
            "medications",
            medication.toValues(),
            "id = ?",
            arrayOf(medication.id.toString()),
        )
    }

    fun deleteMedication(id: Long) {
        writableDatabase.delete("medications", "id = ?", arrayOf(id.toString()))
    }

    fun getMedication(id: Long): Medication? {
        readableDatabase.query(
            "medications", null, "id = ?", arrayOf(id.toString()), null, null, null,
        ).use { cursor -> return if (cursor.moveToFirst()) cursor.toMedication() else null }
    }

    fun getMedications(): List<Medication> {
        readableDatabase.query("medications", null, null, null, null, null, "name COLLATE NOCASE").use { cursor ->
            return buildList {
                while (cursor.moveToNext()) add(cursor.toMedication())
            }
        }
    }

    fun deleteFutureOccurrences(medicationId: Long, from: Long) {
        writableDatabase.delete(
            "occurrences",
            "medication_id = ? AND scheduled_at >= ?",
            arrayOf(medicationId.toString(), from.toString()),
        )
    }

    fun insertOccurrences(medicationId: Long, instants: List<Long>) {
        if (instants.isEmpty()) return
        val db = writableDatabase
        db.beginTransaction()
        try {
            instants.forEach { instant ->
                val values = ContentValues().apply {
                    put("medication_id", medicationId)
                    put("scheduled_at", instant)
                    put("status", DoseStatus.PENDING.name)
                }
                db.insertWithOnConflict("occurrences", null, values, SQLiteDatabase.CONFLICT_IGNORE)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun getDoses(fromInclusive: Long, toExclusive: Long): List<DoseWithMedication> {
        val sql = """
            SELECT o.id AS o_id, o.medication_id AS o_medication_id, o.scheduled_at AS o_scheduled_at,
                   o.status AS o_status, o.taken_at AS o_taken_at, o.snooze_count AS o_snooze_count,
                   m.*
            FROM occurrences o
            JOIN medications m ON m.id = o.medication_id
            WHERE o.scheduled_at >= ? AND o.scheduled_at < ?
            ORDER BY o.scheduled_at ASC
        """.trimIndent()
        readableDatabase.rawQuery(sql, arrayOf(fromInclusive.toString(), toExclusive.toString())).use { cursor ->
            return buildList {
                while (cursor.moveToNext()) add(cursor.toDoseWithMedication())
            }
        }
    }

    fun getDoseHistory(toInclusive: Long): List<DoseWithMedication> {
        val sql = """
            SELECT o.id AS o_id, o.medication_id AS o_medication_id, o.scheduled_at AS o_scheduled_at,
                   o.status AS o_status, o.taken_at AS o_taken_at, o.snooze_count AS o_snooze_count,
                   m.*
            FROM occurrences o
            JOIN medications m ON m.id = o.medication_id
            WHERE o.scheduled_at <= ?
            ORDER BY o.scheduled_at DESC
        """.trimIndent()
        readableDatabase.rawQuery(sql, arrayOf(toInclusive.toString())).use { cursor ->
            return buildList {
                while (cursor.moveToNext()) add(cursor.toDoseWithMedication())
            }
        }
    }

    fun getDose(id: Long): DoseWithMedication? {
        val sql = """
            SELECT o.id AS o_id, o.medication_id AS o_medication_id, o.scheduled_at AS o_scheduled_at,
                   o.status AS o_status, o.taken_at AS o_taken_at, o.snooze_count AS o_snooze_count,
                   m.*
            FROM occurrences o JOIN medications m ON m.id = o.medication_id
            WHERE o.id = ?
        """.trimIndent()
        readableDatabase.rawQuery(sql, arrayOf(id.toString())).use { cursor ->
            return if (cursor.moveToFirst()) cursor.toDoseWithMedication() else null
        }
    }

    fun getPendingDoses(fromInclusive: Long, toExclusive: Long): List<DoseWithMedication> {
        return getDoses(fromInclusive, toExclusive).filter {
            it.occurrence.status == DoseStatus.PENDING && it.medication.active
        }
    }

    fun setTaken(id: Long, taken: Boolean, now: Long = System.currentTimeMillis()) {
        val values = ContentValues().apply {
            put("status", if (taken) DoseStatus.TAKEN.name else DoseStatus.PENDING.name)
            if (taken) put("taken_at", now) else putNull("taken_at")
        }
        writableDatabase.update("occurrences", values, "id = ?", arrayOf(id.toString()))
    }

    fun incrementSnooze(id: Long) {
        writableDatabase.execSQL(
            "UPDATE occurrences SET snooze_count = snooze_count + 1 WHERE id = ?",
            arrayOf(id),
        )
    }

    private fun Medication.toValues() = ContentValues().apply {
        put("name", name.trim())
        put("dosage", dosage.trim())
        put("schedule_kind", scheduleKind.name)
        put("times_minutes", timesMinutes.distinct().sorted().joinToString(","))
        put("interval_hours", intervalHours)
        put("first_dose_at", firstDoseAt)
        put("days_mask", daysMask)
        put("start_epoch_day", startEpochDay)
        if (endEpochDay == null) putNull("end_epoch_day") else put("end_epoch_day", endEpochDay)
        put("sound", sound.asInt())
        put("vibration", vibration.asInt())
        put("active", active.asInt())
    }

    private fun Cursor.toMedication(): Medication = Medication(
        id = getLong(column("id")),
        name = getString(column("name")),
        dosage = getString(column("dosage")),
        scheduleKind = ScheduleKind.valueOf(getString(column("schedule_kind"))),
        timesMinutes = getString(column("times_minutes")).split(',').mapNotNull(String::toIntOrNull),
        intervalHours = getInt(column("interval_hours")),
        firstDoseAt = getLong(column("first_dose_at")),
        daysMask = getInt(column("days_mask")),
        startEpochDay = getLong(column("start_epoch_day")),
        endEpochDay = getNullableLong(column("end_epoch_day")),
        sound = getInt(column("sound")) == 1,
        vibration = getInt(column("vibration")) == 1,
        active = getInt(column("active")) == 1,
    )

    private fun Cursor.toDoseWithMedication() = DoseWithMedication(
        occurrence = DoseOccurrence(
            id = getLong(column("o_id")),
            medicationId = getLong(column("o_medication_id")),
            scheduledAt = getLong(column("o_scheduled_at")),
            status = DoseStatus.valueOf(getString(column("o_status"))),
            takenAt = getNullableLong(column("o_taken_at")),
            snoozeCount = getInt(column("o_snooze_count")),
        ),
        medication = toMedication(),
    )

    private fun Cursor.column(name: String) = getColumnIndexOrThrow(name)
    private fun Cursor.getNullableLong(index: Int) = if (isNull(index)) null else getLong(index)
    private fun Boolean.asInt() = if (this) 1 else 0

    companion object {
        private const val DATABASE_NAME = "hora_certa.db"
        private const val DATABASE_VERSION = 1
    }
}
