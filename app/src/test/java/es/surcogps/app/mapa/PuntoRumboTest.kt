package es.surcogps.app.mapa

import es.surcogps.core.GeoMath
import es.surcogps.core.Enu
import es.surcogps.core.LatLon
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PuntoRumboTest {

    private val ancla = LatLon(41.0, -4.0)

    private fun alEste(m: Double) = GeoMath.fromEnu(Enu(m, 0.0), ancla)
    private fun alNorte(m: Double) = GeoMath.fromEnu(Enu(0.0, m), ancla)
    private fun alSur(m: Double) = GeoMath.fromEnu(Enu(0.0, -m), ancla)
    private fun alOeste(m: Double) = GeoMath.fromEnu(Enu(-m, 0.0), ancla)

    @Test
    fun rumbosCardinales() {
        assertEquals(0.0, rumboDesdeToque(ancla, alNorte(50.0))!!, 0.5)
        assertEquals(90.0, rumboDesdeToque(ancla, alEste(50.0))!!, 0.5)
        assertEquals(180.0, rumboDesdeToque(ancla, alSur(50.0))!!, 0.5)
        assertEquals(270.0, rumboDesdeToque(ancla, alOeste(50.0))!!, 0.5)
    }

    @Test
    fun demasiadoCercaEsNulo() {
        assertNull(rumboDesdeToque(ancla, ancla))
        assertNull(rumboDesdeToque(ancla, alEste(0.5)))
    }

    @Test
    fun firmaEstableYDistingue() {
        val f1 = firmaDibujo(10, ancla, 90.0, 6.0, 0.0, 1, null, null)
        assertEquals(f1, firmaDibujo(10, ancla, 90.0, 6.0, 0.0, 1, null, null))
        assertTrue(f1 != firmaDibujo(11, ancla, 90.0, 6.0, 0.0, 1, null, null))
        assertTrue(f1 != firmaDibujo(10, ancla, 91.0, 6.0, 0.0, 1, null, null))
        assertTrue(f1 != firmaDibujo(10, ancla, 90.0, 6.0, 0.0, 2, null, null))
        assertTrue(
            f1 != firmaDibujo(10, ancla, 90.0, 6.0, 0.0, 1, alEste(20.0), null)
        )
    }
}
