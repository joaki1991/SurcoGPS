package es.surcogps.app.mapa

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.osmdroid.util.MapTileIndex

/**
 * Verificación ejecutable del mapa híbrido (JVM local, sin emulador):
 * el punto delicado es el orden de teselas de Esri (z/y/x, invertido).
 */
class FuentesMapaTest {

    @Test
    fun imageryUsaOrdenEsri() {
        val src = FuentesMapa.esriImagery()
        val idx = MapTileIndex.getTileIndex(10, 100, 200)
        assertEquals(
            "https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/10/200/100",
            src.getTileURLString(idx)
        )
    }

    @Test
    fun referenciaUsaOrdenEsri() {
        val src = FuentesMapa.esriReference()
        val idx = MapTileIndex.getTileIndex(5, 10, 20)
        assertEquals(
            "https://server.arcgisonline.com/ArcGIS/rest/services/Reference/World_Boundaries_and_Places/MapServer/tile/5/20/10",
            src.getTileURLString(idx)
        )
    }

    @Test
    fun modosConfiguranBaseYCapa() {
        assertEquals(ConfigModo(BaseFuente.ESRI, true), configPara(ModoMapa.HIBRIDO))
        assertEquals(ConfigModo(BaseFuente.ESRI, false), configPara(ModoMapa.SATELITE))
        assertEquals(ConfigModo(BaseFuente.MAPNIK, false), configPara(ModoMapa.CALLES))
    }

    @Test
    fun atribucionMencionaFuentes() {
        assertTrue(ATRIBUCION.contains("OpenStreetMap"))
        assertTrue(ATRIBUCION.contains("Esri"))
        assertFalse(ATRIBUCION.isBlank())
    }
}
