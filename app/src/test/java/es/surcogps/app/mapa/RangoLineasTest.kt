package es.surcogps.app.mapa

import es.surcogps.core.GuidanceEngine
import es.surcogps.core.LatLon
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RangoLineasTest {

    private val ancla = LatLon(41.0, -4.0)

    private fun bboxMetro(mitadM: Double): BBox {
        // Caja cuadrada centrada en el ancla (aprox. equirectangular).
        val dLat = mitadM / 111_194.9
        val dLon = mitadM / (111_194.9 * kotlin.math.cos(Math.toRadians(41.0)))
        return BBox(41.0 + dLat, 41.0 - dLat, -4.0 + dLon, -4.0 - dLon)
    }

    @Test
    fun cajaCentradaIncluyeLinea0() {
        val g = GuidanceEngine(ancla, 0.0, 6.0)
        val r = indicesLineasVisibles(g, bboxMetro(50.0))
        assertTrue(r.contains(0))
        // 50 m / 6 m ≈ 8.3 → ±(8+1).
        assertEquals(-9..9, r)
    }

    @Test
    fun cajaDesplazadaSoloIndicesPositivos() {
        val g = GuidanceEngine(ancla, 0.0, 6.0)
        // Caja 100–200 m al este del ancla → líneas 16..34 aprox.
        val este100 = -4.0 + 100.0 / (111_194.9 * kotlin.math.cos(Math.toRadians(41.0)))
        val este200 = -4.0 + 200.0 / (111_194.9 * kotlin.math.cos(Math.toRadians(41.0)))
        val bbox = BBox(41.001, 40.999, este200, este100)
        val r = indicesLineasVisibles(g, bbox)
        assertTrue(r.first >= 14)
        assertTrue(r.last <= 36)
    }

    @Test
    fun rumboEsteCubreNorteSur() {
        val g = GuidanceEngine(ancla, 90.0, 4.0)
        val r = indicesLineasVisibles(g, bboxMetro(40.0))
        assertTrue(r.contains(0))
        assertEquals(-11..11, r) // 40/4 = 10 → ±(10+1).
    }

    @Test
    fun rangoAlongCubreLaCaja() {
        val g = GuidanceEngine(ancla, 0.0, 6.0)
        val (a0, a1) = rangoAlong(g, bboxMetro(50.0))
        assertTrue(a0 < -50.0 && a1 > 50.0)
    }
}
