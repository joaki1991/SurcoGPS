package es.surcogps.app.mapa

import es.surcogps.core.Enu
import es.surcogps.core.GeoMath
import es.surcogps.core.GuidanceEngine
import es.surcogps.core.LatLon
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class LineasSegurasTest {

    private val ancla = LatLon(41.0, -4.0)

    private fun punto(este: Double, norte: Double): LatLon =
        GeoMath.fromEnu(Enu(este, norte), ancla)

    /** Caja cuadrada de [mitadM] metros alrededor de [centro]. */
    private fun bbox(centro: LatLon, mitadM: Double): BBox {
        val dLat = mitadM / 111_194.9
        val dLon = mitadM / (111_194.9 * kotlin.math.cos(Math.toRadians(centro.lat)))
        return BBox(
            centro.lat + dLat, centro.lat - dLat,
            centro.lon + dLon, centro.lon - dLon
        )
    }

    @Test
    fun topeMaximoDeLineasPorFotograma() {
        // Apero mínimo sobre una vista enorme: sin tope serían ~40 000 líneas.
        val g = GuidanceEngine(ancla, 0.0, 0.5)
        val centro = punto(0.0, 0.0)
        val r = indicesLineasSeguras(g, bbox(centro, 20_000.0), centro)
        assertTrue("rango=$r", r.last - r.first + 1 <= MAX_LINEAS_DIBUJO)
    }

    @Test
    fun radioMaximoDeDiezKilometros() {
        val g = GuidanceEngine(ancla, 0.0, 6.0)
        val centro = punto(0.0, 0.0)
        val r = indicesLineasSeguras(g, bbox(centro, 50_000.0), centro)
        // 10 km / 6 m ≈ 1666 índices por lado, pero además tope de 200.
        assertTrue(abs(r.first) <= 100)
        assertTrue(abs(r.last) <= 100)
    }

    @Test
    fun centroDesplazadoNoVuelveAlAncla() {
        val g = GuidanceEngine(ancla, 0.0, 6.0)
        val centro = punto(600.0, 0.0) // 100 líneas al este del ancla.
        val r = indicesLineasSeguras(g, bbox(centro, 30.0), centro)
        assertTrue("rango=$r", r.contains(100))
        // La familia empieza cerca de la línea 100, no en la 0 del ancla.
        assertTrue("rango=$r", r.first > 50)
    }

    @Test
    fun rangoLongitudinalNoSuperaElRadio() {
        val g = GuidanceEngine(ancla, 0.0, 6.0)
        val centro = punto(0.0, 0.0)
        val (a0, a1) = rangoAlongSeguro(g, bbox(centro, 50_000.0), centro)
        assertTrue(a1 - a0 <= 2 * RADIO_MAX_LINEAS_M + 1.0)
        assertTrue(a0 >= -RADIO_MAX_LINEAS_M - 1.0)
        assertTrue(a1 <= RADIO_MAX_LINEAS_M + 1.0)
    }

    @Test
    fun vistaPequenaConservaComportamientoVisible() {
        val g = GuidanceEngine(ancla, 0.0, 6.0)
        val centro = punto(0.0, 0.0)
        val caja = bbox(centro, 50.0)
        val todas = indicesLineasVisibles(g, caja)
        val seguras = indicesLineasSeguras(g, caja, centro)
        assertEquals(todas, seguras)
    }
}
