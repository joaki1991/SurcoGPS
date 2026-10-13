package es.surcogps.core

import kotlin.math.floor

/**
 * Registro de superficie trabajada sobre una cuadrícula raster en el marco ENU local.
 *
 * - Cada tramo trabajado es la cápsula (segmento ± [widthM]/2) de la trayectoria del apero.
 * - El plano se cuantiza en celdas cuadradas de [cellSizeM] (defecto 1 m); una celda
 *   cuenta una sola vez aunque se pise varias veces → los solapamientos NO duplican.
 * - Superficie = nº celdas × cellSizeM².
 *
 * Nota metrológica honesta: la celda cuenta entera aunque la franja la toque
 * parcialmente, así que una pasada aislada sobreestima ~media celda por borde
 * (≈ +12 % con apero de 6 m y celda de 1 m). A cambio, la misma pasada repetida
 * NO suma nada y las adyacentes suman lo suyo: lo garantizado es la ausencia de
 * duplicados, no la exactitud centimétrica con GPS de móvil.
 *
 * La cuadrícula es relativa a [anchor] (el mismo ancla de la guía), de modo que la
 * persistencia y la reanudación son estables mientras el ancla no cambie.
 */
class CoverageTracker(
    val anchor: LatLon,
    val cellSizeM: Double = 1.0
) {
    init {
        require(cellSizeM in 0.25..5.0) { "celda fuera de rango [0.25, 5] m: $cellSizeM" }
    }

    private val cells = mutableSetOf<Long>()

    /** Nº de celdas distintas cubiertas. */
    val cellCount: Int get() = cells.size

    /** Superficie trabajada sin duplicar (m²). */
    val coveredAreaM2: Double get() = cells.size * cellSizeM * cellSizeM

    /** Superficie en hectáreas. */
    val coveredHa: Double get() = coveredAreaM2 / 10_000.0

    /**
     * Registra el tramo [from]→[to] con anchura de apero [widthM].
     * @return nº de celdas NUEVAS aportadas por este tramo.
     */
    fun addSection(from: LatLon, to: LatLon, widthM: Double): Int {
        require(widthM in GuidanceEngine.MIN_WIDTH_M..GuidanceEngine.MAX_WIDTH_M) {
            "ancho de apero fuera de rango: $widthM"
        }
        val a = GeoMath.toEnu(from, anchor)
        val b = GeoMath.toEnu(to, anchor)
        val dx = b.east - a.east
        val dy = b.north - a.north
        val length = kotlin.math.hypot(dx, dy)
        // Dirección de avance y normal (a la derecha).
        val (ux, uy) = if (length < 1e-9) 0.0 to 1.0 else dx / length to dy / length
        val nx = uy
        val ny = -ux

        val step = cellSizeM / 2.0
        val before = cells.size
        // Muestreo a lo largo del segmento (incluye extremos y disco final).
        val n = maxOf(1, kotlin.math.ceil(length / step).toInt())
        for (i in 0..n) {
            val t = i.toDouble() / n
            val px = a.east + dx * t
            val py = a.north + dy * t
            // Barrido transversal de la franja.
            var w = -widthM / 2.0
            while (w <= widthM / 2.0 + 1e-9) {
                quantize(px + nx * w, py + ny * w)?.let { cells.add(it) }
                w += step
            }
        }
        return cells.size - before
    }

    /** Vacía la cuadrícula (nueva parcela / nueva sesión). */
    fun clear() {
        cells.clear()
    }

    /** Foto inmutable de las celdas (para persistencia). */
    fun snapshot(): Set<Long> = cells.toSet()

    /** Restaura celdas previamente guardadas (reanudación). */
    fun restore(saved: Set<Long>) {
        cells.clear()
        cells.addAll(saved)
    }

    private fun quantize(east: Double, north: Double): Long? {
        val ix = floor(east / cellSizeM).toLong()
        val iy = floor(north / cellSizeM).toLong()
        // Rango de coordenadas: ±40 000 km / 0.25 m cabe en 32 bits con holgura.
        if (ix < Int.MIN_VALUE || ix > Int.MAX_VALUE || iy < Int.MIN_VALUE || iy > Int.MAX_VALUE) return null
        return (ix shl 32) xor (iy and 0xFFFFFFFFL)
    }
}
