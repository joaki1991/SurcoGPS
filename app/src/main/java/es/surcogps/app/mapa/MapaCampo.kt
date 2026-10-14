package es.surcogps.app.mapa

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import es.surcogps.app.cobertura.TramoFranja
import es.surcogps.core.LatLon
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.TilesOverlay

/** Modos de mapa disponibles. */
enum class ModoMapa { HIBRIDO, SATELITE, CALLES }

/** Atribución obligatoria de las fuentes (siempre visible bajo el mapa). */
const val ATRIBUCION = "© OpenStreetMap contributors · Imagery © Esri World Imagery"

/** Vista por defecto: península ibérica (se centra en el ancla en el Sprint 08). */
val VISTA_DEFECTO = GeoPoint(40.5, -3.8)

/** Hay red disponible (para el aviso de modo sin conexión). */
fun hayRed(context: Context): Boolean {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val net = cm.activeNetwork ?: return false
    val cap = cm.getNetworkCapabilities(net) ?: return false
    return cap.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
}

/**
 * Mapa híbrido OSMDroid embebido en Compose.
 * - HÍBRIDO: Esri satélite + referencia transparente (límites/topónimos).
 * - SATÉLITE: solo Esri. - CALLES: OSM Mapnik.
 * La caché de teselas de OSMDroid permite ver lo ya visitado sin conexión.
 */
/**
 * Estado aplicado al MapView (sobrevive a recomposiciones; no es estado Compose).
 * Existe para NO tocar fuentes, capas ni cámara salvo que algo haya cambiado:
 * esa era la causa del parpadeo (ver SPRINT-13).
 */
private class EstadoMapaAplicado {
    var ultimoModo: ModoMapa? = null
    var ultimoCentro: LatLon? = null
    var moviendoProgramatico = false
}

/**
 * Mapa híbrido OSMDroid embebido en Compose.
 * - HÍBRIDO: Esri satélite + referencia transparente (límites/topónimos).
 * - SATÉLITE: solo Esri. - CALLES: OSM Mapnik.
 * La caché de teselas de OSMDroid permite ver lo ya visitado sin conexión.
 *
 * Estabilidad: la fuente y las capas solo se aplican al cambiar de [modo]; la
 * cámara solo se mueve por [destino] (un solo uso) o por [seguir] con
 * desplazamiento real; un gesto manual de desplazamiento desactiva el
 * seguimiento ([alGestoManual]).
 */
@Composable
fun MapaCampo(
    modo: ModoMapa,
    alCambiarModo: (ModoMapa) -> Unit,
    modifier: Modifier = Modifier,
    /** Seguimiento de la posición (con umbral; el gesto manual lo desactiva). */
    seguir: Boolean = false,
    /** Posición a seguir (null = no seguir). */
    posicionSeguir: GeoPoint? = null,
    /** Comando de cámara de un solo uso (null = ninguno pendiente). */
    destino: DestinoCamara? = null,
    alDestinoAplicado: () -> Unit = {},
    alGestoManual: () -> Unit = {},
    /** Franja trabajada a colorear (anchura métrica real). */
    tramos: List<TramoFranja> = emptyList()
) {
    val context = LocalContext.current
    val franja = remember { FranjaOverlay(0xFF2E7D32.toInt(), 0xFF1B5E20.toInt()) }
    val aplicado = remember { EstadoMapaAplicado() }
    val gestoActual by rememberUpdatedState(alGestoManual)
    val destinoActual by rememberUpdatedState(alDestinoAplicado)
    val srcEsri = remember { FuentesMapa.esriImagery() }
    val overlayRef = remember {
        val prov = FuentesMapa.referenceOverlayProvider(context.applicationContext)
        TilesOverlay(prov, context)
    }
    val mapView = remember {
        Configuration.getInstance().load(
            context.applicationContext,
            context.applicationContext.getSharedPreferences("osmdroid", Context.MODE_PRIVATE)
        )
        // User-agent obligatorio por la Tile Usage Policy de OSM.
        Configuration.getInstance().userAgentValue = "SurcoGPS/1.0"
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            setBuiltInZoomControls(true)
            controller.setZoom(6.0)
            controller.setCenter(VISTA_DEFECTO)
            // La franja trabajada vive en una única capa (eficiente).
            if (!overlays.contains(franja)) overlays.add(franja)
            addMapListener(object : MapListener {
                override fun onScroll(event: ScrollEvent?): Boolean {
                    // El desplazamiento programático no cuenta como gesto.
                    if (!aplicado.moviendoProgramatico) gestoActual()
                    return false
                }

                override fun onZoom(event: ZoomEvent?): Boolean = false
            })
        }
    }
    DisposableEffect(Unit) {
        mapView.onResume()
        onDispose {
            mapView.onPause()
            mapView.onDetach()
        }
    }

    Column(modifier) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
            FilterChip(
                selected = modo == ModoMapa.HIBRIDO,
                onClick = { alCambiarModo(ModoMapa.HIBRIDO) },
                label = { Text("Híbrido") },
                modifier = Modifier.padding(end = 8.dp)
            )
            FilterChip(
                selected = modo == ModoMapa.SATELITE,
                onClick = { alCambiarModo(ModoMapa.SATELITE) },
                label = { Text("Satélite") },
                modifier = Modifier.padding(end = 8.dp)
            )
            FilterChip(
                selected = modo == ModoMapa.CALLES,
                onClick = { alCambiarModo(ModoMapa.CALLES) },
                label = { Text("Calles") }
            )
        }
        Box(Modifier.fillMaxSize().weight(1f)) {
            AndroidView(
                factory = { mapView },
                modifier = Modifier.fillMaxSize(),
                update = { mv ->
                    // 1. Fuentes y capas SOLO al cambiar de modo (antes: en cada
                    // fix GPS → recarga de teselas y parpadeo).
                    if (modo != aplicado.ultimoModo) {
                        mv.overlays.removeAll { it is TilesOverlay }
                        val cfg = configPara(modo)
                        mv.setTileSource(
                            if (cfg.base == BaseFuente.ESRI) srcEsri
                            else TileSourceFactory.MAPNIK
                        )
                        if (cfg.conReferencia && !mv.overlays.contains(overlayRef)) {
                            mv.overlays.add(overlayRef)
                        }
                        aplicado.ultimoModo = modo
                    }
                    var redibujar = false
                    // 2. Comando de cámara de un solo uso.
                    val d = destino
                    if (d != null) {
                        aplicado.moviendoProgramatico = true
                        mv.controller.setCenter(GeoPoint(d.punto.lat, d.punto.lon))
                        d.zoom?.let {
                            mv.controller.setZoom(
                                it.coerceIn(mv.minZoomLevel.toDouble(), mv.maxZoomLevel.toDouble())
                            )
                        }
                        aplicado.moviendoProgramatico = false
                        aplicado.ultimoCentro = d.punto
                        destinoActual()
                        redibujar = true
                    }
                    // 3. Seguimiento con umbral (antes: setCenter en cada fix).
                    val objetivo = posicionSeguir?.let { LatLon(it.latitude, it.longitude) }
                    if (objetivo != null && PoliticaCamara.debeRecenter(seguir, aplicado.ultimoCentro, objetivo)) {
                        aplicado.moviendoProgramatico = true
                        mv.controller.setCenter(posicionSeguir)
                        aplicado.moviendoProgramatico = false
                        aplicado.ultimoCentro = objetivo
                        redibujar = true
                    }
                    if (!seguir) aplicado.ultimoCentro = null
                    // 4. Franja (capa única; sin tocar fuentes ni teselas).
                    franja.actualizar(tramos)
                    if (redibujar || tramos.isNotEmpty()) mv.invalidate()
                }
            )
            if (!hayRed(context)) {
                Text(
                    "Sin conexión: se muestra la caché de teselas",
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(8.dp)
                )
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(ATRIBUCION, style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f))
            OutlinedButton(onClick = {
                mapView.controller.animateTo(VISTA_DEFECTO)
                mapView.controller.setZoom(6.0)
            }) {
                Text("Mi zona")
            }
        }
    }
}
