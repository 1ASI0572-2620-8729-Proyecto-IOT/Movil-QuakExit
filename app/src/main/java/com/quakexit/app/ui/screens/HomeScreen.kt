package com.quakexit.app.ui.screens

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.quakexit.app.data.model.*
import com.quakexit.app.ui.components.*
import com.quakexit.app.ui.theme.QxColors
import com.quakexit.app.viewmodel.DevicesViewModel
import com.quakexit.app.viewmodel.HomeViewModel

const val MAX_RESPONSE_MS = 5000L

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(vm: HomeViewModel = viewModel(), devicesVm: DevicesViewModel = viewModel()) {
    val profile by vm.profile.collectAsState()
    val drill by vm.drill.collectAsState()
    val devices by devicesVm.devices.collectAsState()
    val loading by vm.loading.collectAsState()
    var name by remember(profile) { mutableStateOf(profile?.name ?: "") }
    var address by remember(profile) { mutableStateOf(profile?.address ?: "") }
    var showLink by remember { mutableStateOf(false) }
    var addRoomTo by remember { mutableStateOf<Int?>(null) }
    var addFloor by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { vm.load(); devicesVm.load() }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { LoadingBar(loading) }
        item {
            QxCard {
                Text("Simulacro de evacuación", style = MaterialTheme.typography.titleLarge)
                Text("Duración prevista: 30 segundos. Abre las puertas y mide el tiempo de respuesta por equipo.", style = MaterialTheme.typography.bodySmall, color = QxColors.TextMuted)
                Button(
                    onClick = { vm.startDrill("DRILL") }, enabled = !loading,
                    modifier = Modifier.fillMaxWidth().height(64.dp), shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = QxColors.Danger, contentColor = Color.White),
                ) {
                    Icon(Icons.Filled.DirectionsRun, null); Spacer(Modifier.width(10.dp))
                    Text("INICIAR SIMULACRO (DRILL)", style = MaterialTheme.typography.titleMedium)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GhostButton("Privado", { vm.startDrill("PRIVATE") }, Modifier.weight(1f), enabled = !loading)
                    GhostButton("Común", { vm.startDrill("COMMON") }, Modifier.weight(1f), enabled = !loading)
                }
            }
        }
        drill?.let { d -> item { DrillResultCard(d) } }

        item { SectionTitle("Perfil de vivienda") }
        item {
            QxCard {
                QxField(name, { name = it }, "Nombre de la vivienda")
                QxField(address, { address = it }, "Dirección")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PrimaryButton("Guardar perfil", { profile?.let { vm.save(it.copy(name = name.trim(), address = address.trim())) } }, Modifier.weight(1f), enabled = name.isNotBlank() && profile != null)
                    GhostButton("Vincular Hub", { showLink = true }, Modifier.weight(1f))
                }
            }
        }

        item { SectionTitle("Pisos y habitaciones") }
        profile?.floors?.forEachIndexed { fi, floor ->
            item {
                QxCard {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(floor.name, style = MaterialTheme.typography.titleMedium)
                        TextButton(onClick = { addRoomTo = fi }) { Text("+ Habitación", color = QxColors.Accent) }
                    }
                    floor.rooms.forEach { r ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("${r.name} · ${r.doors} puerta(s)", style = MaterialTheme.typography.bodyMedium)
                            Text(r.deviceIds.mapNotNull { id -> devices.firstOrNull { it.id == id }?.alias }.joinToString().ifBlank { "Sin equipos" }, style = MaterialTheme.typography.bodySmall, color = QxColors.TextMuted)
                        }
                    }
                }
            }
        }
        item { GhostButton("+ Agregar piso", { addFloor = true }, Modifier.fillMaxWidth()) }
    }

    if (showLink) LinkDeviceDialog(devicesVm) { showLink = false }
    if (addFloor) TextPromptDialog("Nuevo piso", "Nombre del piso", { n -> profile?.let { vm.save(it.copy(floors = it.floors + Floor(n))) } }) { addFloor = false }
    addRoomTo?.let { idx ->
        RoomDialog(devices, { room ->
            profile?.let { p -> vm.save(p.copy(floors = p.floors.mapIndexed { i, f -> if (i == idx) f.copy(rooms = f.rooms + room) else f })) }
        }) { addRoomTo = null }
    }
}

@Composable
fun DrillResultCard(d: DrillResult) {
    QxCard(borderColor = QxColors.Success) {
        Text("Resultado del simulacro ${d.type} · ${d.durationSec} s", style = MaterialTheme.typography.titleMedium)
        d.results.forEach { r ->
            val fast = r.responseMs < MAX_RESPONSE_MS
            val ok = r.doorOpened && fast
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(if (r.doorOpened) Icons.Filled.CheckCircle else Icons.Filled.Cancel, null, tint = if (r.doorOpened) QxColors.Success else QxColors.Danger)
                Column(Modifier.weight(1f)) {
                    Text(r.alias, style = MaterialTheme.typography.titleSmall)
                    Text(if (r.doorOpened) "Puerta abierta" else "La puerta no abrió", style = MaterialTheme.typography.bodySmall, color = QxColors.TextMuted)
                }
                StatusChip("${r.responseMs} ms", if (ok) QxColors.Success else QxColors.Danger)
            }
        }
        val ok = d.results.isNotEmpty() && d.results.all { it.doorOpened && it.responseMs < MAX_RESPONSE_MS }
        Banner(
            if (ok) "Todas las puertas abrieron en menos de $MAX_RESPONSE_MS ms." else "Hay equipos que no cumplen el objetivo de < $MAX_RESPONSE_MS ms.",
            if (ok) QxColors.Success else QxColors.Danger, if (ok) Icons.Filled.CheckCircle else Icons.Filled.Warning,
        )
    }
}

@Composable
fun TextPromptDialog(title: String, label: String, onOk: (String) -> Unit, onClose: () -> Unit) {
    var v by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onClose, containerColor = QxColors.PanelAlt, title = { Text(title) },
        text = { QxField(v, { v = it }, label) },
        confirmButton = { TextButton(enabled = v.isNotBlank(), onClick = { onOk(v.trim()); onClose() }) { Text("Aceptar", color = QxColors.Accent) } },
        dismissButton = { TextButton(onClick = onClose) { Text("Cancelar") } },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RoomDialog(devices: List<Device>, onOk: (Room) -> Unit, onClose: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var doors by remember { mutableStateOf("1") }
    val selected = remember { mutableStateListOf<String>() }
    AlertDialog(
        onDismissRequest = onClose, containerColor = QxColors.PanelAlt, title = { Text("Nueva habitación") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                QxField(name, { name = it }, "Nombre")
                QxField(doors, { doors = it.filter(Char::isDigit) }, "Puertas", keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                Text("Dispositivos asociados", style = MaterialTheme.typography.labelMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    devices.forEach { d ->
                        FilterChip(selected = d.id in selected, onClick = { if (d.id in selected) selected.remove(d.id) else selected.add(d.id) }, label = { Text(d.alias) })
                    }
                }
            }
        },
        confirmButton = {
            TextButton(enabled = name.isNotBlank(), onClick = { onOk(Room(name.trim(), doors.toIntOrNull() ?: 1, selected.toList())); onClose() }) { Text("Agregar", color = QxColors.Accent) }
        },
        dismissButton = { TextButton(onClick = onClose) { Text("Cancelar") } },
    )
}
