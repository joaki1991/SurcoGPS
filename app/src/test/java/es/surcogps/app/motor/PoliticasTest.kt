package es.surcogps.app.motor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PoliticasTest {

    @Test
    fun reanudaSoloConFlagYPermiso() {
        assertTrue(Politicas.debeReanudarServicio(true, true))
        assertFalse(Politicas.debeReanudarServicio(true, false))
        assertFalse(Politicas.debeReanudarServicio(false, true))
        assertFalse(Politicas.debeReanudarServicio(false, false))
    }

    @Test
    fun textoNotificacionConGuia() {
        assertEquals(
            "SurcoGPS · 1.23 ha · L2 +1.5 m →",
            Politicas.textoNotificacion(1.234, 1.5, 2)
        )
        assertEquals(
            "SurcoGPS · 0.00 ha · L0 -0.5 m ←",
            Politicas.textoNotificacion(0.0, -0.5, 0)
        )
        assertEquals(
            "SurcoGPS · 3.00 ha · L1 +0.0 m ✓",
            Politicas.textoNotificacion(3.0, 0.04, 1)
        )
    }

    @Test
    fun textoNotificacionSinGuia() {
        assertEquals(
            "SurcoGPS · 0.50 ha · sin guía",
            Politicas.textoNotificacion(0.5, null, null)
        )
    }
}
