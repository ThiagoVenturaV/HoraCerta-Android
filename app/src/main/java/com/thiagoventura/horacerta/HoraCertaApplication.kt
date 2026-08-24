package com.thiagoventura.horacerta

import android.app.Application
import com.thiagoventura.horacerta.alarm.AlarmScheduler
import com.thiagoventura.horacerta.data.MedicationRepository

class HoraCertaApplication : Application() {
    val repository by lazy { MedicationRepository(this) }
    val alarmScheduler by lazy { AlarmScheduler(this) }
}
