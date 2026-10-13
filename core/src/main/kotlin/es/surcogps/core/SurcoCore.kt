package es.surcogps.core

/** Punto de entrada del núcleo de dominio SurcoGPS (módulo puro Kotlin/JVM). */
object SurcoCore {
    const val VERSION = "1.0.0"

    /** Normaliza un rumbo a [0, 360). */
    fun normalizeHeading(headingDeg: Double): Double {
        var h = headingDeg % 360.0
        if (h < 0) h += 360.0
        // Evita devolver -0.0 o 360.0 por redondeo.
        if (h >= 360.0) h -= 360.0
        return h
    }
}
