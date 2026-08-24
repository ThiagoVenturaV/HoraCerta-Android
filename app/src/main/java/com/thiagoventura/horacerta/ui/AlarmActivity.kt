package com.thiagoventura.horacerta.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Snooze
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.content.ContextCompat
import com.thiagoventura.horacerta.HoraCertaApplication
import com.thiagoventura.horacerta.alarm.ActiveAlarmStore
import com.thiagoventura.horacerta.alarm.AlarmContract
import com.thiagoventura.horacerta.alarm.AlarmPayload
import com.thiagoventura.horacerta.alarm.AlarmRingingService
import com.thiagoventura.horacerta.alarm.putPayload
import com.thiagoventura.horacerta.alarm.toPayload
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class AlarmActivity : ComponentActivity() {
    private lateinit var activeAlarmStore: ActiveAlarmStore
    private val activePayloadsState = mutableStateOf<List<AlarmPayload>>(emptyList())
    private var activeAlarmReceiverRegistered = false
    private val activeAlarmReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (
                intent?.action == AlarmContract.ACTION_ACTIVE_ALARMS_CHANGED &&
                ::activeAlarmStore.isInitialized
            ) {
                val payloads = activeAlarmStore.payloads()
                if (payloads.isNotEmpty()) activePayloadsState.value = payloads
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.rgb(0, 55, 147)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON,
            )
        }
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON,
        )
        val payload = intent.toPayload() ?: run {
            finish()
            return
        }
        activeAlarmStore = ActiveAlarmStore(this)
        activeAlarmStore.add(payload)
        ContextCompat.registerReceiver(
            this,
            activeAlarmReceiver,
            IntentFilter(AlarmContract.ACTION_ACTIVE_ALARMS_CHANGED),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        activeAlarmReceiverRegistered = true
        refreshActivePayloads(payload)
        val app = application as HoraCertaApplication

        setContent {
            HoraCertaTheme {
                BackHandler(enabled = true) { }
                AlarmScreen(
                    payloads = activePayloadsState.value,
                    onConfirm = {
                        stopAlarm()
                        val fallback = activePayloadsState.value.firstOrNull() ?: payload
                        startActivity(Intent(this, ConfirmDoseActivity::class.java).apply {
                            putPayload(fallback)
                            putExtra(AlarmContract.EXTRA_USE_ACTIVE_GROUP, true)
                        })
                        finish()
                    },
                    onSnooze = {
                        stopAlarm()
                        val activePayloads = activePayloadsState.value.ifEmpty { listOf(payload) }
                        activePayloads.forEach { active ->
                            app.alarmScheduler.scheduleSnooze(active)
                            runCatching { app.repository.incrementSnooze(active.occurrenceId) }
                        }
                        activeAlarmStore.removeAll(activePayloads.map(AlarmPayload::occurrenceId))
                        finishAndRemoveTask()
                    },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.toPayload()?.let { payload ->
            if (!::activeAlarmStore.isInitialized) activeAlarmStore = ActiveAlarmStore(this)
            activeAlarmStore.add(payload)
            refreshActivePayloads(payload)
        }
    }

    override fun onDestroy() {
        if (activeAlarmReceiverRegistered) {
            unregisterReceiver(activeAlarmReceiver)
            activeAlarmReceiverRegistered = false
        }
        super.onDestroy()
    }

    private fun refreshActivePayloads(fallback: AlarmPayload) {
        activePayloadsState.value = activeAlarmStore.payloads().ifEmpty { listOf(fallback) }
    }

    private fun stopAlarm() {
        stopService(AlarmRingingService.stopIntent(this))
    }
}

@Composable
private fun AlarmScreen(
    payloads: List<AlarmPayload>,
    onConfirm: () -> Unit,
    onSnooze: () -> Unit,
) {
    val payload = payloads.firstOrNull() ?: return
    val grouped = payloads.size > 1
    val time = Instant.ofEpochMilli(payload.scheduledAt)
        .atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("HH:mm"))

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BlueGradient)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 31.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(14.dp))
        PulsingAlarmGraphic(Modifier.fillMaxWidth().height(if (grouped) 145.dp else 220.dp))
        Text(
            time,
            color = Color.White,
            fontSize = if (grouped) 78.sp else 108.sp,
            lineHeight = if (grouped) 82.sp else 110.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-4).sp,
        )
        Text(
            if (grouped) "${payloads.size} medicamentos agora" else "Hora do medicamento",
            color = Color.White,
            fontSize = if (grouped) 31.sp else 28.sp,
            lineHeight = 34.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(if (grouped) 20.dp else 50.dp))
        if (grouped) {
            GroupedAlarmList(payloads, Modifier.weight(1f))
        } else {
            Text(
                payload.medicationName,
                color = Color.White,
                fontSize = 42.sp,
                lineHeight = 47.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(5.dp))
            Text(
                payload.dosage.ifBlank { "Dose programada" },
                color = Color.White.copy(alpha = .72f),
                fontSize = 25.sp,
                lineHeight = 30.sp,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.weight(1f))
        }
        AlarmActionButton(
            text = if (grouped) "Desligar e revisar" else "Desligar e confirmar",
            icon = { Icon(Icons.Rounded.Check, null, modifier = Modifier.size(28.dp)) },
            filled = true,
            onClick = onConfirm,
        )
        Spacer(Modifier.height(16.dp))
        AlarmActionButton(
            text = "Adiar 15 min",
            icon = { Icon(Icons.Rounded.Snooze, null, modifier = Modifier.size(28.dp)) },
            filled = false,
            onClick = onSnooze,
        )
        Spacer(Modifier.height(25.dp))
        Text(
            if (grouped) "As doses não confirmadas voltarão em 15 minutos" else "O lembrete continuará até você confirmar",
            color = Color.White,
            fontSize = 16.sp,
            lineHeight = 20.sp,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(14.dp))
    }
}

@Composable
private fun GroupedAlarmList(payloads: List<AlarmPayload>, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = .12f), RoundedCornerShape(20.dp))
            .padding(horizontal = 18.dp, vertical = 12.dp),
    ) {
        payloads.take(4).forEachIndexed { index, payload ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .size(34.dp)
                        .background(Color.White.copy(alpha = .18f), RoundedCornerShape(11.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("${index + 1}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                }
                Spacer(Modifier.size(13.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        payload.medicationName,
                        color = Color.White,
                        fontSize = 22.sp,
                        lineHeight = 25.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        payload.dosage.ifBlank { "Dose programada" },
                        color = Color.White.copy(alpha = .72f),
                        fontSize = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        if (payloads.size > 4) {
            Text(
                "+ ${payloads.size - 4} medicamentos",
                modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 4.dp),
                color = Color.White.copy(alpha = .78f),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun AlarmActionButton(
    text: String,
    icon: @Composable () -> Unit,
    filled: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(22.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (filled) 86.dp else 78.dp)
            .then(if (filled) Modifier.background(Color(0xFFF0F6FF), shape) else Modifier.border(2.dp, Color.White, shape))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CompositionLocalProvider(LocalContentColor provides if (filled) CobaltDark else Color.White) { icon() }
            Spacer(Modifier.size(10.dp))
            Text(
                text,
                color = if (filled) CobaltDark else Color.White,
                fontSize = 27.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun PulsingAlarmGraphic(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "alarm-pulse")
    val pulse by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1800, easing = LinearEasing), RepeatMode.Restart),
        label = "alarm-rings",
    )
    val breathe by transition.animateFloat(
        initialValue = .96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(tween(850), RepeatMode.Reverse),
        label = "clock-breathe",
    )

    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height * .56f)
            val base = 55.dp.toPx()
            repeat(3) { index ->
                val local = (pulse + index / 3f) % 1f
                val radius = base + 34.dp.toPx() * (index + 1) + local * 22.dp.toPx()
                drawCircle(
                    Color(0xFF42A5FF).copy(alpha = (1f - local) * .14f),
                    radius,
                    center,
                    style = Stroke(2.dp.toPx()),
                )
            }
            val waveColor = Color(0xFF53ADFF)
            listOf(-1f, 1f).forEach { side ->
                val left = center.x + side * 86.dp.toPx() - if (side < 0) 28.dp.toPx() else 0f
                drawArc(
                    color = waveColor,
                    startAngle = if (side < 0) 120f else -60f,
                    sweepAngle = 120f,
                    useCenter = false,
                    topLeft = Offset(left, center.y - 38.dp.toPx()),
                    size = Size(28.dp.toPx(), 76.dp.toPx()),
                    style = Stroke(7.dp.toPx(), cap = StrokeCap.Round),
                )
            }
        }
        Canvas(Modifier.size(126.dp).graphicsLayer { scaleX = breathe; scaleY = breathe }) {
            val center = Offset(size.width / 2f, size.height / 2f)
            drawCircle(Color.White.copy(alpha = .16f), 62.dp.toPx(), center)
            drawCircle(Color(0xFFBBD6F5), 51.dp.toPx(), center)
            drawCircle(Color(0xFFF7FBFF), 43.dp.toPx(), center)
            drawCircle(Color(0xFF0A4BB9), 19.dp.toPx(), center, style = Stroke(3.dp.toPx()))
            drawLine(Color(0xFF0A4BB9), center, Offset(center.x, center.y - 12.dp.toPx()), 3.dp.toPx(), StrokeCap.Round)
            drawLine(Color(0xFF0A4BB9), center, Offset(center.x + 11.dp.toPx(), center.y + 7.dp.toPx()), 3.dp.toPx(), StrokeCap.Round)
            drawArc(Color(0xFFFF6B6F), 300f, 39f, false, style = Stroke(7.dp.toPx(), cap = StrokeCap.Round))
        }
    }
}
