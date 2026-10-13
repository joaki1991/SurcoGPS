package es.surcogps.app.mapa

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Point
import es.surcogps.app.cobertura.TramoFranja
import es.surcogps.core.GeoMath
import es.surcogps.core.LatLon
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Overlay
import kotlin.math.hypot

/**
 * Capa única que dibuja la franja trabajada con anchura MÉTRICA real
 * (cada tramo = rectángulo perpendicular al avance, escala px/m calculada por
 * tramo con haversine → correcta a cualquier zoom y latitud).
 */
class FranjaOverlay(
    colorRelleno: Int,
    colorBorde: Int
) : Overlay() {

    private var tramos: List<TramoFranja> = emptyList()
    private val paintRelleno = Paint().apply {
        color = colorRelleno
        style = Paint.Style.FILL
        alpha = 110
        isAntiAlias = true
    }
    private val paintBorde = Paint().apply {
        color = colorBorde
        style = Paint.Style.STROKE
        strokeWidth = 2f
        alpha = 160
        isAntiAlias = true
    }

    fun actualizar(nuevos: List<TramoFranja>) {
        tramos = nuevos
    }

    override fun draw(canvas: Canvas, mapView: MapView, shadow: Boolean) {
        if (shadow || tramos.isEmpty()) return
        val proj = mapView.projection
        val p0 = Point()
        val p1 = Point()
        val path = Path()
        for (t in tramos) {
            proj.toPixels(toGeo(t.desde), p0)
            proj.toPixels(toGeo(t.hasta), p1)
            val dx = (p1.x - p0.x).toDouble()
            val dy = (p1.y - p0.y).toDouble()
            val lenPx = hypot(dx, dy)
            val lenM = GeoMath.haversine(t.desde, t.hasta)
            if (lenPx < 1.0 || lenM < 0.2) continue
            val mitadPx = (t.anchoM / 2.0 * (lenPx / lenM)).toFloat()
            val nx = (-dy / lenPx * mitadPx).toFloat()
            val ny = (dx / lenPx * mitadPx).toFloat()
            path.reset()
            path.moveTo(p0.x - nx, p0.y - ny)
            path.lineTo(p1.x - nx, p1.y - ny)
            path.lineTo(p1.x + nx, p1.y + ny)
            path.lineTo(p0.x + nx, p0.y + ny)
            path.close()
            canvas.drawPath(path, paintRelleno)
            canvas.drawPath(path, paintBorde)
        }
    }

    private fun toGeo(p: LatLon): GeoPoint = GeoPoint(p.lat, p.lon)
}
