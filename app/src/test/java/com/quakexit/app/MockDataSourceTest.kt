package com.quakexit.app

import com.quakexit.app.data.mock.MockDataSource
import com.quakexit.app.data.model.Contact
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class MockDataSourceTest {
    private val ds = MockDataSource()

    @Test fun lecturaSobreUmbralGeneraEventoActivoYNotificacionCritica() = runBlocking {
        val r = ds.submitReading(6.0)
        assertTrue(r.thresholdExceeded)
        assertEquals("ACTIVE", r.event?.status)
        assertEquals("CRITICAL", ds.notifications().first().severity)
        assertEquals("ALERT", ds.homeStatus().state)
    }

    @Test fun lecturaBajoUmbralNoGeneraEvento() = runBlocking {
        val r = ds.submitReading(2.0)
        assertFalse(r.thresholdExceeded)
        assertNull(r.event)
    }

    @Test fun deepSleepCambiaEstadoASleepingYRestauraOnline() = runBlocking {
        assertEquals("SLEEPING", ds.setDeepSleep("d1", true).status)
        assertEquals("SLEEPING", ds.homeStatus().state)
        assertEquals("ONLINE", ds.setDeepSleep("d1", false).status)
    }

    @Test fun desbloquearPuertaDejaDoorLockedEnFalse() = runBlocking {
        assertFalse(ds.unlockDoor("d2").doorLocked)
    }

    @Test fun simulacroReportaTiemposYFalloDeChapa() = runBlocking {
        val r = ds.startDrill("DRILL")
        assertEquals("DRILL", r.type)
        r.results.filter { it.doorOpened }.forEach { assertTrue(it.responseMs < 5000) }
        assertFalse(r.results.first { it.deviceId == "d3" }.doorOpened)
    }

    @Test fun maximoCincoContactosDeEmergencia() = runBlocking {
        repeat(4) { ds.addContact(Contact(name = "C$it", phone = "+5198765432$it", priority = it + 2, notifySms = true)) }
        assertEquals(5, ds.contacts().size)
        val ex = runCatching { ds.addContact(Contact(name = "Extra", phone = "+51999999999", priority = 6, notifySms = false)) }
            .exceptionOrNull()
        assertTrue(ex is IllegalArgumentException)
    }

    @Test fun bateriaBajaSeCuentaBajo15() = runBlocking {
        assertEquals(1, ds.batterySummary().lowCount)
    }
}
