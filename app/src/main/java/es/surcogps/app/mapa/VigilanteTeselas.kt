package es.surcogps.app.mapa

import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Looper
import org.osmdroid.tileprovider.MapTileProviderBasic
import org.osmdroid.tileprovider.MapTileRequestState
import java.util.concurrent.atomic.AtomicInteger

/** Contadores de teselas (diagnóstico visible en la píldora del mapa). */
data class StatsTeselas(val ok: Int = 0, val fallos: Int = 0, val caducadas: Int = 0)

/** Texto compacto de la píldora («▦ 24 ok · 0 fallo»). Lógica pura, testeable. */
fun textoTeselas(s: StatsTeselas): String =
    "▦ ${s.ok} ok · ${s.fallos} fallo"

/**
 * Proveedor de teselas que CUENTA resultados (ok/fallo/caducada) y avisa en el
 * hilo principal. Hereda todo el comportamiento de `MapTileProviderBasic` sin
 * alterarlo: solo observa. Sin esto es imposible distinguir «sin red» de
 * «fallo de render» en el dispositivo.
 */
class VigilanteTeselas(
    context: Context,
    private val alCambiar: (StatsTeselas) -> Unit
) : MapTileProviderBasic(context.applicationContext) {

    private val ok = AtomicInteger(0)
    private val fallos = AtomicInteger(0)
    private val caducadas = AtomicInteger(0)
    private val principal = Handler(Looper.getMainLooper())

    val stats: StatsTeselas
        get() = StatsTeselas(ok.get(), fallos.get(), caducadas.get())

    fun reiniciar() {
        ok.set(0)
        fallos.set(0)
        caducadas.set(0)
        avisar()
    }

    private fun avisar() {
        val s = stats
        if (Looper.myLooper() == Looper.getMainLooper()) alCambiar(s)
        else principal.post { alCambiar(s) }
    }

    override fun mapTileRequestCompleted(aState: MapTileRequestState, aDrawable: Drawable) {
        super.mapTileRequestCompleted(aState, aDrawable)
        ok.incrementAndGet()
        avisar()
    }

    override fun mapTileRequestFailed(aState: MapTileRequestState) {
        super.mapTileRequestFailed(aState)
        fallos.incrementAndGet()
        avisar()
    }

    override fun mapTileRequestFailedExceedsMaxQueueSize(aState: MapTileRequestState) {
        super.mapTileRequestFailedExceedsMaxQueueSize(aState)
        fallos.incrementAndGet()
        avisar()
    }

    override fun mapTileRequestExpiredTile(aState: MapTileRequestState, aDrawable: Drawable) {
        super.mapTileRequestExpiredTile(aState, aDrawable)
        caducadas.incrementAndGet()
        avisar()
    }
}
