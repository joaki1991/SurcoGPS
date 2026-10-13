package es.surcogps.core

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class GuidanceEngineTest {

    private val ancla = LatLon(41.0, -4.0)

    @Test
    fun anclaTieneXteCeroRumboNorte() {
        val g = GuidanceEngine(ancla, 0.0, 6.0)
        assertEquals(0, g.nearestLineIndex(ancla))
        assertEquals(0.0, g.crossTrackError(ancla), 1e-9)
    }

    @Test
    fun esteDeAnclaEsLinea1ConRumboNorte() {
        val g = GuidanceEngine(ancla, 0.0, 6.0)
        val este = GeoMath.destination(ancla, 90.0, 6.0)
        assertEquals(1, g.nearestLineIndex(este))
        assertTrue(abs(g.crossTrackError(este)) < 0.05, "xte=${g.crossTrackError(este)}")
    }

    @Test
    fun oesteDeAnclaEsLineaMenos1() {
        val g = GuidanceEngine(ancla, 0.0, 6.0)
        val oeste = GeoMath.destination(ancla, 270.0, 6.0)
        assertEquals(-1, g.nearestLineIndex(oeste))
        assertTrue(g.crossTrackError(oeste, -1) in -0.05..0.05)
    }

    @Test
    fun signoXteDerechaPositivoRumboEste() {
        // Rumbo 90 (E): la derecha es el sur → un punto al sur da XTE > 0.
        val g = GuidanceEngine(ancla, 90.0, 4.0)
        val sur = GeoMath.destination(ancla, 180.0, 1.0)
        val norte = GeoMath.destination(ancla, 0.0, 1.0)
        assertTrue(g.crossTrackError(sur) > 0, "xteSur=${g.crossTrackError(sur)}")
        assertTrue(g.crossTrackError(norte) < 0, "xteNorte=${g.crossTrackError(norte)}")
    }

    @Test
    fun separacionMetricaExacta() {
        val g = GuidanceEngine(ancla, 0.0, 3.5)
        val (a, b) = g.lineSegment(0, 0.0, 200.0)
        val (c, d) = g.lineSegment(3, 0.0, 200.0)
        val medioA = GeoMath.toEnu(LatLon((a.lat + b.lat) / 2, (a.lon + b.lon) / 2), ancla)
        val medioC = GeoMath.toEnu(LatLon((c.lat + d.lat) / 2, (c.lon + d.lon) / 2), ancla)
        val dist = kotlin.math.hypot(medioC.east - medioA.east, medioC.north - medioA.north)
        assertTrue(abs(dist - 10.5) < 0.01, "dist=$dist")
        assertEquals(10.5, GuidanceEngine.separation(0, 3, 3.5), 1e-9)
    }

    @Test
    fun rumbo359CasiNorte() {
        val g = GuidanceEngine(ancla, 359.0, 6.0)
        val norteLejos = GeoMath.destination(ancla, 0.0, 500.0)
        // 500 m al norte con rumbo 359 → deriva lateral ≈ 500·sin(1°) ≈ 8.7 m → línea 1.
        assertEquals(1, g.nearestLineIndex(norteLejos))
    }

    @Test
    fun shiftLateralDesplazaLineas() {
        val g = GuidanceEngine(ancla, 0.0, 6.0, lateralShiftM = 3.0)
        // El ancla queda a -3 m de la línea 0 (a mitad de calle).
        assertTrue(abs(g.crossTrackError(ancla) + 3.0) < 0.05)
        val conShift = g.withShift(0.0)
        assertEquals(0.0, conShift.crossTrackError(ancla), 0.05)
    }

    @Test
    fun reRumboMantieneAncla() {
        val g = GuidanceEngine(ancla, 0.0, 6.0).withHeading(90.0)
        assertEquals(90.0, g.headingDeg)
        assertEquals(0, g.nearestLineIndex(ancla))
    }

    @Test
    fun anchosExtremosValidos() {
        GuidanceEngine(ancla, 0.0, 0.5)
        GuidanceEngine(ancla, 0.0, 30.0)
    }

    @Test
    fun anchosFueraDeRangoFallan() {
        assertFailsWith<IllegalArgumentException> { GuidanceEngine(ancla, 0.0, 0.49) }
        assertFailsWith<IllegalArgumentException> { GuidanceEngine(ancla, 0.0, 30.01) }
        assertFailsWith<IllegalArgumentException> { GuidanceEngine(ancla, 0.0, 0.0) }
    }

    @Test
    fun rumboSeNormaliza() {
        assertEquals(0.0, GuidanceEngine(ancla, 360.0, 6.0).headingDeg)
        assertEquals(270.0, GuidanceEngine(ancla, -90.0, 6.0).headingDeg)
    }

    @Test
    fun segmentoInvertidoFalla() {
        val g = GuidanceEngine(ancla, 0.0, 6.0)
        assertFailsWith<IllegalArgumentException> { g.lineSegment(0, 10.0, -10.0) }
    }

    @Test
    fun lineaSurConRumbo180() {
        // Rumbo 180 (S): avanzar al sur mantiene XTE 0.
        val g = GuidanceEngine(ancla, 180.0, 6.0)
        val sur = GeoMath.destination(ancla, 180.0, 100.0)
        assertEquals(0, g.nearestLineIndex(sur))
        assertTrue(abs(g.crossTrackError(sur)) < 0.05)
        // Un punto al oeste queda a la derecha del rumbo sur → XTE > 0.
        val oeste = GeoMath.destination(ancla, 270.0, 3.0)
        assertTrue(g.crossTrackError(oeste) > 0)
    }
}
