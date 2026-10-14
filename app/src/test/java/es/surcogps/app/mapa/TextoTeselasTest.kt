package es.surcogps.app.mapa

import org.junit.Assert.assertEquals
import org.junit.Test

class TextoTeselasTest {

    @Test
    fun formatoCompacto() {
        assertEquals("▦ 24 ok · 0 fallo · HTTP ok", textoTeselas(StatsTeselas(24, 0, 0), true))
        assertEquals("▦ 0 ok · 9 fallo · HTTP fallo", textoTeselas(StatsTeselas(0, 9, 3), false))
        assertEquals("▦ 1 ok · 2 fallo · HTTP…", textoTeselas(StatsTeselas(1, 2, 0), null))
    }
}
