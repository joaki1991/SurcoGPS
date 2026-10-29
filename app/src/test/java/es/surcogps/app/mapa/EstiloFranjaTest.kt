package es.surcogps.app.mapa

import org.junit.Assert.assertTrue
import org.junit.Test

class EstiloFranjaTest {

    @Test
    fun elRellenoTrabajadoEsClaramenteOpaco() {
        // Antes 110/255: el verde se confundía con la imagen satélite.
        assertTrue(EstiloFranja.ALFA_RELLENO >= 200)
    }

    @Test
    fun elBordeSeDistingueDelRelleno() {
        assertTrue(EstiloFranja.ALFA_BORDE > EstiloFranja.ALFA_RELLENO)
        assertTrue(EstiloFranja.GROSOR_BORDE >= 2f)
    }
}
