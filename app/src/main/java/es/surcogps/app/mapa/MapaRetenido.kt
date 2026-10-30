package es.surcogps.app.mapa

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import es.surcogps.app.soporte.RegistroDiag
import es.surcogps.app.soporte.SondaAlmacen
import es.surcogps.core.GeoMath
import es.surcogps.core.LatLon
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.CopyrightOverlay

/** Modos de mapa disponibles. */
enum class ModoMapa { SATELITE, CALLES, HIBRIDO }

/** Atribución visible de las fuentes (también en el CopyrightOverlay). */
const val ATRIBUCION = "© OpenStreetMap contributors · Imagery © Esri World Imagery"

/** Vista por defecto: península ibérica (hasta el primer fix válido). */
val VISTA_DEFECTO = GeoPoint(40.5, -3.8)

/** Hay red disponible (aviso honesto de modo sin conexión). */
fun hayRed(context: Context): Boolean {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val net = cm.activeNetwork ?: return false
    val cap = cm.getNetworkCapabilities(net) ?: return false
    return cap.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
}

/**
 * Mapa retenido en el ViewModel: el MapView, los proveedores de teselas y las
 * capas se crean UNA vez y sobreviven a la rotación de pantalla.
 *
 * Simplificaciones de la v2:
 * - Una sola fuente de teselas cada vez (satélite Esri o calles OSM), sin capa
 *   de referencia ni mapa híbrido de dos proveedores: menos hilos, menos caché
 *   y menos memoria.
 * - Sin rotación de mapa: el norte siempre arriba, lo que hace que la flecha
 *   del punto azul sea directamente comprensible en campo.
 */
class MapaRetenido(appContext: Context) {

    private val ctx = appContext.applicationContext

    val franja = FranjaOverlay(0xFF2E7D32.toInt(), 0xFF1B5E20.toInt())
    val punto = UbicacionOverlay()
    val lineas = LineasOverlay()
    val srcEsri = FuentesMapa.esriImagery()
    private val copyright = CopyrightOverlay(appContext)

    /** Se invoca cuando el usuario desplaza o hace zoom a mano. */
    var alGesto: () -> Unit = {}

    private var ultimoModo: ModoMapa? = null
    private var moviendoProgramatico = false

    /** Último centro puesto por la app (para el umbral de seguimiento). */
    var ultimoCentro: LatLon? = null
        private set

    val mapView: MapView = run {
        Configuration.getInstance().load(
            ctx,
            ctx.getSharedPreferences("osmdroid", Context.MODE_PRIVATE)
        )
        Configuration.getInstance().userAgentValue =
            "SurcoGPS/2.0 (https://github.com/joaki1991/SurcoGPS)"
        // Caché de teselas en directorio garantizado (solo si el defecto falla).
        try {
            val cfg = Configuration.getInstance()
            val defecto = cfg.osmdroidTileCache
            val usable = defecto != null && SondaAlmacen.probarEscrituraDirecta(defecto).ok
            if (resolverDirCache(usable) == ModoCache.INTERNA) {
                cfg.osmdroidTileCache = java.io.File(ctx.cacheDir, "teselas")
                RegistroDiag.registrar(
                    ctx.filesDir, "mapa", "cache por defecto no escribible → interna"
                )
            }
        } catch (_: Exception) {
        }

        MapView(ctx).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            setTilesScaledToDpi(true)
            // La vista se reutiliza al girar la pantalla. OSMDroid, por defecto,
            // se autodestruye al separarse de la ventana: eso borraba las capas
            // (punto, líneas y franja) y dejaba el mapa «vacío» tras rotar.
            setDestroyMode(false)
            minZoomLevel = 3.0
            maxZoomLevel = 19.0
            controller.setZoom(6.0)
            controller.setCenter(VISTA_DEFECTO)
            overlays.add(franja)
            overlays.add(lineas)
            overlays.add(punto)
            overlays.add(copyright)
            addMapListener(object : MapListener {
                override fun onScroll(event: ScrollEvent?): Boolean {
                    // Solo un desplazamiento REAL del usuario desactiva el
                    // seguimiento. Los eventos de layout y los centrados
                    // programáticos dejan el centro en `ultimoCentro`.
                    if (!moviendoProgramatico) {
                        val objetivo = ultimoCentro
                        if (objetivo != null) {
                            val c = mapView.mapCenter
                            val actual = LatLon(c.latitude, c.longitude)
                            if (GeoMath.haversine(actual, objetivo) > 1.0) alGesto()
                        }
                    }
                    return false
                }

                override fun onZoom(event: ZoomEvent?): Boolean = false
            })
            RegistroDiag.registrar(ctx.filesDir, "mapa", "MapView creado (v2 retenido)")
        }
    }

    /**
     * Reañade las capas de datos si faltan. Es una red de seguridad: si alguna
     * vez la vista se separa de la ventana y OSMDroid limpia sus overlays, el
     * mapa vuelve a dibujar punto, líneas y franja sin reiniciar la app.
     */
    fun garantizarCapas() {
        val ov = mapView.overlays
        if (franja in ov && lineas in ov && punto in ov && copyright in ov) return
        ov.clear()
        ov.add(franja)
        ov.add(lineas)
        ov.add(punto)
        ov.add(copyright)
        RegistroDiag.registrar(ctx.filesDir, "mapa", "capas restauradas")
    }

    /** Aplica la fuente de teselas solo cuando cambia el modo. */
    fun aplicarModo(modo: ModoMapa) {
        if (modo == ultimoModo) return
        mapView.setTileSource(
            if (modo == ModoMapa.CALLES) TileSourceFactory.MAPNIK else srcEsri
        )
        ultimoModo = modo
        RegistroDiag.registrar(ctx.filesDir, "mapa", "modo $modo aplicado")
    }

    /** Centra el mapa (comando explícito; no se repite en cada fix). */
    fun centrar(p: LatLon, zoom: Double? = null) {
        moviendoProgramatico = true
        mapView.controller.setCenter(GeoPoint(p.lat, p.lon))
        zoom?.let { z ->
            mapView.controller.setZoom(
                z.coerceIn(mapView.minZoomLevel, mapView.maxZoomLevel)
            )
        }
        moviendoProgramatico = false
        ultimoCentro = p
    }

    fun olvidarCentro() {
        ultimoCentro = null
    }

    /** Solo al morir el ViewModel (fin de la app), no al girar la pantalla. */
    fun onDetach() {
        try {
            mapView.onDetach()
        } catch (_: Exception) {
        }
    }
}
