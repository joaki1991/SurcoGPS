package es.surcogps.core

import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.round
import kotlin.math.sin

/**
 * Motor de guiado: familia de líneas rectas paralelas separadas [implementWidthM] metros.
 *
 * - La línea índice 0 pasa por [anchor] (más [lateralShiftM] en la normal).
 * - Rumbo [headingDeg]: 0=N, 90=E, 180=S, 270=O (normalizado a [0, 360)).
 * - Toda la geometría vive en el marco ENU local anclado en [anchor].
 * - XTE (cross-track error): distancia con signo a la línea activa;
 *   positivo = a la DERECHA del sentido de avance del rumbo.
 */
class GuidanceEngine(
    val anchor: LatLon,
    headingDeg: Double,
    val implementWidthM: Double,
    val lateralShiftM: Double = 0.0
) {
    init {
        require(implementWidthM in MIN_WIDTH_M..MAX_WIDTH_M) {
            "ancho de apero fuera de rango [0.5, 30] m: $implementWidthM"
        }
        require(lateralShiftM.isFinite()) { "desplazamiento lateral no finito" }
    }

    val headingDeg: Double = SurcoCore.normalizeHeading(headingDeg)

    /** Vector unitario de avance en ENU (este, norte). */
    val direction: Enu = Enu(
        east = sin(Math.toRadians(this.headingDeg)),
        north = cos(Math.toRadians(this.headingDeg))
    )

    /** Normal unitaria a la DERECHA del rumbo en ENU (este, norte). */
    val normal: Enu = Enu(
        east = cos(Math.toRadians(this.headingDeg)),
        north = -sin(Math.toRadians(this.headingDeg))
    )

    private fun coordAlongNormal(p: LatLon): Double {
        val e = GeoMath.toEnu(p, anchor)
        return e.east * normal.east + e.north * normal.north
    }

    /** Índice de la línea paralela más cercana a [p]. */
    fun nearestLineIndex(p: LatLon): Int {
        val s = (coordAlongNormal(p) - lateralShiftM) / implementWidthM
        return round(s).toInt()
    }

    /**
     * Error transversal con signo (m) respecto a la línea [lineIndex].
     * Si se omite, se usa la más cercana.
     */
    fun crossTrackError(p: LatLon, lineIndex: Int = nearestLineIndex(p)): Double {
        return coordAlongNormal(p) - (lateralShiftM + lineIndex * implementWidthM)
    }

    /** Punto ENU sobre la línea [lineIndex] a [alongM] metros del ancla (proyectada). */
    fun linePointEnu(lineIndex: Int, alongM: Double): Enu {
        val offset = lateralShiftM + lineIndex * implementWidthM
        return Enu(
            east = normal.east * offset + direction.east * alongM,
            north = normal.north * offset + direction.north * alongM
        )
    }

    /** Segmento geográfico de la línea [lineIndex] entre [fromAlongM] y [toAlongM]. */
    fun lineSegment(lineIndex: Int, fromAlongM: Double, toAlongM: Double): Pair<LatLon, LatLon> {
        require(toAlongM >= fromAlongM) { "segmento invertido" }
        val a = GeoMath.fromEnu(linePointEnu(lineIndex, fromAlongM), anchor)
        val b = GeoMath.fromEnu(linePointEnu(lineIndex, toAlongM), anchor)
        return a to b
    }

    /** Copia con nuevo desplazamiento lateral (m). */
    fun withShift(shiftM: Double): GuidanceEngine =
        GuidanceEngine(anchor, headingDeg, implementWidthM, shiftM)

    /** Copia con nuevo rumbo (el ancla se mantiene salvo que se indique otra). */
    fun withHeading(newHeadingDeg: Double, newAnchor: LatLon = anchor): GuidanceEngine =
        GuidanceEngine(newAnchor, newHeadingDeg, implementWidthM, lateralShiftM)

    companion object {
        const val MIN_WIDTH_M = 0.5
        const val MAX_WIDTH_M = 30.0

        /** Separación teórica entre las líneas [a] y [b] (m). */
        fun separation(a: Int, b: Int, widthM: Double): Double = abs(a - b) * widthM
    }
}
