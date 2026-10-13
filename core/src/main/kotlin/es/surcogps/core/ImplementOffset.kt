package es.surcogps.core

import kotlin.math.cos
import kotlin.math.sin

/**
 * Brazo de palanca antena → apero, en metros y relativo al rumbo del vehículo:
 * - [lateralM]: + = apero a la DERECHA de la antena (mirando en el rumbo).
 * - [longitudinalM]: + = apero por DELANTE de la antena; − = por detrás
 *   (típico: apero arrastrado 2 m detrás → `longitudinalM = -2`).
 *
 * La cobertura se registra en la posición del APERO, no de la antena.
 */
data class ImplementOffset(
    val lateralM: Double = 0.0,
    val longitudinalM: Double = 0.0
) {
    init {
        require(lateralM.isFinite() && longitudinalM.isFinite()) { "offset no finito" }
        require(kotlin.math.hypot(lateralM, longitudinalM) <= 30.0) {
            "brazo de palanca excesivo (> 30 m)"
        }
    }

    /**
     * Proyecta la posición de la antena [antenna] a la del apero, dado el rumbo
     * del vehículo [vehicleHeadingDeg] y el marco local [anchor].
     */
    fun apply(antenna: LatLon, vehicleHeadingDeg: Double, anchor: LatLon): LatLon {
        if (lateralM == 0.0 && longitudinalM == 0.0) return antenna
        val h = Math.toRadians(SurcoCore.normalizeHeading(vehicleHeadingDeg))
        // right = (cos h, −sin h); forward = (sin h, cos h)
        val dEast = lateralM * cos(h) + longitudinalM * sin(h)
        val dNorth = -lateralM * sin(h) + longitudinalM * cos(h)
        val e = GeoMath.toEnu(antenna, anchor)
        return GeoMath.fromEnu(Enu(e.east + dEast, e.north + dNorth), anchor)
    }
}
