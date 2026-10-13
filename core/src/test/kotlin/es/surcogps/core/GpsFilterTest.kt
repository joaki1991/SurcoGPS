package es.surcogps.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GpsFilterTest {

    private val ancla = LatLon(41.0, -4.0)
    private val t0 = 1_700_000_000_000L

    private fun fix(lat: Double = 41.0, lon: Double = -4.0, acc: Float = 5f, dtMs: Long = 0) =
        Fix(LatLon(lat, lon), acc, timeMs = t0 + dtMs)

    @Test
    fun aceptaFixBueno() {
        val f = GpsFilter()
        val v = f.filter(fix(), t0)
        assertTrue(v is GpsFilter.Verdict.Accepted)
    }

    @Test
    fun rechazaImprecision() {
        val f = GpsFilter()
        val v = f.filter(fix(acc = 25f), t0)
        assertTrue(v is GpsFilter.Verdict.Rejected && v.reason == RejectReason.POOR_ACCURACY)
    }

    @Test
    fun aceptaEnElBordeDePuerta() {
        val f = GpsFilter()
        val v = f.filter(fix(acc = 10f), t0)
        assertTrue(v is GpsFilter.Verdict.Accepted)
    }

    @Test
    fun rechazaPrecisionNoFinita() {
        val f = GpsFilter()
        assertTrue(f.filter(fix(acc = Float.NaN), t0) is GpsFilter.Verdict.Rejected)
        f.reset()
        assertTrue(f.filter(fix(acc = -1f), t0) is GpsFilter.Verdict.Rejected)
    }

    @Test
    fun rechazaSaltoImposible() {
        val f = GpsFilter()
        assertTrue(f.filter(fix(), t0) is GpsFilter.Verdict.Accepted)
        // 500 m en 1 s = 500 m/s > puerta 50 m/s.
        val lejos = GeoMath.destination(ancla, 90.0, 500.0)
        val v = f.filter(fix(lejos.lat, lejos.lon, dtMs = 1000), t0 + 1000)
        assertTrue(v is GpsFilter.Verdict.Rejected && v.reason == RejectReason.JUMP)
    }

    @Test
    fun aceptaVelocidadDeTrabajo() {
        val f = GpsFilter()
        assertTrue(f.filter(fix(), t0) is GpsFilter.Verdict.Accepted)
        // 10 m en 1 s = tractor a 36 km/h (rápido pero posible).
        val p = GeoMath.destination(ancla, 0.0, 10.0)
        val v = f.filter(fix(p.lat, p.lon, dtMs = 1000), t0 + 1000)
        assertTrue(v is GpsFilter.Verdict.Accepted)
    }

    @Test
    fun primerFixSinSalto() {
        val f = GpsFilter()
        val lejos = GeoMath.destination(ancla, 90.0, 5000.0)
        assertTrue(f.filter(fix(lejos.lat, lejos.lon), t0) is GpsFilter.Verdict.Accepted)
    }

    @Test
    fun rechazaFixCaducado() {
        val f = GpsFilter()
        val v = f.filter(fix(dtMs = -30_000), t0)
        assertTrue(v is GpsFilter.Verdict.Rejected && v.reason == RejectReason.STALE)
    }

    @Test
    fun resetPermiteReposicionar() {
        val f = GpsFilter()
        assertTrue(f.filter(fix(), t0) is GpsFilter.Verdict.Accepted)
        f.reset()
        val lejos = GeoMath.destination(ancla, 90.0, 5000.0)
        assertTrue(f.filter(fix(lejos.lat, lejos.lon, dtMs = 1000), t0 + 1000)
            is GpsFilter.Verdict.Accepted)
    }

    @Test
    fun puertaConfigurable() {
        val f = GpsFilter(FilterConfig(maxAccuracyM = 20f))
        assertTrue(f.filter(fix(acc = 15f), t0) is GpsFilter.Verdict.Accepted)
        f.config = FilterConfig(maxAccuracyM = 10f)
        f.reset()
        val v = f.filter(fix(acc = 15f), t0)
        assertTrue(v is GpsFilter.Verdict.Rejected)
    }

    @Test
    fun saltoNoAvanzaElUltimo() {
        val f = GpsFilter()
        f.filter(fix(), t0)
        val lejos = GeoMath.destination(ancla, 90.0, 500.0)
        f.filter(fix(lejos.lat, lejos.lon, dtMs = 1000), t0 + 1000) // rechazado
        // Un fix coherente con el ÚLTIMO ACEPTADO sigue pasando.
        val cerca = GeoMath.destination(ancla, 0.0, 5.0)
        val v = f.filter(fix(cerca.lat, cerca.lon, dtMs = 2000), t0 + 2000)
        assertTrue(v is GpsFilter.Verdict.Accepted, "rechazado tras salto: $v")
    }
}

class ImplementOffsetTest {

    private val ancla = LatLon(41.0, -4.0)

    private fun deltaMeters(from: LatLon, to: LatLon): Enu = GeoMath.toEnu(to, from)

    @Test
    fun offsetCeroDevuelveAntena() {
        val o = ImplementOffset()
        assertEquals(ancla, o.apply(ancla, 0.0, ancla))
    }

    @Test
    fun lateralDerechaRumboNorteEsEste() {
        val o = ImplementOffset(lateralM = 2.0)
        val d = deltaMeters(ancla, o.apply(ancla, 0.0, ancla))
        assertTrue(d.east in 1.9..2.1 && d.north in -0.1..0.1, "d=$d")
    }

    @Test
    fun lateralDerechaRumboEsteEsSur() {
        val o = ImplementOffset(lateralM = 2.0)
        val d = deltaMeters(ancla, o.apply(ancla, 90.0, ancla))
        assertTrue(d.north in -2.1..-1.9 && d.east in -0.1..0.1, "d=$d")
    }

    @Test
    fun lateralDerechaRumboSurEsOeste() {
        val o = ImplementOffset(lateralM = 2.0)
        val d = deltaMeters(ancla, o.apply(ancla, 180.0, ancla))
        assertTrue(d.east in -2.1..-1.9, "d=$d")
    }

    @Test
    fun aperoArrastradoDetras() {
        val o = ImplementOffset(longitudinalM = -2.0)
        val d = deltaMeters(ancla, o.apply(ancla, 0.0, ancla))
        assertTrue(d.north in -2.1..-1.9 && d.east in -0.1..0.1, "d=$d")
    }

    @Test
    fun aperoDelanteRumboOeste() {
        val o = ImplementOffset(longitudinalM = 3.0)
        val d = deltaMeters(ancla, o.apply(ancla, 270.0, ancla))
        assertTrue(d.east in -3.1..-2.9, "d=$d")
    }

    @Test
    fun offsetDesplazaXteEnGuia() {
        // Guía rumbo norte, ancho 6. Antena 0.5 m al este del ancla; apero 2 m a la
        // derecha → XTE del apero ≈ 2.5 m (línea 0, inequívoca).
        val g = GuidanceEngine(ancla, 0.0, 6.0)
        val antena = GeoMath.destination(ancla, 90.0, 0.5)
        val apero = ImplementOffset(lateralM = 2.0).apply(antena, 0.0, ancla)
        assertEquals(0, g.nearestLineIndex(apero))
        val xte = g.crossTrackError(apero)
        assertTrue(xte in 2.3..2.7, "xte=$xte")
    }
}
