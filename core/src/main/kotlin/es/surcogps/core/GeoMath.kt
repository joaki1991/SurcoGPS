package es.surcogps.core

import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/** Coordenada geográfica WGS84 en grados sexagesimales. */
data class LatLon(val lat: Double, val lon: Double) {
    init {
        require(lat in -90.0..90.0) { "lat fuera de rango: $lat" }
        require(lon in -180.0..180.0) { "lon fuera de rango: $lon" }
    }
}

/** Coordenada métrica local este-norte (metros) relativa a un punto de referencia. */
data class Enu(val east: Double, val north: Double)

/**
 * Matemática geodésica del núcleo.
 *
 * Regla de proyecto: el trabajo productivo usa el marco ENU local
 * (proyección equirectangular tangente al ancla con `cos(latRef)`).
 * Haversine / rumbo / destino solo se usan para verificación y
 * distancias largas; NUNCA se restan grados para medir metros.
 */
object GeoMath {

    /** Radio terrestre medio (m). */
    const val EARTH_RADIUS_M = 6_371_000.0

    const val DEG_TO_RAD = PI / 180.0
    const val RAD_TO_DEG = 180.0 / PI

    /** Anclaje del marco local: su ENU es (0, 0) por definición. */
    fun toEnu(p: LatLon, ref: LatLon): Enu {
        val latRefRad = ref.lat * DEG_TO_RAD
        val dLat = (p.lat - ref.lat) * DEG_TO_RAD
        val dLon = (p.lon - ref.lon) * DEG_TO_RAD
        val east = dLon * cos(latRefRad) * EARTH_RADIUS_M
        val north = dLat * EARTH_RADIUS_M
        return Enu(east, north)
    }

    /** Inversa de [toEnu]. */
    fun fromEnu(e: Enu, ref: LatLon): LatLon {
        val latRefRad = ref.lat * DEG_TO_RAD
        val dLat = e.north / EARTH_RADIUS_M
        val dLon = e.east / (cos(latRefRad) * EARTH_RADIUS_M)
        return LatLon(
            lat = ref.lat + dLat * RAD_TO_DEG,
            lon = ref.lon + dLon * RAD_TO_DEG
        )
    }

    /** Distancia de círculo máximo (m) — verificación y distancias largas. */
    fun haversine(a: LatLon, b: LatLon): Double {
        val dLat = (b.lat - a.lat) * DEG_TO_RAD
        val dLon = (b.lon - a.lon) * DEG_TO_RAD
        val sLat = sin(dLat / 2)
        val sLon = sin(dLon / 2)
        val h = sLat * sLat + cos(a.lat * DEG_TO_RAD) * cos(b.lat * DEG_TO_RAD) * sLon * sLon
        return 2 * EARTH_RADIUS_M * asin(sqrt(h.coerceIn(0.0, 1.0)))
    }

    /** Rumbo inicial de [from] a [to], en [0, 360). 0=N, 90=E, 180=S, 270=O. */
    fun initialBearing(from: LatLon, to: LatLon): Double {
        val lat1 = from.lat * DEG_TO_RAD
        val lat2 = to.lat * DEG_TO_RAD
        val dLon = (to.lon - from.lon) * DEG_TO_RAD
        val y = sin(dLon) * cos(lat2)
        val x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(dLon)
        return SurcoCore.normalizeHeading(atan2(y, x) * RAD_TO_DEG)
    }

    /** Punto destino a [distanceM] metros desde [from] con [bearingDeg]. */
    fun destination(from: LatLon, bearingDeg: Double, distanceM: Double): LatLon {
        require(distanceM >= 0) { "distancia negativa: $distanceM" }
        val brng = SurcoCore.normalizeHeading(bearingDeg) * DEG_TO_RAD
        val lat1 = from.lat * DEG_TO_RAD
        val lon1 = from.lon * DEG_TO_RAD
        val ang = distanceM / EARTH_RADIUS_M
        val lat2 = asin(sin(lat1) * cos(ang) + cos(lat1) * sin(ang) * cos(brng))
        val lon2 = lon1 + atan2(
            sin(brng) * sin(ang) * cos(lat1),
            cos(ang) - sin(lat1) * sin(lat2)
        )
        return LatLon(lat2 * RAD_TO_DEG, wrapLon(lon2 * RAD_TO_DEG))
    }

    private fun wrapLon(lon: Double): Double {
        var l = (lon + 540.0) % 360.0 - 180.0
        if (l == -180.0) l = 180.0
        return l
    }
}
