package com.thiagoventura.horacerta.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.DialogInterface
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thiagoventura.horacerta.data.Medication
import com.thiagoventura.horacerta.data.ScheduleKind
import com.thiagoventura.horacerta.data.ScheduleValidator
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun MedicationEditorScreen(
    medication: Medication?,
    onBack: () -> Unit,
    onSave: (Medication) -> Unit,
) {
    var name by remember(medication?.id) { mutableStateOf(medication?.name.orEmpty()) }
    var dosage by remember(medication?.id) { mutableStateOf(medication?.dosage.orEmpty()) }
    var kind by remember(medication?.id) { mutableStateOf(medication?.scheduleKind ?: ScheduleKind.FIXED_TIMES) }
    var times by remember(medication?.id) { mutableStateOf(medication?.timesMinutes ?: emptyList()) }
    var timeInput by remember(medication?.id) { mutableStateOf("") }
    var interval by remember(medication?.id) { mutableIntStateOf(medication?.intervalHours ?: 8) }
    var firstTime by remember(medication?.id) {
        mutableStateOf(medication?.let { formatTime(Instant.ofEpochMilli(it.firstDoseAt).atZone(ZoneId.systemDefault()).toLocalTime()) } ?: "08:00")
    }
    var startDate by remember(medication?.id) {
        mutableStateOf(LocalDate.ofEpochDay(medication?.startEpochDay ?: LocalDate.now().toEpochDay()))
    }
    var endDate by remember(medication?.id) {
        mutableStateOf(medication?.endEpochDay?.let(LocalDate::ofEpochDay))
    }
    var daysMask by remember(medication?.id) { mutableIntStateOf(medication?.daysMask ?: Medication.ALL_DAYS_MASK) }
    var soundAndVibration by remember(medication?.id) { mutableStateOf(medication?.let { it.sound || it.vibration } ?: true) }
    var message by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current

    BackHandler(onBack = onBack)

    Column(Modifier.fillMaxSize().background(Ivory).statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().height(76.dp).padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Voltar", tint = Cobalt, modifier = Modifier.size(30.dp)) }
            PillMark(Modifier.size(width = 32.dp, height = 46.dp))
            Spacer(Modifier.size(14.dp))
            Text(if (medication == null) "Novo medicamento" else "Editar medicamento", color = Ink, fontSize = 29.sp, fontWeight = FontWeight.Bold)
        }

        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 19.dp),
            verticalArrangement = Arrangement.spacedBy(15.dp),
        ) {
            LabeledField("Nome do medicamento", name, { name = it }, "Ex.: Amoxicilina")
            LabeledField("Dosagem", dosage, { dosage = it }, "Ex.: 500 mg")

            Column {
                FieldLabel("Frequência")
                Row(Modifier.fillMaxWidth().height(56.dp).border(1.dp, Color(0xFFCDCDCD), RoundedCornerShape(12.dp)).clip(RoundedCornerShape(12.dp))) {
                    FrequencyOption("Horários fixos", Icons.Rounded.Schedule, kind == ScheduleKind.FIXED_TIMES, Modifier.weight(1f)) { kind = ScheduleKind.FIXED_TIMES }
                    FrequencyOption("A cada X horas", Icons.Rounded.Replay, kind == ScheduleKind.INTERVAL, Modifier.weight(1f)) { kind = ScheduleKind.INTERVAL }
                }
            }

            AnimatedContent(targetState = kind, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "frequency-content") { selectedKind ->
                if (selectedKind == ScheduleKind.INTERVAL) {
                    IntervalPanel(
                        interval = interval,
                        onInterval = { interval = it },
                        firstTime = firstTime,
                        onPickFirstTime = {
                            showTimePicker(context, parseLocalTime(firstTime) ?: LocalTime.of(8, 0)) {
                                firstTime = formatTime(it)
                                message = null
                            }
                        },
                    )
                } else {
                    FixedTimesPanel(times, { times = it }, timeInput, { timeInput = it }, daysMask, { daysMask = it })
                }
            }

            DatePanel(
                start = startDate,
                end = endDate,
                onStartClick = {
                    showDatePicker(
                        context = context,
                        initial = startDate,
                        minimum = LocalDate.now(),
                        maximum = endDate,
                    ) { selected ->
                        startDate = selected
                        if (endDate?.isBefore(selected) == true) endDate = null
                        message = null
                    }
                },
                onEndClick = {
                    showDatePicker(
                        context = context,
                        initial = endDate ?: startDate,
                        minimum = startDate,
                        allowClear = true,
                        onClear = {
                            endDate = null
                            message = null
                        },
                    ) { selected ->
                        endDate = selected
                        message = null
                    }
                },
            )

            Row(Modifier.fillMaxWidth().height(64.dp).border(1.dp, Color(0xFFD7D5D2), RoundedCornerShape(13.dp)).padding(horizontal = 15.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(40.dp).background(CobaltSoft, CircleShape), contentAlignment = Alignment.Center) { Icon(Icons.Rounded.Alarm, null, tint = Ink) }
                Spacer(Modifier.size(14.dp))
                Text("Som e vibração", color = Ink, fontSize = 19.sp, modifier = Modifier.weight(1f))
                Switch(
                    checked = soundAndVibration,
                    onCheckedChange = { soundAndVibration = it },
                    colors = SwitchDefaults.colors(checkedTrackColor = Cobalt, checkedThumbColor = Color.White),
                )
            }

            message?.let { Text(it, color = Danger, fontSize = 15.sp) }
            Spacer(Modifier.height(6.dp))
        }

        GradientPrimaryButton(
            text = "Salvar medicamento",
            onClick = {
                val first = parseLocalTime(firstTime)
                val firstDose = startDate
                    .atTime(first ?: LocalTime.of(8, 0))
                    .atZone(ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli()
                val candidate = Medication(
                    id = medication?.id ?: 0,
                    name = name.trim(),
                    dosage = dosage.trim(),
                    scheduleKind = kind,
                    timesMinutes = if (kind == ScheduleKind.FIXED_TIMES) times else emptyList(),
                    intervalHours = interval,
                    firstDoseAt = firstDose,
                    daysMask = if (kind == ScheduleKind.FIXED_TIMES) daysMask else Medication.ALL_DAYS_MASK,
                    startEpochDay = startDate.toEpochDay(),
                    endEpochDay = endDate?.toEpochDay(),
                    sound = soundAndVibration,
                    vibration = soundAndVibration,
                    active = medication?.active ?: true,
                )
                message = when {
                    name.isBlank() -> "Informe o nome do medicamento."
                    dosage.isBlank() -> "Informe a dosagem."
                    kind == ScheduleKind.FIXED_TIMES && times.isEmpty() -> "Adicione pelo menos um horário."
                    kind == ScheduleKind.FIXED_TIMES && daysMask == 0 -> "Selecione ao menos um dia."
                    kind == ScheduleKind.INTERVAL && first == null -> "Informe um horário inicial válido."
                    medication == null -> ScheduleValidator.newScheduleError(candidate)
                    else -> null
                }
                if (message == null) {
                    onSave(candidate)
                }
            },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 19.dp, vertical = 10.dp),
        )
    }
}

@Composable
private fun LabeledField(label: String, value: String, onValueChange: (String) -> Unit, placeholder: String) {
    Column {
        FieldLabel(label)
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder, color = Color(0xFF9A9A9A)) },
            textStyle = androidx.compose.ui.text.TextStyle(fontFamily = HoraCertaFont, color = Ink, fontSize = 23.sp),
            singleLine = true,
            modifier = Modifier.fillMaxWidth().height(63.dp),
            shape = RoundedCornerShape(12.dp),
            colors = fieldColors(),
        )
    }
}

@Composable
private fun FieldLabel(text: String) = Text(text, color = Ink, fontSize = 17.sp, modifier = Modifier.padding(bottom = 5.dp))

@Composable
private fun FrequencyOption(text: String, icon: ImageVector, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Row(modifier.fillMaxSize().background(if (selected) BlueGradient else androidx.compose.ui.graphics.Brush.linearGradient(listOf(Color.White, Color.White))).clickable(onClick = onClick), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = if (selected) Color.White else Ink)
        Spacer(Modifier.size(9.dp))
        Text(text, color = if (selected) Color.White else Ink, fontSize = 17.sp)
    }
}

@Composable
private fun IntervalPanel(interval: Int, onInterval: (Int) -> Unit, firstTime: String, onPickFirstTime: () -> Unit) {
    var intervalMenuExpanded by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth().border(1.dp, Color(0xFFD7D5D2), RoundedCornerShape(13.dp)).padding(13.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        FieldLabel("A cada")
        Box {
            SelectLikeRow("$interval horas", Icons.Rounded.Replay) { intervalMenuExpanded = true }
            DropdownMenu(
                expanded = intervalMenuExpanded,
                onDismissRequest = { intervalMenuExpanded = false },
                modifier = Modifier.width(284.dp),
            ) {
                listOf(4, 6, 8, 12, 24).forEach { hours ->
                    DropdownMenuItem(
                        text = { Text("A cada $hours horas", color = Ink, fontSize = 18.sp) },
                        onClick = {
                            onInterval(hours)
                            intervalMenuExpanded = false
                        },
                    )
                }
            }
        }
        FieldLabel("Primeira dose")
        OutlinedTextField(
            value = firstTime,
            onValueChange = { },
            leadingIcon = { Box(Modifier.size(34.dp).background(CobaltSoft, CircleShape), contentAlignment = Alignment.Center) { Icon(Icons.Rounded.Schedule, null, tint = Cobalt) } },
            trailingIcon = {
                IconButton(onClick = onPickFirstTime) {
                    Icon(Icons.Rounded.ExpandMore, "Selecionar horário", tint = Ink)
                }
            },
            textStyle = androidx.compose.ui.text.TextStyle(fontFamily = HoraCertaFont, color = Ink, fontSize = 22.sp, fontWeight = FontWeight.Bold),
            singleLine = true,
            readOnly = true,
            modifier = Modifier.fillMaxWidth().height(60.dp),
            shape = RoundedCornerShape(12.dp),
            colors = fieldColors(),
        )
        FieldLabel("Próximos horários")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val first = parseLocalTime(firstTime) ?: LocalTime.of(8, 0)
            repeat(3) { index -> TimeChip(formatTime(first.plusHours(interval.toLong() * index)), Modifier.weight(1f)) }
        }
    }
}

@Composable
private fun FixedTimesPanel(times: List<Int>, onTimes: (List<Int>) -> Unit, input: String, onInput: (String) -> Unit, mask: Int, onMask: (Int) -> Unit) {
    val parsedInput = parseMinutes(input)
    val canAddTime = parsedInput != null && parsedInput !in times

    Column(Modifier.fillMaxWidth().border(1.dp, Color(0xFFD7D5D2), RoundedCornerShape(13.dp)).padding(13.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        FieldLabel("Horários")
        times.sorted().forEach { minutes ->
            Row(Modifier.fillMaxWidth().height(48.dp).background(CobaltSoft, RoundedCornerShape(12.dp)).padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Schedule, null, tint = Cobalt)
                Spacer(Modifier.size(9.dp))
                Text(formatMinutes(minutes), color = Ink, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                IconButton(onClick = { onTimes(times - minutes) }) { Icon(Icons.Rounded.Close, "Remover", tint = Muted) }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(value = input, onValueChange = { onInput(normalizeTimeInput(it)) }, placeholder = { Text("HH:MM") }, singleLine = true, modifier = Modifier.weight(1f).height(58.dp), shape = RoundedCornerShape(12.dp), colors = fieldColors(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            Spacer(Modifier.size(10.dp))
            Box(
                Modifier
                    .size(50.dp)
                    .background(if (canAddTime) Cobalt else Color(0xFFB8C8DC), CircleShape)
                    .clickable(enabled = canAddTime) {
                        onTimes((times + requireNotNull(parsedInput)).sorted())
                        onInput("")
                    },
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.Add, "Adicionar horário", tint = Color.White)
            }
        }
        FieldLabel("Dias da semana")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            listOf("S", "T", "Q", "Q", "S", "S", "D").forEachIndexed { index, label ->
                val bit = 1 shl index
                val selected = mask and bit != 0
                Box(Modifier.size(38.dp).background(if (selected) Cobalt else Color.White, CircleShape).border(1.dp, if (selected) Cobalt else Color(0xFFCFCFCF), CircleShape).clickable { onMask(if (selected) mask and bit.inv() else mask or bit) }, contentAlignment = Alignment.Center) { Text(label, color = if (selected) Color.White else Ink, fontWeight = FontWeight.Bold) }
            }
        }
    }
}

@Composable
private fun SelectLikeRow(value: String, icon: ImageVector, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().height(58.dp).border(1.dp, Color(0xFFD3D0CD), RoundedCornerShape(12.dp)).clickable(onClick = onClick).padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = Cobalt)
        Spacer(Modifier.size(10.dp))
        Text(value, color = Ink, fontSize = 21.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        Icon(Icons.Rounded.ExpandMore, null, tint = Ink)
    }
}

@Composable
private fun TimeChip(text: String, modifier: Modifier) {
    Row(modifier.height(44.dp).background(CobaltSoft, RoundedCornerShape(18.dp)), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(28.dp).background(Cobalt, CircleShape), contentAlignment = Alignment.Center) { Icon(Icons.Rounded.Schedule, null, tint = Color.White, modifier = Modifier.size(17.dp)) }
        Spacer(Modifier.size(7.dp))
        Text(text, color = Ink, fontSize = 18.sp)
    }
}

@Composable
private fun DatePanel(start: LocalDate, end: LocalDate?, onStartClick: () -> Unit, onEndClick: () -> Unit) {
    val formatter = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.forLanguageTag("pt-BR"))
    Column(Modifier.fillMaxWidth().border(1.dp, Color(0xFFD7D5D2), RoundedCornerShape(13.dp)).padding(horizontal = 13.dp)) {
        DateRow("Data de início", start.format(formatter), Cobalt, onStartClick)
        Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFE2DFDC)))
        DateRow("Data de término", end?.format(formatter) ?: "Sem data", Muted, onEndClick)
    }
}

@Composable
private fun DateRow(label: String, value: String, tint: Color, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().height(72.dp).clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(42.dp).background(tint.copy(alpha = .12f), CircleShape), contentAlignment = Alignment.Center) { Icon(Icons.Rounded.CalendarMonth, null, tint = tint) }
        Spacer(Modifier.size(14.dp))
        Column(Modifier.weight(1f)) { Text(label, color = Ink, fontSize = 17.sp); Text(value, color = Ink, fontSize = 20.sp, fontWeight = FontWeight.Bold) }
        Icon(Icons.Rounded.ChevronRight, null, tint = Ink)
    }
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Cobalt,
    unfocusedBorderColor = Color(0xFFCECBC8),
    focusedContainerColor = Color.White,
    unfocusedContainerColor = Color.White,
    cursorColor = Cobalt,
)

private fun parseMinutes(text: String): Int? = parseLocalTime(text)?.let { it.hour * 60 + it.minute }
private fun parseLocalTime(text: String): LocalTime? {
    val parts = text.split(':')
    if (parts.size != 2) return null
    return runCatching { LocalTime.of(parts[0].toInt(), parts[1].toInt()) }.getOrNull()
}
private fun normalizeTimeInput(value: String): String { val digits = value.filter(Char::isDigit).take(4); return if (digits.length <= 2) digits else digits.take(2) + ":" + digits.drop(2) }
private fun formatMinutes(minutes: Int): String = "%02d:%02d".format(minutes / 60, minutes % 60)
private fun formatTime(time: LocalTime): String = "%02d:%02d".format(time.hour, time.minute)

private fun showTimePicker(context: Context, initial: LocalTime, onSelected: (LocalTime) -> Unit) {
    TimePickerDialog(
        context,
        { _, hour, minute -> onSelected(LocalTime.of(hour, minute)) },
        initial.hour,
        initial.minute,
        true,
    ).show()
}

private fun showDatePicker(
    context: Context,
    initial: LocalDate,
    minimum: LocalDate,
    maximum: LocalDate? = null,
    allowClear: Boolean = false,
    onClear: () -> Unit = {},
    onSelected: (LocalDate) -> Unit,
) {
    val zone = ZoneId.systemDefault()
    val dialog = DatePickerDialog(
        context,
        { _, year, month, day -> onSelected(LocalDate.of(year, month + 1, day)) },
        initial.year,
        initial.monthValue - 1,
        initial.dayOfMonth,
    )
    dialog.datePicker.minDate = minimum.atStartOfDay(zone).toInstant().toEpochMilli()
    maximum?.let { dialog.datePicker.maxDate = it.atStartOfDay(zone).toInstant().toEpochMilli() }
    if (allowClear) {
        dialog.setButton(DialogInterface.BUTTON_NEUTRAL, "Sem data") { _, _ -> onClear() }
    }
    dialog.show()
}
