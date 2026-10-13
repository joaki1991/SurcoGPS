package es.surcogps.core

import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ParcelaStoreTest {

    private fun tmpDir(): File = Files.createTempDirectory("surco-parcela").toFile()

    private fun parcela(id: String = "p1"): Parcela = Parcela(
        id = id,
        nombre = "El \"Olivar\" \\ norte",
        ancla = LatLon(41.0, -4.0),
        rumbo = 90.0,
        ancho = 6.0,
        creadaMs = 1000L
    )

    @Test
    fun guardarYListar() {
        val s = FileParcelaStore(tmpDir())
        assertTrue(s.listar().isEmpty())
        s.guardar(parcela())
        s.guardar(parcela("p2").copy(nombre = "Otra", creadaMs = 2000L))
        val lista = s.listar()
        assertEquals(2, lista.size)
        assertEquals("p2", lista[0].id) // Más reciente primero.
    }

    @Test
    fun roundTripConNombreDificil() {
        val s = FileParcelaStore(tmpDir())
        s.guardar(parcela())
        assertEquals(parcela(), assertNotNull(s.porId("p1")))
    }

    @Test
    fun rumboSeNormaliza() {
        val s = FileParcelaStore(tmpDir())
        s.guardar(parcela().copy(rumbo = 370.0))
        assertEquals(10.0, assertNotNull(s.porId("p1")).rumbo)
    }

    @Test
    fun anchoInvalidoFalla() {
        val s = FileParcelaStore(tmpDir())
        var fallo = false
        try {
            s.guardar(parcela().copy(ancho = 0.1))
        } catch (_: IllegalArgumentException) {
            fallo = true
        }
        assertTrue(fallo)
    }

    @Test
    fun borrar() {
        val s = FileParcelaStore(tmpDir())
        s.guardar(parcela())
        assertTrue(s.borrar("p1"))
        assertNull(s.porId("p1"))
        assertFalse(s.borrar("p1"))
    }

    @Test
    fun inexistenteYCorrupto() {
        val dir = tmpDir()
        val s = FileParcelaStore(dir)
        assertNull(s.porId("x"))
        File(dir, "parcelas.json").writeText("{roto", Charsets.UTF_8)
        assertTrue(s.listar().isEmpty())
    }
}
