package es.surcogps.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CoreSmokeTest {

    @Test
    fun versionExpuesta() {
        assertEquals("1.0.0", SurcoCore.VERSION)
    }

    @Test
    fun normalizaRumbos() {
        assertEquals(0.0, SurcoCore.normalizeHeading(360.0))
        assertEquals(359.0, SurcoCore.normalizeHeading(-1.0))
        assertEquals(90.0, SurcoCore.normalizeHeading(90.0))
        assertTrue(SurcoCore.normalizeHeading(720.5) in 0.0..360.0)
    }
}
