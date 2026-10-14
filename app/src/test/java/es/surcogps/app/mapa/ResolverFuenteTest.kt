package es.surcogps.app.mapa

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ResolverFuenteTest {

    @Test
    fun hibridoComprobandoAvisa() {
        val e = resolverFuente(ModoMapa.HIBRIDO, EstadoRed.COMPROBANDO)
        assertEquals(BaseFuente.ESRI, e.base)
        assertTrue(e.conReferencia)
        assertTrue(e.aviso!!.contains("Comprobando"))
    }

    @Test
    fun hibridoOkSinAviso() {
        val e = resolverFuente(ModoMapa.HIBRIDO, EstadoRed.OK)
        assertEquals(BaseFuente.ESRI, e.base)
        assertTrue(e.conReferencia)
        assertNull(e.aviso)
    }

    @Test
    fun hibridoSinRedAvisaCalles() {
        val e = resolverFuente(ModoMapa.HIBRIDO, EstadoRed.FALLO)
        assertEquals(BaseFuente.ESRI, e.base) // Sin conmutación mágica.
        assertTrue(e.aviso!!.contains("Calles"))
    }

    @Test
    fun sateliteSinRedAvisa() {
        val e = resolverFuente(ModoMapa.SATELITE, EstadoRed.FALLO)
        assertEquals(BaseFuente.ESRI, e.base)
        assertTrue(e.aviso!!.contains("Calles"))
    }

    @Test
    fun callesNuncaAvisa() {
        for (red in listOf(null, EstadoRed.COMPROBANDO, EstadoRed.OK, EstadoRed.FALLO)) {
            val e = resolverFuente(ModoMapa.CALLES, red)
            assertEquals(BaseFuente.MAPNIK, e.base)
            assertNull(e.aviso)
        }
    }
}
