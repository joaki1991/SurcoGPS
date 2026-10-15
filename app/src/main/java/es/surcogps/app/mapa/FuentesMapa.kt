package es.surcogps.app.mapa

import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.MapTileIndex

/**
 * Fuentes de teselas autorizadas (sin API key, coste 0 € en uso normal).
 *
 * - Calles: OSM Mapnik (© OpenStreetMap contributors). Tile Usage Policy:
 *   user-agent válido, máx. ~2 teselas/s, caché local (OSMDroid la gestiona).
 * - Híbrido: Esri World Imagery como base + capa transparente de referencia
 *   (límites y topónimos) encima → satélite con referencias cartográficas.
 *
 * Esri exige el orden `tile/{z}/{y}/{x}` (invertido respecto a OSM), de ahí el
 * `getTileURLString` a medida. Atribución visible obligatoria (ver MapaCampo).
 */
object FuentesMapa {

    private const val ESRI_IMAGERY =
        "https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile"
    private const val ESRI_REFERENCE =
        "https://server.arcgisonline.com/ArcGIS/rest/services/Reference/World_Boundaries_and_Places/MapServer/tile"

    fun esriImagery(): OnlineTileSourceBase =
        object : XYTileSource("EsriWorldImagery", 0, 19, 256, ".jpg", arrayOf("$ESRI_IMAGERY/")) {
            override fun getTileURLString(pMapTileIndex: Long): String {
                val z = MapTileIndex.getZoom(pMapTileIndex)
                val x = MapTileIndex.getX(pMapTileIndex)
                val y = MapTileIndex.getY(pMapTileIndex)
                return "$ESRI_IMAGERY/$z/$y/$x"
            }
        }

    fun esriReference(): OnlineTileSourceBase =
        object : XYTileSource("EsriReference", 0, 19, 256, ".png", arrayOf("$ESRI_REFERENCE/")) {
            override fun getTileURLString(pMapTileIndex: Long): String {
                val z = MapTileIndex.getZoom(pMapTileIndex)
                val x = MapTileIndex.getX(pMapTileIndex)
                val y = MapTileIndex.getY(pMapTileIndex)
                return "$ESRI_REFERENCE/$z/$y/$x"
            }
        }

    /** URL de una tesela de muestra (península, z10) para el diagnóstico de red. */
    fun urlMuestraEsri(): String =
        esriImagery().getTileURLString(MapTileIndex.getTileIndex(10, 500, 395))
}

/** Decisión sobre el directorio de caché de teselas. */
enum class ModoCache { DEFECTO, INTERNA }

/**
 * Elige dónde guardar teselas: el directorio por defecto, salvo que no sea
 * escribible (entonces, almacenamiento interno garantizado). Lógica pura.
 */
fun resolverDirCache(defectoEscribible: Boolean): ModoCache =
    if (defectoEscribible) ModoCache.DEFECTO else ModoCache.INTERNA

/** Estado de la comprobación de red hacia el proveedor satélite. */
enum class EstadoRed { COMPROBANDO, OK, FALLO }

/** Configuración efectiva del mapa + aviso visible (lógica pura, testeable). */
data class EstadoFuente(
    val base: BaseFuente,
    val conReferencia: Boolean,
    /** Aviso para la píldora de estado (null = todo bien). */
    val aviso: String?
)

/**
 * Resuelve qué pintar según modo y red, sin conmutaciones mágicas: si el
 * satélite no responde se AVISA (el usuario cambia a Calles si quiere).
 */
fun resolverFuente(modo: ModoMapa, esri: EstadoRed?): EstadoFuente {
    val cfg = configPara(modo)
    val aviso = when {
        cfg.base != BaseFuente.ESRI -> null
        esri == null || esri == EstadoRed.COMPROBANDO -> "Comprobando satélite…"
        esri == EstadoRed.FALLO -> "Satélite sin conexión: revisa la red o usa Calles"
        else -> null
    }
    return EstadoFuente(cfg.base, cfg.conReferencia, aviso)
}

/** Base cartográfica a usar + si necesita la capa de referencia encima. */
enum class BaseFuente { MAPNIK, ESRI }

/** Configuración declarativa de cada modo (lógica pura, unit-testeable). */
data class ConfigModo(val base: BaseFuente, val conReferencia: Boolean)

/** Qué base y capas usa cada modo de mapa. */
fun configPara(modo: ModoMapa): ConfigModo = when (modo) {
    ModoMapa.HIBRIDO -> ConfigModo(BaseFuente.ESRI, conReferencia = true)
    ModoMapa.SATELITE -> ConfigModo(BaseFuente.ESRI, conReferencia = false)
    ModoMapa.CALLES -> ConfigModo(BaseFuente.MAPNIK, conReferencia = false)
}
