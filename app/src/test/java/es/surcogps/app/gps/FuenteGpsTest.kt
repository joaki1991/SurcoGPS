package es.surcogps.app.gps

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FuenteGpsTest {

    @Test
    fun sinPlayServicesSeUsaElGpsDelDispositivo() {
        assertEquals(FuenteGps.PLATAFORMA, elegirFuenteGps(false))
    }

    @Test
    fun conPlayServicesOperativoSeUsaFused() {
        assertEquals(FuenteGps.FUSED, elegirFuenteGps(true))
    }

    @Test
    fun elRespaldoEsperaElUmbralCompleto() {
        assertFalse(debeActivarRespaldo(1_000L, 20_999L, false))
        assertTrue(debeActivarRespaldo(1_000L, 21_000L, false))
    }

    @Test
    fun elRespaldoNoSeReactivaSiYaEstaActivo() {
        assertFalse(debeActivarRespaldo(0L, 999_999L, true))
    }

    @Test
    fun etiquetasLegiblesParaDiagnostico() {
        assertTrue(etiquetaFuente(FuenteGps.FUSED).contains("Fused"))
        assertTrue(etiquetaFuente(FuenteGps.PLATAFORMA).contains("LocationManager"))
        assertEquals("sin iniciar", etiquetaFuente(null))
    }
}
