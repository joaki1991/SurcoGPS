package es.surcogps.app.mapa

import es.surcogps.core.GeoMath
import es.surcogps.core.Enu
import es.surcogps.core.LatLon
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PoliticaCamaraTest {

    private val ref = LatLon(41.0, -4.0)

    private fun alEste(metros: Double): LatLon =
        GeoMath.fromEnu(Enu(metros, 0.0), ref)

    @Test
    fun sinSeguirNuncaRecentra() {
        assertFalse(PoliticaCamara.debeRecenter(false, null, ref))
        assertFalse(PoliticaCamara.debeRecenter(false, ref, alEste(100.0)))
    }

    @Test
    fun primerCentroSiempreRecentra() {
        assertTrue(PoliticaCamara.debeRecenter(true, null, ref))
    }

    @Test
    fun umbralDe2m() {
        assertFalse(PoliticaCamara.debeRecenter(true, ref, alEste(1.0)))
        assertFalse(PoliticaCamara.debeRecenter(true, ref, alEste(1.9)))
        assertTrue(PoliticaCamara.debeRecenter(true, ref, alEste(2.1)))
        assertTrue(PoliticaCamara.debeRecenter(true, ref, alEste(50.0)))
    }

    @Test
    fun jitterNoRecentra() {
        // Deriva típica de GPS parado (< 2 m): la cámara queda quieta.
        assertFalse(PoliticaCamara.debeRecenter(true, ref, alEste(0.5)))
    }
}
