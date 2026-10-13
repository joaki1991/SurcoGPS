package es.surcogps.app.cobertura

import es.surcogps.core.CoverageTracker
import es.surcogps.core.GeoMath
import es.surcogps.core.LatLon

/** Tramo de franja trabajada (posición del apero, anchura real del apero). */
data class TramoFranja(val desde: LatLon, val hasta: LatLon, val anchoM: Double)

/**
 * Registro de cobertura en memoria para la sesión en curso: solo posiciones del
 * apero procedentes de fixes ACEPTADOS. Pausable y reiniciable.
 */
class CoberturaEngine(
    anchor: LatLon,
    val anchoM: Double,
    cellSizeM: Double = 1.0
) {
    private val tracker = CoverageTracker(anchor, cellSizeM)
    private var ultimo: LatLon? = null
    private var pausado = false

    private val _tramos = mutableListOf<TramoFranja>()
    /** Tramos dibujables (capados para no saturar el mapa; el área no se pierde). */
    val tramos: List<TramoFranja> get() = _tramos

    val areaHa: Double get() = tracker.coveredHa
    val areaM2: Double get() = tracker.coveredAreaM2
    val estaPausado: Boolean get() = pausado

    fun pausar() {
        pausado = true
    }

    fun reanudar() {
        pausado = false
        ultimo = null // Evita un tramo fantasma tras la pausa.
    }

    /**
     * Registra una posición del apero. Devuelve las celdas NUEVAS aportadas
     * (0 si pausado, si es el primer punto, si apenas se ha movido (< 10 cm,
     * bajo el suelo de ruido GPS) o si ya estaba cubierto).
     */
    fun registrar(p: LatLon): Int {
        if (pausado) return 0
        val prev = ultimo
        if (prev == null) {
            ultimo = p
            return 0
        }
        if (GeoMath.haversine(prev, p) < MIN_TRAMO_M) return 0
        ultimo = p
        val nuevas = tracker.addSection(prev, p, anchoM)
        if (_tramos.size < MAX_TRAMOS_DIBUJO) {
            _tramos.add(TramoFranja(prev, p, anchoM))
        }
        return nuevas
    }

    fun reiniciar() {
        tracker.clear()
        _tramos.clear()
        ultimo = null
    }

    companion object {
        const val MAX_TRAMOS_DIBUJO = 20_000
        /** Desplazamiento mínimo para registrar (m). */
        const val MIN_TRAMO_M = 0.1
    }
}
