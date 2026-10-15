package es.surcogps.app.soporte

import es.surcogps.core.MiniJson
import java.io.File
import java.util.Locale
import java.util.UUID

/** Evento del registro de diagnóstico. Sin coordenadas exactas ni secretos. */
data class EventoDiag(val ts: Long, val sesion: String, val tag: String, val mensaje: String)

/**
 * Registro local de diagnóstico en `filesDir/diag/diag-eventos.jsonl`.
 * - Sobrevive al cierre del proceso (fichero) y conserva la sesión anterior.
 * - Acotado a [MAX_EVENTOS] (se recortan los más antiguos al escribir).
 * - Hilo-seguro. Nunca incluye coordenadas exactas (usar [zonaGruesa]).
 */
object RegistroDiag {

    const val MAX_EVENTOS = 300
    private const val NOMBRE = "diag-eventos.jsonl"

    private val cerrojo = Any()

    @Volatile
    var sesionActual: String = "arranque"
        private set

    fun archivo(filesDir: File): File = File(File(filesDir, "diag").apply { mkdirs() }, NOMBRE)

    /** Marca una nueva sesión de la app (se llama en `onCreate`). */
    fun nuevaSesion(filesDir: File): String {
        val id = UUID.randomUUID().toString().take(8)
        sesionActual = id
        registrar(filesDir, "sesion", "inicio de sesion $id")
        return id
    }

    fun registrar(filesDir: File, tag: String, mensaje: String) {
        synchronized(cerrojo) {
            try {
                val f = archivo(filesDir)
                f.appendText(linea(System.currentTimeMillis(), sesionActual, tag, mensaje), Charsets.UTF_8)
                recortar(f)
            } catch (_: Exception) {
                // El diagnóstico nunca debe tumbar la app.
            }
        }
    }

    fun excepcion(filesDir: File, tag: String, error: Throwable) {
        val detalle = buildString {
            append(error.javaClass.name).append(": ").append(error.message).append('\n')
            append(error.stackTraceToString().lineSequence().take(12).joinToString("\n"))
        }
        registrar(filesDir, tag, detalle)
    }

    fun ultimos(filesDir: File, n: Int): List<EventoDiag> {
        synchronized(cerrojo) {
            return try {
                val f = archivo(filesDir)
                if (!f.isFile) return emptyList()
                f.readLines(Charsets.UTF_8).takeLast(n.coerceAtLeast(1)).mapNotNull { linea ->
                    try {
                        @Suppress("UNCHECKED_CAST")
                        val m = MiniJson.parseObject(linea) as Map<String, Any?>
                        EventoDiag(
                            (m["ts"] as Number).toLong(),
                            m["sesion"] as String,
                            m["tag"] as String,
                            m["mensaje"] as String
                        )
                    } catch (_: Exception) {
                        null
                    }
                }
            } catch (_: Exception) {
                emptyList()
            }
        }
    }

    fun limpiar(filesDir: File) {
        synchronized(cerrojo) {
            try {
                archivo(filesDir).delete()
            } catch (_: Exception) {
            }
        }
    }

    fun tamano(filesDir: File): Long {
        return try {
            archivo(filesDir).takeIf { it.isFile }?.length() ?: 0L
        } catch (_: Exception) {
            0L
        }
    }

    private fun recortar(f: File) {
        try {
            val lineas = f.readLines(Charsets.UTF_8)
            if (lineas.size > MAX_EVENTOS) {
                f.writeText(lineas.takeLast(MAX_EVENTOS).joinToString("\n", postfix = "\n"), Charsets.UTF_8)
            }
        } catch (_: Exception) {
        }
    }

    internal fun linea(ts: Long, sesion: String, tag: String, mensaje: String): String =
        "{\"ts\":$ts,\"sesion\":\"${esc(sesion)}\",\"tag\":\"${esc(tag)}\",\"mensaje\":\"${esc(mensaje)}\"}\n"

    private fun esc(s: String): String = buildString {
        for (c in s) when (c) {
            '"' -> append("\\\"")
            '\\' -> append("\\\\")
            '\n' -> append("\\n")
            '\r' -> append("\\r")
            '\t' -> append("\\t")
            else -> append(c)
        }
    }
}

/**
 * Zona gruesa (~11 km) para referirse a la posición sin exponer coordenadas
 * exactas en registros ni informes.
 */
fun zonaGruesa(lat: Double, lon: Double): String =
    "%.1f,%.1f".format(Locale.US, lat, lon)
