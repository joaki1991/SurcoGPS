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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import org.osmdroid.config.Configuration
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
@Composable
fun MapaCampo(
    modo: ModoMapa,
    alCambiarModo: (ModoMapa) -> Unit,
    modifier: Modifier = Modifier,
    /** Posición a seguir (null = no seguir). */
    posicionSeguir: GeoPoint? = null
) {
    val context = LocalContext.current
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
                    // Limpia la capa de referencia si existía.
                    mv.overlays.removeAll { it is TilesOverlay }
                    val cfg = configPara(modo)
                    mv.setTileSource(
                        if (cfg.base == BaseFuente.ESRI) FuentesMapa.esriImagery()
                        else TileSourceFactory.MAPNIK
                    )
                    if (cfg.conReferencia) {
                        val prov = FuentesMapa.referenceOverlayProvider(mv.context)
                        mv.overlays.add(TilesOverlay(prov, mv.context))
                    }
                    if (posicionSeguir != null) {
                        mv.controller.setCenter(posicionSeguir)
                    }
                    mv.invalidate()
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
