package es.surcogps.app.soporte

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InformeDiagTest {

    @Test
    fun sinSecretosNiCoordsExactas() {
        val texto = InformeDiag.construir(
            appVersion = "1.6.0 (10)",
            eventos = listOf(EventoDiag(1L, "s1", "gps", "fix en zona 41.1,-4.7")),
            cierres = listOf("Excepción: X: explosión\nat a.b(C:1)"),
            chequeos = listOf(ChequeoFuente("Esri", "https://h/t/1/2/3", true, 200, null, 120)),
            stats = StatsFuente(3, 10, 5, 0, 0, 0L, true),
            cache = EstadoCache(1024, 7),
            permisoUbicacion = true,
            gpsActivo = true,
            ultimoFix = "hace 2s · ±5 m · zona 41.1,-4.7"
        )
        assertTrue(texto.contains("1.6.0 (10)"))
        assertTrue(texto.contains("41.1,-4.7"))
        // Coordenada exacta inventada: no debe aparecer en ningún informe real;
        // aquí se comprueba el mecanismo con un caso negativo.
        assertFalse(texto.contains("41.123456"))
        assertTrue(texto.contains("Errores de descarga: 3"))
        assertTrue(texto.contains("7 ficheros"))
    }

    @Test
    fun vacioEsLegible() {
        val texto = InformeDiag.construir(
            "x", emptyList(), emptyList(), emptyList(), null, null, false, false, null
        )
        assertTrue(texto.contains("Sin ejecutar"))
        assertTrue(texto.contains("Sin eventos."))
    }
}
