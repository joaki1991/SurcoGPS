package es.surcogps.app.mapa

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Point
import android.view.MotionEvent
import android.widget.Toast
import es.surcogps.core.GuidanceEngine
import es.surcogps.core.LatLon
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Overlay

/**
 * Capa única de líneas guía: familia de paralelas del ancho del apero sobre la
 * vista visible, línea activa resaltada y marcador del ancla. Además, una
 * pulsación LARGA fija el ancla en el punto tocado (con aviso).
 */
class LineasOverlay(
    private val alFijarAncla: (LatLon) -> Unit
) : Overlay() {

    private var guia: GuidanceEngine? = null
    private var lineaActiva: Int? = null

    private val pLinea = Paint().apply {
        color = 0xFFFFFFFF.toInt()
        style = Paint.Style.STROKE
        strokeWidth = 4f
        alpha = 200
        isAntiAlias = true
    }
    private val pActiva = Paint().apply {
        color = 0xFFFFEB3B.toInt()
        style = Paint.Style.STROKE
        strokeWidth = 8f
        alpha = 230
        isAntiAlias = true
    }
    private val pAncla = Paint().apply {
        color = 0xFFE53935.toInt()
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    private val pAnclaBorde = Paint().apply {
        color = 0xFFFFFFFF.toInt()
        style = Paint.Style.STROKE
        strokeWidth = 3f
        isAntiAlias = true
    }

    fun actualizar(g: GuidanceEngine?, activa: Int?) {
        guia = g
        lineaActiva = activa
    }

    override fun draw(canvas: Canvas, mapView: MapView, shadow: Boolean) {
        val g = guia ?: return
        if (shadow) return
        val bb = mapView.boundingBox
        val bbox = BBox(bb.latNorth, bb.latSouth, bb.lonEast, bb.lonWest)
        val (a0, a1) = rangoAlong(g, bbox)
        val proj = mapView.projection
        val p0 = Point()
        val p1 = Point()
        for (i in indicesLineasVisibles(g, bbox)) {
            val (a, b) = g.lineSegment(i, a0, a1)
            proj.toPixels(GeoPoint(a.lat, a.lon), p0)
            proj.toPixels(GeoPoint(b.lat, b.lon), p1)
            val paint = if (i == lineaActiva) pActiva else pLinea
            canvas.drawLine(p0.x.toFloat(), p0.y.toFloat(), p1.x.toFloat(), p1.y.toFloat(), paint)
        }
        // Marcador del ancla (origen de la familia de paralelas).
        val c = proj.toPixels(GeoPoint(g.anchor.lat, g.anchor.lon), Point())
        canvas.drawCircle(c.x.toFloat(), c.y.toFloat(), 14f, pAnclaBorde)
        canvas.drawCircle(c.x.toFloat(), c.y.toFloat(), 10f, pAncla)
    }

    override fun onLongPress(e: MotionEvent, mapView: MapView): Boolean {
        val gp = mapView.projection.fromPixels(e.x.toInt(), e.y.toInt()) as GeoPoint
        alFijarAncla(LatLon(gp.latitude, gp.longitude))
        Toast.makeText(mapView.context, "Ancla fijada aquí", Toast.LENGTH_SHORT).show()
        return true
    }
}
