package es.surcogps.app.mapa

/**
 * Estilo de la superficie trabajada. Separado del `Overlay` para poder fijar
 * por test que la opacidad no vuelva a bajar a valores casi invisibles.
 */
object EstiloFranja {
    /** Opacidad del relleno (0–255). Antes 110: se confundía con el satélite. */
    const val ALFA_RELLENO = 210

    /** Opacidad del borde: marca el límite exacto de la pasada. */
    const val ALFA_BORDE = 245

    /** Grosor del borde en píxeles de lienzo. */
    const val GROSOR_BORDE = 2.5f
}
