package es.surcogps.app.mapa

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/**
 * Suavizado del rumbo de marcha (rumbo del DESPLAZAMIENTO, no brújula).
 * Media exponencial sobre el vector unitario → sin saltos en el paso 359°→0°.
 * Parado o sin dato → null (no se inventa orientación).
 */
class SuavizadorRumbo(private val alfa: Double = 0.35) {

    private var sx = 0.0
    private var sy = 0.0
    private var hay = false

    /** Devuelve el rumbo suavizado en [0, 360), o null si no hay marcha fiable. */
    fun actualizar(rumboDeg: Double?, enMovimiento: Boolean): Double? {
        if (!enMovimiento || rumboDeg == null || !rumboDeg.isFinite()) {
            hay = false
            sx = 0.0
            sy = 0.0
            return null
        }
        val r = Math.toRadians(((rumboDeg % 360) + 360) % 360)
        val vx = sin(r)
        val vy = cos(r)
        if (!hay) {
            sx = vx
            sy = vy
            hay = true
        } else {
            sx = alfa * vx + (1 - alfa) * sx
            sy = alfa * vy + (1 - alfa) * sy
        }
        return (Math.toDegrees(atan2(sx, sy)) + 360) % 360
    }
}
