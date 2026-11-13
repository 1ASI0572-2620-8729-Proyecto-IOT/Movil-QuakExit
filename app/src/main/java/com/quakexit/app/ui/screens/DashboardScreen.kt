package com.quakexit.app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.quakexit.app.data.model.*
import com.quakexit.app.ui.components.*
import com.quakexit.app.ui.theme.QxColors
import com.quakexit.app.util.fmt
import com.quakexit.app.viewmodel.DashboardViewModel
import com.quakexit.app.viewmodel.HomeViewModel
import com.quakexit.app.viewmodel.DevicesViewModel
import kotlinx.coroutines.delay

fun magnitudeColor(m: Double) = when { m >= 6.0 -> QxColors.Danger; m >= 4.5 -> QxColors.Warning; else -> QxColors.Info }
fun stateColor(s: String) = when (s) { "ALERT" -> QxColors.Danger; "SLEEPING" -> QxColors.Info; else -> QxColors.Success }

@Composable
fun DashboardScreen(vm: DashboardViewModel = viewModel(), homeVm: HomeViewModel = viewModel(), devicesVm: DevicesViewModel = viewModel()) {
    val status by vm.status.collectAsState()
    val quakes by vm.quakes.collectAsState()
    val event by vm.lastEvent.collectAsState()
    val loading by vm.loading.collectAsState()
    val profile by homeVm.profile.collectAsState()
    val devices by devicesVm.devices.collectAsState()
    var reading by remember { mutableStateOf("5.0") }

    LaunchedEffect(Unit) {
        homeVm.load(); devicesVm.refresh()
        while (true) { vm.refresh(); delay(60_000) }
    }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { LoadingBar(loading) }
        item { StatusCard(status) }
        item { SectionTitle("Esquema del hogar") }
        item { HomeSchema(profile, devices) }
        item { SectionTitle("Sismos recientes (USGS)") }
        item { if (quakes.isNotEmpty()) QuakeChart(quakes) else EmptyState("Cargando sismos…") }
        items(quakes) { QuakeRow(it) }
        item { SectionTitle("Gestión de emergencias") }
        item {
            QxCard(borderColor = if (event != null) QxColors.Danger else QxColors.Border) {
                Text("Procesar o simular una lectura sísmica. Si supera el umbral (${com.quakexit.app.data.mock.MockDataSource.THRESHOLD}) se genera un evento.", style = MaterialTheme.typography.bodyMedium, color = QxColors.TextMuted)
                QxField(reading, { reading = it }, "Magnitud simulada", keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal)
                PrimaryButton("Simular lectura", onClick = { reading.toDoubleOrNull()?.let(vm::submitReading) }, enabled = reading.toDoubleOrNull() != null, modifier = Modifier.fillMaxWidth(), icon = Icons.Filled.Waves)
                event?.let { e ->
                    HorizontalDivider(color = QxColors.Border)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Column {
                            Text("Evento M${e.magnitude}", style = MaterialTheme.typography.titleMedium)
                            Text(e.createdAt.fmt(), style = MaterialTheme.typography.bodySmall, color = QxColors.TextMuted)
                        }
                        StatusChip(e.status, QxColors.Danger)
                    }
                    GhostButton("Marcar como falsa alarma", onClick = { vm.falseAlarm(e.id) }, color = QxColors.Warning, modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}

@Composable
fun StatusCard(status: HomeStatus?) {
    if (status == null) { QxCard { Text("Consultando estado de la vivienda…", color = QxColors.TextMuted) }; return }
    val color = stateColor(status.state)
    val lowBattery = status.batteryPercent < 15
    QxCard(borderColor = color) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text("Estado de la vivienda", style = MaterialTheme.typography.labelMedium, color = QxColors.TextMuted)
                Text(status.state, style = MaterialTheme.typography.headlineMedium, color = color)
            }
            Icon(if (status.state == "ALERT") Icons.Filled.Warning else Icons.Filled.Home, null, tint = color, modifier = Modifier.size(36.dp))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Batería de respaldo", style = MaterialTheme.typography.titleSmall)
            Text("${status.batteryPercent}%", style = MaterialTheme.typography.titleSmall, color = if (lowBattery) QxColors.Danger else QxColors.TextPrimary)
        }
        BatteryBar(status.batteryPercent)
        if (lowBattery) Banner("Batería crítica (<15%). Conecta el equipo a la red eléctrica.", QxColors.Danger, Icons.Filled.BatteryAlert)
        if (status.powerSource == "BACKUP") {
            StatusChip("Fuente: BACKUP — corte de luz", QxColors.Warning)
            Banner("Se detectó un corte de energía. El equipo opera con batería de respaldo.", QxColors.Warning, Icons.Filled.PowerOff)
        } else StatusChip("Fuente: MAIN", QxColors.Success)
    }
}

@Composable
fun Banner(text: String, color: Color, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(
        Modifier.fillMaxWidth().border(1.dp, color, RoundedCornerShape(12.dp)).padding(10.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(icon, null, tint = color)
        Text(text, style = MaterialTheme.typography.bodySmall, color = color)
    }
}

@Composable
fun QuakeChart(quakes: List<Earthquake>) {
    val sorted = quakes.sortedBy { it.time }
    QxCard {
        Text("Magnitud por evento", style = MaterialTheme.typography.titleSmall)
        Canvas(Modifier.fillMaxWidth().height(110.dp)) {
            val max = 8f
            val slot = size.width / sorted.size
            sorted.forEachIndexed { i, q ->
                val h = size.height * (q.magnitude.toFloat() / max)
                drawRoundRect(
                    magnitudeColor(q.magnitude), Offset(i * slot + slot * 0.2f, size.height - h),
                    Size(slot * 0.6f, h), CornerRadius(8f, 8f),
                )
            }
        }
    }
}

@Composable
fun QuakeRow(q: Earthquake) {
    QxCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("M${q.magnitude}", style = MaterialTheme.typography.titleLarge, color = magnitudeColor(q.magnitude))
            Column(Modifier.weight(1f)) {
                Text(q.place, style = MaterialTheme.typography.titleSmall)
                Text("Profundidad ${q.depthKm.toInt()} km · ${q.time.fmt()}", style = MaterialTheme.typography.bodySmall, color = QxColors.TextMuted)
            }
        }
    }
}

@Composable
fun HomeSchema(profile: HomeProfile?, devices: List<Device>) {
    QxCard {
        if (profile == null || profile.floors.isEmpty()) { Text("Configura pisos y habitaciones en la pestaña Hogar.", color = QxColors.TextMuted); return@QxCard }
        profile.floors.reversed().forEach { floor ->
            Text(floor.name, style = MaterialTheme.typography.labelMedium, color = QxColors.Accent)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                floor.rooms.forEach { room ->
                    Column(
                        Modifier.weight(1f).background(QxColors.PanelAlt, RoundedCornerShape(12.dp)).border(1.dp, QxColors.Border, RoundedCornerShape(12.dp)).padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(room.name, style = MaterialTheme.typography.labelLarge)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.MeetingRoom, null, Modifier.size(14.dp), tint = QxColors.TextMuted)
                            Text(" ${room.doors}", style = MaterialTheme.typography.bodySmall, color = QxColors.TextMuted)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            room.deviceIds.forEach { id ->
                                val d = devices.firstOrNull { it.id == id }
                                val c = when { d == null -> QxColors.Border; !d.lockOk -> QxColors.Danger; d.deepSleep -> QxColors.Info; else -> QxColors.Success }
                                Box(Modifier.size(10.dp).background(c, androidx.compose.foundation.shape.CircleShape))
                            }
                        }
                    }
                }
            }
        }
    }
}

