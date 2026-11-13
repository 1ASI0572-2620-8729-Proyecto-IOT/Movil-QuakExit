package com.quakexit.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.quakexit.app.data.model.Device
import com.quakexit.app.data.model.LinkDeviceRequest
import com.quakexit.app.ui.components.*
import com.quakexit.app.ui.theme.QxColors
import com.quakexit.app.util.Validators
import com.quakexit.app.util.fmt
import com.quakexit.app.viewmodel.DevicesViewModel
import kotlinx.coroutines.delay

@Composable
fun DevicesScreen(vm: DevicesViewModel = viewModel()) {
    val devices by vm.devices.collectAsState()
    val query by vm.query.collectAsState()
    val loading by vm.loading.collectAsState()
    var showLink by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        vm.load()
        while (true) { delay(30_000); vm.refresh() }
    }
    val filtered = devices.filter {
        query.isBlank() || it.alias.contains(query, true) || it.code.contains(query, true) || it.mac.contains(query, true)
    }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 96.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { LoadingBar(loading) }
            item { QxField(query, { vm.query.value = it }, "Buscar por alias, código o MAC") }
            if (filtered.isEmpty()) item { EmptyState("No hay dispositivos vinculados") }
            items(filtered, key = { it.id }) { DeviceCard(it, vm) }
        }
        ExtendedFloatingActionButton(
            onClick = { showLink = true }, containerColor = QxColors.Accent, contentColor = androidx.compose.ui.graphics.Color(0xFF1A1200),
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
            icon = { Icon(Icons.Filled.Add, null) }, text = { Text("Vincular") },
        )
    }
    if (showLink) LinkDeviceDialog(vm) { showLink = false }
}

@Composable
fun DeviceCard(d: Device, vm: DevicesViewModel) {
    var confirmUnlock by remember { mutableStateOf(false) }
    QxCard(borderColor = if (!d.lockOk) QxColors.Danger else QxColors.Border) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(d.alias, style = MaterialTheme.typography.titleMedium)
                Text("${d.code} · ${d.mac}", style = MaterialTheme.typography.bodySmall, color = QxColors.TextMuted)
            }
            StatusChip(d.status, stateColor(d.status))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatusChip(if (d.lockOk) "Chapa activa" else "Chapa con fallo", if (d.lockOk) QxColors.Success else QxColors.Danger)
            StatusChip(if (d.lightsOk) "Luces OK" else "Luces con fallo", if (d.lightsOk) QxColors.Success else QxColors.Danger)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Batería", style = MaterialTheme.typography.labelMedium, color = QxColors.TextMuted)
            Text("${d.battery}%", style = MaterialTheme.typography.labelMedium, color = if (d.battery < 15) QxColors.Danger else QxColors.TextPrimary)
        }
        BatteryBar(d.battery)
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text("Deep Sleep (bajo consumo)", style = MaterialTheme.typography.titleSmall)
                Text(if (d.deepSleep) "Ahorro profundo" else "Energía normal", style = MaterialTheme.typography.bodySmall, color = QxColors.TextMuted)
            }
            Switch(
                checked = d.deepSleep, onCheckedChange = { vm.deepSleep(d.id, it) },
                colors = SwitchDefaults.colors(checkedTrackColor = QxColors.Accent, checkedThumbColor = androidx.compose.ui.graphics.Color(0xFF1A1200)),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GhostButton("Actualizar", { vm.refreshOne(d.id) }, Modifier.weight(1f))
            PrimaryButton(if (d.doorLocked) "Desbloquear puerta" else "Puerta abierta", { confirmUnlock = true }, Modifier.weight(1f), enabled = d.doorLocked)
        }
        Text("Actualizado ${d.updatedAt.fmt()}", style = MaterialTheme.typography.bodySmall, color = QxColors.TextMuted)
    }
    if (confirmUnlock) ConfirmDialog("Desbloquear puerta", "¿Abrir la puerta de ${d.alias}?", { vm.unlock(d.id) }) { confirmUnlock = false }
}

@Composable
fun LinkDeviceDialog(vm: DevicesViewModel, onClose: () -> Unit) {
    var alias by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var mac by remember { mutableStateOf("") }
    var submitted by remember { mutableStateOf(false) }
    val loading by vm.loading.collectAsState()
    AlertDialog(
        onDismissRequest = onClose, containerColor = QxColors.PanelAlt,
        title = { Text("Vincular QuakExit Hub") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                QxField(alias, { alias = it }, "Alias", error = if (submitted) Validators.notBlank(alias, "Alias") else null)
                QxField(code, { code = it.uppercase() }, "Código identificador", error = if (submitted) Validators.notBlank(code, "Código") else null)
                QxField(mac, { mac = it.uppercase() }, "Dirección MAC", error = if (submitted) Validators.mac(mac) else null)
                LoadingBar(loading)
            }
        },
        confirmButton = {
            TextButton(enabled = !loading, onClick = {
                submitted = true
                if (alias.isNotBlank() && code.isNotBlank() && Validators.mac(mac) == null) vm.link(LinkDeviceRequest(alias.trim(), code.trim(), mac), onClose)
            }) { Text("Vincular", color = QxColors.Accent) }
        },
        dismissButton = { TextButton(onClick = onClose) { Text("Cancelar") } },
    )
}
