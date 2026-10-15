package es.surcogps.app.soporte

import java.io.File

/** Foto del almacenamiento de teselas (solo lectura salvo la sonda). */
data class InfoAlmacen(
    val existeDir: Boolean,
    val escribibleDir: Boolean,
    val libreMB: Long,
    val ficheros: Int,
    val bytes: Long,
    val top: List<String>
)

/** Resultado de una sonda de escritura (mensaje apto para el informe). */
data class ResultadoSonda(val ok: Boolean, val detalle: String)

/**
 * Sonda del almacenamiento de teselas (java.io puro salvo la parte SQL,
 * que vive en el ViewModel por necesitar Android). Nada se borra.
 */
object SondaAlmacen {

    /** Inspecciona el directorio (no escribe). */
    fun inspeccionar(dir: File?): InfoAlmacen {
        if (dir == null) {
            return InfoAlmacen(false, false, -1, 0, 0, emptyList())
        }
        return try {
            val existe = dir.isDirectory
            var bytes = 0L
            var n = 0
            val nombres = mutableListOf<String>()
            if (existe) {
                dir.walkTopDown().take(6000).forEach {
                    if (it.isFile) {
                        bytes += it.length()
                        n++
                        if (nombres.size < 20) nombres.add(it.name + " " + it.length() + "B")
                    }
                }
            }
            val libre = try {
                dir.freeSpace / (1024 * 1024)
            } catch (_: Exception) {
                -1L
            }
            InfoAlmacen(existe, dir.canWrite(), libre, n, bytes, nombres)
        } catch (e: Exception) {
            InfoAlmacen(false, false, -1, 0, 0, listOf("error: ${e.javaClass.simpleName}"))
        }
    }

    /** Escribe, relee y borra 1 KB en [dir]. Prueba el permiso real de escritura. */
    fun probarEscrituraDirecta(dir: File): ResultadoSonda {
        return try {
            dir.mkdirs()
            val f = File(dir, "sonda.tmp")
            val datos = ByteArray(1024) { it.toByte() }
            f.writeBytes(datos)
            val leido = f.readBytes()
            f.delete()
            if (leido.contentEquals(datos)) {
                ResultadoSonda(true, "escritura directa OK")
            } else {
                ResultadoSonda(false, "leído distinto de lo escrito")
            }
        } catch (e: Exception) {
            ResultadoSonda(false, "${e.javaClass.simpleName}: ${e.message}")
        }
    }
}

/**
 * Veredicto legible de la sonda SQL (lógica pura, testeable).
 */
fun veredictoSonda(guardado: Boolean, existe: Boolean): String =
    if (guardado && existe) "SQL OK (escribe+lee+borra)"
    else "SQL fallo (guardado=$guardado existe=$existe)"
