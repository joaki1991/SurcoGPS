package es.surcogps.core

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GeoMathTest {

    private val ref = LatLon(41.0, -4.0)

    @Test
    fun anclaEsOrigenEnu() {
        val e = GeoMath.toEnu(ref, ref)
        assertEquals(0.0, e.east, 1e-9)
        assertEquals(0.0, e.north, 1e-9)
    }

    @Test
    fun gradoLatitudSon111km() {
        val p = LatLon(42.0, -4.0)
        val e = GeoMath.toEnu(p, ref)
        assertEquals(0.0, e.east, 1e-6)
        assertTrue(abs(e.north - 111_194.9) < 500.0, "north=${e.north}")
    }

    @Test
    fun longitudEscalaConCoseno() {
        val p = LatLon(41.0, -3.0)
        val e = GeoMath.toEnu(p, ref)
        val esperado = 111_194.9 * kotlin.math.cos(Math.toRadians(41.0))
        assertTrue(abs(e.east - esperado) < 500.0, "east=${e.east} esperado=$esperado")
        assertEquals(0.0, e.north, 1e-6)
    }

    @Test
    fun roundTripEnu() {
        val p = LatLon(41.1234, -4.5678)
        val back = GeoMath.fromEnu(GeoMath.toEnu(p, ref), ref)
        assertTrue(abs(back.lat - p.lat) < 1e-9)
        assertTrue(abs(back.lon - p.lon) < 1e-9)
    }

    @Test
    fun haversineCero() {
        assertEquals(0.0, GeoMath.haversine(ref, ref), 1e-9)
    }

    @Test
    fun rumbosCardinales() {
        val n = LatLon(42.0, -4.0)
        val e = LatLon(41.0, -3.0)
        val s = LatLon(40.0, -4.0)
        val o = LatLon(41.0, -5.0)
        assertEquals(0.0, GeoMath.initialBearing(ref, n), 0.5)
        assertEquals(90.0, GeoMath.initialBearing(ref, e), 0.5)
        assertEquals(180.0, GeoMath.initialBearing(ref, s), 0.5)
        assertEquals(270.0, GeoMath.initialBearing(ref, o), 0.5)
    }

    @Test
    fun destino100mNorte() {
        val d = GeoMath.destination(ref, 90.0, 0.0)
        assertEquals(ref.lat, d.lat, 1e-12)
        val norte = GeoMath.destination(ref, 0.0, 100.0)
        val dist = GeoMath.haversine(ref, norte)
        assertTrue(abs(dist - 100.0) < 0.5, "dist=$dist")
        assertTrue(norte.lat > ref.lat)
        assertEquals(ref.lon, norte.lon, 1e-9)
    }

    @Test
    fun destinoEsteAumentaLon() {
        val este = GeoMath.destination(ref, 90.0, 100.0)
        assertTrue(este.lon > ref.lon)
        assertTrue(abs(GeoMath.haversine(ref, este) - 100.0) < 0.5)
    }
}
