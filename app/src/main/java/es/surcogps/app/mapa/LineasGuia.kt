package es.surcogps.app.mapa

import es.surcogps.core.GeoMath
import es.surcogps.core.GuidanceEngine
import es.surcogps.core.LatLon
import kotlin.math.floor

/** Caja geográfica visible (grados sexagesimales). */
data class BBox(val norte: Double, val sur: Double, val este: Double, val oeste: Double)

/**
 * Guía visible en el mapa: familia de paralelas + línea activa + centro actual.
 * [centro] limita la plantilla a un radio útil alrededor de la persona; sin él
 * se usa el ancla.
 */
data class EstadoGuia(
    val engine: GuidanceEngine,
    val lineaActiva: Int?,
    val centro: LatLon? = null
)

/** Radio máximo de la plantilla respecto a la posición actual (m). */
const val RADIO_MAX_LINEAS_M = 10_000.0

/** Nº máximo de paralelas dibujadas por fotograma (rendimiento predecible). */
const val MAX_LINEAS_DIBUJO = 200

/** Si las líneas quedan más juntas que esto en pantalla, se oculta la familia. */
const val SEPARACION_MINIMA_PX = 6f

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

/**
 * Índices realmente dibujables: intersección de la caja visible, el radio
 * máximo alrededor de [centro] y el tope de líneas.
 *
 * Sin este límite, un apero de 0,5 m sobre una vista amplia genera miles de
 * segmentos por fotograma. Si la intersección queda vacía se devuelve solo la
 * línea activa (la que pasa por la persona), nunca una familia descontrolada.
 */
fun indicesLineasSeguras(
    guia: GuidanceEngine,
    bbox: BBox,
    centro: LatLon,
    maxLineas: Int = MAX_LINEAS_DIBUJO,
    radioMaxM: Double = RADIO_MAX_LINEAS_M
): IntRange {
    val visibles = indicesLineasVisibles(guia, bbox)
    val central = guia.nearestLineIndex(centro)
    val radioIndices = floor(radioMaxM / guia.implementWidthM).toInt().coerceAtLeast(0)
    val medio = (maxLineas.coerceAtLeast(1) - 1) / 2
    val desde = maxOf(visibles.first, central - radioIndices, central - medio)
    val hasta = minOf(visibles.last, central + radioIndices, central + medio)
    return if (desde <= hasta) desde..hasta else central..central
}

/**
 * Rango longitudinal acotado al radio útil alrededor de [centro]. La caja
 * visible puede abarcar cientos de km a poco zoom; las líneas no deben
 * extenderse más allá de [radioMaxM] desde la persona.
 */
fun rangoAlongSeguro(
    guia: GuidanceEngine,
    bbox: BBox,
    centro: LatLon,
    radioMaxM: Double = RADIO_MAX_LINEAS_M
): Pair<Double, Double> {
    val (visible0, visible1) = rangoAlong(guia, bbox)
    val e = GeoMath.toEnu(centro, guia.anchor)
    val central = e.east * guia.direction.east + e.north * guia.direction.north
    val desde = maxOf(visible0, central - radioMaxM)
    val hasta = minOf(visible1, central + radioMaxM)
    return if (desde <= hasta) desde to hasta else central to (central + 1.0)
}
