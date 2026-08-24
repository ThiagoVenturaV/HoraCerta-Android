package com.thiagoventura.horacerta.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.thiagoventura.horacerta.HoraCertaApplication

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        Thread {
            try {
                val scheduler = AlarmScheduler(context)
                if (intent.action == Intent.ACTION_LOCKED_BOOT_COMPLETED) {
                    scheduler.restoreFromDeviceProtectedMirror()
                } else {
                    val app = context.applicationContext as HoraCertaApplication
                    if (intent.action == Intent.ACTION_TIMEZONE_CHANGED || intent.action == Intent.ACTION_TIME_CHANGED) {
                        app.repository.pendingForScheduling().forEach {
                            scheduler.cancel(it.occurrence.id)
                        }
                        app.repository.regenerateFuture()
                    }
                    scheduler.rescheduleAll(app.repository)
                }
            } finally {
                pendingResult.finish()
            }
        }.start()
    }
}
