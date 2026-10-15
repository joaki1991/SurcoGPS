package es.surcogps.app.soporte

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Resultado de una comprobación manual (para el informe). */
data class ChequeoFuente(
    val nombre: String,
    val url: String,
    val ok: Boolean,
    val codigo: Int?,
    val error: String?,
    val ms: Long,
    val ts: Long = System.currentTimeMillis()
)

/** Estadísticas de teselas leídas de osmdroid (sin subclases). */
data class StatsFuente(
    val erroresDescarga: Int,
    val aciertosCache: Int,
    val fallosCache: Int,
    val erroresGuardado: Int,
    val oom: Int,
    val cola: Long,
    val httpOk: Boolean?
)

/** Estado de caché de teselas (solo lectura). */
data class EstadoCache(val bytes: Long, val ficheros: Int)

/**
 * Constructor del informe TXT (lógica pura, testeable). No incluye coordenadas
 * exactas, contraseñas ni tokens: solo zona gruesa y contadores.
 */
object InformeDiag {

    fun construir(
        appVersion: String,
        eventos: List<EventoDiag>,
        cierres: List<String>,
        chequeos: List<ChequeoFuente>,
        stats: StatsFuente?,
        cache: EstadoCache?,
        permisoUbicacion: Boolean,
        gpsActivo: Boolean,
        ultimoFix: String?
    ): String = buildString {
        appendLine("SurcoGPS - informe de diagnóstico")
        appendLine("App: $appVersion")
        appendLine("Generado: ${fecha(System.currentTimeMillis())}")
        appendLine()
        appendLine("== Permisos y GPS ==")
        appendLine("Ubicación concedida: $permisoUbicacion")
        appendLine("Proveedor GPS activo: $gpsActivo")
        appendLine("Último fix: ${ultimoFix ?: "sin datos"}")
        appendLine()
        appendLine("== Teselas (osmdroid) ==")
        if (stats == null) {
            appendLine("Sin lectura (abrir Campo antes de exportar).")
        } else {
            appendLine("Errores de descarga: ${stats.erroresDescarga}")
            appendLine("Caché: ${stats.aciertosCache} aciertos · ${stats.fallosCache} fallos · ${stats.erroresGuardado} errores de guardado")
            appendLine("Memoria: ${stats.oom} OOM")
            appendLine("Cola pendiente: ${stats.cola}")
            appendLine("HTTP directo: ${stats.httpOk?.let { if (it) "ok" else "fallo" } ?: "sin comprobar"}")
        }
        appendLine()
        appendLine("== Caché de teselas ==")
        if (cache == null) appendLine("Sin lectura.")
        else appendLine("${cache.ficheros} ficheros · ${cache.bytes / 1024} KB")
        appendLine()
        appendLine("== Comprobaciones manuales ==")
        if (chequeos.isEmpty()) appendLine("Sin ejecutar (botón Ejecutar diagnóstico).")
        else chequeos.forEach {
            appendLine("- ${it.nombre}: ${if (it.ok) "OK" else "FALLO"} " +
                "(HTTP ${it.codigo ?: "?"}, ${it.ms} ms${it.error?.let { e -> ", $e" } ?: ""}) [${fecha(it.ts)}]")
        }
        appendLine()
        appendLine("== Cierres anteriores ==")
        if (cierres.isEmpty()) appendLine("Ninguno registrado.")
        else cierres.forEach { appendLine(it.take(1500)); appendLine("---") }
        appendLine()
        appendLine("== Eventos (últimos ${eventos.size}) ==")
        if (eventos.isEmpty()) appendLine("Sin eventos.")
        else eventos.forEach {
            appendLine("[${fecha(it.ts)}] [${it.sesion}] ${it.tag}: ${it.mensaje.take(500)}")
        }
    }

    private fun fecha(ts: Long): String =
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(ts))
}
