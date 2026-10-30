package es.surcogps.app.mapa

/**
 * Decide qué rumbo pinta la flecha del punto azul. Lógica pura, unit-testeable.
 *
 * Prioridad:
 * 1. Brújula del teléfono: es la orientación real de la persona, incluso parada.
 * 2. Rumbo de desplazamiento del GPS, solo con marcha fiable (≥ [UMBRAL_MARCHA_MS]).
 * 3. Sin dato: la flecha no se dibuja (nunca se inventa una orientación).
 */
object RumboFlecha {

    /** Velocidad mínima para fiarse del rumbo GPS (m/s). */
    const val UMBRAL_MARCHA_MS = 0.8f

    fun resolver(
        brujulaDeg: Double?,
        rumboGpsDeg: Double?,
        velocidadMps: Float?
    ): Double? {
        if (brujulaDeg != null && brujulaDeg.isFinite()) {
            return normalizar(brujulaDeg)
        }
        val velocidad = velocidadMps ?: 0f
        if (velocidad >= UMBRAL_MARCHA_MS &&
            rumboGpsDeg != null && rumboGpsDeg.isFinite()
        ) {
            return normalizar(rumboGpsDeg)
        }
        return null
    }

    private fun normalizar(grados: Double): Double =
        ((grados % 360.0) + 360.0) % 360.0
}
