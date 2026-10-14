package es.surcogps.app.mapa

import es.surcogps.core.GeoMath
import es.surcogps.core.GuidanceEngine
import es.surcogps.core.LatLon

/** Caja geográfica visible (grados sexagesimales). */
data class BBox(val norte: Double, val sur: Double, val este: Double, val oeste: Double)

/** Guía visible en el mapa: familia de paralelas + línea activa resaltada. */
data class EstadoGuia(val engine: GuidanceEngine, val lineaActiva: Int?)

/**
 * Índices de líneas paralelas que cubren la caja visible (±1 de margen).
 * Lógica pura, unit-testeable.
 */
fun indicesLineasVisibles(guia: GuidanceEngine, bbox: BBox): IntRange {
    val idx = listOf(
        LatLon(bbox.norte, bbox.oeste), LatLon(bbox.norte, bbox.este),
        LatLon(bbox.sur, bbox.oeste), LatLon(bbox.sur, bbox.este)
    ).map { guia.nearestLineIndex(it) }
    return (idx.min() - 1)..(idx.max() + 1)
}

/**
 * Rango longitudinal (m sobre la dirección de avance) que cubre la caja,
 * con 10 % de margen. Lógica pura, unit-testeable.
 */
fun rangoAlong(guia: GuidanceEngine, bbox: BBox): Pair<Double, Double> {
    val dir = guia.direction
    val proy = listOf(
        LatLon(bbox.norte, bbox.oeste), LatLon(bbox.norte, bbox.este),
        LatLon(bbox.sur, bbox.oeste), LatLon(bbox.sur, bbox.este)
    ).map { p ->
        val e = GeoMath.toEnu(p, guia.anchor)
        e.east * dir.east + e.north * dir.north
    }
    val min = proy.min()
    val max = proy.max()
    val margen = (max - min) * 0.1 + 5.0
    return (min - margen) to (max + margen)
}
