package es.surcogps.app.mapa

import android.content.Context
import es.surcogps.app.soporte.RegistroDiag
import es.surcogps.app.soporte.SondaAlmacen
import es.surcogps.core.LatLon
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.tileprovider.MapTileProviderBasic
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.CopyrightOverlay
import org.osmdroid.views.overlay.TilesOverlay
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay

/**
 * Mapa retenido en ámbito de actividad (ViewModel): el MapView, los
 * proveedores de teselas y las capas se crean UNA vez y sobreviven a la
 * navegación entre pestañas. Sin esto, cada vuelta a Campo destruía y
 * recreaba todo (hilos, cachés en memoria y re-descarga de teselas).
 *
 * Los callbacks ([alFijarAncla], [alGesto]) se reasignan en cada composición;
 * los overlays los leen en el momento del evento.
 */
class MapaRetenido(appContext: Context) {

    private val ctx = appContext.applicationContext

    val franja = FranjaOverlay(0xFF2E7D32.toInt(), 0xFF1B5E20.toInt())
    val punto = UbicacionOverlay()
    val srcEsri = FuentesMapa.esriImagery()
    val aplicado = EstadoMapaAplicado()

    var alFijarAncla: (LatLon) -> Unit = {}
    var alGesto: () -> Unit = {}

    val lineas = LineasOverlay { p -> alFijarAncla(p) }
    val overlayRef: TilesOverlay = TilesOverlay(
        MapTileProviderBasic(ctx).apply { tileSource = FuentesMapa.esriReference() },
        ctx
    )

    val mapView: MapView = run {
        Configuration.getInstance().load(
            ctx,
            ctx.getSharedPreferences("osmdroid", Context.MODE_PRIVATE)
        )
        Configuration.getInstance().userAgentValue =
            "SurcoGPS/1.2 (https://github.com/joaki1991/SurcoGPS)"
        // Caché de teselas en directorio garantizado (solo si el defecto falla).
        val cfg = Configuration.getInstance()
        try {
            val defecto = cfg.osmdroidTileCache
            val usable = defecto != null && SondaAlmacen.probarEscrituraDirecta(defecto).ok
            if (resolverDirCache(usable) == ModoCache.INTERNA) {
                cfg.osmdroidTileCache = java.io.File(ctx.cacheDir, "teselas")
                RegistroDiag.registrar(
                    ctx.filesDir, "mapa",
                    "cache por defecto no escribible → interna"
                )
            }
        } catch (_: Exception) {
        }
        MapView(ctx).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            setBuiltInZoomControls(false)
            controller.setZoom(6.0)
            controller.setCenter(VISTA_DEFECTO)
            overlays.add(RotationGestureOverlay(this))
            overlays.add(franja)
            overlays.add(lineas)
            overlays.add(punto)
            overlays.add(CopyrightOverlay(ctx))
            addMapListener(object : MapListener {
                override fun onScroll(event: ScrollEvent?): Boolean {
                    if (!aplicado.moviendoProgramatico) alGesto()
                    return false
                }

                override fun onZoom(event: ZoomEvent?): Boolean = false
            })
            RegistroDiag.registrar(ctx.filesDir, "mapa", "MapView creado (retenido)")
        }
    }

    /** Solo al morir el ViewModel (no al cambiar de pestaña). */
    fun onDetach() {
        try {
            mapView.onDetach()
        } catch (_: Exception) {
        }
    }
}
