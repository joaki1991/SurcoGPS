package es.surcogps.core

/**
 * Lectura de posición con su precisión estimada (m, 68 % de confianza).
 * La precisión SIEMPRE acompaña a la posición: el GPS de móvil convencional es
 * métrico, nunca centimétrico, y la app debe mostrarla sin excepciones.
 */
data class Fix(
    val position: LatLon,
    /** Radio de incertidumbre en metros. Debe ser finito y >= 0. */
    val accuracyM: Float,
    /** Velocidad informada por el receptor (m/s), si existe. */
    val speedMps: Float? = null,
    /** Marca temporal (ms desde epoch). */
    val timeMs: Long
)

/** Motivo de rechazo de un fix (visible en diagnóstico, nunca silencioso). */
enum class RejectReason { STALE, POOR_ACCURACY, JUMP }

/** Configuración del filtro (puertas ajustables desde Ajustes). */
data class FilterConfig(
    /** Precisión máxima aceptada (m). Defecto 10 m. */
    val maxAccuracyM: Float = 10f,
    /** Velocidad implícita máxima entre fixes consecutivos (m/s). Defecto 50. */
    val maxJumpSpeedMps: Double = 50.0,
    /** Antigüedad máxima del fix (ms). Defecto 10 s. */
    val maxAgeMs: Long = 10_000L
)

/**
 * Puerta de calidad para ubicaciones: solo los fixes aceptados alimentan la
 * cobertura. Orden de comprobación: caducidad → precisión → salto.
 */
class GpsFilter(var config: FilterConfig = FilterConfig()) {

    private var last: Fix? = null

    /** Resultado del filtrado: fix aceptado o motivo de rechazo. */
    sealed interface Verdict {
        data class Accepted(val fix: Fix) : Verdict
        data class Rejected(val fix: Fix, val reason: RejectReason) : Verdict
    }

    fun filter(fix: Fix, nowMs: Long): Verdict {
        if (nowMs - fix.timeMs > config.maxAgeMs) return Verdict.Rejected(fix, RejectReason.STALE)
        val acc = fix.accuracyM
        if (!acc.isFinite() || acc < 0f || acc > config.maxAccuracyM) {
            return Verdict.Rejected(fix, RejectReason.POOR_ACCURACY)
        }
        val prev = last
        if (prev != null) {
            val dtS = (fix.timeMs - prev.timeMs) / 1000.0
            if (dtS > 0) {
                val dist = GeoMath.haversine(prev.position, fix.position)
                if (dist / dtS > config.maxJumpSpeedMps) {
                    return Verdict.Rejected(fix, RejectReason.JUMP)
                }
            }
        }
        last = fix
        return Verdict.Accepted(fix)
    }

    /** Olvida el último fix (p. ej. al reanudar sesión o tras pérdida de señal). */
    fun reset() {
        last = null
    }
}
