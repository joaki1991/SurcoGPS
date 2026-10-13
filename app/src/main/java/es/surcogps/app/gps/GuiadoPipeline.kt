package es.surcogps.app.gps

import es.surcogps.core.FilterConfig
import es.surcogps.core.Fix
import es.surcogps.core.GeoMath
import es.surcogps.core.GpsFilter
import es.surcogps.core.GuidanceEngine
import es.surcogps.core.ImplementOffset
import es.surcogps.core.LatLon
import es.surcogps.core.RejectReason

/** Estado del banderillero tras procesar un fix. Lógica pura (testeable en JVM). */
data class EstadoGuiado(
    val fix: Fix? = null,
    val aceptado: Boolean = false,
    val motivoRechazo: RejectReason? = null,
    /** Posición del apero (antena + offset), solo si el fix fue aceptado. */
    val posicionApero: LatLon? = null,
    /** Error transversal con signo (m). + = derecha del rumbo. */
    val xteM: Double? = null,
    /** Índice de la línea activa. */
    val linea: Int? = null,
    /** Rumbo del vehículo estimado por desplazamiento (grados). */
    val rumboVehiculoDeg: Double? = null
)

/**
 * Tubería Fix → Filtro → Offset → Guía. Solo los fixes aceptados generan estado
 * de guiado; un rechazo conserva el último estado válido.
 */
class GuiadoPipeline(puertaPrecisionM: Float = 10f) {

    private val filtro = GpsFilter(FilterConfig(maxAccuracyM = puertaPrecisionM))
    private var ultimoApero: LatLon? = null
    private var ultimoRumbo: Double? = null
    private var ultimoEstado = EstadoGuiado()

    fun configurarPuerta(puertaM: Float) {
        filtro.config = filtro.config.copy(maxAccuracyM = puertaM)
    }

    /** Procesa un fix crudo. [guia] null = ancla sin fijar (sin XTE). */
    fun procesar(
        fix: Fix,
        ahoraMs: Long,
        guia: GuidanceEngine?,
        offset: ImplementOffset
    ): EstadoGuiado {
        when (val v = filtro.filter(fix, ahoraMs)) {
            is GpsFilter.Verdict.Rejected -> {
                ultimoEstado = ultimoEstado.copy(
                    fix = fix, aceptado = false, motivoRechazo = v.reason
                )
                return ultimoEstado
            }
            is GpsFilter.Verdict.Accepted -> Unit
        }
        val rumboRef = guia?.headingDeg ?: 0.0
        val apero = offset.apply(fix.position, ultimoRumbo ?: rumboRef, guia?.anchor ?: fix.position)
        val prev = ultimoApero
        if (prev != null && GeoMath.haversine(prev, apero) > 1.0) {
            ultimoRumbo = GeoMath.initialBearing(prev, apero)
        }
        ultimoApero = apero
        ultimoEstado = if (guia == null) {
            EstadoGuiado(fix = fix, aceptado = true, posicionApero = apero,
                rumboVehiculoDeg = ultimoRumbo)
        } else {
            val linea = guia.nearestLineIndex(apero)
            EstadoGuiado(
                fix = fix,
                aceptado = true,
                posicionApero = apero,
                xteM = guia.crossTrackError(apero, linea),
                linea = linea,
                rumboVehiculoDeg = ultimoRumbo ?: guia.headingDeg
            )
        }
        return ultimoEstado
    }

    fun reiniciar() {
        filtro.reset()
        ultimoApero = null
        ultimoRumbo = null
        ultimoEstado = EstadoGuiado()
    }
}
