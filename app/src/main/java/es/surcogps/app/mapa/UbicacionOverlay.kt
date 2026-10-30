package es.surcogps.app.mapa

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Point
import es.surcogps.core.GeoMath
import es.surcogps.core.LatLon
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Overlay
import kotlin.math.hypot

/** Estado del indicador de posición (null = sin posición que mostrar). */
data class EstadoUbicacion(
    val lat: Double,
    val lon: Double,
    val precisionM: Float,
    /** Rumbo de marcha suavizado, o null si parado/sin dato. */
    val rumboDeg: Double?
)

/**
 * Ángulo de la flecha en PANTALLA: rumbo de marcha menos orientación del mapa.
 * Con el mapa girado, el norte ya no está arriba y hay que compensarlo.
 */
fun anguloFlecha(rumboMarchaDeg: Double, orientacionMapaDeg: Double): Float =
    (((rumboMarchaDeg - orientacionMapaDeg) % 360 + 360) % 360).toFloat()

/**
 * Indicador de ubicación estilo navegación (capa única, creada una vez):
 * cono de precisión + punto azul con halo + flecha de rumbo de marcha.
 * Supone mapa con el norte arriba (no se rota el mapa en la app).
 * Sin fix no dibuja nada (nunca se duplica: una sola instancia).
 */
class UbicacionOverlay : Overlay() {

    private var estado: EstadoUbicacion? = null
    private var orientacionMapa = 0f

    private val pPrecisionRelleno = Paint().apply {
        color = 0xFF2196F3.toInt()
        style = Paint.Style.FILL
        alpha = 30
        isAntiAlias = true
    }
    private val pPrecisionBorde = Paint().apply {
        color = 0xFF2196F3.toInt()
        style = Paint.Style.STROKE
        strokeWidth = 2f
        alpha = 120
        isAntiAlias = true
    }
    private val pHalo = Paint().apply {
        color = 0xFFFFFFFF.toInt()
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    private val pPunto = Paint().apply {
        color = 0xFF1A73E8.toInt()
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    private val pFlecha = Paint().apply {
        color = 0xFF1A73E8.toInt()
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    private val pFlechaBorde = Paint().apply {
        color = 0xFFFFFFFF.toInt()
        style = Paint.Style.STROKE
        strokeWidth = 3f
        isAntiAlias = true
    }

    fun actualizar(e: EstadoUbicacion?, orientacionMapaDeg: Float = 0f) {
        estado = e
        orientacionMapa = orientacionMapaDeg
    }

    override fun draw(canvas: Canvas, mapView: MapView, shadow: Boolean) {
        val e = estado ?: return
        if (shadow) return
        val proj = mapView.projection
        val centro = proj.toPixels(GeoPoint(e.lat, e.lon), Point())
        // Escala px/m con un segmento de 10 m hacia el este.
        val hasta = GeoMath.destination(LatLon(e.lat, e.lon), 90.0, 10.0)
        val ref = proj.toPixels(GeoPoint(hasta.lat, hasta.lon), Point())
        val pxPorM = hypot((ref.x - centro.x).toDouble(), (ref.y - centro.y).toDouble()) / 10.0
        if (!pxPorM.isFinite() || pxPorM <= 0) return

        // Tamaños en dp reales: en pantallas de alta densidad el punto y la
        // flecha deben verse desde la cabina del tractor.
        val d = mapView.resources.displayMetrics.density
        val cx = centro.x.toFloat()
        val cy = centro.y.toFloat()

        // Cono de precisión (radio = incertidumbre estimada).
        val radioPx = (e.precisionM * pxPorM).toFloat().coerceAtLeast(4f * d)
        canvas.drawCircle(cx, cy, radioPx, pPrecisionRelleno)
        canvas.drawCircle(cx, cy, radioPx, pPrecisionBorde)

        // Flecha de orientación (brújula o rumbo de marcha), compensada por el
        // giro del mapa (la app mantiene el norte arriba).
        e.rumboDeg?.let { rumbo ->
            canvas.save()
            canvas.rotate(
                anguloFlecha(rumbo, orientacionMapa.toDouble()), cx, cy
            )
            val path = Path()
            val h = 42f * d
            val w = 32f * d
            path.moveTo(cx, cy - h * 0.78f)
            path.lineTo(cx + w / 2, cy + h * 0.28f)
            path.lineTo(cx, cy + h * 0.08f)
            path.lineTo(cx - w / 2, cy + h * 0.28f)
            path.close()
            canvas.drawPath(path, pFlecha)
            canvas.drawPath(path, pFlechaBorde)
            canvas.restore()
        }
        // Punto azul con halo (siempre encima).
        canvas.drawCircle(cx, cy, 13f * d, pHalo)
        canvas.drawCircle(cx, cy, 9f * d, pPunto)
    }
}
