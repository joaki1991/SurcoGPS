package es.surcogps.app.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PasosAjusteTest {

    @Test
    fun elAnchoTiene294PasosIntermedios() {
        assertEquals(
            294,
            PasosAjuste.pasosSlider(
                PasosAjuste.ANCHO_MIN_M,
                PasosAjuste.ANCHO_MAX_M,
                PasosAjuste.ANCHO_PASO_M
            )
        )
    }

    @Test
    fun elRangoCompletoSon295PosicionesDeCeroComaUno() {
        val intervalos =
            ((PasosAjuste.ANCHO_MAX_M - PasosAjuste.ANCHO_MIN_M) / PasosAjuste.ANCHO_PASO_M)
        assertEquals(295.0, intervalos.toDouble(), 1e-3)
    }

    @Test
    fun redondeaAlDecimoMasCercano() {
        assertEquals(12.3, PasosAjuste.redondearAncho(12.34), 1e-9)
        assertEquals(12.4, PasosAjuste.redondearAncho(12.36), 1e-9)
        assertEquals(29.9, PasosAjuste.redondearAncho(29.94), 1e-9)
    }

    @Test
    fun respetaLosLimitesDePersistencia() {
        assertEquals(0.5, PasosAjuste.redondearAncho(0.45), 1e-9)
        assertEquals(0.5, PasosAjuste.redondearAncho(0.0), 1e-9)
        assertEquals(30.0, PasosAjuste.redondearAncho(30.04), 1e-9)
        assertEquals(30.0, PasosAjuste.redondearAncho(99.0), 1e-9)
    }

    @Test
    fun unValorNoFinitoNoRompeElGuardado() {
        val r = PasosAjuste.redondearAncho(Double.NaN)
        assertEquals(0.5, r, 1e-9)
        assertTrue(r in 0.5..30.0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun unPasoInvalidoSeRechaza() {
        PasosAjuste.pasosSlider(0.5f, 30f, 0f)
    }
}
