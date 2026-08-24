package com.thiagoventura.horacerta.alarm

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.media.AudioAttributes
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import com.thiagoventura.horacerta.R
import com.thiagoventura.horacerta.ui.AlarmActivity

class AlarmRingingService : Service() {
    private var ringtone: Ringtone? = null
    private var vibrator: Vibrator? = null
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == AlarmContract.ACTION_STOP_RINGING) {
            stopSelf()
            return START_NOT_STICKY
        }

        val payload = intent?.toPayload() ?: return START_NOT_STICKY
        ringtone?.stop()
        vibrator?.cancel()
        acquireWakeLock()
        startForeground(NOTIFICATION_ID, buildNotification(payload))
        if (payload.sound) startSound()
        if (payload.vibration) startVibration()
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        ringtone?.stop()
        vibrator?.cancel()
        if (wakeLock?.isHeld == true) wakeLock?.release()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun buildNotification(payload: AlarmPayload): Notification {
        val alarmIntent = Intent(this, AlarmActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putPayload(payload)
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            this,
            payload.occurrenceId.toInt(),
            alarmIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Hora do medicamento")
            .setContentText("${payload.medicationName} • ${payload.dosage}")
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .setContentIntent(fullScreenPendingIntent)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .build()
    }

    private fun startSound() {
        val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        ringtone = RingtoneManager.getRingtone(this, uri)?.apply {
            audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) isLooping = true
            play()
        }
    }

    private fun startVibration() {
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            getSystemService(VibratorManager::class.java).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(VIBRATOR_SERVICE) as Vibrator
        }
        val pattern = longArrayOf(0, 700, 350, 700, 700)
        vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
    }

    private fun acquireWakeLock() {
        if (wakeLock?.isHeld == true) wakeLock?.release()
        val powerManager = getSystemService(PowerManager::class.java)
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "HoraCerta:AlarmWakeLock",
        ).apply { acquire(10 * 60_000L) }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.alarm_channel_name),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = getString(R.string.alarm_channel_description)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            setSound(null, null)
            enableVibration(false)
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    companion object {
        const val CHANNEL_ID = "medication_alarms_v1"
        private const val NOTIFICATION_ID = 4107

        fun stopIntent(context: android.content.Context) =
            Intent(context, AlarmRingingService::class.java).setAction(AlarmContract.ACTION_STOP_RINGING)
    }
}

fun Intent.toPayload(): AlarmPayload? {
    val id = getLongExtra(AlarmContract.EXTRA_OCCURRENCE_ID, -1L)
    if (id < 0) return null
    return AlarmPayload(
        occurrenceId = id,
        medicationName = getStringExtra(AlarmContract.EXTRA_MEDICATION_NAME).orEmpty(),
        dosage = getStringExtra(AlarmContract.EXTRA_DOSAGE).orEmpty(),
        scheduledAt = getLongExtra(AlarmContract.EXTRA_SCHEDULED_AT, System.currentTimeMillis()),
        sound = getBooleanExtra(AlarmContract.EXTRA_SOUND, true),
        vibration = getBooleanExtra(AlarmContract.EXTRA_VIBRATION, true),
    )
}

fun Intent.putPayload(payload: AlarmPayload) {
    putExtra(AlarmContract.EXTRA_OCCURRENCE_ID, payload.occurrenceId)
    putExtra(AlarmContract.EXTRA_MEDICATION_NAME, payload.medicationName)
    putExtra(AlarmContract.EXTRA_DOSAGE, payload.dosage)
    putExtra(AlarmContract.EXTRA_SCHEDULED_AT, payload.scheduledAt)
    putExtra(AlarmContract.EXTRA_SOUND, payload.sound)
    putExtra(AlarmContract.EXTRA_VIBRATION, payload.vibration)
}
