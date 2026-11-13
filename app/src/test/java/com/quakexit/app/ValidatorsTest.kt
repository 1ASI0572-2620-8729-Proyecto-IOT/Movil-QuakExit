package com.quakexit.app

import com.quakexit.app.util.Validators
import org.junit.Assert.*
import org.junit.Test

class ValidatorsTest {
    @Test fun passwordValida() = assertNull(Validators.password("Abcdefg1"))
    @Test fun passwordCortaRechazada() = assertNotNull(Validators.password("Ab1"))
    @Test fun passwordSinMayusculaRechazada() = assertNotNull(Validators.password("abcdefg1"))
    @Test fun passwordSinNumeroRechazada() = assertNotNull(Validators.password("Abcdefgh"))
    @Test fun macValida() = assertNull(Validators.mac("AA:BB:CC:00:00:01"))
    @Test fun macInvalida() = assertNotNull(Validators.mac("AABBCC000001"))
    @Test fun telefonoValido() = assertNull(Validators.phone("+51987654321"))
    @Test fun telefonoInvalido() = assertNotNull(Validators.phone("12ab"))
    @Test fun campoVacioEsObligatorio() = assertNotNull(Validators.notBlank("  ", "Alias"))
}
