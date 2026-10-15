package es.surcogps.app.soporte

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

class RegistroDiagTest {

    private fun tmpDir(): File = Files.createTempDirectory("surco-diag").toFile()

    @Test
    fun registraYLee() {
        val dir = tmpDir()
        RegistroDiag.nuevaSesion(dir)
        RegistroDiag.registrar(dir, "mapa", "hola")
        val ev = RegistroDiag.ultimos(dir, 10)
        assertEquals(2, ev.size)
        assertEquals("mapa", ev[1].tag)
        assertEquals("hola", ev[1].mensaje)
        assertEquals(ev[0].sesion, ev[1].sesion)
    }

    @Test
    fun acotaElTamano() {
        val dir = tmpDir()
        repeat(RegistroDiag.MAX_EVENTOS + 50) {
            RegistroDiag.registrar(dir, "t", "m-$it")
        }
        val ev = RegistroDiag.ultimos(dir, 1000)
        assertEquals(RegistroDiag.MAX_EVENTOS, ev.size)
        assertEquals("m-${RegistroDiag.MAX_EVENTOS + 49}", ev.last().mensaje)
    }

    @Test
    fun sobreviveAReapertura() {
        val dir = tmpDir()
        RegistroDiag.registrar(dir, "a", "uno")
        // Nueva "instancia" (mismo fichero): el historial se conserva.
        val ev = RegistroDiag.ultimos(dir, 10)
        assertEquals(1, ev.size)
        assertEquals("uno", ev[0].mensaje)
    }

    @Test
    fun excepcionConTraza() {
        val dir = tmpDir()
        RegistroDiag.excepcion(dir, "red", IllegalStateException("fallo", RuntimeException("causa")))
        val texto = RegistroDiag.ultimos(dir, 1)[0].mensaje
        assertTrue(texto.contains("IllegalStateException"))
        assertTrue(texto.contains("at "))
    }

    @Test
    fun limpiar() {
        val dir = tmpDir()
        RegistroDiag.registrar(dir, "a", "b")
        assertTrue(RegistroDiag.tamano(dir) > 0)
        RegistroDiag.limpiar(dir)
        assertTrue(RegistroDiag.ultimos(dir, 10).isEmpty())
        assertEquals(0, RegistroDiag.tamano(dir))
    }

    @Test
    fun zonaGruesaNoEsExacta() {
        assertEquals("41.1,-4.7", zonaGruesa(41.1234, -4.6789))
    }
}
