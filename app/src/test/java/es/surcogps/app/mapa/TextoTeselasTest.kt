package es.surcogps.app.mapa

import org.junit.Assert.assertEquals
import org.junit.Test

class TextoTeselasTest {

    @Test
    fun formatoCompacto() {
        assertEquals("▦ 24 ok · 0 fallo", textoTeselas(StatsTeselas(24, 0, 0)))
        assertEquals("▦ 0 ok · 9 fallo", textoTeselas(StatsTeselas(0, 9, 3)))
    }
}
