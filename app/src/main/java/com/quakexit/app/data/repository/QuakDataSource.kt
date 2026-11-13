package com.quakexit.app.data.repository

import com.quakexit.app.data.model.*

/** Contrato común del origen remoto (Retrofit) y del origen simulado (in-memory). */
interface QuakDataSource {
    suspend fun login(email: String, password: String, role: String): AuthResponse
    suspend fun register(name: String, email: String, password: String, role: String): AuthResponse
    suspend fun health(): SystemHealth

    suspend fun earthquakes(): List<Earthquake>
    suspend fun homeStatus(): HomeStatus
    suspend fun submitReading(magnitude: Double): ReadingResult
    suspend fun markFalseAlarm(eventId: String): EmergencyEvent

    suspend fun devices(): List<Device>
    suspend fun linkDevice(req: LinkDeviceRequest): Device
    suspend fun refreshDevice(id: String): Device
    suspend fun unlockDoor(id: String): Device
    suspend fun setDeepSleep(id: String, enabled: Boolean): Device

    suspend fun homeProfile(): HomeProfile
    suspend fun updateHome(profile: HomeProfile): HomeProfile
    suspend fun startDrill(type: String): DrillResult

    suspend fun batterySummary(): BatterySummary
    suspend fun contacts(): List<Contact>
    suspend fun addContact(contact: Contact): Contact
    suspend fun deleteContact(id: String)

    suspend fun preferences(): NotificationPrefs
    suspend fun updatePreferences(prefs: NotificationPrefs): NotificationPrefs
    suspend fun registerPushToken(token: String)
    suspend fun deletePushToken(token: String)
    suspend fun notifications(): List<AppNotification>

    suspend fun report(type: String, from: Long?, to: Long?, page: Int): PageResponse<ReportItem>
    suspend fun maintenance(): List<MaintenanceAlert>
    suspend fun ackMaintenance(id: String): MaintenanceAlert
    suspend fun resolveMaintenance(id: String): MaintenanceAlert

    suspend fun buildings(): List<Building>
    suspend fun createBuilding(name: String, address: String): Building
    suspend fun addUnit(buildingId: String, name: String): Building
    suspend fun registerBatch(buildingId: String, devices: List<LinkDeviceRequest>): BatchResult
    suspend fun triggerCommonAlarm(buildingId: String): Ack
    suspend fun subscription(): Subscription
    suspend fun activatePlan(plan: String): Subscription
    suspend fun audit(): List<AuditEntry>
}
