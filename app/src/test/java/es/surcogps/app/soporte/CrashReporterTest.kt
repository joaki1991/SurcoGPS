package es.surcogps.app.soporte

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

class CrashReporterTest {

    private fun tmpDir(): File = Files.createTempDirectory("surco-crash").toFile()

    @Test
    fun guardaYLee() {
        val dir = tmpDir()
        val f = CrashReporter.guardar(dir, "prueba", "detalle-123")
        assertTrue(f.isFile)
        val texto = CrashReporter.leer(f)
        assertTrue(texto.contains("detalle-123"))
        assertEquals(listOf(f), CrashReporter.listar(dir))
    }

    @Test
    fun rotaAMaximo() {
        val dir = tmpDir()
        repeat(7) {
            CrashReporter.guardar(dir, "prueba", "x-$it")
            Thread.sleep(1100)
        }
        assertEquals(CrashReporter.MAX_ARCHIVOS, CrashReporter.listar(dir).size)
    }

    @Test
    fun cierreConTraza() {
        val dir = tmpDir()
        val error = IllegalStateException("fallo de prueba", RuntimeException("causa"))
        val f = CrashReporter.guardarCierre(dir, Thread.currentThread(), error)
        val texto = CrashReporter.leer(f)
        assertTrue(texto.contains("IllegalStateException"))
        assertTrue(texto.contains("fallo de prueba"))
        assertTrue(texto.contains("Causa: java.lang.RuntimeException: causa"))
        assertTrue(texto.contains("at "))
    }

    @Test
    fun borrarTodos() {
        val dir = tmpDir()
        CrashReporter.guardar(dir, "a", "b")
        CrashReporter.borrarTodos(dir)
        assertTrue(CrashReporter.listar(dir).isEmpty())
    }
}
