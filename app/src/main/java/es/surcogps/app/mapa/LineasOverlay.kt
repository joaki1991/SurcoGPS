package es.surcogps.app.mapa

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Point
import android.view.MotionEvent
import es.surcogps.core.GuidanceEngine
import es.surcogps.core.LatLon
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Overlay

/**
 * Capa única de líneas guía: familia de paralelas del ancho del apero sobre la
 * vista visible, línea activa resaltada, marcador del ancla (rojo) y del punto
 * final B (azul). La pulsación LARGA la gestiona el llamador ([alFijarAncla]).
 */
class LineasOverlay(
    private val alFijarAncla: (LatLon) -> Unit
) : Overlay() {

    private var guia: EstadoGuia? = null

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

    private val pFin = Paint().apply {
        color = 0xFF1A73E8.toInt()
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    fun actualizar(g: EstadoGuia?) {
        guia = g
    }

    override fun draw(canvas: Canvas, mapView: MapView, shadow: Boolean) {
        val estado = guia ?: return
        if (shadow) return
        val g = estado.engine
        val lineaActiva = estado.lineaActiva
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
        // Marcador del punto final B (define el rumbo).
        estado.puntoFin?.let { fin ->
            val f = proj.toPixels(GeoPoint(fin.lat, fin.lon), Point())
            canvas.drawCircle(f.x.toFloat(), f.y.toFloat(), 13f, pAnclaBorde)
            canvas.drawCircle(f.x.toFloat(), f.y.toFloat(), 9f, pFin)
        }
    }

    override fun onLongPress(e: MotionEvent, mapView: MapView): Boolean {
        val gp = mapView.projection.fromPixels(e.x.toInt(), e.y.toInt()) as GeoPoint
        alFijarAncla(LatLon(gp.latitude, gp.longitude))
        return true
    }
}
