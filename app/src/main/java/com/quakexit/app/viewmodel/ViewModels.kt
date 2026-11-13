package com.quakexit.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quakexit.app.ServiceLocator
import com.quakexit.app.data.mock.MockDataSource
import com.quakexit.app.data.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.util.UUID

private fun Throwable.friendly(): String? = when (this) {
    is HttpException -> when (code()) {
        401, 403 -> null // lo maneja el interceptor (cierre de sesión / aviso de restricción)
        else -> "Error del servidor (${code()})"
    }
    else -> message ?: "Error inesperado"
}

abstract class BaseViewModel : ViewModel() {
    protected val repo = ServiceLocator.repository
    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    protected fun <T> run(success: String? = null, onDone: (T) -> Unit = {}, block: suspend () -> T) {
        viewModelScope.launch {
            _loading.value = true
            runCatching { block() }
                .onSuccess { onDone(it); success?.let(ServiceLocator::toast) }
                .onFailure { it.friendly()?.let(ServiceLocator::toast) }
            _loading.value = false
        }
    }

    /** Refresco silencioso (polling): sin spinner ni mensajes de error. */
    protected fun <T> quiet(onDone: (T) -> Unit, block: suspend () -> T) {
        viewModelScope.launch { runCatching { block() }.onSuccess(onDone) }
    }
}

class AuthViewModel : BaseViewModel() {
    val user = ServiceLocator.store.user

    fun login(email: String, password: String, role: String) = run<AuthResponse>(onDone = { ServiceLocator.store.save(it.token, it.user) }) {
        repo.login(email.trim(), password, role)
    }

    fun register(name: String, email: String, password: String, role: String) =
        run<AuthResponse>("Cuenta creada", { ServiceLocator.store.save(it.token, it.user) }) {
            repo.register(name.trim(), email.trim(), password, role)
        }

    fun logout() {
        ServiceLocator.store.clearSession()
    }
}

class DashboardViewModel : BaseViewModel() {
    val status = MutableStateFlow<HomeStatus?>(null)
    val quakes = MutableStateFlow<List<Earthquake>>(emptyList())
    val lastEvent = MutableStateFlow<EmergencyEvent?>(null)

    fun refresh() {
        quiet<HomeStatus>({ status.value = it }) { repo.homeStatus() }
        quiet<List<Earthquake>>({ quakes.value = it }) { repo.earthquakes() }
    }

    fun submitReading(magnitude: Double) = run<ReadingResult>(onDone = {
        if (it.thresholdExceeded) {
            lastEvent.value = it.event
            ServiceLocator.toast("Umbral superado: evento de emergencia generado")
        } else ServiceLocator.toast("Lectura por debajo del umbral (${MockDataSource.THRESHOLD})")
        refresh()
    }) { repo.submitReading(magnitude) }

    fun falseAlarm(id: String) = run<EmergencyEvent>("Evento marcado como falsa alarma", { lastEvent.value = null; refresh() }) {
        repo.markFalseAlarm(id)
    }
}

class DevicesViewModel : BaseViewModel() {
    val devices = MutableStateFlow<List<Device>>(emptyList())
    val query = MutableStateFlow("")

    fun refresh() = quiet<List<Device>>({ devices.value = it }) { repo.devices() }
    fun load() = run<List<Device>>(onDone = { devices.value = it }) { repo.devices() }

    private fun replace(d: Device) { devices.value = devices.value.map { if (it.id == d.id) d else it } }

    fun link(req: LinkDeviceRequest, onOk: () -> Unit) = run<Device>("Dispositivo vinculado", { devices.value = devices.value + it; onOk() }) { repo.linkDevice(req) }
    fun refreshOne(id: String) = run<Device>("Estado actualizado", ::replace) { repo.refreshDevice(id) }
    fun unlock(id: String) = run<Device>("Puerta desbloqueada", ::replace) { repo.unlockDoor(id) }
    fun deepSleep(id: String, on: Boolean) =
        run<Device>(if (on) "Ahorro profundo activado" else "Modo normal activado", ::replace) { repo.setDeepSleep(id, on) }
}

class HomeViewModel : BaseViewModel() {
    val profile = MutableStateFlow<HomeProfile?>(null)
    val drill = MutableStateFlow<DrillResult?>(null)

    fun load() = run<HomeProfile>(onDone = { profile.value = it }) { repo.homeProfile() }
    fun save(p: HomeProfile) = run<HomeProfile>("Hogar guardado", { profile.value = it }) { repo.updateHome(p) }
    fun startDrill(type: String) = run<DrillResult>("Simulacro ejecutado (30 s)", { drill.value = it }) { repo.startDrill(type) }
}

class SecurityViewModel : BaseViewModel() {
    val battery = MutableStateFlow<BatterySummary?>(null)
    val contacts = MutableStateFlow<List<Contact>>(emptyList())

    fun load() {
        run<BatterySummary>(onDone = { battery.value = it }) { repo.batterySummary() }
        quiet<List<Contact>>({ contacts.value = it }) { repo.contacts() }
    }

    fun add(c: Contact, onOk: () -> Unit) {
        if (contacts.value.size >= MockDataSource.MAX_CONTACTS) {
            ServiceLocator.toast("Máximo ${MockDataSource.MAX_CONTACTS} contactos de emergencia"); return
        }
        run<Contact>("Contacto agregado", { contacts.value = (contacts.value + it).sortedBy { c -> c.priority }; onOk() }) { repo.addContact(c) }
    }

    fun delete(id: String) = run<Unit>("Contacto eliminado", { contacts.value = contacts.value.filter { it.id != id } }) { repo.deleteContact(id) }
}

class NotificationsViewModel : BaseViewModel() {
    val items = MutableStateFlow<List<AppNotification>>(emptyList())
    val prefs = MutableStateFlow(NotificationPrefs())
    val pushToken = MutableStateFlow(ServiceLocator.store.pushToken)

    fun load() {
        run<List<AppNotification>>(onDone = { items.value = it }) { repo.notifications() }
        quiet<NotificationPrefs>({ prefs.value = it }) { repo.preferences() }
    }

    fun savePrefs(p: NotificationPrefs) = run<NotificationPrefs>("Preferencias guardadas", { prefs.value = it }) { repo.updatePreferences(p) }

    fun registerToken() {
        val t = ServiceLocator.store.pushToken ?: "qx-${UUID.randomUUID()}"
        run<Unit>("Token push registrado", { ServiceLocator.store.pushToken = t; pushToken.value = t }) { repo.registerPushToken(t) }
    }

    fun deleteToken() {
        val t = ServiceLocator.store.pushToken ?: return
        run<Unit>("Token push eliminado", { ServiceLocator.store.pushToken = null; pushToken.value = null }) { repo.deletePushToken(t) }
    }
}

class ReportsViewModel : BaseViewModel() {
    val page = MutableStateFlow<PageResponse<ReportItem>?>(null)
    val alerts = MutableStateFlow<List<MaintenanceAlert>>(emptyList())

    fun load(type: String, from: Long?, to: Long?, pageIdx: Int) = run<PageResponse<ReportItem>>(onDone = { page.value = it }) { repo.report(type, from, to, pageIdx) }
    fun loadAlerts() = run<List<MaintenanceAlert>>(onDone = { alerts.value = it }) { repo.maintenance() }

    private fun swap(a: MaintenanceAlert) { alerts.value = alerts.value.map { if (it.id == a.id) a else it } }
    fun ack(id: String) = run<MaintenanceAlert>("Alerta reconocida", ::swap) { repo.ackMaintenance(id) }
    fun resolve(id: String) = run<MaintenanceAlert>("Alerta resuelta", ::swap) { repo.resolveMaintenance(id) }
}

class AdminViewModel : BaseViewModel() {
    val buildings = MutableStateFlow<List<Building>>(emptyList())
    val subscription = MutableStateFlow<Subscription?>(null)
    val audit = MutableStateFlow<List<AuditEntry>>(emptyList())
    val health = MutableStateFlow<SystemHealth?>(null)

    fun loadBuildings() = run<List<Building>>(onDone = { buildings.value = it }) { repo.buildings() }
    fun loadPlans() = run<Subscription>(onDone = { subscription.value = it }) { repo.subscription() }
    fun loadSystem() {
        run<List<AuditEntry>>(onDone = { audit.value = it }) { repo.audit() }
        quiet<SystemHealth>({ health.value = it }) { repo.health() }
    }

    private fun swap(b: Building) { buildings.value = buildings.value.map { if (it.id == b.id) b else it } }
    fun createBuilding(name: String, address: String) = run<Building>("Edificio creado", { buildings.value = buildings.value + it }) { repo.createBuilding(name, address) }
    fun addUnit(id: String, name: String) = run<Building>("Unidad agregada", ::swap) { repo.addUnit(id, name) }
    fun batch(id: String, list: List<LinkDeviceRequest>) = run<BatchResult>(onDone = { ServiceLocator.toast("${it.registered} equipos registrados") }) { repo.registerBatch(id, list) }
    fun commonAlarm(id: String) = run<Ack>(onDone = { ServiceLocator.toast(it.message ?: "Alarma común activada") }) { repo.triggerCommonAlarm(id) }
    fun activate(plan: String) = run<Subscription>("Plan actualizado", { subscription.value = it }) { repo.activatePlan(plan) }
}

class SettingsViewModel : BaseViewModel() {
    val simulated = repo.simulated
    val baseUrl = MutableStateFlow(ServiceLocator.store.baseUrl)
    val forced = MutableStateFlow(ServiceLocator.store.forceSimulated)
    val outage = MutableStateFlow(repo.mock.powerOutage)

    fun setForced(on: Boolean) { forced.value = on; if (on) repo.setForceSimulated(true) else reconnect() }
    fun saveUrl(url: String) { ServiceLocator.store.baseUrl = url; baseUrl.value = ServiceLocator.store.baseUrl; ServiceLocator.toast("URL guardada") }

    fun reconnect() = run<Boolean>(onDone = {
        forced.value = false
        ServiceLocator.toast(if (it) "Conectado al servidor real" else "Servidor no disponible: se mantiene el modo simulado")
    }) { repo.reconnect() }

    fun toggleOutage(on: Boolean) { repo.mock.powerOutage = on; outage.value = on }
}
