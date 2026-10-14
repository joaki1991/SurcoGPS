package es.surcogps.app.soporte

import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Informe de cierres inesperados: guarda la traza en fichero local para poder
 * compartirla (sin red, sin terceros). Lógica pura de ficheros, testeable en JVM.
 */
object CrashReporter {

    const val MAX_ARCHIVOS = 5

    fun dirRaiz(filesDir: File): File = File(filesDir, "cierres").apply { mkdirs() }

    /** Guarda un cierre y rota (máx. [MAX_ARCHIVOS]). Devuelve el fichero. */
    fun guardar(filesDir: File, titulo: String, detalle: String): File {
        val dir = dirRaiz(filesDir)
        val ts = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())
        val f = File(dir, "cierre-$ts.log")
        f.writeText("SurcoGPS · $titulo · $ts\n\n$detalle\n", Charsets.UTF_8)
        val resto = dir.listFiles()?.sortedByDescending { it.name } ?: emptyList()
        resto.drop(MAX_ARCHIVOS).forEach { it.delete() }
        return f
    }

    fun guardarCierre(filesDir: File, hilo: Thread, error: Throwable): File {
        val sb = StringBuilder()
        sb.append("Hilo: ").append(hilo.name).append('\n')
        sb.append("Excepción: ").append(error.javaClass.name).append(": ").append(error.message).append('\n')
        var causa: Throwable? = error.cause
        while (causa != null) {
            sb.append("Causa: ").append(causa.javaClass.name).append(": ").append(causa.message).append('\n')
            causa = causa.cause
        }
        sb.append(error.stackTraceToString())
        return guardar(filesDir, "cierre", sb.toString())
    }

    /** Nota no fatal (p. ej. arranque degradado). */
    fun nota(filesDir: File, texto: String): File =
        guardar(filesDir, "nota", texto)

    fun listar(filesDir: File): List<File> =
        dirRaiz(filesDir).listFiles()?.sortedByDescending { it.name }?.toList() ?: emptyList()

    fun leer(f: File): String = try {
        f.readText(Charsets.UTF_8)
    } catch (_: Exception) {
        "(ilegible)"
    }

    fun borrarTodos(filesDir: File) {
        dirRaiz(filesDir).listFiles()?.forEach { it.delete() }
    }
}
