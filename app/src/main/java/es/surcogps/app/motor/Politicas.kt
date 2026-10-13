package es.surcogps.app.motor

import java.util.Locale

/**
 * Decisiones de robustez del seguimiento (lógica pura, unit-testeable).
 */
object Politicas {

    /**
     * ¿Debe el servicio reanudar el tracking? Solo si el usuario lo dejó activo
     * Y hay permiso de ubicación (nunca tracking encubierto).
     */
    fun debeReanudarServicio(trackingActivo: Boolean, tienePermisoUbicacion: Boolean): Boolean =
        trackingActivo && tienePermisoUbicacion

    /** Texto de la notificación persistente (ha + XTE + línea; formato estable). */
    fun textoNotificacion(ha: Double, xteM: Double?, linea: Int?): String {
        val base = "SurcoGPS · ${"%.2f".format(Locale.US, ha)} ha"
        if (xteM == null || linea == null) return "$base · sin guía"
        val flecha = if (xteM > 0.3) "→" else if (xteM < -0.3) "←" else "✓"
        return "$base · L$linea ${"%+.1f".format(Locale.US, xteM)} m $flecha"
    }
}
