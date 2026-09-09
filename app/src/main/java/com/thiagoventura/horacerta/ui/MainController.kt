package com.thiagoventura.horacerta.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.thiagoventura.horacerta.HoraCertaApplication
import com.thiagoventura.horacerta.data.DoseStatus
import com.thiagoventura.horacerta.data.DoseWithMedication
import com.thiagoventura.horacerta.data.Medication
import com.thiagoventura.horacerta.data.ProgressSummary
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

enum class MainTab { TODAY, CALENDAR, MEDICATIONS, PROGRESS }

class MainController(private val app: HoraCertaApplication) {
    var selectedTab by mutableStateOf(MainTab.TODAY)
    var selectedDate by mutableStateOf(LocalDate.now())
    var visibleMonth by mutableStateOf(YearMonth.now())
    var medications by mutableStateOf(emptyList<Medication>())
    var doses by mutableStateOf(emptyList<DoseWithMedication>())
    var progress by mutableStateOf(ProgressSummary(0, 100, 0, 0))
    var editorMedication by mutableStateOf<Medication?>(null)
    var editorVisible by mutableStateOf(false)
    var historyVisible by mutableStateOf(false)
    var historyDoses by mutableStateOf(emptyList<DoseWithMedication>())
    var inventoryMedication by mutableStateOf<Medication?>(null)
    var dataRevision by mutableStateOf(0)
        private set

    init {
        refresh()
    }

    fun refresh() {
        app.repository.ensureHorizon()
        medications = app.repository.medications()
        doses = app.repository.dosesForDay(selectedDate)
        progress = app.repository.progress()
        if (historyVisible) historyDoses = app.repository.doseHistory()
        dataRevision++
    }

    fun selectDate(date: LocalDate) {
        selectedDate = date
        visibleMonth = YearMonth.from(date)
        doses = app.repository.dosesForDay(date)
    }

    fun setMonth(month: YearMonth) {
        visibleMonth = month
    }

    fun openEditor(medication: Medication? = null) {
        editorMedication = medication
        editorVisible = true
    }

    fun closeEditor() {
        editorVisible = false
        editorMedication = null
    }

    fun openHistory() {
        historyDoses = app.repository.doseHistory()
        historyVisible = true
    }

    fun closeHistory() {
        historyVisible = false
    }

    fun openInventory(medication: Medication) {
        inventoryMedication = medication
    }

    fun closeInventory() {
        inventoryMedication = null
    }

    fun addStock(medication: Medication, quantity: Int) {
        if (quantity <= 0) return
        app.repository.addStock(medication.id, quantity)
        closeInventory()
        refresh()
    }

    fun saveMedication(medication: Medication) {
        if (medication.id != 0L) {
            app.repository.pendingForScheduling()
                .filter { it.medication.id == medication.id }
                .forEach { app.alarmScheduler.cancel(it.occurrence.id) }
        }
        app.repository.saveMedication(medication)
        app.alarmScheduler.rescheduleAll(app.repository)
        closeEditor()
        refresh()
    }

    fun deleteMedication(medication: Medication) {
        app.repository.pendingForScheduling()
            .filter { it.medication.id == medication.id }
            .forEach { app.alarmScheduler.cancel(it.occurrence.id) }
        app.repository.deleteMedication(medication.id)
        refresh()
    }

    fun toggleMedicationActive(medication: Medication) {
        saveMedication(medication.copy(active = !medication.active))
    }

    fun toggleDose(dose: DoseWithMedication) {
        val wasTaken = dose.occurrence.status == DoseStatus.TAKEN
        app.repository.markTaken(dose.occurrence.id, !wasTaken)
        if (wasTaken) {
            app.repository.dose(dose.occurrence.id)?.takeIf {
                it.occurrence.scheduledAt >= System.currentTimeMillis() - 24L * 60L * 60L * 1000L
            }?.let {
                app.alarmScheduler.schedule(
                    it,
                    maxOf(it.occurrence.scheduledAt, System.currentTimeMillis() + 15 * 60_000L),
                )
            }
        } else {
            app.alarmScheduler.cancel(dose.occurrence.id)
        }
        refresh()
    }

    fun monthDoses(): List<DoseWithMedication> {
        val zone = ZoneId.systemDefault()
        val start = visibleMonth.atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val end = visibleMonth.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
        return app.repository.doses(start, end)
    }
}
