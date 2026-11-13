package com.quakexit.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.quakexit.app.data.mock.MockDataSource
import com.quakexit.app.data.model.Contact
import com.quakexit.app.ui.components.*
import com.quakexit.app.ui.theme.QxColors
import com.quakexit.app.util.Validators
import com.quakexit.app.viewmodel.SecurityViewModel

/** El backend actual no expone eliminación de contactos; ponerlo en false para ocultarla. */
const val CONTACT_DELETE_ENABLED = true

@Composable
fun SecurityScreen(vm: SecurityViewModel = viewModel()) {
    val battery by vm.battery.collectAsState()
    val contacts by vm.contacts.collectAsState()
    val loading by vm.loading.collectAsState()
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf("1") }
    var sms by remember { mutableStateOf(true) }
    var submitted by remember { mutableStateOf(false) }
    var toDelete by remember { mutableStateOf<Contact?>(null) }
    val full = contacts.size >= MockDataSource.MAX_CONTACTS

    LaunchedEffect(Unit) { vm.load() }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { LoadingBar(loading) }
        item {
            QxCard {
                Text("Baterías del hogar", style = MaterialTheme.typography.titleLarge)
                battery?.let { b ->
                    Text("Promedio ${b.average}% · ${b.lowCount} equipo(s) bajo 15%", color = if (b.lowCount > 0) QxColors.Warning else QxColors.TextMuted, style = MaterialTheme.typography.bodyMedium)
                    b.devices.forEach {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(it.alias); Text("${it.battery}%") }
                        BatteryBar(it.battery)
                    }
                } ?: Text("Cargando…", color = QxColors.TextMuted)
            }
        }
        item { SectionTitle("Contactos de emergencia (${contacts.size}/${MockDataSource.MAX_CONTACTS})") }
        items(contacts, key = { it.id }) { c ->
            QxCard {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(c.name, style = MaterialTheme.typography.titleMedium)
                        Text(c.phone, style = MaterialTheme.typography.bodyMedium, color = QxColors.TextMuted)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            StatusChip("Prioridad ${c.priority}", QxColors.Info)
                            if (c.notifySms) StatusChip("SMS", QxColors.Success)
                        }
                    }
                    if (CONTACT_DELETE_ENABLED) IconButton(onClick = { toDelete = c }) { Icon(Icons.Filled.Delete, "Eliminar", tint = QxColors.Danger) }
                }
            }
        }
        item {
            QxCard {
                Text("Agregar contacto", style = MaterialTheme.typography.titleMedium)
                if (full) Banner("Alcanzaste el máximo de ${MockDataSource.MAX_CONTACTS} contactos.", QxColors.Warning, Icons.Filled.Warning)
                QxField(name, { name = it }, "Nombre", error = if (submitted) Validators.notBlank(name, "Nombre") else null)
                QxField(phone, { phone = it }, "Teléfono", error = if (submitted) Validators.phone(phone) else null, keyboardType = androidx.compose.ui.text.input.KeyboardType.Phone)
                QxField(priority, { priority = it.filter(Char::isDigit).take(1) }, "Prioridad (1 = más alta)", keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text("Avisar por SMS")
                    Switch(sms, { sms = it }, colors = SwitchDefaults.colors(checkedTrackColor = QxColors.Accent))
                }
                PrimaryButton("Agregar contacto", enabled = !full && !loading, modifier = Modifier.fillMaxWidth(), onClick = {
                    submitted = true
                    if (name.isNotBlank() && Validators.phone(phone) == null) {
                        vm.add(Contact(name = name.trim(), phone = phone.trim(), priority = priority.toIntOrNull() ?: 1, notifySms = sms)) {
                            name = ""; phone = ""; priority = "1"; submitted = false
                        }
                    }
                })
            }
        }
    }
    toDelete?.let { c -> ConfirmDialog("Eliminar contacto", "¿Eliminar a ${c.name}?", { vm.delete(c.id) }) { toDelete = null } }
}


