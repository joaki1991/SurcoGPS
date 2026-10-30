package es.surcogps.app.mapa

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import android.graphics.Point
import es.surcogps.app.cobertura.TramoFranja
import es.surcogps.core.GeoMath
import es.surcogps.core.LatLon
import org.osmdroid.util.GeoPoint
import kotlin.math.hypot

/**
 * Mapa a pantalla completa con las tres capas de datos (franja, líneas y punto).
 *
 * El `MapView` vive en [MapaRetenido] (ViewModel): girar el móvil no lo recrea.
 * Aquí solo se aplican los cambios de estado; las fuentes de teselas se tocan
 * únicamente al cambiar de modo.
 */
@Composable
fun MapaSimple(
    retenido: MapaRetenido,
    modo: ModoMapa,
    seguir: Boolean,
    posicion: LatLon?,
    destino: DestinoCamara?,
    alDestinoAplicado: () -> Unit,
    alGestoManual: () -> Unit,
    tramos: List<TramoFranja>,
    ubicacion: EstadoUbicacion?,
    guia: EstadoGuia?,
    /** Desplazamiento del centro del mapa en pantalla (-1..1 de ancho/alto). */
    offsetX: Float = 0f,
    offsetY: Float = 0f,
    modifier: Modifier = Modifier
) {
    val mapView = retenido.mapView
    val gestoActual by rememberUpdatedState(alGestoManual)
    val destinoActual by rememberUpdatedState(alDestinoAplicado)
    retenido.alGesto = { gestoActual() }

    var avisoZoom by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        mapView.onResume()
        onDispose { mapView.onPause() }
    }

    Box(modifier) {
        AndroidView(
            factory = { mapView },
            modifier = Modifier.fillMaxSize().clipToBounds(),
            update = { mv ->
                retenido.garantizarCapas()
                retenido.aplicarModo(modo)

                // El panel de control tapa parte del mapa: se desplaza el
                // centro geográfico para que el punto azul quede a la vista.
                if (mv.width > 0 && mv.height > 0) {
                    val ox = (mv.width * offsetX).toInt()
                    val oy = (mv.height * offsetY).toInt()
                    if (mv.mapCenterOffsetX != ox || mv.mapCenterOffsetY != oy) {
                        mv.setMapCenterOffset(ox, oy)
                    }
                }

                val d = destino
                if (d != null) {
                    retenido.centrar(d.punto, d.zoom)
                    destinoActual()
                }

                if (seguir && posicion != null &&
                    (
                        // Si la cámara se ha desplazado (p. ej. al girar la
                        // pantalla) hay que volver a la posición aunque el fix
                        // no se haya movido.
                        GeoMath.haversine(
                            LatLon(mv.mapCenter.latitude, mv.mapCenter.longitude),
                            posicion
                        ) > PoliticaCamara.UMBRAL_SEGUIR_M ||
                            PoliticaCamara.debeRecenter(
                                true, retenido.ultimoCentro, posicion
                            )
                        )
                ) {
                    retenido.centrar(posicion)
                }
                if (!seguir) retenido.olvidarCentro()

                retenido.franja.actualizar(tramos)
                retenido.punto.actualizar(ubicacion, mv.mapOrientation)
                retenido.lineas.actualizar(guia)
                // El aviso solo aparece si de verdad no se pueden distinguir
                // las líneas a este zoom y con este ancho.
                val g = guia
                val centro = g?.centro ?: g?.engine?.anchor
                avisoZoom = if (g != null && centro != null) {
                    val a = mv.projection.toPixels(GeoPoint(centro.lat, centro.lon), Point())
                    val h = GeoMath.destination(centro, 90.0, 10.0)
                    val b = mv.projection.toPixels(GeoPoint(h.lat, h.lon), Point())
                    val pxPorM = hypot((b.x - a.x).toDouble(), (b.y - a.y).toDouble()) / 10.0
                    pxPorM <= 0.0 || g.engine.implementWidthM * pxPorM < SEPARACION_MINIMA_PX
                } else {
                    false
                }
                mv.invalidate()
            }
        )

        if (avisoZoom) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 72.dp),
                tonalElevation = 3.dp,
                shape = MaterialTheme.shapes.small
            ) {
                Text(
                    "Acerca el mapa para ver las líneas",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }
        }
    }
}
