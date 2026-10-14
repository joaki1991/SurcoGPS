package es.surcogps.app.mapa

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import es.surcogps.app.cobertura.TramoFranja
import es.surcogps.core.LatLon
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.CopyrightOverlay
import org.osmdroid.views.overlay.TilesOverlay

/** Modos de mapa disponibles. */
enum class ModoMapa { HIBRIDO, SATELITE, CALLES }

/** Atribución obligatoria de las fuentes (también en el CopyrightOverlay). */
const val ATRIBUCION = "© OpenStreetMap contributors · Imagery © Esri World Imagery"

/** Vista por defecto: península ibérica (hasta el primer fix válido). */
val VISTA_DEFECTO = GeoPoint(40.5, -3.8)

/** Hay red disponible (para el aviso de modo sin conexión). */
fun hayRed(context: Context): Boolean {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val net = cm.activeNetwork ?: return false
    val cap = cm.getNetworkCapabilities(net) ?: return false
    return cap.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
}

/**
 * Estado aplicado al MapView (sobrevive a recomposiciones; no es estado Compose).
 * El mapa se configura UNA vez (estilo Traccar); los updates solo tocan lo que cambia.
 */
private class EstadoMapaAplicado {
    var ultimoModo: ModoMapa? = null
    var ultimoCentro: LatLon? = null
    var moviendoProgramatico = false
}

/**
 * Cuadro de mapa determinista: el MapView ocupa todo el cuadro (`fillMaxSize`
 * dentro de un `Box` con altura por peso, sin pesos anidados) y los controles
 * (modos, estado, zoom, ubicación) flotan ENCIMA del mapa, siempre visibles.
 *
 * - HÍBRIDO: Esri satélite + referencia transparente. SATÉLITE: solo Esri.
 *   CALLES: OSM Mapnik. Fuentes y capas creadas una vez; se aplican al cambiar
 *   de modo, nunca por fix GPS.
 * - Cámara solo por comando ([destino], un uso) o [seguir] con umbral; el gesto
 *   manual de desplazamiento desactiva el seguimiento ([alGestoManual]).
 * - Atribución: `CopyrightOverlay` dentro del mapa.
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
    tramos: List<TramoFranja> = emptyList(),
    /** Indicador de posición (null = sin fix; no dibuja nada). */
    ubicacion: EstadoUbicacion? = null,
    /** Botón «Mi ubicación» (false = deshabilitado, sin posición). */
    puedeIrAUbicacion: Boolean = false,
    alIrAUbicacion: () -> Unit = {}
) {
    val context = LocalContext.current
    val franja = remember { FranjaOverlay(0xFF2E7D32.toInt(), 0xFF1B5E20.toInt()) }
    val punto = remember { UbicacionOverlay() }
    val aplicado = remember { EstadoMapaAplicado() }
    val gestoActual by rememberUpdatedState(alGestoManual)
    val destinoActual by rememberUpdatedState(alDestinoAplicado)
    val srcEsri = remember { FuentesMapa.esriImagery() }
    val overlayRef = remember {
        TilesOverlay(
            FuentesMapa.referenceOverlayProvider(context.applicationContext),
            context
        )
    }
    // Diagnóstico de red hacia Esri (una comprobación por cambio de modo).
    var estadoRed by remember(modo) {
        mutableStateOf<EstadoRed?>(
            if (configPara(modo).base == BaseFuente.ESRI) EstadoRed.COMPROBANDO else null
        )
    }
    LaunchedEffect(modo) {
        if (configPara(modo).base == BaseFuente.ESRI) {
            val ok = withContext(Dispatchers.IO) {
                DiagnosticoTeselas.comprobar(FuentesMapa.urlMuestraEsri())
            }
            estadoRed = if (ok) EstadoRed.OK else EstadoRed.FALLO
        } else {
            estadoRed = null
        }
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
            setBuiltInZoomControls(false)
            controller.setZoom(6.0)
            controller.setCenter(VISTA_DEFECTO)
            // Capas persistentes (orden: franja, punto, atribución encima).
            overlays.add(franja)
            overlays.add(punto)
            overlays.add(CopyrightOverlay(context))
            addMapListener(object : MapListener {
                override fun onScroll(event: ScrollEvent?): Boolean {
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

    val sinRed = !hayRed(context)
    val aviso = if (sinRed) "Sin conexión: se muestra la caché de teselas"
    else resolverFuente(modo, estadoRed).aviso

    Box(modifier) {
        AndroidView(
            factory = { mapView },
            modifier = Modifier.fillMaxSize(),
            update = { mv ->
                // 1. Fuentes y capas SOLO al cambiar de modo.
                if (modo != aplicado.ultimoModo) {
                    mv.overlays.removeAll { it is TilesOverlay }
                    val cfg = configPara(modo)
                    mv.setTileSource(
                        if (cfg.base == BaseFuente.ESRI) srcEsri
                        else TileSourceFactory.MAPNIK
                    )
                    if (cfg.conReferencia && !mv.overlays.contains(overlayRef)) {
                        mv.overlays.add(0, overlayRef)
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
                // 3. Seguimiento con umbral.
                val objetivo = posicionSeguir?.let { LatLon(it.latitude, it.longitude) }
                if (objetivo != null && PoliticaCamara.debeRecenter(seguir, aplicado.ultimoCentro, objetivo)) {
                    aplicado.moviendoProgramatico = true
                    mv.controller.setCenter(posicionSeguir)
                    aplicado.moviendoProgramatico = false
                    aplicado.ultimoCentro = objetivo
                    redibujar = true
                }
                if (!seguir) aplicado.ultimoCentro = null
                // 4. Capas de datos (nunca tocan fuentes ni teselas).
                franja.actualizar(tramos)
                punto.actualizar(ubicacion)
                if (redibujar || tramos.isNotEmpty() || ubicacion != null) mv.invalidate()
            }
        )
        // Controles SIEMPRE visibles, flotando sobre el mapa.
        Column(
            Modifier.align(Alignment.TopStart).padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Surface(
                tonalElevation = 2.dp,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.alpha(0.94f)
            ) {
                Row(Modifier.padding(horizontal = 4.dp)) {
                    FilterChip(
                        selected = modo == ModoMapa.HIBRIDO,
                        onClick = { alCambiarModo(ModoMapa.HIBRIDO) },
                        label = { Text("Híbrido") }
                    )
                    FilterChip(
                        selected = modo == ModoMapa.SATELITE,
                        onClick = { alCambiarModo(ModoMapa.SATELITE) },
                        label = { Text("Satélite") }
                    )
                    FilterChip(
                        selected = modo == ModoMapa.CALLES,
                        onClick = { alCambiarModo(ModoMapa.CALLES) },
                        label = { Text("Calles") }
                    )
                }
            }
            aviso?.let {
                Surface(
                    tonalElevation = 2.dp,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.alpha(0.94f)
                ) {
                    Text(
                        it,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
        Column(
            Modifier.align(Alignment.BottomEnd).padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalAlignment = Alignment.End
        ) {
            Surface(
                tonalElevation = 2.dp,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.alpha(0.94f)
            ) {
                Row {
                    IconButton(onClick = { mapView.controller.zoomIn() }) {
                        Icon(Icons.Filled.Add, contentDescription = "Acercar")
                    }
                    IconButton(onClick = { mapView.controller.zoomOut() }) {
                        Text("−", style = MaterialTheme.typography.titleLarge)
                    }
                }
            }
            Surface(
                tonalElevation = 2.dp,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.alpha(0.94f)
            ) {
                IconButton(onClick = alIrAUbicacion, enabled = puedeIrAUbicacion) {
                    Text("◎", style = MaterialTheme.typography.titleLarge)
                }
            }
        }
    }
}
