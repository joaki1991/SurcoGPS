package es.surcogps.app.ui

import kotlin.math.round
import kotlin.math.roundToInt

/**
 * Pasos del ancho del apero: 0,5–30 m en incrementos de 0,1 m.
 *
 * Separado de la UI para poder probarlo sin Android: el número de pasos de un
 * `Slider` de Compose es el de posiciones intermedias, no el de intervalos.
 */
object PasosAjuste {

    const val ANCHO_MIN_M = 0.5f
    const val ANCHO_MAX_M = 30f
    const val ANCHO_PASO_M = 0.1f

    /** Posiciones intermedias necesarias para recorrer [min]..[max] cada [paso]. */
    fun pasosSlider(min: Float, max: Float, paso: Float): Int {
        require(paso > 0f && max > min) { "rango de slider inválido" }
        return (((max - min) / paso).roundToInt() - 1).coerceAtLeast(0)
    }

    /**
     * Deja el ancho en el múltiplo de 0,1 m más cercano y dentro del rango
     * admitido por persistencia (`0.5..30.0`), evitando valores como 12.299999.
     */
    fun redondearAncho(m: Double): Double {
        if (!m.isFinite()) return ANCHO_MIN_M.toDouble()
        return (round(m * 10.0) / 10.0)
            .coerceIn(ANCHO_MIN_M.toDouble(), ANCHO_MAX_M.toDouble())
    }
}
