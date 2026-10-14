package es.surcogps.app.mapa

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EncuadreTest {

    @Test
    fun primerFixValidoEncuadraUnaVez() {
        val e = EncuadreInicial()
        assertTrue(e.alFix(true, 5f, 10f))
        assertFalse(e.alFix(true, 5f, 10f))
        assertFalse(e.alFix(true, 4f, 10f))
    }

    @Test
    fun imprecisoORechazadoNoEncuadra() {
        val e = EncuadreInicial()
        assertFalse(e.alFix(false, 5f, 10f))
        assertFalse(e.alFix(true, 25f, 10f))
        assertFalse(e.alFix(true, Float.NaN, 10f))
        // …pero el siguiente válido sí.
        assertTrue(e.alFix(true, 8f, 10f))
    }

    @Test
    fun reiniciarPermiteReencuadrar() {
        val e = EncuadreInicial()
        assertTrue(e.alFix(true, 5f, 10f))
        e.reiniciar()
        assertTrue(e.alFix(true, 5f, 10f))
    }

    @Test
    fun zoomDetalleRespetaMaximoDelProveedor() {
        assertEquals(19.0, PoliticaEncuadre.zoomDetallePara(19.0), 1e-9)
        assertEquals(18.0, PoliticaEncuadre.zoomDetallePara(18.0), 1e-9)
        assertEquals(19.0, PoliticaEncuadre.zoomDetallePara(22.0), 1e-9)
    }
}
