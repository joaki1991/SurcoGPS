package es.surcogps.app.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class CardinalTest {

    @Test
    fun puntosCardinales() {
        assertEquals("N", cardinal(0.0))
        assertEquals("E", cardinal(90.0))
        assertEquals("S", cardinal(180.0))
        assertEquals("O", cardinal(270.0))
        assertEquals("N", cardinal(359.0))
    }

    @Test
    fun intermedios() {
        assertEquals("NE", cardinal(45.0))
        assertEquals("SE", cardinal(135.0))
        assertEquals("SO", cardinal(225.0))
        assertEquals("NO", cardinal(315.0))
    }
}
