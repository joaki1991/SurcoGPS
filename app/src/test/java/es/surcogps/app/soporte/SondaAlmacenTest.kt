package es.surcogps.app.soporte

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

class SondaAlmacenTest {

    private fun tmpDir(): File = Files.createTempDirectory("surco-sonda").toFile()

    @Test
    fun inspeccionaDirectorio() {
        val dir = tmpDir()
        File(dir, "a.bin").writeBytes(ByteArray(100))
        File(dir, "sub").mkdir()
        File(dir, "sub/b.bin").writeBytes(ByteArray(50))
        val info = SondaAlmacen.inspeccionar(dir)
        assertTrue(info.existeDir)
        assertEquals(2, info.ficheros)
        assertEquals(150, info.bytes)
        assertTrue(info.libreMB > 0)
        assertEquals(2, info.top.size)
    }

    @Test
    fun directorioInexistente() {
        val info = SondaAlmacen.inspeccionar(File(tmpDir(), "no-existe"))
        assertFalse(info.existeDir)
        assertEquals(0, info.ficheros)
    }

    @Test
    fun escrituraDirectaOk() {
        val r = SondaAlmacen.probarEscrituraDirecta(tmpDir())
        assertTrue(r.ok)
    }

    @Test
    fun escrituraEnFicheroFallaConMotivo() {
        // Un fichero no es un directorio escribible: debe fallar con motivo.
        val f = File(tmpDir(), "bloqueo")
        f.writeText("x")
        val r = SondaAlmacen.probarEscrituraDirecta(File(f, "sub"))
        assertFalse(r.ok)
        assertTrue(r.detalle.isNotBlank())
    }
}
