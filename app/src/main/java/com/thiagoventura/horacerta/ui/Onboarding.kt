package com.thiagoventura.horacerta.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thiagoventura.horacerta.R

@Composable
fun OnboardingFlow(
    initialName: String,
    notificationsGranted: Boolean,
    exactAlarmsGranted: Boolean,
    fullScreenGranted: Boolean,
    permissionRevision: Int,
    requestNotifications: ((Boolean) -> Unit) -> Unit,
    requestExactAlarms: () -> Unit,
    requestFullScreen: () -> Unit,
    onFinished: (String) -> Unit,
) {
    var page by rememberSaveable { mutableIntStateOf(0) }
    var name by rememberSaveable { mutableStateOf(initialName) }
    var permissionMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var exactRequestStarted by rememberSaveable { mutableStateOf(false) }
    var fullScreenRequestStarted by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(permissionRevision, page, notificationsGranted, exactAlarmsGranted, fullScreenGranted) {
        when {
            page == 2 && notificationsGranted -> {
                permissionMessage = null
                page = 3
            }
            page == 3 && exactAlarmsGranted -> {
                permissionMessage = null
                page = 4
            }
            page == 3 && exactRequestStarted -> {
                permissionMessage = "A permissão de alarmes exatos ainda não foi concedida."
            }
            page >= 4 && fullScreenGranted -> onFinished(name.ifBlank { "Você" })
            page >= 4 && fullScreenRequestStarted -> {
                permissionMessage = "A exibição na tela bloqueada ainda não foi permitida."
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Ivory),
    ) {
        AnimatedContent(
            targetState = page,
            transitionSpec = { fadeIn(tween(360)) togetherWith fadeOut(tween(220)) },
            label = "onboarding-page",
        ) { currentPage ->
            when (currentPage) {
                0 -> NamePage(
                    name = name,
                    onNameChanged = { name = it },
                    onContinue = { if (name.isNotBlank()) page = 1 },
                )
                1 -> HowPage(onContinue = { page = 2 })
                2 -> NotificationsPage(
                    message = permissionMessage,
                    onAllow = {
                        permissionMessage = null
                        requestNotifications { granted ->
                            if (granted) page = 3
                            else permissionMessage = "As notificações não foram permitidas. Você pode tentar novamente."
                        }
                    },
                    onSkip = {
                        permissionMessage = null
                        page = 3
                    },
                )
                3 -> ExactAlarmPage(
                    message = permissionMessage,
                    onAllow = {
                        if (exactAlarmsGranted) {
                            page = 4
                        } else {
                            permissionMessage = null
                            exactRequestStarted = true
                            requestExactAlarms()
                        }
                    },
                    onSkip = {
                        permissionMessage = null
                        page = 4
                    },
                )
                else -> LockScreenPage(
                    message = permissionMessage,
                    onAllow = {
                        if (fullScreenGranted) {
                            onFinished(name.ifBlank { "Você" })
                        } else {
                            permissionMessage = null
                            fullScreenRequestStarted = true
                            requestFullScreen()
                        }
                    },
                    onSkip = { onFinished(name.ifBlank { "Você" }) },
                )
            }
        }
    }
}

@Composable
private fun NamePage(
    name: String,
    onNameChanged: (String) -> Unit,
    onContinue: () -> Unit,
) {
    OnboardingColumn {
        HoraCertaBrand(modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(34.dp))
        Text("Bem-vindo!", style = MaterialTheme.typography.displayLarge, color = Ink)
        Text("Como podemos chamar você?", fontSize = 25.sp, color = Muted)
        Spacer(Modifier.height(18.dp))
        FloatingAsset(
            resource = R.drawable.mockup_onboarding_name,
            sourceX = 112,
            sourceY = 500,
            sourceWidth = 650,
            sourceHeight = 465,
            modifier = Modifier
                .fillMaxWidth()
                .height(225.dp),
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = name,
            onValueChange = onNameChanged,
            label = { Text("Seu nome", fontWeight = FontWeight.Bold) },
            singleLine = true,
            textStyle = MaterialTheme.typography.headlineSmall.copy(color = Ink),
            modifier = Modifier
                .fillMaxWidth()
                .height(82.dp),
            shape = RoundedCornerShape(11.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Cobalt,
                unfocusedBorderColor = Cobalt,
                focusedLabelColor = Cobalt,
                unfocusedLabelColor = Cobalt,
                cursorColor = Cobalt,
            ),
        )
        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier.padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(38.dp)
                    .background(CobaltSoft, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.Lock, null, tint = Cobalt, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.size(13.dp))
            Text("Usaremos seu nome apenas para\npersonalizar o app.", color = Muted, fontSize = 16.sp)
        }
        Spacer(Modifier.weight(1f))
        GradientPrimaryButton(
            text = "Continuar",
            onClick = onContinue,
            enabled = name.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(21.dp))
        DotPager(0, 5)
    }
}

@Composable
private fun HowPage(onContinue: () -> Unit) {
    OnboardingColumn {
        HoraCertaBrand(modifier = Modifier.fillMaxWidth(), titleSize = 25)
        Spacer(Modifier.height(25.dp))
        Text(
            "É simples cuidar\ndos seus horários",
            style = MaterialTheme.typography.headlineLarge,
            color = Ink,
        )
        Spacer(Modifier.height(22.dp))
        HowStep(
            number = 1,
            resource = R.drawable.mockup_onboarding_how,
            crop = intArrayOf(185, 445, 260, 260),
            title = "Cadastre",
            description = "Adicione o medicamento\ne escolha os horários.",
            drawLine = true,
        )
        HowStep(
            number = 2,
            resource = R.drawable.mockup_onboarding_how,
            crop = intArrayOf(170, 760, 300, 285),
            title = "Receba o alarme",
            description = "Ele toca mesmo com\no app fechado.",
            drawLine = true,
        )
        HowStep(
            number = 3,
            resource = R.drawable.mockup_onboarding_how,
            crop = intArrayOf(185, 1060, 270, 270),
            title = "Confirme a dose",
            description = "Marque quando tomar.\nSe não confirmar,\nlembraremos em 15 minutos.",
            drawLine = false,
        )
        Spacer(Modifier.height(10.dp))
        InfoStrip("Você pode corrigir uma confirmação\na qualquer momento.")
        Spacer(Modifier.height(14.dp))
        GradientPrimaryButton("Continuar", onContinue, Modifier.fillMaxWidth())
        Spacer(Modifier.height(13.dp))
        DotPager(1, 5, labelFirst = true)
    }
}

@Composable
private fun HowStep(
    number: Int,
    resource: Int,
    crop: IntArray,
    title: String,
    description: String,
    drawLine: Boolean,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (number == 3) 145.dp else 132.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.size(width = 56.dp, height = if (number == 3) 145.dp else 132.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                Modifier
                    .size(44.dp)
                    .background(BlueGradient, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(number.toString(), color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            }
            if (drawLine) {
                Box(Modifier.weight(1f).size(width = 2.dp, height = 1.dp).background(Color(0xFFCFE1FA)))
            }
        }
        CroppedMockupAsset(
            resource,
            crop[0], crop[1], crop[2], crop[3],
            modifier = Modifier.size(width = 130.dp, height = 124.dp),
        )
        Spacer(Modifier.size(7.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = Ink, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text(description, color = Ink, fontSize = 16.sp, lineHeight = 22.sp)
        }
    }
}

@Composable
private fun NotificationsPage(message: String?, onAllow: () -> Unit, onSkip: () -> Unit) {
    OnboardingColumn(horizontal = 31.dp) {
        DotPager(2, 5, labelFirst = true)
        Spacer(Modifier.height(18.dp))
        FloatingAsset(
            R.drawable.mockup_onboarding_notifications,
            248, 306, 380, 400,
            Modifier.fillMaxWidth().height(205.dp),
            rotation = 2.4f,
        )
        Text(
            "Não perca nenhum\nlembrete",
            style = MaterialTheme.typography.headlineLarge,
            color = Ink,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(10.dp))
        Text(
            "Permita as notificações para o Hora Certa\navisar quando chegar o momento de\ntomar seu medicamento.",
            color = Muted,
            fontSize = 19.sp,
            lineHeight = 24.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(18.dp))
        CheckLine("Avisar na hora certa")
        CheckLine("Mostrar nome e dosagem")
        CheckLine("Repetir a cada 15 min, se necessário", divider = false)
        message?.let { Text(it, color = Danger, fontSize = 15.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) }
        Spacer(Modifier.weight(1f))
        GradientPrimaryButton("Permitir notificações", onAllow, Modifier.fillMaxWidth())
        TextButton(onClick = onSkip, modifier = Modifier.fillMaxWidth()) {
            Text("Agora não", color = Cobalt, fontSize = 21.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ExactAlarmPage(message: String?, onAllow: () -> Unit, onSkip: () -> Unit) {
    OnboardingColumn(horizontal = 37.dp) {
        HoraCertaBrand(modifier = Modifier.fillMaxWidth(), titleSize = 26)
        FloatingAsset(
            R.drawable.mockup_onboarding_exact,
            95, 165, 675, 545,
            Modifier.fillMaxWidth().height(235.dp),
            rotation = 1.2f,
        )
        Text(
            "Alarmes sempre\nno horário",
            style = MaterialTheme.typography.headlineLarge.copy(fontSize = 34.sp, lineHeight = 36.sp),
            color = Ink,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(5.dp))
        Text(
            "Para tocar no horário exato, mesmo com o\napp fechado ou em economia de bateria,\no Hora Certa precisa acessar Alarmes\ne lembretes.",
            color = Muted,
            fontSize = 16.sp,
            lineHeight = 19.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))
        PermissionFeature(Icons.Rounded.Schedule, "Alarme exato", "Notificações no minuto certo.", compact = true)
        PermissionFeature(Icons.Rounded.Lock, "Funciona com o app fechado", "Você recebe o lembrete mesmo fora do app.", compact = true)
        PermissionFeature(Icons.Rounded.Refresh, "Reagenda após reiniciar", "O alarme volta a tocar se o celular reiniciar.", compact = true)
        message?.let { Text(it, color = Danger, fontSize = 14.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) }
        Spacer(Modifier.weight(1f))
        GradientPrimaryButton(
            "Permitir alarmes exatos",
            onAllow,
            Modifier.fillMaxWidth(),
            leading = { Icon(Icons.Rounded.NotificationsActive, null, tint = Color.White) },
        )
        TextButton(onClick = onSkip, modifier = Modifier.fillMaxWidth()) {
            Text("Agora não", color = Cobalt, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        }
        Row(
            modifier = Modifier.fillMaxWidth().background(CobaltSoft, RoundedCornerShape(12.dp)).padding(10.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.Settings, null, tint = Ink)
            Spacer(Modifier.size(10.dp))
            Text("Abriremos as configurações do Android.", color = Ink, fontSize = 15.sp)
        }
        Spacer(Modifier.height(9.dp))
        DotPager(3, 5, labelFirst = true)
    }
}

@Composable
private fun LockScreenPage(message: String?, onAllow: () -> Unit, onSkip: () -> Unit) {
    OnboardingColumn(horizontal = 26.dp) {
        HoraCertaBrand(modifier = Modifier.fillMaxWidth(), titleSize = 26)
        Spacer(Modifier.height(20.dp))
        Text("Alarme visível\nna tela bloqueada", style = MaterialTheme.typography.headlineLarge, color = Ink)
        Spacer(Modifier.height(8.dp))
        Text(
            "Permita que o Hora Certa mostre a tela do\nalarme quando o celular estiver bloqueado.",
            color = Muted,
            fontSize = 17.sp,
            lineHeight = 22.sp,
        )
        FloatingAsset(
            R.drawable.mockup_onboarding_lock,
            150, 495, 560, 490,
            Modifier.fillMaxWidth().height(245.dp),
        )
        PermissionFeature(Icons.Rounded.Vibration, "Tocar som e vibrar", "Para você não perder a dose.", filled = true)
        PermissionFeature(Icons.Rounded.Schedule, "Mostrar medicamento e horário", "Informações claras na tela bloqueada.", filled = true)
        PermissionFeature(Icons.Rounded.Check, "Abrir a confirmação da dose", "Confirme sua dose direto do alarme.", filled = true)
        message?.let { Text(it, color = Danger, fontSize = 14.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) }
        Spacer(Modifier.weight(1f))
        GradientPrimaryButton("Permitir e concluir", onAllow, Modifier.fillMaxWidth())
        TextButton(onClick = onSkip, modifier = Modifier.fillMaxWidth()) {
            Text("Concluir sem permitir", color = Cobalt, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(9.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.Security, null, tint = Cobalt, modifier = Modifier.size(28.dp))
            Spacer(Modifier.size(10.dp))
            Text("Você poderá revisar as permissões\nnas configurações.", color = Muted, fontSize = 15.sp)
        }
        Spacer(Modifier.height(9.dp))
        DotPager(4, 5)
    }
}

@Composable
private fun OnboardingColumn(
    horizontal: androidx.compose.ui.unit.Dp = 24.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = horizontal, vertical = 15.dp),
        content = content,
    )
}

@Composable
private fun FloatingAsset(
    resource: Int,
    sourceX: Int,
    sourceY: Int,
    sourceWidth: Int,
    sourceHeight: Int,
    modifier: Modifier,
    rotation: Float = 0f,
) {
    val transition = rememberInfiniteTransition(label = "illustration-motion")
    val motion by transition.animateFloat(
        initialValue = -3f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "float",
    )
    CroppedMockupAsset(
        resource, sourceX, sourceY, sourceWidth, sourceHeight,
        modifier = modifier.graphicsLayer {
            translationY = motion
            rotationZ = if (rotation == 0f) 0f else (motion / 3f) * rotation
        },
    )
}

@Composable
private fun CheckLine(text: String, divider: Boolean = true) {
    Row(
        modifier = Modifier.fillMaxWidth().height(66.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(34.dp).background(BlueGradient, CircleShape), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.Check, null, tint = Color.White, modifier = Modifier.size(21.dp))
        }
        Spacer(Modifier.size(15.dp))
        Text(text, color = Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }
    if (divider) Box(Modifier.fillMaxWidth().padding(start = 50.dp).height(1.dp).background(Color(0xFFE5E5E5)))
}

@Composable
private fun PermissionFeature(
    icon: ImageVector,
    title: String,
    description: String,
    filled: Boolean = false,
    compact: Boolean = false,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .background(if (filled) CobaltSoft.copy(alpha = .72f) else Color.White, RoundedCornerShape(13.dp))
            .border(1.dp, Color(0xFFEDEAE7), RoundedCornerShape(13.dp))
            .padding(horizontal = 12.dp, vertical = if (compact) 4.dp else 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(if (compact) 38.dp else 46.dp).background(if (filled) Cobalt else CobaltSoft, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = if (filled) Color.White else Cobalt, modifier = Modifier.size(if (compact) 23.dp else 25.dp))
        }
        Spacer(Modifier.size(12.dp))
        Column {
            Text(title, color = Ink, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Text(description, color = Muted, fontSize = 14.sp)
        }
    }
}

@Composable
private fun InfoStrip(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth().background(CobaltSoft, RoundedCornerShape(12.dp)).border(1.dp, Color(0xFF8CBDF8), RoundedCornerShape(12.dp)).padding(13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(34.dp).background(Ink, CircleShape), contentAlignment = Alignment.Center) {
            Text("i", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.size(13.dp))
        Text(text, color = Ink, fontSize = 16.sp)
    }
}
