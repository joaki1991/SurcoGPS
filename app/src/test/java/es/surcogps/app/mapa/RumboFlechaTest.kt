package es.surcogps.app.mapa

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RumboFlechaTest {

    @Test
    fun brujulaMandaAunqueEsteParado() {
        assertEquals(
            123.0,
            RumboFlecha.resolver(123.0, 5.0, 0f)!!,
            1e-9
        )
    }

    @Test
    fun sinBrujulaUsaRumboGpsConMarcha() {
        assertEquals(
            87.0,
            RumboFlecha.resolver(null, 87.0, 1.5f)!!,
            1e-9
        )
    }

    @Test
    fun paradoSinBrujulaNoInventaOrientacion() {
        assertNull(RumboFlecha.resolver(null, 87.0, 0.2f))
        assertNull(RumboFlecha.resolver(null, 87.0, null))
    }

    @Test
    fun sinDatosDevuelveNull() {
        assertNull(RumboFlecha.resolver(null, null, 2f))
    }

    @Test
    fun normalizaRumbosFueraDeRango() {
        assertEquals(10.0, RumboFlecha.resolver(370.0, null, null)!!, 1e-9)
        assertEquals(350.0, RumboFlecha.resolver(-10.0, null, null)!!, 1e-9)
    }

    @Test
    fun brujulaNoFinitaSeIgnora() {
        assertEquals(
            45.0,
            RumboFlecha.resolver(Double.NaN, 45.0, 1.0f)!!,
            1e-9
        )
    }
}
