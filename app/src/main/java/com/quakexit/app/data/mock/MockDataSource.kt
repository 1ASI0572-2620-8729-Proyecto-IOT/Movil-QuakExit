package com.quakexit.app.data.mock

import com.quakexit.app.data.model.*
import com.quakexit.app.data.repository.QuakDataSource
import kotlinx.coroutines.delay
import java.util.UUID
import kotlin.random.Random

/** Motor simulado con estado en memoria: cada acción modifica datos y responde como un HTTP 200. */
class MockDataSource : QuakDataSource {
    var powerOutage = false

    private val now get() = System.currentTimeMillis()
    private fun id() = UUID.randomUUID().toString().take(8)
    private suspend fun latency() = delay(Random.nextLong(120, 350))

    private val users = mutableMapOf<String, User>()
    private val devices = mutableListOf(
        Device("d1", "Hub Sala", "QX-HUB-0001", "AA:BB:CC:00:00:01", battery = 92),
        Device("d2", "Puerta Principal", "QX-DOOR-0002", "AA:BB:CC:00:00:02", battery = 64),
        Device("d3", "Salida Patio", "QX-DOOR-0003", "AA:BB:CC:00:00:03", battery = 13, lockOk = false),
    )
    private var home = HomeProfile(
        "Mi Departamento", "Av. Primavera 123, Lima",
        listOf(
            Floor("Piso 1", listOf(Room("Sala", 1, listOf("d1")), Room("Cocina", 1, listOf("d3")))),
            Floor("Piso 2", listOf(Room("Dormitorio", 1, listOf("d2")))),
        ),
    )
    private val contacts = mutableListOf(Contact("c1", "Madre", "+51987654321", 1, true))
    private var prefs = NotificationPrefs()
    private val pushTokens = mutableSetOf<String>()
    private val events = mutableListOf<EmergencyEvent>()
    private val notifications = mutableListOf(
        AppNotification("n1", "Bienvenido a QuakExit", "Modo simulado activo.", "INFO", now - 3_600_000),
        AppNotification("n2", "Batería baja", "Salida Patio tiene 13% de batería.", "WARNING", now - 1_800_000),
    )
    private val maintenance = mutableListOf(
        MaintenanceAlert("m1", "Chapa con fallo", "Salida Patio no responde a la chapa electromagnética.", "OPEN"),
        MaintenanceAlert("m2", "Batería baja", "Salida Patio por debajo de 15%.", "OPEN"),
    )
    private val reportRows = mutableMapOf<String, MutableList<ReportItem>>()
    private val buildings = mutableListOf(Building("b1", "Torre Aurora", "Jr. Los Pinos 450", listOf("101", "102")))
    private var plan = "FREE"
    private val audit = mutableListOf(AuditEntry("a1", "sistema", "Inicio del motor simulado", now))
    private val plans = listOf(
        Plan("FREE", "Residencial", "S/ 0", listOf("1 hogar", "5 contactos")),
        Plan("PRO", "Pro Edificios", "S/ 49", listOf("Edificios y unidades", "Alarma común", "Registro en lote")),
    )

    private fun log(action: String) { audit.add(0, AuditEntry(id(), "simulador", action, now)) }
    private fun note(title: String, body: String, sev: String) { notifications.add(0, AppNotification(id(), title, body, sev, now)) }
    private fun row(type: String, title: String, detail: String) {
        reportRows.getOrPut(type) { mutableListOf() }.add(0, ReportItem(id(), type, title, detail, now))
    }
    private fun userFor(name: String, email: String, role: String): User {
        val caps = when (role) {
            Roles.ADMIN -> listOf(Capabilities.BUILDINGS, Capabilities.SYSTEM)
            Roles.OWNER -> if (plan == "PRO") listOf(Capabilities.BUILDINGS) else emptyList()
            else -> emptyList()
        }
        return User("u-${email.hashCode().toUInt()}", name, email, role, plan, caps)
    }
    private fun update(id: String, f: (Device) -> Device): Device {
        val i = devices.indexOfFirst { it.id == id }
        if (i < 0) error("Dispositivo no encontrado")
        devices[i] = f(devices[i]).copy(updatedAt = now)
        return devices[i]
    }

    override suspend fun login(email: String, password: String, role: String): AuthResponse {
        latency()
        val u = users[email.lowercase()]?.copy(role = role) ?: userFor(email.substringBefore('@').replaceFirstChar { it.uppercase() }, email, role)
        users[email.lowercase()] = u
        log("Login $email ($role)")
        return AuthResponse("sim-token-${id()}", userFor(u.name, u.email, role))
    }

    override suspend fun register(name: String, email: String, password: String, role: String): AuthResponse {
        latency()
        val u = userFor(name, email, role)
        users[email.lowercase()] = u
        log("Registro $email ($role)")
        return AuthResponse("sim-token-${id()}", u)
    }

    override suspend fun health(): SystemHealth {
        latency()
        return SystemHealth("UP", mapOf("api" to "simulada", "db" to "memoria", "mqtt" to "simulado"))
    }

    override suspend fun earthquakes(): List<Earthquake> {
        latency()
        val places = listOf("Ica, Perú", "Arequipa, Perú", "Valparaíso, Chile", "Guayaquil, Ecuador", "Tacna, Perú", "Cusco, Perú")
        val r = Random((now / 60_000).toInt())
        return List(8) { i ->
            Earthquake("eq$i", (30 + r.nextInt(40)) / 10.0, places[r.nextInt(places.size)], 10.0 + r.nextInt(150), now - i * 1_800_000L - r.nextLong(600_000))
        }
    }

    override suspend fun homeStatus(): HomeStatus {
        latency()
        val hub = devices.firstOrNull()
        val state = when {
            events.any { it.status == "ACTIVE" } -> "ALERT"
            hub?.deepSleep == true -> "SLEEPING"
            else -> "ONLINE"
        }
        return HomeStatus(state, hub?.battery ?: 0, if (powerOutage) "BACKUP" else "MAIN")
    }

    override suspend fun submitReading(magnitude: Double): ReadingResult {
        latency()
        if (magnitude < THRESHOLD) return ReadingResult(false)
        val e = EmergencyEvent(id(), magnitude, "ACTIVE", now)
        events.add(e)
        note("Evento sísmico", "Lectura de magnitud $magnitude superó el umbral.", "CRITICAL")
        row("earthquakes", "Evento M$magnitude", "Umbral superado")
        return ReadingResult(true, e)
    }

    override suspend fun markFalseAlarm(eventId: String): EmergencyEvent {
        latency()
        val i = events.indexOfFirst { it.id == eventId }
        if (i < 0) error("Evento no encontrado")
        events[i] = events[i].copy(status = "FALSE_ALARM")
        row("false-alarms", "Falsa alarma M${events[i].magnitude}", "Marcada por el usuario")
        note("Falsa alarma", "El evento fue descartado.", "INFO")
        return events[i]
    }

    override suspend fun devices(): List<Device> {
        latency()
        devices.indices.forEach { i ->
            val d = devices[i]
            if (d.battery > 5 && Random.nextInt(10) == 0) devices[i] = d.copy(battery = d.battery - 1)
        }
        return devices.toList()
    }

    override suspend fun linkDevice(req: LinkDeviceRequest): Device {
        latency()
        require(devices.none { it.code.equals(req.code, true) }) { "El código ya está vinculado" }
        val d = Device(id(), req.alias, req.code, req.mac.uppercase(), battery = 100)
        devices.add(d)
        log("Dispositivo vinculado ${req.code}")
        note("Dispositivo vinculado", "${req.alias} se vinculó correctamente.", "INFO")
        return d
    }

    override suspend fun refreshDevice(id: String): Device { latency(); return update(id) { it } }

    override suspend fun unlockDoor(id: String): Device {
        latency()
        val d = update(id) { it.copy(doorLocked = false) }
        row("devices", "Puerta desbloqueada", d.alias)
        return d
    }

    override suspend fun setDeepSleep(id: String, enabled: Boolean): Device {
        latency()
        return update(id) { it.copy(deepSleep = enabled, status = if (enabled) "SLEEPING" else "ONLINE") }
    }

    override suspend fun homeProfile(): HomeProfile { latency(); return home }
    override suspend fun updateHome(profile: HomeProfile): HomeProfile { latency(); home = profile; log("Hogar actualizado"); return home }

    override suspend fun startDrill(type: String): DrillResult {
        latency()
        val results = devices.map {
            val ok = it.lockOk
            DrillDeviceResult(it.id, it.alias, ok, if (ok) Random.nextLong(600, 3200) else 5000L)
        }
        val r = DrillResult(id(), type, 30, results)
        row("drills", "Simulacro $type", "${results.count { it.doorOpened }}/${results.size} puertas abiertas")
        note("Simulacro finalizado", "Tipo $type con ${results.size} equipos.", "INFO")
        return r
    }

    override suspend fun batterySummary(): BatterySummary {
        latency()
        return BatterySummary(
            devices.map { it.battery }.average().toInt(),
            devices.count { it.battery < 15 },
            devices.map { BatteryEntry(it.id, it.alias, it.battery) },
        )
    }

    override suspend fun contacts(): List<Contact> { latency(); return contacts.sortedBy { it.priority } }

    override suspend fun addContact(contact: Contact): Contact {
        latency()
        require(contacts.size < MAX_CONTACTS) { "Máximo $MAX_CONTACTS contactos de emergencia" }
        val c = contact.copy(id = id())
        contacts.add(c)
        return c
    }

    override suspend fun deleteContact(id: String) { latency(); contacts.removeAll { it.id == id } }

    override suspend fun preferences(): NotificationPrefs { latency(); return prefs }
    override suspend fun updatePreferences(prefs: NotificationPrefs): NotificationPrefs { latency(); this.prefs = prefs; return prefs }
    override suspend fun registerPushToken(token: String) { latency(); pushTokens.add(token) }
    override suspend fun deletePushToken(token: String) { latency(); pushTokens.remove(token) }
    override suspend fun notifications(): List<AppNotification> { latency(); return notifications.toList() }

    override suspend fun report(type: String, from: Long?, to: Long?, page: Int): PageResponse<ReportItem> {
        latency()
        if (reportRows[type].isNullOrEmpty()) {
            repeat(23) { i ->
                row(type, "${type.replace('-', ' ').replaceFirstChar { it.uppercase() }} #${23 - i}", "Registro histórico simulado")
                val list = reportRows[type]!!
                list[0] = list[0].copy(date = now - i * 86_400_000L / 2)
            }
        }
        val filtered = reportRows[type]!!.filter { (from == null || it.date >= from) && (to == null || it.date <= to) }
        val size = 10
        val pages = maxOf(1, (filtered.size + size - 1) / size)
        return PageResponse(filtered.drop(page * size).take(size), page, pages, filtered.size)
    }

    override suspend fun maintenance(): List<MaintenanceAlert> { latency(); return maintenance.toList() }
    private fun setMaint(id: String, status: String): MaintenanceAlert {
        val i = maintenance.indexOfFirst { it.id == id }
        maintenance[i] = maintenance[i].copy(status = status)
        return maintenance[i]
    }
    override suspend fun ackMaintenance(id: String): MaintenanceAlert { latency(); return setMaint(id, "ACK") }
    override suspend fun resolveMaintenance(id: String): MaintenanceAlert { latency(); return setMaint(id, "RESOLVED") }

    override suspend fun buildings(): List<Building> { latency(); return buildings.toList() }
    override suspend fun createBuilding(name: String, address: String): Building {
        latency(); val b = Building(id(), name, address); buildings.add(b); log("Edificio creado $name"); return b
    }
    override suspend fun addUnit(buildingId: String, name: String): Building {
        latency()
        val i = buildings.indexOfFirst { it.id == buildingId }
        buildings[i] = buildings[i].copy(units = buildings[i].units + name)
        return buildings[i]
    }
    override suspend fun registerBatch(buildingId: String, devices: List<LinkDeviceRequest>): BatchResult {
        latency()
        devices.forEach { this.devices.add(Device(id(), it.alias, it.code, it.mac.uppercase())) }
        log("Lote de ${devices.size} equipos en $buildingId")
        return BatchResult(devices.size)
    }
    override suspend fun triggerCommonAlarm(buildingId: String): Ack {
        latency(); log("Alarma común en $buildingId"); note("Alarma común", "Se activó la alarma del edificio.", "CRITICAL")
        return Ack(true, "Alarma común activada")
    }
    override suspend fun subscription(): Subscription { latency(); return Subscription(plan, "ACTIVE", now + 30 * 86_400_000L, plans) }
    override suspend fun activatePlan(plan: String): Subscription { latency(); this.plan = plan; log("Plan $plan"); return subscription() }
    override suspend fun audit(): List<AuditEntry> { latency(); return audit.toList() }

    companion object {
        const val THRESHOLD = 4.5
        const val MAX_CONTACTS = 5
    }
}
