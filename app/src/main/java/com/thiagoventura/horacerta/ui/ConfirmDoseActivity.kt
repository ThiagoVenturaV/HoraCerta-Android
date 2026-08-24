package com.thiagoventura.horacerta.ui

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
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
import com.thiagoventura.horacerta.HoraCertaApplication
import com.thiagoventura.horacerta.MainActivity
import com.thiagoventura.horacerta.alarm.AlarmPayload
import com.thiagoventura.horacerta.alarm.AlarmScheduler
import com.thiagoventura.horacerta.alarm.toPayload
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class ConfirmDoseActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.rgb(254, 251, 248)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
        }
        val payload = intent.toPayload() ?: run {
            finish()
            return
        }
        val app = application as HoraCertaApplication

        setContent {
            HoraCertaTheme {
                ConfirmDoseScreen(
                    payload = payload,
                    onBack = { returnToApp() },
                    onConfirmed = {
                        runCatching { app.repository.markTaken(payload.occurrenceId, true) }
                        app.alarmScheduler.cancel(payload.occurrenceId)
                        returnToApp()
                    },
                    onNotYet = {
                        app.alarmScheduler.scheduleSnooze(payload)
                        runCatching { app.repository.incrementSnooze(payload.occurrenceId) }
                        returnToApp()
                    },
                )
            }
        }
    }

    private fun returnToApp() {
        startActivity(Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        })
        finish()
    }
}

@Composable
private fun ConfirmDoseScreen(
    payload: AlarmPayload,
    onBack: () -> Unit,
    onConfirmed: () -> Unit,
    onNotYet: () -> Unit,
) {
    val doseTime = payload.scheduledAt.asTime()
    val nextTime = remember(payload.occurrenceId) { AlarmScheduler.snoozeTriggerAt(payload).asTime() }
    var entered by remember { mutableStateOf(false) }
    val cardScale by animateFloatAsState(if (entered) 1f else .94f, tween(420), label = "confirm-card")
    val cardAlpha by animateFloatAsState(if (entered) 1f else 0f, tween(320), label = "confirm-alpha")
    LaunchedEffect(Unit) { entered = true }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Ivory)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 18.dp),
    ) {
        Row(Modifier.fillMaxWidth().height(88.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack, modifier = Modifier.size(54.dp)) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Voltar", tint = CobaltDark, modifier = Modifier.size(34.dp))
            }
            Spacer(Modifier.size(18.dp))
            Text("Confirmar dose", color = Ink, fontSize = 29.sp, fontWeight = FontWeight.Bold)
        }

        MedicationDoseCard(
            payload = payload,
            doseTime = doseTime,
            modifier = Modifier.graphicsLayer {
                scaleX = cardScale
                scaleY = cardScale
                alpha = cardAlpha
            },
        )
        Spacer(Modifier.height(47.dp))
        Text(
            "Você tomou este medicamento?",
            color = Ink,
            fontSize = 29.sp,
            lineHeight = 34.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
        )
        Spacer(Modifier.weight(1f))

        Row(
            Modifier.fillMaxWidth().padding(horizontal = 2.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.Info, null, tint = Color(0xFF244D70), modifier = Modifier.size(25.dp))
            Spacer(Modifier.size(12.dp))
            Text("Você poderá alterar esta confirmação depois.", color = Color(0xFF244D70), fontSize = 17.sp)
        }
        Spacer(Modifier.height(26.dp))
        ConfirmActionButton("Confirmar", true, onConfirmed)
        Spacer(Modifier.height(17.dp))
        ConfirmActionButton("Adiar 15 min", false, onNotYet)
        Spacer(Modifier.height(24.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(88.dp)
                .background(Color(0xFFE2EEFF), RoundedCornerShape(16.dp))
                .padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(43.dp).background(Color.White.copy(alpha = .7f), CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Schedule, null, tint = Color(0xFF0A63E8), modifier = Modifier.size(30.dp))
            }
            Spacer(Modifier.size(17.dp))
            Text(
                "Sem confirmação, o alarme\ntocará novamente às $nextTime",
                color = Ink,
                fontSize = 18.sp,
                lineHeight = 23.sp,
                fontWeight = FontWeight.Medium,
            )
        }
        Spacer(Modifier.height(28.dp))
    }
}

@Composable
private fun MedicationDoseCard(payload: AlarmPayload, doseTime: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(150.dp)
            .background(BlueGradient, RoundedCornerShape(18.dp)),
    ) {
        PillMark(Modifier.align(Alignment.CenterStart).padding(start = 23.dp).size(width = 42.dp, height = 58.dp))
        Column(
            Modifier
                .align(Alignment.CenterStart)
                .padding(start = 80.dp, end = 112.dp),
        ) {
            Text(
                payload.medicationName,
                color = Color.White,
                fontSize = 28.sp,
                lineHeight = 33.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(9.dp))
            Text("Dose das $doseTime", color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.Medium)
        }
        ConfirmClock(Modifier.align(Alignment.CenterEnd).padding(end = 20.dp).size(84.dp))
    }
}

@Composable
private fun ConfirmClock(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val center = Offset(size.width / 2f, size.height / 2f)
        drawCircle(Color.White.copy(alpha = .25f), size.minDimension * .5f)
        drawCircle(Color(0xFFF6FAFF), size.minDimension * .39f)
        drawCircle(Color(0xFF0C50BA), size.minDimension * .17f, center, style = Stroke(2.5.dp.toPx()))
        drawLine(Color(0xFF0C50BA), center, Offset(center.x, center.y - 11.dp.toPx()), 2.5.dp.toPx(), StrokeCap.Round)
        drawLine(Color(0xFF0C50BA), center, Offset(center.x + 9.dp.toPx(), center.y + 6.dp.toPx()), 2.5.dp.toPx(), StrokeCap.Round)
        drawArc(Color.White, 278f, 77f, false, style = Stroke(5.dp.toPx(), cap = StrokeCap.Round))
    }
}

@Composable
private fun ConfirmActionButton(text: String, filled: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(13.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(66.dp)
            .then(if (filled) Modifier.background(BlueGradient, shape) else Modifier.border(2.dp, Color(0xFF0757CF), shape))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (filled) {
                Icon(Icons.Rounded.Check, null, tint = Color.White, modifier = Modifier.size(25.dp))
                Spacer(Modifier.size(8.dp))
            }
            Text(
                text,
                color = if (filled) Color.White else Color(0xFF0757CF),
                fontSize = 25.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
        }
    }
}

private fun Long.asTime(): String = Instant.ofEpochMilli(this)
    .atZone(ZoneId.systemDefault())
    .format(DateTimeFormatter.ofPattern("HH:mm"))
