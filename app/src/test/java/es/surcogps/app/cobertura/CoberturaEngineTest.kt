package es.surcogps.app.cobertura

import es.surcogps.core.GeoMath
import es.surcogps.core.Enu
import es.surcogps.core.LatLon
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Motor de registro en memoria verificado con recorridos simulados. */
class CoberturaEngineTest {

    private val ancla = LatLon(41.0, -4.0)

    private fun punto(este: Double, norte: Double): LatLon =
        GeoMath.fromEnu(Enu(este, norte), ancla)

    @Test
    fun avanceRegistraAreaYTramos() {
        val e = CoberturaEngine(ancla, 6.0)
        var nuevas = 0
        for (d in 0..100 step 10) {
            nuevas += e.registrar(punto(0.0, d.toDouble()))
        }
        assertTrue(nuevas > 0)
        assertTrue("area=${e.areaM2}", e.areaM2 in 600.0..780.0)
        assertEquals(10, e.tramos.size) // 11 puntos → 10 tramos.
    }

    @Test
    fun quietoNoColorea() {
        // Acercarse a la guía sin moverse: 5 veces el mismo punto → 0 tramos.
        val e = CoberturaEngine(ancla, 6.0)
        repeat(5) { e.registrar(punto(3.0, 50.0)) }
        assertEquals(0, e.tramos.size)
        assertEquals(0.0, e.areaM2, 1e-9)
    }

    @Test
    fun pausaNoRegistra() {
        val e = CoberturaEngine(ancla, 6.0)
        e.registrar(punto(0.0, 0.0))
        e.registrar(punto(0.0, 10.0))
        val area = e.areaM2
        e.pausar()
        assertTrue(e.estaPausado)
        assertEquals(0, e.registrar(punto(0.0, 20.0)))
        assertEquals(area, e.areaM2, 1e-9)
        e.reanudar()
        // Tras reanudar no hay tramo fantasma con el punto de la pausa.
        assertEquals(0, e.registrar(punto(0.0, 30.0)))
        assertEquals(area, e.areaM2, 1e-9)
    }

    @Test
    fun repetirNoDuplica() {
        val e = CoberturaEngine(ancla, 6.0)
        e.registrar(punto(0.0, 0.0))
        e.registrar(punto(0.0, 50.0))
        val area = e.areaM2
        // Vuelta atrás por el mismo sitio.
        e.registrar(punto(0.0, 0.0))
        assertEquals(area, e.areaM2, 1e-9)
    }

    @Test
    fun reiniciarLimpia() {
        val e = CoberturaEngine(ancla, 6.0)
        e.registrar(punto(0.0, 0.0))
        e.registrar(punto(0.0, 10.0))
        assertTrue(e.areaM2 > 0)
        e.reiniciar()
        assertEquals(0.0, e.areaM2, 1e-9)
        assertTrue(e.tramos.isEmpty())
    }

    @Test
    fun haCoincideConCore() {
        val e = CoberturaEngine(ancla, 6.0)
        e.registrar(punto(0.0, 0.0))
        e.registrar(punto(0.0, 100.0))
        assertEquals(e.areaM2 / 10_000.0, e.areaHa, 1e-12)
    }
}
