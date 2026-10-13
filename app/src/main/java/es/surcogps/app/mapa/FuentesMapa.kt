package es.surcogps.app.mapa

import org.osmdroid.tileprovider.MapTileProviderBasic
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.MapTileIndex
import android.content.Context

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

    /** Proveedor dedicado para la capa de referencia transparente. */
    fun referenceOverlayProvider(context: Context): MapTileProviderBasic {
        val provider = MapTileProviderBasic(context.applicationContext)
        provider.tileSource = esriReference()
        return provider
    }
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
