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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import es.surcogps.app.cobertura.TramoFranja
import es.surcogps.app.soporte.RegistroDiag
import es.surcogps.app.soporte.SondaAlmacen
import es.surcogps.core.LatLon
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.tileprovider.MapTileProviderArray
import org.osmdroid.tileprovider.MapTileProviderBasic
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.CopyrightOverlay
import org.osmdroid.views.overlay.TilesOverlay
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay

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
class EstadoMapaAplicado {
    var ultimoModo: ModoMapa? = null
    var ultimoCentro: LatLon? = null
    var moviendoProgramatico = false
    var firmaDibujo: Int? = null
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
    retenido: MapaRetenido,
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
    /** Guía de paralelas (null = sin ancla; no dibuja nada). */
    guia: EstadoGuia? = null,
    /** Long-press en el mapa → fijar ancla. */
    alFijarAncla: (LatLon) -> Unit = { _ -> },
    /** Botón «Mi ubicación» (false = deshabilitado, sin posición). */
    puedeIrAUbicacion: Boolean = false,
    alIrAUbicacion: () -> Unit = {},
    /** Publica la cola de teselas (diagnóstico, 1×/s). */
    alCola: (Long) -> Unit = {}
) {
    val context = LocalContext.current
    // Objetos retenidos en el ViewModel: sobreviven a la navegación entre
    // pestañas (antes se destruían y recreaban con hilos y recargas).
    val mapView = retenido.mapView
    val franja = retenido.franja
    val punto = retenido.punto
    val lineas = retenido.lineas
    val overlayRef = retenido.overlayRef
    val srcEsri = retenido.srcEsri
    val aplicado = retenido.aplicado
    retenido.alFijarAncla = alFijarAncla
    retenido.alGesto = alGestoManual
    val gestoActual by rememberUpdatedState(alGestoManual)
    val destinoActual by rememberUpdatedState(alDestinoAplicado)
    // Cola visible en la píldora (1×/s) + publicada al VM (diagnóstico).
    var colaVista by remember { mutableStateOf(0L) }
    // Diagnóstico de red hacia Esri (una comprobación por cambio de modo).
    var estadoRed by remember(modo) {
        mutableStateOf<EstadoRed?>(
            if (configPara(modo).base == BaseFuente.ESRI) EstadoRed.COMPROBANDO else null
        )
    }
    var httpOk by remember(modo) { mutableStateOf<Boolean?>(null) }
    LaunchedEffect(modo) {
        if (configPara(modo).base == BaseFuente.ESRI) {
            val ok = withContext(Dispatchers.IO) {
                DiagnosticoTeselas.comprobar(FuentesMapa.urlMuestraEsri())
            }
            httpOk = ok
            estadoRed = if (ok) EstadoRed.OK else EstadoRed.FALLO
        } else {
            estadoRed = null
        }
    }
    DisposableEffect(Unit) {
        // Solo pausa/reanuda el renderizado: los proveedores e hilos siguen
        // vivos en el retenido (sin reconstruir al volver a Campo).
        mapView.onResume()
        onDispose {
            mapView.onPause()
        }
    }
    // Sondeo de la cola de teselas (1×/s): si drena, el mapa carga; si no,
    // es red/servidor. Sin esto es imposible saberlo en el dispositivo.
    val colaActual by rememberUpdatedState(alCola)
    LaunchedEffect(Unit) {
        while (true) {
            val prov = mapView.tileProvider
            val n = if (prov is MapTileProviderArray) prov.queueSize else -1L
            colaVista = n
            colaActual(n)
            delay(1_000)
        }
    }

    val sinRed = !hayRed(context)
    val aviso = if (sinRed) "Sin conexión: se muestra la caché de teselas"
    else resolverFuente(modo, estadoRed).aviso

    Box(modifier) {
        AndroidView(
            factory = { mapView },
            // clipToBounds: el MapView pinta teselas fuera de su cuadro al
            // desplazar; sin recorte sangra sobre el menú inferior.
            modifier = Modifier.fillMaxSize().clipToBounds(),
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
                    RegistroDiag.registrar(
                        mv.context.applicationContext.filesDir, "mapa", "modo $modo aplicado"
                    )
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
                    // 4. Capas de datos (nunca tocan fuentes ni teselas). La firma
                    // evita redibujar ante re-emisiones idénticas (ticks de slider).
                    val eg = guia?.engine
                    val orientacion = mv.mapOrientation
                    val firma = firmaDibujo(
                        tramos.size,
                        eg?.anchor,
                        eg?.headingDeg ?: 0.0,
                        eg?.implementWidthM ?: 0.0,
                        eg?.lateralShiftM ?: 0.0,
                        guia?.lineaActiva,
                        guia?.puntoFin,
                        ubicacion,
                        orientacion
                    )
                    franja.actualizar(tramos)
                    punto.actualizar(ubicacion, orientacion)
                    lineas.actualizar(guia)
                    if (redibujar || firma != aplicado.firmaDibujo) {
                        aplicado.firmaDibujo = firma
                        mv.invalidate()
                    }
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
            // Contador honesto del proveedor estándar (cola pendiente).
            Surface(
                tonalElevation = 2.dp,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.alpha(0.94f)
            ) {
                Text(
                    textoCola(colaVista, httpOk),
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
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
