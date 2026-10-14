package es.surcogps.app.mapa

/**
 * Política de encuadre inicial (lógica pura, unit-testeable): el zoom de detalle
 * se aplica UNA vez ante el primer fix válido, nunca por cada actualización.
 */
object PoliticaEncuadre {

    /** Zoom de detalle para trabajo en campo (máximo útil de Esri/Mapnik). */
    const val ZOOM_DETALLE = 19.0

    /** Recorta al máximo admitido por el proveedor (sin errores por exceso). */
    fun zoomDetallePara(maxProveedor: Double): Double =
        ZOOM_DETALLE.coerceIn(1.0, maxProveedor.coerceAtLeast(1.0))
}

/** Estado de «primer fix»: decide si un fix dispara el encuadre inicial. */
class EncuadreInicial {

    private var hecho = false

    /**
     * @return true solo la primera vez con fix aceptado y precisión validada.
     */
    fun alFix(aceptado: Boolean, precisionM: Float, puertaM: Float): Boolean {
        if (hecho || !aceptado) return false
        if (!precisionM.isFinite() || precisionM < 0f || precisionM > puertaM) return false
        hecho = true
        return true
    }

    /** Nuevo arranque (permite reencuadrar en la próxima posición válida). */
    fun reiniciar() {
        hecho = false
    }
}
