package es.surcogps.core

import org.junit.jupiter.api.Test
import java.io.File
import java.nio.file.Files
import kotlin.system.measureTimeMillis

/**
 * Diagnóstico de rendimiento de la persistencia de sesiones (SPRINT-46).
 *
 * No mide microsegundos: detecta si el listado o la carga vuelven a hacer
 * trabajo cuadrático (expandir celdas de TODAS las sesiones). Los umbrales son
 * deliberadamente holgados para no fallar por ruido de la máquina.
 */
class RendimientoSesionesTest {

    private fun tmpDir(): File = Files.createTempDirectory("surco-rend").toFile()

    /** Sesión sintética: campo de [celdas] m² contiguos + recorrido largo. */
    private fun sesion(id: String, celdas: Int, puntos: Int): WorkSession {
        val cells = HashSet<Long>(celdas * 2)
        // Franjas horizontales de 200 m: compacta bien, como un campo real.
        var restantes = celdas
        var iy = 0
        while (restantes > 0) {
            val fila = minOf(200, restantes)
            for (ix in 0 until fila) cells.add((ix.toLong() shl 32) xor iy.toLong())
            restantes -= fila
            iy++
        }
        val track = (0 until puntos).map { LatLon(40.0 + it * 1e-5, -3.0 + it * 1e-5) }
        return WorkSession(
            id = id, name = "Sesión $id", anchor = LatLon(40.0, -3.0),
            headingDeg = 45.0, widthM = 6.0, track = track, cells = cells,
            startedMs = 1L, updatedMs = 2L
        )
    }

    @Test
    fun listarYcargarConDatosAcumulados() {
        val dir = tmpDir()
        val store = FileSessionStore(dir)
        val sesiones = 4
        val celdas = 100_000
        val puntos = 20_000
        repeat(sesiones) { store.save(sesion("s$it", celdas, puntos)) }

        // Calentamiento (JIT + caché de disco).
        store.list()
        store.load("s3")

        fun mejor(veces: Int, bloque: () -> Unit): Long {
            var min = Long.MAX_VALUE
            repeat(veces) {
                val ms = measureTimeMillis(bloque)
                if (ms < min) min = ms
            }
            return min
        }

        val msList = mejor(3) { store.list() }
        val msLoad = mejor(3) { store.load("s3") }
        val bytes = dir.listFiles()!!.sumOf { it.length() }
        println(
            "[REND] $sesiones sesiones x $celdas celdas / $puntos puntos " +
                "(${bytes / 1024} KB en disco): list()=${msList} ms, load()=${msLoad} ms (mejor de 3)"
        )

        // Umbrales de regresión: si alguien vuelve a expandir todo en list(), se dispara.
        org.junit.jupiter.api.Assertions.assertTrue(
            msList < 200, "list() tardó $msList ms: ¿vuelve a expandir celdas de todas las sesiones?"
        )
        org.junit.jupiter.api.Assertions.assertTrue(
            msLoad < 1_500, "load() tardó $msLoad ms"
        )
    }
}
