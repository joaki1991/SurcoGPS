package es.surcogps.app.gps

/** Origen efectivo de los fixes publicados al motor. */
enum class FuenteGps {
    /** Google Play Services (FusedLocationProvider). */
    FUSED,

    /** `LocationManager` del propio dispositivo (GPS y/o red). */
    PLATAFORMA
}

/** Sin fix de la fuente principal en este tiempo → se añade el GPS del sistema. */
const val UMBRAL_RESPALDO_MS = 20_000L

/**
 * Fuente primaria. Play Services es opcional: si no está operativo, se usa
 * directamente el GPS del dispositivo, sin esperas ni degradación.
 */
fun elegirFuenteGps(playServicesOperativo: Boolean): FuenteGps =
    if (playServicesOperativo) FuenteGps.FUSED else FuenteGps.PLATAFORMA

/**
 * ¿Toca activar el `LocationManager` como respaldo?
 *
 * [ultimoFixMs] debe ser el instante del último fix fresco (o el de arranque si
 * aún no hay ninguno). Una vez activo el respaldo no se vuelve a pedir.
 */
fun debeActivarRespaldo(
    ultimoFixMs: Long,
    ahoraMs: Long,
    respaldoYaActivo: Boolean
): Boolean {
    if (respaldoYaActivo) return false
    return ahoraMs - ultimoFixMs >= UMBRAL_RESPALDO_MS
}

/** Texto para el diagnóstico autónomo (sin jerga innecesaria). */
fun etiquetaFuente(f: FuenteGps?): String = when (f) {
    FuenteGps.FUSED -> "Google Play Services (Fused)"
    FuenteGps.PLATAFORMA -> "GPS del dispositivo (LocationManager)"
    null -> "sin iniciar"
}
