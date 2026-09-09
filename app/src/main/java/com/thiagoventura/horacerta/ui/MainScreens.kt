package com.thiagoventura.horacerta.ui

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.Medication
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Undo
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thiagoventura.horacerta.R
import com.thiagoventura.horacerta.alarm.AlarmPayload
import com.thiagoventura.horacerta.alarm.putPayload
import com.thiagoventura.horacerta.data.DayProgress
import com.thiagoventura.horacerta.data.DayProgressStatus
import com.thiagoventura.horacerta.data.DoseStatus
import com.thiagoventura.horacerta.data.DoseWithMedication
import com.thiagoventura.horacerta.data.Medication
import com.thiagoventura.horacerta.data.ScheduleKind
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.max

private val PtBr = Locale.forLanguageTag("pt-BR")
private val LongDate = DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM", PtBr)
private val HistoryDate = DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM 'de' yyyy", PtBr)
private val TimeFormat = DateTimeFormatter.ofPattern("HH:mm", PtBr)

@Composable
fun MainRoot(controller: MainController, userName: String) {
    if (controller.editorVisible) {
        MedicationEditorScreen(controller.editorMedication, controller::closeEditor, controller::saveMedication)
        return
    }
    if (controller.historyVisible) {
        HistoryScreen(controller)
        return
    }
    Scaffold(
        containerColor = Ivory,
        bottomBar = { HoraBottomBar(controller.selectedTab) { controller.selectedTab = it; controller.refresh() } },
    ) { innerPadding ->
        Box(Modifier.fillMaxSize().padding(innerPadding)) {
            when (controller.selectedTab) {
                MainTab.TODAY -> TodayScreen(controller, userName)
                MainTab.CALENDAR -> CalendarScreen(controller)
                MainTab.MEDICATIONS -> MedicationsScreen(controller)
                MainTab.PROGRESS -> ProgressScreen(controller)
            }
        }
    }
}

@Composable
private fun HoraBottomBar(selected: MainTab, onSelected: (MainTab) -> Unit) {
    val tabs = listOf(
        Triple(MainTab.TODAY, "Hoje", Icons.Rounded.Home),
        Triple(MainTab.CALENDAR, "Calendário", Icons.Rounded.CalendarMonth),
        Triple(MainTab.MEDICATIONS, "Remédios", Icons.Rounded.Medication),
        Triple(MainTab.PROGRESS, "Progresso", Icons.Rounded.BarChart),
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(10.dp, RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
            .background(Color.White, RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
            .navigationBarsPadding()
            .height(88.dp)
            .padding(horizontal = 9.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceAround,
    ) {
        tabs.forEach { (tab, label, icon) ->
            val active = tab == selected
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(22.dp))
                    .clickable { onSelected(tab) },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .height(42.dp)
                        .width(72.dp)
                        .background(if (active) CobaltSoft else Color.Transparent, RoundedCornerShape(22.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(icon, label, tint = if (active) Cobalt else Color(0xFF303B4A), modifier = Modifier.size(28.dp))
                }
                Text(label, color = if (active) Cobalt else Color(0xFF303B4A), fontSize = 14.sp, fontWeight = if (active) FontWeight.Bold else FontWeight.Normal)
            }
        }
    }
}

@Composable
private fun TodayScreen(controller: MainController, userName: String) {
    val doses = controller.doses
    val now = System.currentTimeMillis()
    val context = LocalContext.current
    val next = doses.firstOrNull { it.occurrence.status == DoseStatus.PENDING && it.occurrence.scheduledAt >= now }
        ?: doses.firstOrNull { it.occurrence.status == DoseStatus.PENDING }
    val greeting = when (java.time.LocalTime.now().hour) {
        in 5..11 -> "Bom dia"
        in 12..17 -> "Boa tarde"
        else -> "Boa noite"
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, top = 4.dp, end = 18.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(17.dp),
    ) {
        item {
            HoraCertaBrand()
            Spacer(Modifier.height(22.dp))
            Text("$greeting, ${userName.ifBlank { "você" }}", color = Ink, fontSize = 32.sp, fontWeight = FontWeight.Bold)
            Text(controller.selectedDate.format(LongDate).replaceFirstChar { it.uppercase(PtBr) }, color = Muted, fontSize = 20.sp)
        }
        if (next != null) item {
            HeroDoseCard(next, now) {
                context.startActivity(
                    Intent(context, ConfirmDoseActivity::class.java).apply {
                        putPayload(next.toAlarmPayload())
                    },
                )
            }
        }
        item { DaySummary(doses) }
        if (doses.isEmpty()) {
            item { EmptyMedicationState { controller.selectedTab = MainTab.MEDICATIONS } }
        } else {
            item { TimelineCard(doses, controller::toggleDose) }
        }
    }
}

@Composable
private fun HeroDoseCard(dose: DoseWithMedication, now: Long, onDetails: () -> Unit) {
    val time = doseTime(dose)
    val minutes = max(0, ((dose.occurrence.scheduledAt - now) / 60_000L).toInt())
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(235.dp)
            .shadow(9.dp, RoundedCornerShape(22.dp))
            .background(BlueGradient, RoundedCornerShape(22.dp))
            .padding(23.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text("Próxima dose", color = Color.White.copy(alpha = .9f), fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text(dose.medication.name, color = Color.White, fontSize = 29.sp, fontWeight = FontWeight.Bold, maxLines = 2)
            Box(Modifier.padding(top = 12.dp).width(136.dp).height(2.dp).background(Color.White.copy(alpha = .38f)))
            Text(time, color = Color.White, fontSize = 49.sp, fontWeight = FontWeight.Bold, lineHeight = 55.sp)
            Text(if (minutes == 0) "agora" else "em $minutes min", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            AnimatedClock(Modifier.size(104.dp))
            Spacer(Modifier.height(18.dp))
            Box(
                Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFFEAF3FF), RoundedCornerShape(16.dp))
                    .clickable(onClick = onDetails)
                    .padding(horizontal = 17.dp, vertical = 13.dp),
                contentAlignment = Alignment.Center,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Ver detalhes", color = Ink, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    Icon(Icons.Rounded.ChevronRight, null, tint = Ink)
                }
            }
        }
    }
}

@Composable
private fun AnimatedClock(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "clock-pulse")
    val sweep by transition.animateFloat(250f, 335f, infiniteRepeatable(tween(1800), RepeatMode.Reverse), label = "clock-sweep")
    Canvas(modifier) {
        drawCircle(Color(0xFFDDEEFF))
        drawCircle(Cobalt, style = Stroke(width = size.minDimension * .055f))
        drawArc(Color.White, -90f, sweep, false, style = Stroke(size.minDimension * .065f, cap = StrokeCap.Round))
        drawCircle(Color.White, radius = size.minDimension * .32f)
        drawCircle(Cobalt, radius = size.minDimension * .18f, style = Stroke(size.minDimension * .035f))
        val center = Offset(size.width / 2, size.height / 2)
        drawLine(Cobalt, center, Offset(center.x, center.y - size.height * .12f), size.minDimension * .03f, StrokeCap.Round)
        drawLine(Cobalt, center, Offset(center.x + size.width * .1f, center.y + size.height * .07f), size.minDimension * .03f, StrokeCap.Round)
    }
}

@Composable
private fun DaySummary(doses: List<DoseWithMedication>) {
    val taken = doses.count { it.occurrence.status == DoseStatus.TAKEN }
    Row(
        modifier = Modifier.fillMaxWidth().height(78.dp).shadow(7.dp, RoundedCornerShape(19.dp)).background(Color.White, RoundedCornerShape(19.dp)).padding(horizontal = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(48.dp).background(CobaltSoft, CircleShape), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.Check, null, tint = Cobalt, modifier = Modifier.size(30.dp))
        }
        Spacer(Modifier.size(14.dp))
        Column(Modifier.weight(1f)) {
            Text("Seu dia", color = Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text("$taken de ${doses.size} doses tomadas", color = Muted, fontSize = 16.sp)
        }
        Row {
            repeat(doses.size.coerceAtMost(5)) { index ->
                Box(Modifier.padding(horizontal = 5.dp).size(12.dp).background(if (index < taken) Cobalt else Color(0xFFE0E0E0), CircleShape))
            }
        }
    }
}

@Composable
private fun TimelineCard(doses: List<DoseWithMedication>, onToggle: (DoseWithMedication) -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().shadow(3.dp, RoundedCornerShape(20.dp)).background(Color.White, RoundedCornerShape(20.dp)).border(1.dp, Color(0xFFE3E0DD), RoundedCornerShape(20.dp)).padding(horizontal = 11.dp),
    ) {
        doses.forEachIndexed { index, dose ->
            TimelineRow(dose, index, doses.lastIndex, onToggle)
            if (index < doses.lastIndex) Box(Modifier.fillMaxWidth().padding(start = 92.dp).height(1.dp).background(Color(0xFFE7E4E1)))
        }
    }
}

@Composable
private fun TimelineRow(dose: DoseWithMedication, index: Int, lastIndex: Int, onToggle: (DoseWithMedication) -> Unit) {
    val taken = dose.occurrence.status == DoseStatus.TAKEN
    val now = System.currentTimeMillis()
    val isNow = !taken && kotlin.math.abs(dose.occurrence.scheduledAt - now) < 30 * 60_000L
    Row(
        modifier = Modifier.fillMaxWidth().height(77.dp).clickable { onToggle(dose) },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(doseTime(dose), color = Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(66.dp), textAlign = TextAlign.Center)
        Box(Modifier.width(42.dp).height(77.dp), contentAlignment = Alignment.Center) {
            if (index > 0) Box(Modifier.align(Alignment.TopCenter).width(2.dp).height(38.dp).background(if (taken) Color(0xFF77AFFF) else Color(0xFFD3D3D3)))
            if (index < lastIndex) Box(Modifier.align(Alignment.BottomCenter).width(2.dp).height(38.dp).background(if (taken) Color(0xFF77AFFF) else Color(0xFFD3D3D3)))
            Box(
                Modifier.size(25.dp).background(if (taken) Cobalt else Color.White, CircleShape).border(2.dp, if (taken || isNow) Cobalt else Color(0xFFC8C8C8), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                if (taken) Icon(Icons.Rounded.Check, null, tint = Color.White, modifier = Modifier.size(18.dp))
                else if (isNow) Box(Modifier.size(15.dp).background(Cobalt, CircleShape))
            }
        }
        Spacer(Modifier.size(8.dp))
        Column(Modifier.weight(1f)) {
            Text(dose.medication.name, color = Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            val confirmation = if (taken) "Tomado${dose.occurrence.takenAt?.let { " às ${formatTime(it)}" }.orEmpty()}" else null
            val stock = dose.medication.takeIf(Medication::inventoryEnabled)?.let {
                formatStockQuantity(it.stockQuantity, it.stockUnit)
            }
            if (confirmation != null || stock != null) {
                Text(
                    listOfNotNull(confirmation, stock).joinToString(" • "),
                    color = if (!taken && dose.medication.stockQuantity <= dose.medication.lowStockThreshold) Warning else Cobalt,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        StatusPill(if (taken) "Tomado" else if (isNow) "Agora" else "Pendente", if (taken || isNow) CobaltSoft else Color(0xFFE7E7E7))
    }
}

@Composable
private fun CalendarScreen(controller: MainController) {
    val month = controller.visibleMonth
    val monthDoses = remember(month, controller.dataRevision) { controller.monthDoses() }
    val dosesByDate = monthDoses.groupBy { Instant.ofEpochMilli(it.occurrence.scheduledAt).atZone(ZoneId.systemDefault()).toLocalDate() }
    val first = month.atDay(1)
    val firstCell = first.minusDays((first.dayOfWeek.value % 7).toLong())
    val cells = (0 until 42).map { firstCell.plusDays(it.toLong()) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, top = 4.dp, end = 18.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { HoraCertaBrand("Calendário", titleSize = 32) }
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                MonthArrow(Icons.Rounded.ChevronLeft) { controller.setMonth(month.minusMonths(1)) }
                Text("${month.month.getDisplayName(TextStyle.FULL, PtBr).replaceFirstChar { it.uppercase(PtBr) }} ${month.year}", color = Ink, fontSize = 21.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                MonthArrow(Icons.Rounded.ChevronRight) { controller.setMonth(month.plusMonths(1)) }
            }
        }
        item {
            Column {
                Row(Modifier.fillMaxWidth()) { listOf("D", "S", "T", "Q", "Q", "S", "S").forEach { Text(it, Modifier.weight(1f), color = Ink, textAlign = TextAlign.Center, fontWeight = FontWeight.Bold) } }
                cells.chunked(7).forEach { week ->
                    Row(Modifier.fillMaxWidth()) {
                        week.forEach { date ->
                            val doses = dosesByDate[date].orEmpty()
                            val allTaken = doses.isNotEmpty() && doses.all { it.occurrence.status == DoseStatus.TAKEN }
                            CalendarCell(date, date.month == month.month, date == controller.selectedDate, doses.isNotEmpty(), allTaken, { controller.selectDate(date) }, Modifier.weight(1f))
                        }
                    }
                }
            }
        }
        item {
            Column(Modifier.fillMaxWidth().background(BlueGradient, RoundedCornerShape(13.dp)).padding(18.dp)) {
                Text("${controller.selectedDate.dayOfMonth}   ${controller.selectedDate.format(LongDate).replaceFirstChar { it.uppercase(PtBr) }}", color = Color.White, fontSize = 17.sp)
                Text("${controller.doses.size} doses", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            }
        }
        if (controller.doses.isEmpty()) item { Text("Nenhuma dose neste dia.", color = Muted, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(30.dp)) }
        else items(controller.doses, key = { it.occurrence.id }) { CalendarDoseRow(it) { controller.toggleDose(it) } }
    }
}

@Composable
private fun MonthArrow(icon: ImageVector, onClick: () -> Unit) {
    Box(Modifier.size(38.dp).background(CobaltSoft, RoundedCornerShape(11.dp)).clickable(onClick = onClick), contentAlignment = Alignment.Center) { Icon(icon, null, tint = Cobalt) }
}

@Composable
private fun CalendarCell(date: LocalDate, inMonth: Boolean, selected: Boolean, hasDoses: Boolean, allTaken: Boolean, onClick: () -> Unit, modifier: Modifier) {
    Column(modifier.height(48.dp).clip(CircleShape).background(if (selected) Cobalt else Color.Transparent).clickable(onClick = onClick), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text(date.dayOfMonth.toString(), color = if (selected) Color.White else if (inMonth) Ink else Color(0xFF9D9D9D), fontWeight = FontWeight.Bold)
        if (hasDoses) Box(Modifier.size(6.dp).background(if (selected) Color(0xFF8AC3FF) else if (allTaken) Cobalt else Danger, CircleShape))
    }
}

@Composable
private fun CalendarDoseRow(dose: DoseWithMedication, onToggle: () -> Unit) {
    Row(Modifier.fillMaxWidth().height(68.dp).shadow(5.dp, RoundedCornerShape(12.dp)).background(Color.White, RoundedCornerShape(12.dp)).clickable(onClick = onToggle).padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(doseTime(dose), color = Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(62.dp))
        Box(Modifier.size(29.dp).background(if (dose.occurrence.status == DoseStatus.TAKEN) Cobalt else Color.White, CircleShape).border(2.dp, if (dose.occurrence.status == DoseStatus.TAKEN) Cobalt else Color(0xFFC8C8C8), CircleShape), contentAlignment = Alignment.Center) {
            if (dose.occurrence.status == DoseStatus.TAKEN) Icon(Icons.Rounded.Check, null, tint = Color.White, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.size(14.dp))
        Text(dose.medication.name, color = Ink, fontSize = 17.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), maxLines = 1)
        StatusPill(if (dose.occurrence.status == DoseStatus.TAKEN) "Tomado" else "Pendente", if (dose.occurrence.status == DoseStatus.TAKEN) CobaltSoft else Color(0xFFE7E7E7))
    }
}

@Composable
private fun MedicationsScreen(controller: MainController) {
    var query by remember { mutableStateOf("") }
    var pendingDelete by remember { mutableStateOf<Medication?>(null) }
    val filtered = controller.medications.filter { it.name.contains(query, true) }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, top = 4.dp, end = 20.dp, bottom = 18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                HoraCertaBrand("Medicamentos", titleSize = 30, modifier = Modifier.weight(1f))
                Row(Modifier.background(CobaltSoft, RoundedCornerShape(12.dp)).clickable { controller.openEditor() }.padding(horizontal = 13.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Add, null, tint = Cobalt)
                    Spacer(Modifier.size(6.dp))
                    Text("Adicionar", color = Cobalt, fontWeight = FontWeight.Bold)
                }
            }
            Text("${controller.medications.count { it.active }} ativos", color = Muted, fontSize = 18.sp)
        }
        item {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Buscar medicamento", color = Muted, fontSize = 19.sp) },
                leadingIcon = { Icon(Icons.Rounded.Search, null, tint = Color(0xFF263344), modifier = Modifier.size(29.dp)) },
                modifier = Modifier.fillMaxWidth().height(62.dp),
                shape = RoundedCornerShape(15.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(unfocusedContainerColor = CobaltSoft.copy(alpha = .7f), focusedContainerColor = CobaltSoft.copy(alpha = .7f), unfocusedBorderColor = Color(0xFFC9D5E4), focusedBorderColor = Cobalt),
            )
        }
        if (filtered.isEmpty()) item { EmptyMedicationState { controller.openEditor() } }
        else items(filtered, key = Medication::id) { medication ->
            MedicationPanel(
                medication,
                onEdit = { controller.openEditor(medication) },
                onDelete = { pendingDelete = medication },
                onToggle = { controller.toggleMedicationActive(medication) },
                onRestock = { controller.openInventory(medication) },
            )
        }
    }
    pendingDelete?.let { medication ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Excluir ${medication.name}?") },
            text = { Text("Os alarmes e o histórico desse medicamento serão removidos.") },
            confirmButton = { TextButton(onClick = { controller.deleteMedication(medication); pendingDelete = null }) { Text("Excluir", color = Danger) } },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("Cancelar") } },
        )
    }
    controller.inventoryMedication?.let { medication ->
        InventoryRestockSheet(
            medication = medication,
            onDismiss = controller::closeInventory,
            onAdd = { controller.addStock(medication, it) },
        )
    }
}

@Composable
private fun MedicationPanel(
    medication: Medication,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggle: () -> Unit,
    onRestock: () -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val schedule = when (medication.scheduleKind) {
        ScheduleKind.FIXED_TIMES -> medication.timesMinutes.sorted().joinToString(" e ") { "%02d:%02d".format(it / 60, it % 60) }
        ScheduleKind.INTERVAL -> "A cada ${medication.intervalHours} horas"
    }
    Row(
        Modifier
            .fillMaxWidth()
            .animateContentSize()
            .heightIn(min = if (medication.inventoryEnabled) 202.dp else 166.dp)
            .shadow(6.dp, RoundedCornerShape(18.dp))
            .background(Color.White, RoundedCornerShape(18.dp))
            .clickable(onClick = onEdit)
            .padding(15.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(62.dp).background(CobaltSoft, RoundedCornerShape(15.dp)), contentAlignment = Alignment.Center) { PillMark(Modifier.size(width = 28.dp, height = 42.dp)) }
        Spacer(Modifier.size(18.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(medication.name, color = Ink, fontSize = 25.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                StatusPill(if (medication.active) "Ativo" else "Pausado", CobaltSoft)
                Box {
                    IconButton(onClick = { menuExpanded = true }, modifier = Modifier.size(32.dp)) { Icon(Icons.Rounded.MoreVert, "Opções", tint = Color(0xFF303B4A)) }
                    DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                        DropdownMenuItem(
                            text = { Text("Editar") },
                            leadingIcon = { Icon(Icons.Rounded.Edit, null) },
                            onClick = { menuExpanded = false; onEdit() },
                        )
                        DropdownMenuItem(
                            text = { Text("Excluir", color = Danger) },
                            leadingIcon = { Icon(Icons.Rounded.DeleteOutline, null, tint = Danger) },
                            onClick = { menuExpanded = false; onDelete() },
                        )
                    }
                }
            }
            Text(medication.dosage, color = Muted, fontSize = 20.sp)
            Text(if (medication.scheduleKind == ScheduleKind.FIXED_TIMES) "Todos os dias • $schedule" else schedule, color = Cobalt, fontSize = 16.sp, maxLines = 1)
            if (medication.inventoryEnabled) {
                InventoryStatusRow(medication, onRestock)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Row(
                    Modifier
                        .height(48.dp)
                        .border(1.5.dp, Cobalt, RoundedCornerShape(14.dp))
                        .clickable(onClick = onToggle)
                        .padding(horizontal = 13.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(if (medication.active) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, null, tint = Cobalt, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.size(6.dp))
                    Text(if (medication.active) "Pausar" else "Ativar", color = Cobalt, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun InventoryStatusRow(medication: Medication, onRestock: () -> Unit) {
    val statusColor = when {
        medication.stockQuantity == 0 -> Danger
        medication.stockQuantity <= medication.lowStockThreshold -> Warning
        else -> Success
    }
    Row(
        Modifier.fillMaxWidth().heightIn(min = 48.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.Inventory2, null, tint = statusColor, modifier = Modifier.size(20.dp))
        Spacer(Modifier.size(7.dp))
        Text(
            inventoryStatusText(medication),
            color = statusColor,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Row(
            Modifier
                .height(48.dp)
                .clip(RoundedCornerShape(13.dp))
                .clickable(onClick = onRestock)
                .padding(horizontal = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.Add, null, tint = Cobalt, modifier = Modifier.size(20.dp))
            Spacer(Modifier.size(4.dp))
            Text("Repor", color = Cobalt, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InventoryRestockSheet(
    medication: Medication,
    onDismiss: () -> Unit,
    onAdd: (Int) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var quantity by remember(medication.id) { mutableStateOf("") }
    val amount = quantity.toIntOrNull()?.takeIf { it > 0 }
    val afterRestock = medication.stockQuantity + (amount ?: 0)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Ivory,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(start = 22.dp, end = 22.dp, bottom = 24.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(48.dp).background(CobaltSoft, RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Inventory2, null, tint = Cobalt, modifier = Modifier.size(27.dp))
                }
                Spacer(Modifier.size(13.dp))
                Column {
                    Text("Repor estoque", color = Ink, fontSize = 27.sp, fontWeight = FontWeight.Bold)
                    Text(medication.name, color = Muted, fontSize = 17.sp)
                }
            }
            Spacer(Modifier.height(22.dp))
            Text(
                "Agora: ${formatStockQuantity(medication.stockQuantity, medication.stockUnit)}",
                color = Ink,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = quantity,
                onValueChange = { quantity = it.filter(Char::isDigit).take(6) },
                label = { Text("Quantidade a adicionar") },
                placeholder = { Text("Ex.: 30") },
                suffix = { Text(medication.stockUnit, color = Muted) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth().height(66.dp),
                shape = RoundedCornerShape(13.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Cobalt,
                    unfocusedBorderColor = Color(0xFFCECBC8),
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                ),
            )
            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                listOf(10, 20, 30).forEach { quickAmount ->
                    Box(
                        Modifier
                            .weight(1f)
                            .height(48.dp)
                            .border(1.dp, Cobalt, RoundedCornerShape(13.dp))
                            .clickable { quantity = quickAmount.toString() },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("+$quickAmount", color = Cobalt, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(Modifier.height(18.dp))
            Text(
                "Depois: ${formatStockQuantity(afterRestock, medication.stockUnit)}",
                color = if (amount == null) Muted else Success,
                fontSize = 17.sp,
            )
            Spacer(Modifier.height(18.dp))
            GradientPrimaryButton(
                text = "Adicionar ao estoque",
                enabled = amount != null,
                onClick = { amount?.let(onAdd) },
                modifier = Modifier.fillMaxWidth(),
                leading = { Icon(Icons.Rounded.Add, null, tint = Color.White, modifier = Modifier.size(23.dp)) },
            )
        }
    }
}

@Composable
private fun ProgressScreen(controller: MainController) {
    val progress = controller.progress
    val flameTransition = rememberInfiniteTransition(label = "streak-flame")
    val flamePulse by flameTransition.animateFloat(
        initialValue = .98f,
        targetValue = 1.025f,
        animationSpec = infiniteRepeatable(tween(1100), RepeatMode.Reverse),
        label = "streak-flame-pulse",
    )
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, top = 4.dp, end = 18.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(15.dp),
    ) {
        item { HoraCertaBrand("Progresso", titleSize = 32) }
        item {
            Box(Modifier.fillMaxWidth().height(245.dp).shadow(7.dp, RoundedCornerShape(22.dp)).background(BlueGradient, RoundedCornerShape(22.dp)).clip(RoundedCornerShape(22.dp))) {
                CroppedMockupAsset(
                    R.drawable.mockup_progress,
                    505, 205, 285, 480,
                    Modifier
                        .align(Alignment.CenterEnd)
                        .width(152.dp)
                        .height(228.dp)
                        .graphicsLayer {
                            scaleX = flamePulse
                            scaleY = flamePulse
                            translationY = (1f - flamePulse) * 75f
                        },
                )
                Column(Modifier.padding(27.dp).fillMaxSize()) {
                    Text("${progress.currentStreak} dias\nseguidos", color = Color.White, fontSize = 43.sp, lineHeight = 42.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(9.dp))
                    Text("Sem perder nenhuma dose", color = Color.White, fontSize = 18.sp)
                    Spacer(Modifier.height(16.dp))
                    Row(Modifier.background(Color(0xFFEAF3FF), RoundedCornerShape(18.dp)).padding(horizontal = 12.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Star, null, tint = Cobalt, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(7.dp))
                        Text("Melhor sequência: ${progress.bestStreak} dias", color = Cobalt, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        item { AdherencePanel(progress.adherencePercent) }
        item { LastSevenDays(progress.lastSevenDays) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricBox(progress.takenCount.toString(), "confirmadas", CobaltSoft, Cobalt, Modifier.weight(1f))
                MetricBox(progress.pendingCount.toString(), "pendentes", Color(0xFFFFF1D9), Warning, Modifier.weight(1f))
                MetricBox(progress.missedCount.toString(), "perdidas", Color(0xFFFFE3E1), Danger, Modifier.weight(1f))
            }
        }
        item {
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .shadow(4.dp, RoundedCornerShape(17.dp))
                    .clip(RoundedCornerShape(17.dp))
                    .background(Color.White, RoundedCornerShape(17.dp))
                    .clickable(onClick = controller::openHistory)
                    .padding(horizontal = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.BarChart, null, tint = Cobalt)
                Spacer(Modifier.size(14.dp))
                Text("Ver histórico completo", color = Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Icon(Icons.Rounded.ChevronRight, null, tint = Ink)
            }
        }
    }
}

@Composable
private fun HistoryScreen(controller: MainController) {
    BackHandler(onBack = controller::closeHistory)
    val history = controller.historyDoses
    val grouped = remember(controller.dataRevision, history) {
        history.groupBy {
            Instant.ofEpochMilli(it.occurrence.scheduledAt)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
        }
    }
    val taken = history.count { it.occurrence.status == DoseStatus.TAKEN }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Ivory)
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentPadding = PaddingValues(start = 18.dp, top = 8.dp, end = 18.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth().height(68.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(CobaltSoft)
                        .clickable(onClick = controller::closeHistory),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Voltar", tint = Cobalt, modifier = Modifier.size(28.dp))
                }
                Spacer(Modifier.size(14.dp))
                Column {
                    Text("Histórico completo", color = Ink, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                    Text("Todas as doses registradas", color = Muted, fontSize = 15.sp)
                }
            }
        }
        item {
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(92.dp)
                    .shadow(5.dp, RoundedCornerShape(18.dp))
                    .background(Color.White, RoundedCornerShape(18.dp))
                    .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                HistorySummaryValue(taken.toString(), "confirmadas", Cobalt, Modifier.weight(1f))
                Box(Modifier.width(1.dp).height(48.dp).background(Color(0xFFE2E2E2)))
                HistorySummaryValue((history.size - taken).toString(), "pendentes", Warning, Modifier.weight(1f))
                Box(Modifier.width(1.dp).height(48.dp).background(Color(0xFFE2E2E2)))
                HistorySummaryValue(history.size.toString(), "total", Ink, Modifier.weight(1f))
            }
        }

        if (history.isEmpty()) {
            item {
                Column(
                    Modifier.fillMaxWidth().padding(vertical = 72.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(Icons.Rounded.BarChart, null, tint = Cobalt, modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(12.dp))
                    Text("Nenhuma dose no histórico", color = Ink, fontSize = 21.sp, fontWeight = FontWeight.Bold)
                    Text("As doses aparecerão aqui após o horário programado.", color = Muted, textAlign = TextAlign.Center)
                }
            }
        } else {
            grouped.forEach { (date, doses) ->
                item(key = "history-date-$date") {
                    Text(
                        date.format(HistoryDate).replaceFirstChar { it.uppercase(PtBr) },
                        color = Ink,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp, start = 4.dp),
                    )
                }
                items(doses, key = { "history-dose-${it.occurrence.id}" }) { dose ->
                    HistoryDoseRow(dose) { controller.toggleDose(dose) }
                }
            }
        }
    }
}

@Composable
private fun HistorySummaryValue(value: String, label: String, color: Color, modifier: Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = color, fontSize = 27.sp, fontWeight = FontWeight.Bold)
        Text(label, color = Muted, fontSize = 13.sp)
    }
}

@Composable
private fun HistoryDoseRow(dose: DoseWithMedication, onToggle: () -> Unit) {
    val taken = dose.occurrence.status == DoseStatus.TAKEN
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(88.dp)
            .shadow(3.dp, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .clickable(onClick = onToggle)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(doseTime(dose), color = Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(62.dp))
        Box(
            Modifier
                .size(30.dp)
                .background(if (taken) Cobalt else Color.White, CircleShape)
                .border(2.dp, if (taken) Cobalt else Warning, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (taken) Icon(Icons.Rounded.Check, null, tint = Color.White, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.size(13.dp))
        Column(Modifier.weight(1f)) {
            Text(dose.medication.name, color = Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                if (taken) "Confirmada${dose.occurrence.takenAt?.let { " às ${formatTime(it)}" }.orEmpty()}" else "Não confirmada • ${dose.medication.dosage}",
                color = if (taken) Cobalt else Muted,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        StatusPill(if (taken) "Tomado" else "Pendente", if (taken) CobaltSoft else Color(0xFFFFF1D9))
    }
}

@Composable
private fun AdherencePanel(percent: Int) {
    Row(Modifier.fillMaxWidth().height(130.dp).shadow(5.dp, RoundedCornerShape(20.dp)).background(Color.White, RoundedCornerShape(20.dp)).padding(24.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text("Adesão neste período", color = Ink, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text("$percent%", color = Ink, fontSize = 48.sp, fontWeight = FontWeight.Bold)
        }
        Canvas(Modifier.size(90.dp)) {
            drawArc(Color(0xFFE8E8E8), -90f, 360f, false, style = Stroke(13.dp.toPx(), cap = StrokeCap.Round))
            drawArc(Cobalt, -90f, 360f * percent / 100f, false, style = Stroke(13.dp.toPx(), cap = StrokeCap.Round))
        }
        Text("%", color = Ink, fontSize = 28.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 8.dp))
    }
}

@Composable
private fun LastSevenDays(days: List<DayProgress>) {
    Column(Modifier.fillMaxWidth().height(130.dp).shadow(5.dp, RoundedCornerShape(20.dp)).background(Color.White, RoundedCornerShape(20.dp)).padding(22.dp)) {
        Text("Últimos 7 dias", color = Ink, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(14.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            days.forEach { day ->
                val label = day.date.dayOfWeek.getDisplayName(TextStyle.SHORT, PtBr).take(1).uppercase(PtBr)
                val color = when (day.status) {
                    DayProgressStatus.COMPLETE -> Cobalt
                    DayProgressStatus.PENDING -> Warning
                    DayProgressStatus.MISSED -> Danger
                    DayProgressStatus.NO_DOSES -> Color(0xFFE4E4E4)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(label, color = Ink, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Box(Modifier.size(31.dp).background(color, CircleShape), contentAlignment = Alignment.Center) {
                        when (day.status) {
                            DayProgressStatus.COMPLETE -> Icon(Icons.Rounded.Check, null, tint = Color.White, modifier = Modifier.size(20.dp))
                            DayProgressStatus.MISSED -> Icon(Icons.Rounded.Close, null, tint = Color.White, modifier = Modifier.size(19.dp))
                            DayProgressStatus.PENDING -> Box(Modifier.size(9.dp).background(Color.White, CircleShape))
                            DayProgressStatus.NO_DOSES -> Unit
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricBox(value: String, label: String, background: Color, accent: Color, modifier: Modifier) {
    Column(modifier.height(116.dp).background(background, RoundedCornerShape(17.dp)).padding(16.dp)) {
        Text(value, color = Ink, fontSize = 35.sp, fontWeight = FontWeight.Bold)
        Text(label, color = Ink, fontSize = 14.sp)
        Spacer(Modifier.weight(1f))
        Box(Modifier.align(Alignment.End).size(28.dp).background(accent.copy(alpha = .22f), CircleShape))
    }
}

@Composable
private fun StatusPill(text: String, background: Color) {
    Text(text, color = if (text == "Pendente") Ink else Cobalt, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.background(background, RoundedCornerShape(16.dp)).padding(horizontal = 10.dp, vertical = 5.dp))
}

@Composable
private fun EmptyMedicationState(onAdd: () -> Unit) {
    Column(Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(20.dp)).padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        PillMark(Modifier.size(width = 36.dp, height = 54.dp))
        Spacer(Modifier.height(12.dp))
        Text("Nenhum medicamento cadastrado", color = Ink, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text("Adicione o primeiro para criar seus horários.", color = Muted, textAlign = TextAlign.Center)
        TextButton(onClick = onAdd) { Text("Adicionar medicamento", color = Cobalt, fontWeight = FontWeight.Bold) }
    }
}

private fun doseTime(dose: DoseWithMedication): String = formatTime(dose.occurrence.scheduledAt)
private fun formatTime(millis: Long): String = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalTime().format(TimeFormat)

private fun inventoryStatusText(medication: Medication): String = when {
    medication.stockQuantity == 0 -> "Sem ${medication.stockUnit}"
    medication.stockQuantity <= medication.lowStockThreshold ->
        "${formatStockQuantity(medication.stockQuantity, medication.stockUnit)} • estoque baixo"
    else -> "${formatStockQuantity(medication.stockQuantity, medication.stockUnit)} restantes"
}

private fun formatStockQuantity(quantity: Int, unit: String): String {
    val normalized = unit.trim().ifBlank { "unidades" }
    val displayUnit = if (quantity == 1 && normalized.endsWith("s", ignoreCase = true)) {
        normalized.dropLast(1)
    } else {
        normalized
    }
    return "$quantity $displayUnit"
}

private fun DoseWithMedication.toAlarmPayload() = AlarmPayload(
    occurrenceId = occurrence.id,
    medicationName = medication.name,
    dosage = medication.dosage,
    scheduledAt = occurrence.scheduledAt,
    sound = medication.sound,
    vibration = medication.vibration,
)
