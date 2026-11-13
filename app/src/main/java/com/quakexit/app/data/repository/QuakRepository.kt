package com.quakexit.app.data.repository

import com.quakexit.app.data.local.SessionStore
import com.quakexit.app.data.mock.MockDataSource
import com.quakexit.app.data.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import retrofit2.HttpException
import java.io.IOException

/**
 * Repository que alterna entre remoto (Retrofit) y simulado (in-memory).
 * Ante fallo de red/timeout/5xx pasa a simulado de forma transparente.
 */
class QuakRepository(
    private val remote: QuakDataSource,
    val mock: MockDataSource,
    private val store: SessionStore,
) : QuakDataSource {
    private val _simulated = MutableStateFlow(store.forceSimulated)
    val simulated: StateFlow<Boolean> = _simulated

    fun setForceSimulated(force: Boolean) {
        store.forceSimulated = force
        _simulated.value = force
    }

    /** Intenta volver al servidor real; devuelve true si respondió. */
    suspend fun reconnect(): Boolean {
        store.forceSimulated = false
        return try {
            remote.health()
            _simulated.value = false
            true
        } catch (e: Exception) {
            _simulated.value = true
            false
        }
    }

    private fun Throwable.isConnectivityFailure() =
        this is IOException || (this is HttpException && code() in 502..504)

    private suspend fun <T> call(block: suspend QuakDataSource.() -> T): T {
        if (_simulated.value) return mock.block()
        return try {
            remote.block()
        } catch (e: Exception) {
            if (e.isConnectivityFailure()) {
                _simulated.value = true
                mock.block()
            } else throw e
        }
    }

    override suspend fun login(email: String, password: String, role: String) = call { login(email, password, role) }
    override suspend fun register(name: String, email: String, password: String, role: String) = call { register(name, email, password, role) }
    override suspend fun health() = call { health() }
    override suspend fun earthquakes() = call { earthquakes() }
    override suspend fun homeStatus() = call { homeStatus() }
    override suspend fun submitReading(magnitude: Double) = call { submitReading(magnitude) }
    override suspend fun markFalseAlarm(eventId: String) = call { markFalseAlarm(eventId) }
    override suspend fun devices() = call { devices() }
    override suspend fun linkDevice(req: LinkDeviceRequest) = call { linkDevice(req) }
    override suspend fun refreshDevice(id: String) = call { refreshDevice(id) }
    override suspend fun unlockDoor(id: String) = call { unlockDoor(id) }
    override suspend fun setDeepSleep(id: String, enabled: Boolean) = call { setDeepSleep(id, enabled) }
    override suspend fun homeProfile() = call { homeProfile() }
    override suspend fun updateHome(profile: HomeProfile) = call { updateHome(profile) }
    override suspend fun startDrill(type: String) = call { startDrill(type) }
    override suspend fun batterySummary() = call { batterySummary() }
    override suspend fun contacts() = call { contacts() }
    override suspend fun addContact(contact: Contact) = call { addContact(contact) }
    override suspend fun deleteContact(id: String) = call { deleteContact(id) }
    override suspend fun preferences() = call { preferences() }
    override suspend fun updatePreferences(prefs: NotificationPrefs) = call { updatePreferences(prefs) }
    override suspend fun registerPushToken(token: String) = call { registerPushToken(token) }
    override suspend fun deletePushToken(token: String) = call { deletePushToken(token) }
    override suspend fun notifications() = call { notifications() }
    override suspend fun report(type: String, from: Long?, to: Long?, page: Int) = call { report(type, from, to, page) }
    override suspend fun maintenance() = call { maintenance() }
    override suspend fun ackMaintenance(id: String) = call { ackMaintenance(id) }
    override suspend fun resolveMaintenance(id: String) = call { resolveMaintenance(id) }
    override suspend fun buildings() = call { buildings() }
    override suspend fun createBuilding(name: String, address: String) = call { createBuilding(name, address) }
    override suspend fun addUnit(buildingId: String, name: String) = call { addUnit(buildingId, name) }
    override suspend fun registerBatch(buildingId: String, devices: List<LinkDeviceRequest>) = call { registerBatch(buildingId, devices) }
    override suspend fun triggerCommonAlarm(buildingId: String) = call { triggerCommonAlarm(buildingId) }
    override suspend fun subscription() = call { subscription() }
    override suspend fun activatePlan(plan: String) = call { activatePlan(plan) }
    override suspend fun audit() = call { audit() }
}
