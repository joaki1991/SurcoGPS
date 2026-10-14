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
