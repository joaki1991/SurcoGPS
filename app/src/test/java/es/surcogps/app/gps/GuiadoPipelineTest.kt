package es.surcogps.app.gps

import es.surcogps.core.Fix
import es.surcogps.core.GeoMath
import es.surcogps.core.GuidanceEngine
import es.surcogps.core.ImplementOffset
import es.surcogps.core.LatLon
import es.surcogps.core.RejectReason
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Verificación del pipeline con ubicaciones simuladas (sustituto ejecutable de la
 * prueba con mock locations en dispositivo, pendiente de Sprint 12).
 */
class GuiadoPipelineTest {

    private val ancla = LatLon(41.0, -4.0)
    private val t0 = 1_700_000_000_000L
    private val guia = GuidanceEngine(ancla, 0.0, 6.0)
    private val sinOffset = ImplementOffset()

    private fun fixEn(esteM: Double, norteM: Double, acc: Float = 5f, dtMs: Long = 0): Fix {
        val p = GeoMath.fromEnu(es.surcogps.core.Enu(esteM, norteM), ancla)
        return Fix(p, acc, timeMs = t0 + dtMs)
    }

    @Test
    fun sinFixInicialVacio() {
        val e = GuiadoPipeline().procesar(fixEn(0.0, 0.0), t0, null, sinOffset)
        assertTrue(e.aceptado)
        assertNull(e.xteM) // Sin guía no hay XTE.
    }

    @Test
    fun avanceRectoMantieneLinea0() {
        val p = GuiadoPipeline()
        var e = EstadoGuiado()
        for (d in longArrayOf(0, 1000, 2000, 3000)) {
            e = p.procesar(fixEn(0.0, d / 100.0 * 10), t0, guia, sinOffset)
        }
        assertTrue(e.aceptado)
        assertEquals(0, e.linea)
        assertTrue(kotlin.math.abs(e.xteM!!) < 0.2)
        assertEquals(0.0, e.rumboVehiculoDeg!!, 5.0)
    }

    @Test
    fun derivaLateralCambiaDeLinea() {
        val p = GuiadoPipeline()
        // Camina 100 m al norte pero 7 m al este → línea 1.
        var e = p.procesar(fixEn(0.0, 0.0), t0, guia, sinOffset)
        e = p.procesar(fixEn(7.0, 100.0, dtMs = 30_000), t0 + 30_000, guia, sinOffset)
        assertEquals(1, e.linea)
        assertTrue(e.xteM!! > 0.5 && e.xteM!! < 1.5)
    }

    @Test
    fun rechazoConservaUltimoValido() {
        val p = GuiadoPipeline()
        val bueno = p.procesar(fixEn(0.0, 0.0), t0, guia, sinOffset)
        assertTrue(bueno.aceptado)
        val malo = p.procesar(fixEn(0.0, 5.0, acc = 40f, dtMs = 1000), t0 + 1000, guia, sinOffset)
        assertFalse(malo.aceptado)
        assertEquals(RejectReason.POOR_ACCURACY, malo.motivoRechazo)
        // El XTE conservado es el del último válido.
        assertEquals(bueno.xteM!!, malo.xteM!!, 1e-9)
    }

    @Test
    fun offsetMueveLaPosicionDelApero() {
        val p = GuiadoPipeline()
        val e = p.procesar(fixEn(0.0, 0.0), t0, guia, ImplementOffset(lateralM = 2.0))
        assertTrue(e.aceptado)
        assertTrue(e.xteM!! in 1.9..2.1)
    }

    @Test
    fun reiniciarLimpiaEstado() {
        val p = GuiadoPipeline()
        p.procesar(fixEn(0.0, 0.0), t0, guia, sinOffset)
        p.reiniciar()
        val e = p.procesar(fixEn(50.0, 0.0), t0 + 1000, guia, sinOffset)
        // Tras reiniciar no hay salto (el filtro olvidó el anterior).
        assertTrue(e.aceptado)
    }
}
