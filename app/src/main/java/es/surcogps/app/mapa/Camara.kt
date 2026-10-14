package es.surcogps.app.mapa

import es.surcogps.core.GeoMath
import es.surcogps.core.LatLon

/**
 * Comando de cámara de un solo uso (centrar + zoom opcional).
 * Se aplica una vez por [id]; el receptor debe consumirlo.
 */
data class DestinoCamara(val id: Long, val punto: LatLon, val zoom: Double?)

/**
 * Política de cámara (lógica pura, unit-testeable): la cámara solo se mueve por
 * comando explícito o por seguimiento con desplazamiento real; nunca por cada fix.
 */
object PoliticaCamara {

    /** Desplazamiento mínimo para recentrar en seguimiento (m). */
    const val UMBRAL_SEGUIR_M = 2.0

    /**
     * ¿Debe recentrarse la cámara? Solo si [seguir] está activo y (no hay centro
     * previo o el desplazamiento supera el umbral).
     */
    fun debeRecenter(seguir: Boolean, ultimo: LatLon?, nuevo: LatLon): Boolean {
        if (!seguir) return false
        val prev = ultimo ?: return true
        return GeoMath.haversine(prev, nuevo) > UMBRAL_SEGUIR_M
    }
}

/** Texto de la píldora de teselas («Cola: 3 · HTTP ok»). Lógica pura, testeable. */
fun textoCola(cola: Long, httpOk: Boolean?): String {
    val http = when (httpOk) {
        null -> "HTTP…"
        true -> "HTTP ok"
        false -> "HTTP fallo"
    }
    return if (cola < 0) "Teselas … · $http" else "Cola: $cola · $http"
}

/**
 * Rumbo del segmento ancla→punto para definir la dirección de arado desde casa.
 * Null si están demasiado cerca (< 1 m): no hay dirección fiable.
 */
fun rumboDesdeToque(ancla: LatLon, punto: LatLon): Double? {
    if (GeoMath.haversine(ancla, punto) < 1.0) return null
    return GeoMath.initialBearing(ancla, punto)
}

/**
 * Firma del estado dibujado: solo cambia si cambia algo visible. Evita
 * `invalidate()` (redibujado completo) ante re-emisiones idénticas, como los
 * ticks de un slider antes de soltar.
 */
fun firmaDibujo(
    nTramos: Int,
    ancla: LatLon?,
    rumbo: Double,
    ancho: Double,
    shift: Double,
    linea: Int?,
    fin: LatLon?,
    ubi: EstadoUbicacion?
): Int = listOf(nTramos, ancla, rumbo, ancho, shift, linea, fin, ubi).hashCode()
