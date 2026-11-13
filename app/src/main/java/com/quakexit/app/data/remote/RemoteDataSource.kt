package com.quakexit.app.data.remote

import com.quakexit.app.data.model.*
import com.quakexit.app.data.repository.QuakDataSource

class RemoteDataSource(private val api: QuakApi) : QuakDataSource {
    override suspend fun login(email: String, password: String, role: String) = api.login(LoginRequest(email, password, role))
    override suspend fun register(name: String, email: String, password: String, role: String) =
        api.register(RegisterRequest(name, email, password, role))
    override suspend fun health() = api.health()

    override suspend fun earthquakes() = api.earthquakes()
    override suspend fun homeStatus() = api.homeStatus()
    override suspend fun submitReading(magnitude: Double) = api.submitReading(ReadingRequest(magnitude))
    override suspend fun markFalseAlarm(eventId: String) = api.markFalseAlarm(eventId)

    override suspend fun devices() = api.devices()
    override suspend fun linkDevice(req: LinkDeviceRequest) = api.linkDevice(req)
    override suspend fun refreshDevice(id: String) = api.device(id)
    override suspend fun unlockDoor(id: String) = api.unlockDoor(id)
    override suspend fun setDeepSleep(id: String, enabled: Boolean) = api.setDeepSleep(id, DeepSleepRequest(enabled))

    override suspend fun homeProfile() = api.homeProfile()
    override suspend fun updateHome(profile: HomeProfile) = api.updateHome(profile)
    override suspend fun startDrill(type: String) = api.startDrill(DrillRequest(type))

    override suspend fun batterySummary() = api.batterySummary()
    override suspend fun contacts() = api.contacts()
    override suspend fun addContact(contact: Contact) = api.addContact(contact)
    override suspend fun deleteContact(id: String) = api.deleteContact(id)

    override suspend fun preferences() = api.preferences()
    override suspend fun updatePreferences(prefs: NotificationPrefs) = api.updatePreferences(prefs)
    override suspend fun registerPushToken(token: String) = api.registerPushToken(PushTokenRequest(token))
    override suspend fun deletePushToken(token: String) = api.deletePushToken(PushTokenRequest(token))
    override suspend fun notifications() = api.notifications()

    override suspend fun report(type: String, from: Long?, to: Long?, page: Int) = api.report(type, from, to, page)
    override suspend fun maintenance() = api.maintenance()
    override suspend fun ackMaintenance(id: String) = api.ackMaintenance(id)
    override suspend fun resolveMaintenance(id: String) = api.resolveMaintenance(id)

    override suspend fun buildings() = api.buildings()
    override suspend fun createBuilding(name: String, address: String) = api.createBuilding(BuildingRequest(name, address))
    override suspend fun addUnit(buildingId: String, name: String) = api.addUnit(buildingId, UnitRequest(name))
    override suspend fun registerBatch(buildingId: String, devices: List<LinkDeviceRequest>) =
        api.registerBatch(BatchDevicesRequest(buildingId, devices))
    override suspend fun triggerCommonAlarm(buildingId: String) = api.triggerCommonAlarm(buildingId)
    override suspend fun subscription() = api.subscription()
    override suspend fun activatePlan(plan: String) = api.activatePlan(PlanRequest(plan))
    override suspend fun audit() = api.audit()
}
