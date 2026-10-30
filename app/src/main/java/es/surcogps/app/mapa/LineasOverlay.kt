package es.surcogps.app.mapa

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Point
import es.surcogps.core.GeoMath
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Overlay
import kotlin.math.hypot

/**
 * Plantilla de líneas guía paralelas, acotada para no consumir memoria:
 *
 * - solo las líneas que cortan la pantalla;
 * - solo dentro de [RADIO_MAX_LINEAS_M] alrededor de la posición actual;
 * - como máximo [MAX_LINEAS_DIBUJO] líneas;
 * - si en pantalla quedan a menos de [SEPARACION_MINIMA_PX], se dibuja solo la
 *   línea activa (a ese zoom la familia sería una mancha ilegible).
 *
 * La línea activa (la más cercana a la persona) se resalta en amarillo. El
 * ancla de la familia se marca en rojo.
 */
class LineasOverlay : Overlay() {

    private var guia: EstadoGuia? = null

    private val pLinea = Paint().apply {
        color = 0xFFFFFFFF.toInt()
        style = Paint.Style.STROKE
        strokeWidth = 3f
        alpha = 190
        isAntiAlias = true
    }
    private val pActiva = Paint().apply {
        color = 0xFFFFEB3B.toInt()
        style = Paint.Style.STROKE
        strokeWidth = 7f
        alpha = 235
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

    fun actualizar(g: EstadoGuia?) {
        guia = g
    }

    override fun draw(canvas: Canvas, mapView: MapView, shadow: Boolean) {
        val estado = guia ?: return
        if (shadow) return
        val g = estado.engine
        val bb = mapView.boundingBox
        val bbox = BBox(bb.latNorth, bb.latSouth, bb.lonEast, bb.lonWest)
        val centro = estado.centro ?: g.anchor
        val (a0, a1) = rangoAlongSeguro(g, bbox, centro)
        if (a1 - a0 < 1.0) return

        val proj = mapView.projection
        val p0 = Point()
        val p1 = Point()
        val activa = estado.lineaActiva ?: g.nearestLineIndex(centro)
        // Grosor en dp reales: en pantallas de alta densidad 3 px sería una
        // línea de 1 dp, casi invisible desde la cabina del tractor.
        val d = mapView.resources.displayMetrics.density
        pLinea.strokeWidth = 1.6f * d
        pActiva.strokeWidth = 3.8f * d
        pAnclaBorde.strokeWidth = 1.6f * d
        val pxPorM = escalaPxPorMetro(mapView, centro)
        val familiaLegible = pxPorM > 0.0 &&
            g.implementWidthM * pxPorM >= SEPARACION_MINIMA_PX
        val indices = if (familiaLegible) {
            indicesLineasSeguras(g, bbox, centro)
        } else {
            activa..activa
        }

        for (i in indices) {
            val (a, b) = g.lineSegment(i, a0, a1)
            proj.toPixels(GeoPoint(a.lat, a.lon), p0)
            proj.toPixels(GeoPoint(b.lat, b.lon), p1)
            val paint = if (i == activa) pActiva else pLinea
            canvas.drawLine(
                p0.x.toFloat(), p0.y.toFloat(),
                p1.x.toFloat(), p1.y.toFloat(), paint
            )
        }

        // Marcador del ancla (origen de la familia de paralelas).
        val c = proj.toPixels(GeoPoint(g.anchor.lat, g.anchor.lon), Point())
        canvas.drawCircle(c.x.toFloat(), c.y.toFloat(), 7f * d, pAnclaBorde)
        canvas.drawCircle(c.x.toFloat(), c.y.toFloat(), 5f * d, pAncla)
    }

    /** Píxeles por metro a la latitud del centro (10 m hacia el este). */
    private fun escalaPxPorMetro(mapView: MapView, centro: es.surcogps.core.LatLon): Double {
        val proj = mapView.projection
        val a = proj.toPixels(GeoPoint(centro.lat, centro.lon), Point())
        val hasta = GeoMath.destination(centro, 90.0, 10.0)
        val b = proj.toPixels(GeoPoint(hasta.lat, hasta.lon), Point())
        return hypot((b.x - a.x).toDouble(), (b.y - a.y).toDouble()) / 10.0
    }
}
