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
 * Indicador de ubicación estilo navegación (capa única, creada una vez):
 * cono de precisión + punto azul con halo + flecha de rumbo de marcha.
 * Supone mapa con el norte arriba (no se rota el mapa en la app).
 * Sin fix no dibuja nada (nunca se duplica: una sola instancia).
 */
class UbicacionOverlay : Overlay() {

    private var estado: EstadoUbicacion? = null

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

    fun actualizar(e: EstadoUbicacion?) {
        estado = e
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
        // Cono de precisión (radio = incertidumbre estimada).
        val radioPx = (e.precisionM * pxPorM).toFloat().coerceAtLeast(4f)
        canvas.drawCircle(centro.x.toFloat(), centro.y.toFloat(), radioPx, pPrecisionRelleno)
        canvas.drawCircle(centro.x.toFloat(), centro.y.toFloat(), radioPx, pPrecisionBorde)
        // Flecha de rumbo (solo con marcha fiable).
        e.rumboDeg?.let { rumbo ->
            canvas.save()
            canvas.rotate(rumbo.toFloat(), centro.x.toFloat(), centro.y.toFloat())
            val path = Path()
            val h = 34f
            val w = 26f
            path.moveTo(centro.x.toFloat(), centro.y.toFloat() - h / 2)
            path.lineTo(centro.x.toFloat() + w / 2, centro.y.toFloat() + h / 2)
            path.lineTo(centro.x.toFloat() - w / 2, centro.y.toFloat() + h / 2)
            path.close()
            canvas.drawPath(path, pFlecha)
            canvas.drawPath(path, pFlechaBorde)
            canvas.restore()
        }
        // Punto azul con halo (siempre encima).
        canvas.drawCircle(centro.x.toFloat(), centro.y.toFloat(), 13f, pHalo)
        canvas.drawCircle(centro.x.toFloat(), centro.y.toFloat(), 9f, pPunto)
    }
}
