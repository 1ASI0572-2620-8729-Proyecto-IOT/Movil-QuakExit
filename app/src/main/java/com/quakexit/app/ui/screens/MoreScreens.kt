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
import com.quakexit.app.data.model.*
import com.quakexit.app.ui.components.*
import com.quakexit.app.ui.theme.QxColors
import com.quakexit.app.util.fmt
import com.quakexit.app.util.fmtDate
import com.quakexit.app.util.parseDate
import com.quakexit.app.viewmodel.*

fun severityColor(s: String) = when (s) { "CRITICAL" -> QxColors.Danger; "WARNING" -> QxColors.Warning; else -> QxColors.Info }

@Composable
fun NotificationsScreen(vm: NotificationsViewModel = viewModel()) {
    val items by vm.items.collectAsState()
    val prefs by vm.prefs.collectAsState()
    val token by vm.pushToken.collectAsState()
    val loading by vm.loading.collectAsState()
    LaunchedEffect(Unit) { vm.load() }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { LoadingBar(loading) }
        item {
            QxCard {
                Text("Preferencias de alerta", style = MaterialTheme.typography.titleMedium)
                PrefRow("Notificaciones push", prefs.push) { vm.savePrefs(prefs.copy(push = it)) }
                PrefRow("Avisos por SMS", prefs.sms) { vm.savePrefs(prefs.copy(sms = it)) }
                PrefRow("Avisos por correo", prefs.email) { vm.savePrefs(prefs.copy(email = it)) }
                PrefRow("Solo alertas críticas", prefs.criticalOnly) { vm.savePrefs(prefs.copy(criticalOnly = it)) }
                HorizontalDivider(color = QxColors.Border)
                Text(if (token == null) "Este dispositivo no está registrado para push." else "Token: ${token!!.take(18)}…", style = MaterialTheme.typography.bodySmall, color = QxColors.TextMuted)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PrimaryButton("Registrar token", vm::registerToken, Modifier.weight(1f))
                    GhostButton("Eliminar token", vm::deleteToken, Modifier.weight(1f), color = QxColors.Danger, enabled = token != null)
                }
            }
        }
        item { SectionTitle("Historial") }
        if (items.isEmpty()) item { EmptyState("Sin notificaciones") }
        items(items, key = { it.id }) { n ->
            QxCard(borderColor = if (n.severity == "CRITICAL") QxColors.Danger else QxColors.Border) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(n.title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                    StatusChip(if (n.severity == "CRITICAL") "MODO EMERGENCIA" else n.severity, severityColor(n.severity))
                }
                Text(n.body, style = MaterialTheme.typography.bodyMedium)
                Text(n.createdAt.fmt(), style = MaterialTheme.typography.bodySmall, color = QxColors.TextMuted)
            }
        }
    }
}

@Composable
private fun PrefRow(label: String, value: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label); Switch(value, onChange, colors = SwitchDefaults.colors(checkedTrackColor = QxColors.Accent))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReportsScreen(vm: ReportsViewModel = viewModel()) {
    val page by vm.page.collectAsState()
    val loading by vm.loading.collectAsState()
    val types = listOf("earthquakes" to "Sismos", "drills" to "Simulacros", "devices" to "Equipos", "false-alarms" to "Falsas alarmas")
    var type by remember { mutableStateOf(types[0].first) }
    var from by remember { mutableStateOf("") }
    var to by remember { mutableStateOf("") }
    var idx by remember { mutableIntStateOf(0) }

    fun query(p: Int) {
        idx = p
        vm.load(type, parseDate(from), parseDate(to)?.plus(86_399_999L), p)
    }
    LaunchedEffect(type) { query(0) }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { LoadingBar(loading) }
        item {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                types.forEach { (k, l) -> FilterChip(selected = type == k, onClick = { type = k }, label = { Text(l) }) }
            }
        }
        item {
            QxCard {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    QxField(from, { from = it }, "Desde (dd/MM/aaaa)", Modifier.weight(1f), error = if (from.isNotBlank() && parseDate(from) == null) "Fecha inválida" else null)
                    QxField(to, { to = it }, "Hasta", Modifier.weight(1f), error = if (to.isNotBlank() && parseDate(to) == null) "Fecha inválida" else null)
                }
                PrimaryButton("Filtrar", { query(0) }, Modifier.fillMaxWidth())
            }
        }
        val rows = page?.items.orEmpty()
        if (rows.isEmpty() && !loading) item { EmptyState("Sin resultados") }
        items(rows, key = { it.id }) { r ->
            QxCard {
                Text(r.title, style = MaterialTheme.typography.titleSmall)
                Text(r.detail, style = MaterialTheme.typography.bodyMedium, color = QxColors.TextMuted)
                Text(r.date.fmt(), style = MaterialTheme.typography.bodySmall, color = QxColors.TextMuted)
            }
        }
        page?.let { pg ->
            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    GhostButton("Anterior", { query(idx - 1) }, enabled = idx > 0)
                    Text("Página ${pg.page + 1} de ${pg.totalPages} (${pg.total})", style = MaterialTheme.typography.bodySmall)
                    GhostButton("Siguiente", { query(idx + 1) }, enabled = idx + 1 < pg.totalPages)
                }
            }
        }
    }
}

@Composable
fun MaintenanceScreen(vm: ReportsViewModel = viewModel()) {
    val alerts by vm.alerts.collectAsState()
    val loading by vm.loading.collectAsState()
    LaunchedEffect(Unit) { vm.loadAlerts() }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { LoadingBar(loading) }
        if (alerts.isEmpty() && !loading) item { EmptyState("Sin alertas de mantenimiento") }
        items(alerts, key = { it.id }) { a ->
            val c = when (a.status) { "RESOLVED" -> QxColors.Success; "ACK" -> QxColors.Info; else -> QxColors.Warning }
            QxCard {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(a.title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                    StatusChip(a.status, c)
                }
                Text(a.detail, style = MaterialTheme.typography.bodyMedium, color = QxColors.TextMuted)
                if (a.status != "RESOLVED") Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GhostButton("Reconocer", { vm.ack(a.id) }, Modifier.weight(1f), enabled = a.status == "OPEN")
                    PrimaryButton("Resolver", { vm.resolve(a.id) }, Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(onLogout: () -> Unit, vm: SettingsViewModel = viewModel(), auth: AuthViewModel = viewModel()) {
    val simulated by vm.simulated.collectAsState()
    val forced by vm.forced.collectAsState()
    val outage by vm.outage.collectAsState()
    val loading by vm.loading.collectAsState()
    val user by auth.user.collectAsState()
    var url by remember { mutableStateOf(vm.baseUrl.value) }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { LoadingBar(loading) }
        item {
            QxCard {
                Text("Conexión", style = MaterialTheme.typography.titleMedium)
                StatusChip(if (simulated) "Modo simulado activo" else "Conectado al servidor real", if (simulated) QxColors.Warning else QxColors.Success)
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(Modifier.weight(1f)) {
                        Text("Forzar modo simulado", style = MaterialTheme.typography.titleSmall)
                        Text("Usa datos en memoria sin tocar el servidor.", style = MaterialTheme.typography.bodySmall, color = QxColors.TextMuted)
                    }
                    Switch(forced, vm::setForced, colors = SwitchDefaults.colors(checkedTrackColor = QxColors.Accent))
                }
                GhostButton("Reintentar conexión con el servidor", vm::reconnect, Modifier.fillMaxWidth(), enabled = !loading)
                QxField(url, { url = it }, "URL base de la API")
                PrimaryButton("Guardar URL", { vm.saveUrl(url.trim()) }, Modifier.fillMaxWidth(), enabled = url.startsWith("http"))
                if (simulated) Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Simular corte de luz (BACKUP)")
                    Switch(outage, vm::toggleOutage, colors = SwitchDefaults.colors(checkedTrackColor = QxColors.Warning))
                }
            }
        }
        item {
            QxCard {
                Text("Sesión", style = MaterialTheme.typography.titleMedium)
                user?.let { Text("${it.name} · ${Roles.label(it.role)} · Plan ${it.plan}", color = QxColors.TextMuted) }
                GhostButton("Cerrar sesión", { auth.logout(); onLogout() }, Modifier.fillMaxWidth(), color = QxColors.Danger)
            }
        }
    }
}

@Composable
fun BuildingsScreen(vm: AdminViewModel = viewModel()) {
    val buildings by vm.buildings.collectAsState()
    val loading by vm.loading.collectAsState()
    var newName by remember { mutableStateOf("") }
    var newAddress by remember { mutableStateOf("") }
    var unitName by remember { mutableStateOf("") }
    var batchCount by remember { mutableStateOf("3") }
    LaunchedEffect(Unit) { vm.loadBuildings() }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { LoadingBar(loading) }
        item {
            QxCard {
                Text("Nuevo edificio", style = MaterialTheme.typography.titleMedium)
                QxField(newName, { newName = it }, "Nombre")
                QxField(newAddress, { newAddress = it }, "Dirección")
                PrimaryButton("Crear edificio", { vm.createBuilding(newName.trim(), newAddress.trim()); newName = ""; newAddress = "" }, Modifier.fillMaxWidth(), enabled = newName.isNotBlank())
            }
        }
        items(buildings, key = { it.id }) { b ->
            QxCard {
                Text(b.name, style = MaterialTheme.typography.titleMedium)
                Text(b.address, style = MaterialTheme.typography.bodySmall, color = QxColors.TextMuted)
                Text("Unidades: ${b.units.joinToString().ifBlank { "—" }}", style = MaterialTheme.typography.bodyMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    QxField(unitName, { unitName = it }, "Unidad", Modifier.weight(1f))
                    GhostButton("Agregar", { vm.addUnit(b.id, unitName.trim()); unitName = "" }, enabled = unitName.isNotBlank())
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    QxField(batchCount, { batchCount = it.filter(Char::isDigit).take(2) }, "Equipos en lote", Modifier.weight(1f), keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                    GhostButton("Registrar", {
                        val n = batchCount.toIntOrNull() ?: 0
                        val base = System.currentTimeMillis() % 100000
                        vm.batch(b.id, List(n) { i -> LinkDeviceRequest("${b.name} #${i + 1}", "QX-B-${base + i}", "AA:BB:CC:%02X:%02X:%02X".format((base % 256).toInt(), i, n)) })
                    }, enabled = (batchCount.toIntOrNull() ?: 0) > 0)
                }
                PrimaryButton("Activar alarma común", { vm.commonAlarm(b.id) }, Modifier.fillMaxWidth(), icon = Icons.Filled.NotificationsActive)
            }
        }
    }
}

@Composable
fun PlansScreen(vm: AdminViewModel = viewModel()) {
    val sub by vm.subscription.collectAsState()
    val loading by vm.loading.collectAsState()
    LaunchedEffect(Unit) { vm.loadPlans() }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { LoadingBar(loading) }
        sub?.let { s ->
            item { QxCard { Text("Plan actual: ${s.plan} (${s.status})", style = MaterialTheme.typography.titleMedium); Text("Renueva ${s.renewsAt.fmtDate()}", color = QxColors.TextMuted) } }
            items(s.available, key = { it.code }) { p ->
                QxCard(borderColor = if (p.code == s.plan) QxColors.Accent else QxColors.Border) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(p.name, style = MaterialTheme.typography.titleMedium); Text(p.price, color = QxColors.Accent) }
                    p.features.forEach { Text("• $it", style = MaterialTheme.typography.bodyMedium) }
                    PrimaryButton(if (p.code == s.plan) "Plan activo" else "Activar", { vm.activate(p.code) }, Modifier.fillMaxWidth(), enabled = p.code != s.plan)
                }
            }
        }
    }
}

@Composable
fun SystemScreen(vm: AdminViewModel = viewModel()) {
    val audit by vm.audit.collectAsState()
    val health by vm.health.collectAsState()
    val loading by vm.loading.collectAsState()
    LaunchedEffect(Unit) { vm.loadSystem() }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { LoadingBar(loading) }
        item {
            QxCard {
                Text("Salud del sistema", style = MaterialTheme.typography.titleMedium)
                health?.let { h ->
                    StatusChip(h.status, if (h.status == "UP") QxColors.Success else QxColors.Danger)
                    h.components.forEach { (k, v) -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(k); Text(v, color = QxColors.TextMuted) } }
                } ?: Text("Consultando…", color = QxColors.TextMuted)
            }
        }
        item { SectionTitle("Auditoría") }
        items(audit, key = { it.id }) { a ->
            QxCard { Text(a.action, style = MaterialTheme.typography.titleSmall); Text("${a.actor} · ${a.at.fmt()}", style = MaterialTheme.typography.bodySmall, color = QxColors.TextMuted) }
        }
    }
}
