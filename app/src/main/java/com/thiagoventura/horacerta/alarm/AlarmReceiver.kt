package com.thiagoventura.horacerta.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        intent.toPayload()?.let { payload ->
            // A próxima repetição já fica armada. Confirmar a dose é a única ação
            // que cancela esse ciclo; ignorar a tela também repete em 15 minutos.
            AlarmScheduler(context).scheduleSnooze(payload)
        }
        val serviceIntent = Intent(context, AlarmRingingService::class.java).apply {
            putExtras(intent)
        }
        ContextCompat.startForegroundService(context, serviceIntent)
    }
}
