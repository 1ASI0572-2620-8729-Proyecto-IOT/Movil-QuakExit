package com.quakexit.app.data.model

object Roles {
    const val OWNER = "OWNER"
    const val TENANT = "TENANT"
    const val ADMIN = "ADMIN"
    val all = listOf(OWNER, TENANT, ADMIN)
    fun label(r: String) = when (r) { OWNER -> "Propietario"; TENANT -> "Inquilino"; ADMIN -> "Administrador"; else -> r }
}

object Capabilities {
    const val BUILDINGS = "BUILDINGS"
    const val SYSTEM = "SYSTEM"
}

data class User(
    val id: String,
    val name: String,
    val email: String,
    val role: String,
    val plan: String = "FREE",
    val capabilities: List<String> = emptyList(),
)

data class LoginRequest(val email: String, val password: String, val role: String)
data class RegisterRequest(val name: String, val email: String, val password: String, val role: String)
data class AuthResponse(val token: String, val user: User)

data class Earthquake(val id: String, val magnitude: Double, val place: String, val depthKm: Double, val time: Long)

data class HomeStatus(val state: String, val batteryPercent: Int, val powerSource: String) // ONLINE|SLEEPING|ALERT, MAIN|BACKUP

data class Device(
    val id: String,
    val alias: String,
    val code: String,
    val mac: String,
    val status: String = "ONLINE",
    val battery: Int = 100,
    val lockOk: Boolean = true,
    val lightsOk: Boolean = true,
    val deepSleep: Boolean = false,
    val doorLocked: Boolean = true,
    val updatedAt: Long = System.currentTimeMillis(),
)

data class LinkDeviceRequest(val alias: String, val code: String, val mac: String)
data class DeepSleepRequest(val enabled: Boolean)

data class Room(val name: String, val doors: Int = 1, val deviceIds: List<String> = emptyList())
data class Floor(val name: String, val rooms: List<Room> = emptyList())
data class HomeProfile(val name: String, val address: String, val floors: List<Floor> = emptyList())

data class ReadingRequest(val magnitude: Double, val simulated: Boolean = true)
data class EmergencyEvent(val id: String, val magnitude: Double, val status: String, val createdAt: Long)
data class ReadingResult(val thresholdExceeded: Boolean, val event: EmergencyEvent? = null)

data class DrillRequest(val type: String) // PRIVATE | COMMON | DRILL
data class DrillDeviceResult(val deviceId: String, val alias: String, val doorOpened: Boolean, val responseMs: Long)
data class DrillResult(val id: String, val type: String, val durationSec: Int, val results: List<DrillDeviceResult>)

data class BatteryEntry(val deviceId: String, val alias: String, val battery: Int)
data class BatterySummary(val average: Int, val lowCount: Int, val devices: List<BatteryEntry>)

data class Contact(val id: String = "", val name: String, val phone: String, val priority: Int, val notifySms: Boolean)
data class NotificationPrefs(val push: Boolean = true, val sms: Boolean = false, val email: Boolean = false, val criticalOnly: Boolean = false)
data class PushTokenRequest(val token: String, val platform: String = "ANDROID")
data class AppNotification(val id: String, val title: String, val body: String, val severity: String, val createdAt: Long)

data class PageResponse<T>(val items: List<T>, val page: Int, val totalPages: Int, val total: Int)
data class ReportItem(val id: String, val type: String, val title: String, val detail: String, val date: Long)
data class MaintenanceAlert(val id: String, val title: String, val detail: String, val status: String) // OPEN|ACK|RESOLVED

data class Building(val id: String, val name: String, val address: String, val units: List<String> = emptyList())
data class BuildingRequest(val name: String, val address: String)
data class UnitRequest(val name: String)
data class BatchDevicesRequest(val buildingId: String, val devices: List<LinkDeviceRequest>)
data class BatchResult(val registered: Int)
data class Plan(val code: String, val name: String, val price: String, val features: List<String>)
data class Subscription(val plan: String, val status: String, val renewsAt: Long, val available: List<Plan> = emptyList())
data class PlanRequest(val plan: String)
data class AuditEntry(val id: String, val actor: String, val action: String, val at: Long)
data class SystemHealth(val status: String, val components: Map<String, String> = emptyMap())
data class Ack(val ok: Boolean = true, val message: String? = null)
