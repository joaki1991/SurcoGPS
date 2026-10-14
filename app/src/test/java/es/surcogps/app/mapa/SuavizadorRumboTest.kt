package es.surcogps.app.mapa

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SuavizadorRumboTest {

    @Test
    fun primerDatoPasaTalCual() {
        assertEquals(90.0, SuavizadorRumbo().actualizar(90.0, true)!!, 1e-9)
    }

    @Test
    fun paradoDevuelveNullYSinFlecha() {
        val s = SuavizadorRumbo()
        s.actualizar(90.0, true)
        assertNull(s.actualizar(90.0, false))
    }

    @Test
    fun sinDatoDevuelveNull() {
        assertNull(SuavizadorRumbo().actualizar(null, true))
    }

    @Test
    fun convergeAlRumboConstante() {
        val s = SuavizadorRumbo()
        var r = 0.0
        repeat(30) { r = s.actualizar(180.0, true)!! }
        assertEquals(180.0, r, 0.5)
    }

    @Test
    fun pasoPorNorteSinSalto() {
        // Secuencia 350 → 0 → 10: el suavizado debe cruzar el norte, no saltar a 180.
        val s = SuavizadorRumbo()
        s.actualizar(350.0, true)
        s.actualizar(0.0, true)
        val r = s.actualizar(10.0, true)!!
        assertTrue("rumbo=$r", r < 45.0 || r > 315.0)
    }

    @Test
    fun saltoBruscoSeAtenua() {
        val s = SuavizadorRumbo()
        s.actualizar(0.0, true)
        val r = s.actualizar(90.0, true)!!
        // Con alfa 0.35 un solo salto no llega ni a la mitad.
        assertTrue("rumbo=$r", r in 0.0..45.0)
    }

    @Test
    fun trasParadaNoHaySaltoFantasma() {
        val s = SuavizadorRumbo()
        s.actualizar(90.0, true)
        s.actualizar(90.0, false)
        // Al reanudar con otro rumbo, parte de ese dato (sin memoria vieja).
        assertEquals(270.0, s.actualizar(270.0, true)!!, 1e-9)
    }
}
